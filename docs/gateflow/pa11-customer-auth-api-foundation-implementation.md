# PA11 S1 implementation entry — blocked contract

## Identity and disposition

- Task: `pa11-s1-implementation-gpt-20261101-01`.
- Work unit/slice: `PA11-customer-auth-api-foundation`, implementation S1 only.
- Branch: `codex/pa11-customer-auth-api-foundation`.
- Accepted-plan checkpoint: `c6f1aee`; start HEAD: `42863d1d1a9d2f25cec892e9b2cf661a8fc964fb`.
- No product implementation was started. Stop follows the assignment's explicit instruction to report an internally impossible accepted contract rather than invent behavior. This is not implementation completion, review acceptance, or a controller-state change.

## Exact contradiction

Accepted plan §8, “Executable positive-first firewall,” requires: “First associate every resolved artifact with its BOOT-INF/lib entry and require one-to-one presence, versions and no duplicates/collisions.” The same section and the assignment freeze **55 resolved runtime coordinates and 48 packaged JARs**. The actual controller entry artifacts reproduce those counts and the exact recorded web JAR hash. A one-to-one mapping of those sets is impossible.

The difference is concrete, not a dependency-resolution failure. These eight resolved coordinates have no corresponding packaged JAR (type `jar`, no classifier, scope `compile` for all):

| Group | Artifact | Version |
| --- | --- | --- |
| org.springframework.boot | spring-boot-starter | 4.1.2 |
| org.springframework.boot | spring-boot-starter-logging | 4.1.2 |
| org.springframework.boot | spring-boot-starter-jackson | 4.1.2 |
| org.springframework.boot | spring-boot-starter-tomcat | 4.1.2 |
| org.springframework.boot | spring-boot-starter-tomcat-runtime | 4.1.2 |
| org.springframework.boot | spring-boot-starter-webmvc | 4.1.2 |
| org.springframework.modulith | spring-modulith-starter-core | 2.1.2 |
| org.springframework.modulith | spring-modulith-apt | 2.1.2 |

Conversely, `BOOT-INF/lib/spring-boot-jarmode-tools-4.1.2.jar` is present without a project runtime coordinate in the dependency-list evidence. Thus **55 − 8 + 1 = 48**. This also intersects §8's prohibition on freezing “test/tools” artifacts; no exception for this existing packager-added artifact is explicit. The cardinality contradiction alone is sufficient to stop; no interpretation of that second wording is needed for the blocker.

Required controller/Backend-CI destination: authorize an exact reconciliation rule separating the complete resolved-coordinate allowlist from the exact packaged-library inventory, explicitly accounting for these eight omissions and the packager-added artifact. Preserve positive-first full-coordinate-plus-filename checks, group-aware Jackson rejection, all adversarial probes, exact counts, fixed versions, and the 30-minute ceiling. This report proposes no plan edit and implements no exception.

## Entry artifact evidence

Read-only entry source directory:

`/mnt/c/Users/Administrator/AppData/Local/Temp/pa11-web-probe-20261101-233703`

| Evidence | Observed result |
| --- | --- |
| `baseline-runtime-dependencies.txt` | 37 unique runtime coordinates |
| `runtime-dependencies.txt` | 55 unique runtime coordinates; exactly 18 additions, no removals |
| Baseline JAR `baseline-repo/backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar` | 34 unique packaged JARs |
| Web JAR `repo/backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar` | 48 unique packaged JARs; 14 additions, no removals |
| Baseline JAR SHA256 | `392bd78f71fc13f2889166f9b06b8ead4ce1c9278db9ba726c310444fe8e6ef3` |
| Web JAR SHA256 | `873230fd0b5253b106769872c729c686b141bba1331b2fc28dc786b4ddf5b1f8` |
| Diagnostic output/full coordinate-and-filename listing | `backend/target/pa11-entry-manifest-check.log` |

The final diagnostic parses group/artifact/version/scope, assigns type `jar` and absent classifier from the source format, reads both JAR ZIP central directories, checks uniqueness/counts/additions/removals, verifies the web hash against the controller, and computes both set differences. It does not regenerate or install a candidate allowlist. No new product JAR, source inventory, XML report, or passing production firewall is claimed.

## Commands and outcomes

Commands ran from the supplied workspace unless otherwise stated. Git requires these process-local overrides because the existing `.git` file contains the Windows path `D:/codex-call/.git/worktrees/codex-call9`:

```bash
export GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call9
export GIT_WORK_TREE="$PWD"
```

No `.git` file or repository configuration was changed.

| Command/check | Exit/result and retained output |
| --- | --- |
| Initial plain `git status --short`, `git branch --show-current`, `git rev-parse HEAD` | Each failed 128 on Windows gitdir translation; subsequent explicit-path commands succeeded. |
| Explicit-path `git status --short` | 0, empty clean baseline. |
| Explicit-path `git branch --show-current`; `git rev-parse HEAD` | 0 each; branch/HEAD above. |
| `JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -version` | 0; Maven 3.9.9, Java 21.0.13. |
| `/opt/jdk21/bin/java -version` | 0; Temurin 21.0.13+9 LTS. |
| `docker version --format '{{.Server.Version}}'` | 0; server 29.9.2; no container started. |
| `JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH /opt/maven-3.9.9/bin/mvn -B -ntp -f backend/pom.xml validate help:effective-pom -Doutput=target/pa11-entry-effective-pom.xml` | 0; baseline validation/effective POM only; console redirected to `/dev/null`; retained `backend/target/pa11-entry-effective-pom.xml`. Not an MVC candidate build. |
| Inline Python manifest diagnostics over entry files, emitted with `tee backend/target/pa11-entry-manifest-check.log` | Two preliminary diagnostics failed assertions (Python exit 1); corrected group regex to include hyphens, including `commons-logging`. Final diagnostic exit 0 with all evidence assertions passing and explicit strict-bijection FAIL. Pipeline without pipefail in the second preliminary invocation returned 0 despite the printed Python assertion; never treated as successful evidence. |
| `/opt/jdk21/bin/jar tf /root/.m2/repository/org/springframework/boot/spring-boot-loader-tools/4.1.2/spring-boot-loader-tools-4.1.2.jar` and `javap -c -p ... org.springframework.boot.loader.tools.Packager` | 0; read-only local packaging-class inspection; no build/version changes. |
| `git diff --check` | 0; existing autocrlf warnings, no whitespace errors. |
| `git diff --exit-code HEAD -- .` before report | 0; every tracked file unchanged. |
| `git check-ignore backend/target/pa11-entry-manifest-check.log backend/target/pa11-entry-effective-pom.xml` | 0; both generated evidence files ignored. |

Other commands were read-only `pwd`, `cat`, `sed`, `rg`, `find`, `ls`, `wc`, and SHA256 inspection of binding documents, local entry artifacts, existing POM/tests/CI and packaging classes. Expected absent-path probes (`/tmp/pa11-web-probe`, `/mnt/c/codex-call/.git`, and an absent Windows Maven cache path) failed; the actual controller-recorded entry directory and `/root/.m2` artifacts were then found without modifying them. No shell environment, DB secret or bearer material was dumped.

Minimal independent reproduction of the decisive mismatch (read-only; use the entry directory above):

```python
from pathlib import Path
import re
import zipfile

probe = Path('/mnt/c/Users/Administrator/AppData/Local/Temp/pa11-web-probe-20261101-233703')
runtime = re.findall(
    r'^\s*([\w.-]+):([\w.-]+):jar:([^:\s]+):(compile|runtime)\b',
    (probe / 'runtime-dependencies.txt').read_text(), re.M)
with zipfile.ZipFile(probe / 'repo/backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar') as archive:
    packaged = {name.removeprefix('BOOT-INF/lib/') for name in archive.namelist()
                if name.startswith('BOOT-INF/lib/') and name.endswith('.jar')}
resolved_names = {f'{artifact}-{version}.jar' for group, artifact, version, scope in runtime}
assert len(runtime) == len(resolved_names) == 55
assert len(packaged) == 48
assert len(resolved_names - packaged) == 8
assert packaged - resolved_names == {'spring-boot-jarmode-tools-4.1.2.jar'}
assert resolved_names != packaged
```

## Exact changes and boundary proof

The sole deliverable change is this report:

`docs/gateflow/pa11-customer-auth-api-foundation-implementation.md`

Two ignored generated evidence files under existing `backend/target/` are identified above. No production Java file, test, POM, profile, CI, README, migration, OpenAPI, design/security document, controller, accepted plan/history, private file, index, commit, or branch was changed. The tracked-tree equality command above proves V001–V009, `application.yml`, `application-persistence.yml`, and all other tracked inputs unchanged before report creation. Final status is expected to contain only this new report.

Input hashes observed in this checkout:

| Input | SHA256 |
| --- | --- |
| Current controller working-tree bytes | `ed8f17a7e549f4b8833d57ed4d1130efeb038d40c2f4856f5ab7ca288ffbcab7` |
| Accepted plan | `ffb3c9e5fd0e1ca6b3535934e0b9ea9b05cb8723f1490186545a6ee6daa2d764` |
| Fix-1 | `ffee8ff46f6b97bfa0638ce854179c366e21105c992ae93268fa33e922c89725` |
| Fix-2 | `39751c5fde1762b54b89f0883471349e2c0e72654bf2aea7a96e149c87c34bef` |

## Goal-to-evidence and unrun checks

| Goal/check | Evidence/disposition |
| --- | --- |
| Frozen dependency/runtime contract | Exact controller entry JAR/hash and resolved manifests reproduced; strict one-to-one requirement contradicted. |
| Opt-in web, opaque verifier boundary, ACTIVE context/PERSONAL, SQL/ACL, redaction and cleanup | Not implemented or tested in this attempt; no success claim. |
| Targeted tests, three PostgreSQL Failsafe classes, 49/84/133 reconciliation | Unrun because the required pre-edit contract cannot pass literally. Zero new test/XML results; no assertion weakened or test skipped through configuration. |
| Positive-first candidate/adversarial firewall and default/web candidate JAR smokes | Unrun; only read-only entry-manifest diagnosis performed. |
| PostgreSQL image/server provenance | No fresh PostgreSQL run. Controller records PostgreSQL 16.15 and image `sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`; these remain historical entry evidence, not independently reverified here. |
| SQL/no-write/resource cleanup | No DB connection, query, fixture, executor, HTTP server, or child runtime created by this attempt; nothing to tear down. SQL/ACL/no-write runtime evidence remains unproduced. |
| A1:OP:getCustomerMe, A0:INV:INV-01..03, A3:TS:TS1 | No new integration evidence. Login-provider/e2e A5 evidence and denial audit remain deferred, not completed or absorbed. |

## Residual ownership and stop

- Controller / Backend-CI: resolve the exact resolved-versus-packaged inventory contradiction before resuming S1; no review or state transition is performed here.
- Identity/Backend/Security: issuer/provider/store/TTL/channel and denial audit remain the accepted plan's future authentication destinations.
- Platform/Database/Security: provisioning, image provenance, capacity, edge and external logging remain deferred as accepted; no production readiness claimed.

No login, sign-up, logout, provider selection/adapter, Redis, session issuance/storage/TTL, cookie, other API, mutation, frontend or provisioning was added. No staging, commit, reset, stash, push, PR operation, approval, reviewer request, external comment or issue was performed.

status: implementation-blocked
