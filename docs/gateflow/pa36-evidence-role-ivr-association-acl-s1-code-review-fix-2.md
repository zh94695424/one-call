# PA36 S1 — bounded code re-review fix2

Task: `pa36-evidence-role-ivr-acl-code-review-fix2-gpt-20261116-01`  
Date: 2026-11-16 (Asia/Shanghai)  
Route: Codex provider `gpt`; requested GPT `gpt-6.1-sol` / high  
Status: source correction and reports complete; targeted runtime validation **BLOCKED**, not passed. Finding is fixed in candidate, pending independent validation/review; no gate acceptance is claimed.

## Accepted finding and authority

Read PA36 Control's current bounded code re-review fix2 gate, A re-review `docs/reviews/code-review-20261116-180600.md`, and accepted plan §7.3. Controller accepted `PA36-CODE-REV-03` (A `PA36-CREREV-A-01`, High): the historical V017 fresh fixture still used moving `null`/latest semantics, which now migrate through V018 and provision PA25. Controller reproduced `CustomerEvidenceReadPrivilegeIT.java:489` → `history():416`, Expected17 but was18. That prior runtime failure is Controller evidence, not this author's execution result. Plan §7.3 explicitly requires pinning this historical fresh call to017, retaining17 history rows and288 ACL tuples.

## Exact change and bounded scope

Only Java source change, at `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceReadPrivilegeIT.java:489`, inside unchanged method `v017FreshAndUpgradeHaveExactSeventeenHistoryAndFrozenPredecessors`:

```diff
-        fixture.close(); fixture = new CustomerBusinessNumbersFixture(); fixture.start(null); history();
+        fixture.close(); fixture = new CustomerBusinessNumbersFixture(); fixture.start("017"); history();
```

Full-class call scan: setup at43 remains `fixture.start("016")`; the credentialed replay calls that setup indirectly; the only fresh recreation at489 now explicitly targets017. No other historical fresh-prefix latest/null call exists. `current()` remains target017; `latest()` remains target018, and the current PA25 method at640 still calls it before provisioning. No current-V018 call, assertion, method name, test identity/count or oracle changes. All10 JUnit methods retain their exact ordered identities. The entire Java file equals task-entry bytes with precisely the displayed replacement, including unchanged mixed line endings (597 CRLF lines).

Exactly three task write paths:
- `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceReadPrivilegeIT.java`
- `docs/gateflow/pa36-evidence-role-ivr-association-acl-s1-implementation.md`
- New `docs/gateflow/pa36-evidence-role-ivr-association-acl-s1-code-review-fix-2.md`

No fixture, V018, PA25, production, docs17, CI/POM, other test, Control or read-only review/plan edits. Pre-existing candidate changes remain untouched. The Windows `.git` pointer cannot resolve under WSL; read-only Git inspection uses explicit `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD"` without changing metadata/configuration.

## Commands and actual results

Maven commands run in supplied worktree `backend`, using Linux JDK21/Maven, offline cached dependencies; normal test-source compilation is not narrowed:

```sh
JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -o \
  -Pcontainer-smoke -DskipTests clean test-compile
JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -o \
  -Pcontainer-smoke -Dtest=CustomerBusinessNumberBoundaryTests \
  -Dit.test=CustomerEvidenceReadPrivilegeIT#v017FreshAndUpgradeHaveExactSeventeenHistoryAndFrozenPredecessors verify
docker info
```

| Check | Exit | Actual evidence |
| --- | --- | --- |
| Normal `clean test-compile` | 0 |262 main and149 test sources compiled; BUILD SUCCESS |
| Required targeted `verify` | 1 |Surefire4/4; Failsafe1 selected test,0 failures,1 setup error,0 skipped; BUILD FAILURE |
| `docker info` | 1 |Permission denied connecting to Docker API at `unix:///var/run/docker.sock` |
| Full-class source scan | 0 |Exactly one source-line substitution;10 unchanged test identities; no remaining `fixture.start(null)` |
| Final entry-byte manifest audit | 0 |Only the three allowed task paths differ; V018 raw digest unchanged |

The Maven subprocess reports `DOCKER_HOST unix:///var/run/docker.sock is not listening`, followed by `java.net.SocketException: Operation not permitted`, then `Could not find a valid Docker environment`. The selected method errors during `@BeforeEach` at `CustomerEvidenceReadPrivilegeIT.start:43` → `CustomerBusinessNumbersFixture.start:61`. No PostgreSQL container starts; neither the test body nor corrected line489 executes. Therefore Failsafe1/1 **is not passed**; the setup error is not the historical Expected17/actual18 failure. No daemon/socket/settings workaround was attempted.

XML independently confirms unit `tests=4, failures=0, errors=0, skipped=0` and IT `tests=1, failures=0, errors=1, skipped=0`. Evidence directory `/tmp/pa36-code-review-fix2-evidence/` contains `test-compile.log`/`.exit`, `targeted-verify.log`/`.exit`, `docker-info.log`/`.exit`, copied Surefire/Failsafe XML, `xml-counts.json`, `source-scan.txt`, entry Java/report copies, `before.json`, `audit.py` and `final-audit.json`. Audit command from worktree root: `python3 /tmp/pa36-code-review-fix2-evidence/audit.py after`.

## Integrity, residuals and completion

Task-entry manifest covers2123 non-generated regular files, excluding Git, private/helper trees and build outputs. Final audit checks the exact changed-path set and exact Java replacement, ordered method names/count and unchanged V018 raw bytes. SHA256 remains `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817`. All other audited files, including V001–V017, fixture semantics, PA25, production, docs17, CI/POM, Control and read-only review/plan inputs remain byte-identical to task entry. All pre-existing unrelated diffs are preserved, not attributed to fix2. No weakening, skip, duplicate or rename is introduced. Artifact/report size, line count and SHA256 are measured after writing and reported separately to avoid a self-referential digest.

Residuals: Docker access in this subprocess prevents runtime confirmation. Required next validation is the exact targeted command with expected Surefire4/4 and Failsafe1/1, followed by the other nine methods and existing focus if feasible; mandatory full109-class/1232-method validation, hosted CI and external production acceptance remain outstanding and were not rerun here. Older Controller/fix1 evidence is historical, not a fresh pass for this correction. No V018/PA25/product defect is inferred from this environment blocker.

Bounded source/report work is complete; runtime acceptance remains blocked and independent review is pending. Stopped without modifying Control or entering final re-review. No stage, commit, push, branch creation, PR mutation, merge, approval, mark-ready, reviewer request, comment or issue mutation performed.
