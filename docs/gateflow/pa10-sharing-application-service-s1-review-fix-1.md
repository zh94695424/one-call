# PA10-S1 accepted review fix 1

## Identity and scope

- Task: `pa10-s1-review-fix-gpt-20261101-01`; work unit: `PA10-sharing-application-service-foundation`; gate: `fix S1 code review`.
- Date: 2026-11-01, Asia/Shanghai. Isolated checkout HEAD remains controller checkpoint `7224e81ef4b8ce352dfddfe80843725e535d7ded`.
- Only controller-accepted B-F1, B-F3 and B-F5 were implemented. Findings F2/F4 were not implemented or re-dispositioned. Implementation/completion reports were treated as claims, not validation evidence.
- Repository writes are limited to `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java`, `.github/workflows/ci.yml`, `backend/pom.xml` and this new artifact. Existing candidate edits/untracked sources were preserved.
- Outcome: local fix complete; final exact inventory is **21 Surefire + 74 Failsafe**, zero failures/errors/skips. An initial container startup failure and the unchanged passing retry are both disclosed below.

## Accepted fixes and exact locations

### B-F1 — executable REQUIRES_NEW suspension proof

Location: `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java:330`, inside the existing `runtimeRoleAndPoolCleanupHoldOnSuccessAndFailure` method; nested observations/assertions at lines 338–350, rollback at line 351 and pool restoration at line 353.

The test snapshots acquisitions before starting the embedding transaction, permits exactly two pool connections, observes the caller's backend PID and read/write transaction, and verifies the outer acquisition is baseline +1. It delimits the nested call's grant executions by the preceding listing count and requires exactly one new captured listing. That actual execution must have effective role `sharing_role`, `readOnly=on`, isolation `read committed`, a PID different from the still-active outer caller, and checkout baseline +2. Total acquisitions must also be baseline +2. The resumed caller still observes `transaction_read_only=off`.

These are captured JDBC/PostgreSQL execution facts, not just Spring propagation configuration. Reusing the outer connection/transaction would violate PID, read-only and acquisition assertions. `status.setRollbackOnly()` now executes in the callback's `finally`, including assertion failures; Spring unwinds/rolls back the transaction, and the enclosing `finally` restores pool size 1. Existing success/failure role cleanup assertions remain intact. No fixture or production transaction behavior changed.

### B-F3 — intentional PostgreSQL 16 version guard

Location: `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java:49` and `:51`, inside existing `queryRuntimeStartsAfterFixtureProvisioning`.

The facts query additionally reads numeric `server_version_num`; the test requires the inclusive range `160000..169999`. This enforces the PostgreSQL 16 major line/floor while tolerating patch upgrades, rather than requiring the incidental string `16.15`. The original full `version()`, collation, ctype, template flag, session/JVM zones, image name and actual image ID remain printed at lines 55–58. No image tag/digest or production upgrade policy was selected.

### B-F5 — direct Boot SQL starter firewall

Locations: `.github/workflows/ci.yml:266`; `backend/pom.xml:133` and `:134`.

The negative JAR pattern retains all prior alternatives and adds `spring-boot-starter-(?:jdbc|jooq|flyway|web|security|data-)`. Starter aggregate names now fail directly without depending on a forbidden transitive module. The POM retains every prior enforcer rule/include/exclude and adds exclusions for `org.springframework.boot:spring-boot-starter-jooq` and `org.springframework.boot:spring-boot-starter-flyway`, alongside the existing JDBC exclusion. No dependencies were added.

## Commands and results

All commands were local. Maven ran from `backend` after:

```bash
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:$PATH"
```

Evidence directory `E` below means `/tmp/pa10-s1-review-fix-evidence`. Logs and auxiliary JSON/scripts are local evidence outside repository source; Maven also regenerated ignored `backend/target` outputs. Retain/collect these before cleanup.

| Command/check | Exit | Observed result / evidence |
| --- | --- | --- |
| `./mvnw -B -ntp -Dtest=CustomerShareQueryServiceTests,SharingPersistenceConfigurationTests,SharingQueryBoundaryTests test` | 0 | 11 tests, zero failures/errors/skips; `E/targeted-test.log` |
| `./mvnw -B -ntp -Pcontainer-smoke verify` — first attempt | 1 | Surefire 21 passed; historical IT 64 passed; sharing class failed in `@BeforeAll` with SQLSTATE 08001 / `Connection to localhost:60872 refused`, before its test methods executed. Failsafe aggregate 65, errors 1, failures/skips 0; `E/container-verify.log`; failed reports retained in `E/first-failed-failsafe-reports` |
| `./mvnw -B -ntp -Pcontainer-smoke verify` — unchanged retry | 0 | BUILD SUCCESS, 58.231 s; Surefire 21, Failsafe 74, all failures/errors/skips 0; `E/container-verify-retry.log` |
| `./mvnw -B -ntp validate` | 0 | All five existing enforcer rules pass, including `BannedDependencies`; `E/enforcer-validate.log` |
| `bash -e -o pipefail /tmp/pa10-s1-review-fix-evidence/ci-inventory.sh` | 0 | Current CI inventory step executed verbatim; `E/ci-inventory.log` |
| `bash -e -o pipefail /tmp/pa10-s1-review-fix-evidence/ci-jar-firewall.sh` | 0 | Current CI JAR step executed verbatim, including `jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar` and `./mvnw -B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa10-runtime-dependencies.txt`; `E/ci-jar-firewall.log` |
| Python firewall negative/positive controls and SQL enforcer coordinate inspection | 0 | All controls below pass; `E/firewall-negative-probes.log` |
| Python frozen-byte, candidate-hash and XML/source inventory checks | 0 | All checks below pass; `E/boundary-and-inventory.log`, `E/frozen-hashes.json`, `E/exact-test-inventory.json` |
| `git diff --check` | 0 | No whitespace errors; repeated after artifact creation |
| `git diff --cached --binary` | 0 | Empty; HEAD unchanged; no staging/commit |

The two CI scripts were extracted from the current named workflow `run: |` blocks, dedented without changing their contents, and executed with fail-fast/pipefail. Extracted inventory script SHA256: `67ea979fff069304fa260977f67a393f5d0110be7e0698e1796c331f80e58682`; extracted JAR script SHA256: `92bf6110211b46f3947088da1a0632cde1ab50391b4a172629e1e4bedce4844d`.

No tests, assertions or startup waits were weakened to obtain the retry. No H2, mock DSL substitute or skip was used. The fixture startup failure is not hidden by the final passing reports. The historical broken-migration test's intentional Flyway ERROR is separate from that startup failure.

### Exact XML/source inventory

The current CI validator printed:

```text
PA10 exact source/CI/XML inventory passed: Surefire 21; Failsafe 64+10=74; zero skips/errors/failures
```

For every row, fully qualified class names map to the source path under `backend/src/test/java/` with dots replaced by slashes and `.java` appended. XML is `backend/target/{runner}-reports/TEST-{fully-qualified-class}.xml`. The validator checks exact source method-name sets, exact XML class/method pairs, unique counts, the complete eight-class source inventory, and absence of conditional/disabled/parameterized tests, against the current CI's explicit name sets. Independent parsing also confirmed exact source/XML names and zero failures/errors/skips per class.

| Runner | Fully qualified class | Source methods / XML cases |
| --- | --- | --- |
| surefire | `com.onecall.archtests.OneCallApplicationTests` | 1 / 1 |
| surefire | `com.onecall.archtests.SharingPersistenceConfigurationTests` | 2 / 2 |
| surefire | `com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests` | 1 / 1 |
| surefire | `com.onecall.archtests.architecture.ModuleBoundaryTests` | 8 / 8 |
| surefire | `com.onecall.archtests.architecture.SharingQueryBoundaryTests` | 1 / 1 |
| surefire | `com.onecall.core.sharing.internal.application.CustomerShareQueryServiceTests` | 8 / 8 |
| failsafe | `com.onecall.archtests.container.PostgresContainerIT` | 64 / 64 |
| failsafe | `com.onecall.archtests.container.SharingQueryServiceIT` | 10 / 10 |
| Total | Surefire / Failsafe | **21 / 74** |

`E/exact-test-inventory.json` retains every exact method name, source/report path, report SHA256 and per-class result; SHA256 `aa602d92bce61ff11a687238a8116ee6e33d04ecf44cc1774a22ede4c1f428d2`. Final Failsafe report SHA256 values: historical `4b6f02daa305fe4b8e634776b9376ec571df4f4c49b62c566410dc7d2122dd94`; sharing `aedc1edd53ad654fb1e9a5932d2dd5b201ba8456324dd060ea984b25623b4060`. No test method was added, removed or renamed.

### Database and JAR evidence

The unchanged full facts output from the passing run is:

```text
PA10 database facts: PostgreSQL 16.15 on x86_64-pc-linux-musl, compiled by gcc (Alpine 15.2.0) 15.2.0, 64-bit; collation=C; ctype=C; template=false; sessionZone=Asia/Shanghai; JVM=Asia/Shanghai; image=postgres:16; imageId=sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab
```

The JAR guard printed `PA10 exact resolved query JAR inventory and negative firewall passed`. `backend/target/jar-index.txt` and `backend/target/pa10-runtime-dependencies.txt` are the inspected inputs. The exact approved resolved query coordinates/scopes remain:

| Coordinate | Version | Scope |
| --- | --- | --- |
| `org.jooq:jooq` | `3.21.9` | compile |
| `com.zaxxer:HikariCP` | `7.0.2` | compile |
| `org.postgresql:postgresql` | `42.7.14` | runtime |
| `org.springframework:spring-jdbc` | `7.0.10` | compile |
| `org.springframework:spring-tx` | `7.0.10` | compile |

The guard required exactly one resolved coordinate and one version-matched JAR for each approved query dependency and found no forbidden JAR. Additional direct regex controls rejected `spring-boot-starter-{jdbc,jooq,flyway,web,security,data-jpa}-4.1.2.jar`, existing Boot JDBC/jOOQ/Flyway module names, Flyway core, H2 and JUnit. Positive controls left the five approved query JARs and the approved base `spring-boot-starter-4.1.2.jar` unmatched by the negative pattern. XML inspection verified JDBC/jOOQ/Flyway starter exclusions exist and none is added as a direct dependency. Controls are synthetic names only; no dependency injection or remote mutation occurred.

## Frozen identity and boundaries

An arrival snapshot hashed all 1,089 tracked/nonignored-untracked source files before editing: `/tmp/pa10-s1-review-fix-before.json`, SHA256 `68ea85c991ab21dcaa28b48f949933dfa56f8687e2b4d70d54435b1a6cb83f14`. All **1,086** pre-existing files outside the three permitted edit paths remain byte-identical. The only added source path is this artifact. Frozen per-path hashes are retained in `E/frozen-hashes.json`, SHA256 `ffe8de10cbf213f285d02e40271e5c57aa8b2dc91ca211eba11408f4e61e20c6`.

The arrival checkout represents candidate additions as untracked files and uses the newer controller checkpoint. Therefore its raw `git diff --binary` hash was `02caf6704562d7b359c33254a747646ef74f12180d8e8927a74ad2ffd42aff0c`, not the old review serialization. To verify the supplied frozen identity without staging, the original 26-path candidate was reconstructed in temporary directories against the review's historical base `c5f180fa5d7127ca5e6fd5f3d53df6710884991c`. Every reconstructed candidate file was first checked against its arrival SHA256, including reversal of only these fixes in temporary copies. Normalized `git diff --no-index --binary` output has SHA256 **`1805959a839347a4d0faa770d55b2e9aa1b0c95b6b7ad7c4d264e9466d6f4fb0`**, exactly the supplied frozen candidate hash. Diff: `E/reconstructed-frozen-candidate.diff`. No repository/index content was changed for this reconstruction.

Selected frozen hashes, all unchanged:

| Input | SHA256 |
| --- | --- |
| V001 | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| V002 | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| V003 | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| V004 | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| V005 | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| V006 | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| V007 | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |
| V008 | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| V009 | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| Accepted plan | `b323f5368ed215c389b7448b8c90643cfe1cd22c55597e3419e65568cb54ec86` |
| Reviewer A artifact | `4aa385334c7bfbb7ebdf9b3458821cb12461c1e9400b6e63f5a95b5a2c0f82e9` |
| Reviewer B artifact | `1e6d41d16bd3f04394c0d7136c1fbf2491b3044e70fa41d32fa5e50ddc3b5c8d` |

The complete frozen manifest also covers product services/repositories/configuration, OpenAPI, design/security docs, control, README files, historical sources and both implementation/completion artifacts. Product query behavior, schema/migrations/ACLs, API shape and accepted plan remain unchanged. No image digest was pinned, production policy selected or additional feature implemented.

### Retained log identities

| Evidence under `E` | SHA256 |
| --- | --- |
| `targeted-test.log` | `967f01a5ddab9bc1ae2cf28bb62625a3e173e01cdd20639e029f0f9711e316f5` |
| `container-verify.log` — failed startup | `71c89d07f886777fb37f50dceadbb34901c92a5ac0a3dbbaf38ea1fb64f0afdb` |
| `container-verify-retry.log` — final pass | `e277734eeb0a938685863a1d969e599300a06b92fc3fa24ac98e0b01176334b8` |
| `enforcer-validate.log` | `7a1275c604be576232d239279cee3af4915706bd2e86bf7ee4c248ebc331cf14` |
| `ci-inventory.log` | `ada5c5bc4470ecbfb46a77a603cda9f677b47f75893901bb5912815787f11be4` |
| `ci-jar-firewall.log` | `acd00620338f0bcb992bee49bc51c037357fea264ce12dde48f834b2d86edb58` |
| `firewall-negative-probes.log` | `e1ae17a95e57c8a235664830878c886f2ae68793403f9314b9c6da89af602917` |
| `boundary-and-inventory.log` | `65b0c4513703b7976f5708025249257da33f32654167d883f025162a1e86e168` |

## Residual risks and stop boundary

- **Backend/CI:** the first run's fixture readiness/port refusal is a local startup reliability risk; the unmodified retry passed all 95 tests. Root cause beyond the observed refusal was not established. Fixture readiness changes are outside the allowed paths and were not attempted.
- **Backend/CI and Platform/Database/Security:** `postgres:16` remains mutable. Only the currently available 16.15 image was executed; future patches are admitted by the numeric guard but were not run. Production provisioning/image/upgrade policy remains Q17/Q23-owned, not decided here.
- **Sharing/Backend:** suspension is proved for this existing two-connection embedding scenario, not arbitrary caller isolation combinations, pool exhaustion or load/SLO behavior. No production pool-sizing policy follows.
- **Controller and Backend/CI:** F2/F4 and all rejected findings are untouched; this artifact neither accepts them nor proceeds to re-review. The unchanged Node contract job, production deployment and remote GitHub Actions job were not executed by this task.
- **Controller:** collect local logs/manifests/reports before temporary/generated output cleanup. This artifact reports local validation only, **not remote CI green**, review acceptance or production readiness.

No staging, commit, push, branch creation, PR/issue mutation, approval, reviewer request, external comment or re-review was performed. Stop at controller intake.
