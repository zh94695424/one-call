# PA36 S1 — bounded code-review fix1

Task: `pa36-evidence-role-ivr-acl-code-review-fix1-gpt-20261116-01`  
Date: 2026-11-16 (Asia/Shanghai)  
Gate: **code-review fix**, through the current PA36 Controller boundary only.  
Status: **author fix complete**; LF/CRLF architecture validation passes; activation IT runtime blocked. No review approval, full-suite acceptance or production readiness claimed.

## Authority and accepted findings

Read PA36 Control through its bounded code-review-fix entry, accepted cumulative plan (including fix1/fix2), and both valid reviews: A3 `docs/reviews/code-review-20261116-162504.md` and B `docs/reviews/code-review-20261116-143119.md`. Controller adjudication takes precedence over B's characterization of CRLF as an environment prerequisite.

- `PA36-CODE-REV-01` / A3-01, accepted High: three `historicalBytes()` helpers and the inline docs17 raw pin omit uniform CRLF; helpers also search for an LF marker before normalization. This regresses accepted PA34 newline portability.
- `PA36-CODE-REV-02` / A3-02, accepted High: V018's raw hash is compared with the canonical LF digest, falsely rejecting CRLF despite identical normalized content and Flyway checksum.

## Exact changes and write set

1. `backend/src/test/java/com/onecall/archtests/architecture/OperationsLifecycleBoundaryTests.java`
2. `backend/src/test/java/com/onecall/archtests/architecture/OperationsActivationReadBoundaryTests.java`
3. `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadsIT.java`
4. `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`
5. `docs/gateflow/pa36-evidence-role-ivr-association-acl-s1-implementation.md`
6. `docs/gateflow/pa36-evidence-role-ivr-association-acl-s1-code-review-fix-1.md` (new).

The three existing helpers now normalize CRLF to LF immediately after UTF-8 decoding, hash that canonical full document, then locate the unique historical marker and return the canonical historical body. Non-docs17 paths still return their exact original byte array. Removed only redundant docs17 raw-representation assertions; retained the exact canonical full-document digest `663b6cb4bce60c2937a8d52b4bc788595912fe1908e1847f8d7a7e152ab2eeaf`, historical body pins and marker existence/uniqueness. The fourth class already normalizes its marker/body path; its inline pin now retains only the canonical exact docs17 hash. V018 uses existing `hash(lf(bytes))` with the unchanged exact digest. No third raw hash, new helper class or new test identity.

All frozen semantic assertions remain: canonical content (not just token presence), historical prefix maps, V018 CRC32/checksum1606293398, owner/grant/history/provenance/replay/ACL assertions. Only newline representation is ignored; other content drift still fails. No migration, production Java/configuration, PA25, ACL tuple, topology7/9/11, facts53/290/18/410, docs17/history, CI/POM/count, OpenAPI, Control, plan, review, EOL setting or `.gitattributes` change.

## Commands and validation

Evidence root: `/tmp/pa36-code-review-fix1-evidence/`. All Maven commands below run from each candidate's `backend` directory, offline with the existing Linux JDK/Maven and cached dependencies. No source-selection override narrows compiler inputs.

```sh
export JAVA_HOME=/opt/jdk21
MVN=/opt/maven-3.9.9/bin/mvn
FOCUSED=EvidenceRoleIvrAssociationAclBoundaryTests,OperationsActivationReadBoundaryTests,OperationsLifecycleBoundaryTests
# Current supplied worktree/backend:
$MVN -B -ntp -o -Dtest="$FOCUSED" test
# Current supplied worktree, root; copies to external /tmp only:
python3 /tmp/pa36-code-review-fix1-evidence/project-crlf.py
python3 /tmp/pa36-code-review-fix1-evidence/project-lf.py
# /tmp/pa36-code-review-fix1-crlf/backend:
$MVN -B -ntp -o -Dtest="$FOCUSED" test
# /tmp/pa36-code-review-fix1-lf/backend:
$MVN -B -ntp -o -Dtest="$FOCUSED" test
# Current supplied worktree/backend; ordinary compilation with IT profile:
$MVN -B -ntp -o -Pcontainer-smoke -DskipTests clean test-compile
$MVN -B -ntp -o -Pcontainer-smoke -Dtest="$FOCUSED" -Dit.test=OperationsActivationReadsIT verify
docker info
python3 /tmp/pa36-ci-static-extracted.py
/opt/jdk21/bin/java --class-path "$(cat /tmp/pa36-code-review-fix1-evidence/probe-classpath.txt)" /tmp/pa36-code-review-fix1-evidence/HelperProbe.java
# Current supplied worktree, root:
python3 /tmp/pa36-code-review-fix1-evidence/audit.py
```

| Validation | Exit | Actual result |
| --- | --- | --- |
| Current mixed-EOL worktree focused | 0 | 3 classes ×8 =24/24; failures0/errors0/skipped0 |
| External canonical LF focused | 0 | 3 classes ×8 =24/24; failures0/errors0/skipped0 |
| External uniform CRLF focused | 0 | 3 classes ×8 =24/24; failures0/errors0/skipped0 |
| Normal `clean test-compile`, container profile | 0 | Compiled262 main and149 test sources, including corrected IT |
| Current activation IT `verify` | 1 | Surefire24/24; Failsafe1 class-setup error, failures0/skipped0; no IT body executed |
| `docker info` | 1 | Permission denied connecting to `unix:///var/run/docker.sock` |
| Existing CI static extraction | 0 | Source/CI/POM identities and historical arithmetic unchanged |
| External compiled-helper reflection probe | 0 | 9 EOL inputs accepted,18 tamper inputs rejected,3 non-target passthrough checks |
| Entry-byte/identity/allowed-path audit | 0 | Exactly six authorized task deltas; all other audited input bytes unchanged |

The activation IT fails in `OperationsActivationReadsIT.start:21` → fixture start → Testcontainers: `Could not find a valid Docker environment`. Its XML records one class-level setup error, **not**18 executed methods or a pass. Docker socket permissions were not changed and no workaround/approval attempted. The direct helper probe invokes only compiled private static helpers and does not start PostgreSQL or execute the IT lifecycle.

Projection scripts copy the2128 entry-audited files, omit Git/private/generated trees, and normalize UTF-8 non-binary files externally only. CRLF projection converted1825 text files; no candidate source was edited there after projection. Both projections contain the exact corrected four Java files modulo line endings. The current worktree remains mixed-EOL; the extra canonical-LF run supplies an explicit all-LF result, not an assumption about current checkout shape.

CRLF docs17 has866 CRLF lines and0 bare LF; raw SHA256 `ff3a04667822d6aa8da9a41273c142e9d560d1279dd7ad458facfa5ea89d2c3c`, canonical SHA256 `663b6cb4bce60c2937a8d52b4bc788595912fe1908e1847f8d7a7e152ab2eeaf`. CRLF V018 has1071 CRLF lines and0 bare LF; raw SHA256 `ea2911ea92fa4b3d043f7fe63777d8cf35fb0733202784952886ac7978a4b37c`, canonical SHA256 `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817`. Thus the CRLF run exercises both formerly failing raw-hash gates and normalized-marker paths, rather than reusing LF inputs.

Reflection probe covers LF, mixed and uniform CRLF for each of the three existing helpers, requires exact historical digest `8f96a69ce5309319a8c8fab1cc6cab419e79e90781945ddcd6fd2b41a311d38f`, rejects mutations to both current section and historical content, and confirms non-target byte-array passthrough. It adds no repository test identity. Initial diagnostic attempts are retained: Maven from repository root exited1 (no root POM), JShell default engine exited1 (socket operation denied), local-engine probes exited1 (classloader/module resolution). The successful ordinary Java source launcher replaces those probe launch attempts; none is a product or CRLF failure.

## Inventory, byte custody and completion

Entry snapshot and final SHA256 manifest audit cover2128 input files, excluding private/generated trees. The only five changed existing files and sole new file are the six paths above; unrelated pre-existing implementation/review/Control deltas are preserved, not attributed to this task. Read-only Git uses explicit WSL-compatible `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD"`; the repository's Windows `.git` pointer/settings are not rewritten.

All149 Java test-source method-name lists match entry exactly; ordinary inventory remains77 Surefire classes/712 tests +32 Failsafe classes/520 tests =109 classes/1232 methods, annotation-line count1233. Focused classes remain8+8+8; `OperationsActivationReadsIT` remains18 methods. No skip/disable or identity/count change. All18 migration raw digests match entry, including V001–V017 byte-identical and V018 current raw SHA256 `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817`. Control, accepted plan/reviews, docs17, OpenAPI, PA25, CI/POM, production and all other tests match task-entry bytes. Full exact audit and XML counts: `final-audit.json`; focused XML retained under `worktree-surefire/` and both external projections; blocked IT XML under current `backend/target/failsafe-reports/`.

Residuals: current subprocess cannot execute PostgreSQL-backed activation IT because of Docker socket denial; full109-class/1232-method suite and hosted CI were not rerun; prior Controller runtime evidence is historical, not this author's new runtime pass. Existing external production/Platform/Security acceptance and paused PA35 remain unchanged. Helper duplication is preserved deliberately to avoid unauthorized refactoring.

Completed the bounded test-only correction and reports; stop here. No staging, commit, push, branch/PR mutation, merge, approval, mark-ready, reviewer request, comment, issue mutation, Control edit or re-review entry performed. Artifact/report byte sizes, line counts and SHA256 are measured after writing and reported separately to avoid a self-referential digest.
