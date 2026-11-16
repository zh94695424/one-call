# PA36 S1 — final-validation fix3

Task: `pa36-evidence-role-ivr-acl-final-validation-fix3-gpt-20261116-01`  
Date: 2026-11-16 (Asia/Shanghai)  
Provider: Codex `gpt`; requested dispatch GPT `gpt-6.1-sol` / high.  
Status: bounded source correction complete; Docker-blocked runtime acceptance remains pending Controller validation. No final review entered.

## Authority and accepted finding

Read `docs/gateflow/project-execution-control.md` through the current PA36 `final-validation fix3` boundary. Controller's fix2 full-suite validation passed Surefire712/712 and reached Failsafe519/520. Its sole error, accepted High `PA36-CODE-REV-04`, was `CustomerSubjectDetailReadPrivilegeIT.pa25RequiresExactV016HistoryAndIndependent269TupleOracleWithoutRepair`: after V018 occupied rank18, changing V016's installed rank to18 raised `flyway_schema_history_pk` instead of allowing PA25 to reject history drift. Those results are Controller evidence, not this author's execution result. This is a test-fixture collision, not a V018/PA25/production defect.

## Exact one-line change

Only source change, `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailReadPrivilegeIT.java:551`:

```diff
-        for (String mutation : List.of("checksum=0", "version='16'", "installed_rank=18", "type='BASELINE'", "success=false", "description='wrong'", "script='wrong.sql'")) {
+        for (String mutation : List.of("checksum=0", "version='16'", "installed_rank=19", "type='BASELINE'", "success=false", "description='wrong'", "script='wrong.sql'")) {
```

Rank19 is unique in the exact current V001–V018 history. The method still captures and updates the V016 row (`WHERE installed_rank=16`), tries all seven mutations, requires both PA25 apply/check to reject, restores the captured row with the unchanged delete/`jsonb_populate_record` insert, and requires the final successful PA25 check. No retargeting to V018 and no weakening/removal/skip. Scanning only this method finds this sole rank-changing mutation; the remaining six mutations do not introduce the same installed-rank collision. Version mutation `16` and exact restore remain unchanged.

Byte audit proves the complete Java file equals entry bytes with exactly this one-byte substitution. All433 CRLF lines and all10 ordered JUnit names remain unchanged. No other Java line is edited.

## Commands, exits and counts

Cwd: supplied worktree `backend`. Linux Temurin21.0.13+9, Maven3.9.9, offline cached dependencies; unchanged POM/wrapper/toolchain. Commands:

```sh
docker info
JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -o \
  -Pcontainer-smoke -Dtest=CustomerBusinessNumberBoundaryTests \
  -Dit.test=CustomerSubjectDetailReadPrivilegeIT verify
JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -o \
  -Pcontainer-smoke -DskipTests clean test-compile
```

| Check | Exit | Observed result |
| --- | --- | --- |
| `docker info` | 1 |Permission denied connecting to Docker API at `unix:///var/run/docker.sock` |
| Complete class `verify` | 1 |Surefire1 class/4 tests,0 failures/errors/skips; Failsafe1 class/10 tests,0 failures/10 setup errors/0 skips; BUILD FAILURE |
| Normal `clean test-compile` | 0 |262 main and149 test sources compiled without narrowing; BUILD SUCCESS; no runtime tests run |
| Exact source/method audit | 0 |One-byte substitution only; same10 ordered names; V016 capture/update, mutation list length and exact restore preserved |

The subprocess logs `DOCKER_HOST unix:///var/run/docker.sock is not listening`, `java.net.SocketException: Operation not permitted`, and `Could not find a valid Docker environment`. All10 integration tests error during `@BeforeEach` at `CustomerSubjectDetailReadPrivilegeIT.start:43` → `CustomerBusinessNumbersFixture.start:61`; no PostgreSQL container or IT body executes. This is not a runtime pass and not a reproduction of the former duplicate-key failure. No socket/daemon/settings workaround was attempted.

Full `JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -o -Pcontainer-smoke clean verify` was **not run**, with no exit/count claimed, because Docker is unavailable in this subprocess and the authorized fallback is normal compilation. Required Controller validation remains complete class10/10, then full77 Surefire classes/712 tests +32 Failsafe classes/520 tests =109 classes/1232 tests, zero failures/errors/skips and BUILD SUCCESS. Inventory expectations are unchanged, not observed passes here.

## Evidence and scope

`/tmp/pa36-final-validation-fix3-evidence/` contains `docker-info.log`/`.exit`, `class-verify.log`/`.exit`, `test-compile.log`/`.exit`, copied class-run XML in `class-xml/` before clean, `xml-counts.json`, entry Java/report byte copies, `before.json`, and `source-audit.json`. XML independently confirms4 unit passes and10 IT setup errors. Final integrity evidence is stored in `final-audit.json`; report/artifact bytes, lines and SHA256 are measured after writing and reported separately to avoid a self-referential digest.

Exactly three authorized task write paths:
- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailReadPrivilegeIT.java`
- `docs/gateflow/pa36-evidence-role-ivr-association-acl-s1-implementation.md`
- `docs/gateflow/pa36-evidence-role-ivr-association-acl-s1-final-validation-fix-3.md`

Task-entry manifest covers2128 non-generated files, excluding Git/private/helper/build/dependency outputs. The final manifest audit checks that only these three paths differ. All other audited bytes, including V018, PA25, production, docs17, CI/POM, fixtures, other tests and Control remain unchanged; pre-existing candidate changes are preserved. No Git metadata/configuration is changed; the Windows `.git` pointer is left intact.

## Residuals and completion

Bounded correction and reporting are complete. Runtime verification is blocked by Docker access; Controller owns full-suite execution and subsequent acceptance. Neither complete-class10/10, full109/1232, review acceptance, slice readiness nor production readiness is claimed. Historical Controller712/712 +519/520 does not establish this correction's runtime pass. PA35 remains paused; existing external production-acceptance prerequisites are unaffected.

No staging, commit, push, branch/PR/issue mutation, merge, approval, mark-ready, reviewer request, comment or final-review entry is performed. Control is not updated.
