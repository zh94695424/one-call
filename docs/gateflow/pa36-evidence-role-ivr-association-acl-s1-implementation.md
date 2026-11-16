# PA36 evidence-role IVR association ACL — S1 implementation

Task: `pa36-evidence-role-ivr-acl-implementation-gpt-20261116-01`  
Date: 2026-11-16 (Asia/Shanghai)  
Entry HEAD: `41bf643571af19904d730243451e459d8cee399f`  
Status: bounded final-validation fix3 implemented for accepted `PA36-CODE-REV-04`; only the V016-row mutation literal in `CustomerSubjectDetailReadPrivilegeIT:551` changes from `installed_rank=18` to `installed_rank=19`. Complete-class verification exits1: Surefire **4/4 passed**, Failsafe **10 selected/10 setup errors/0 failures/0 skips**, blocked by Docker socket access before any IT body executes. Full runtime validation, review acceptance and production readiness are not claimed. Current evidence: `pa36-evidence-role-ivr-association-acl-s1-final-validation-fix-3.md`; earlier evidence below is historical.

## Final-validation fix3 addendum — 2026-11-16

Task `pa36-evidence-role-ivr-acl-final-validation-fix3-gpt-20261116-01`. Read PA36 Control through the current bounded final-validation fix3 boundary. Controller's prior full-suite result was Surefire712/712 and Failsafe519/520, with the sole error accepted as `PA36-CODE-REV-04`: mutating V016's installed rank to18 collided with the existing V018 primary key before PA25 could reject drift. That is Controller evidence, not this author's runtime pass.

The one-byte Java change uses unique invalid rank19 while retaining the V016 row capture/update predicate, all seven mutations, apply/check rejection checks, exact JSON-row restore, final successful PA25 check and all10 ordered test identities. A scan limited to this method finds no second rank-changing mutation or same unique-key collision. The entire Java file equals entry bytes with just `"installed_rank=18"` replaced by `"installed_rank=19"`; mixed EOL remains unchanged.

Commands run from `backend`, with `JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -o`:
- `-Pcontainer-smoke -Dtest=CustomerBusinessNumberBoundaryTests -Dit.test=CustomerSubjectDetailReadPrivilegeIT verify`: exit1, Surefire4/4 passed; Failsafe10 tests/0 failures/10 setup errors/0 skips. All errors occur at `start:43` → `CustomerBusinessNumbersFixture.start:61`; neither the corrected mutation nor any other IT body runs.
- `docker info`: exit1, `permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`. Testcontainers reports `DOCKER_HOST unix:///var/run/docker.sock is not listening`, `java.net.SocketException: Operation not permitted`, then `Could not find a valid Docker environment`. No permission/daemon workaround attempted.
- Normal fallback command: `-Pcontainer-smoke -DskipTests clean test-compile`: exit0,262 main and149 test sources compiled, BUILD SUCCESS; no narrowed source includes.
- Full `-Pcontainer-smoke clean verify` is not run after the demonstrated Docker blocker, under the authorized fallback. Controller must run complete-class10/10 and full77 Surefire classes/712 tests +32 Failsafe classes/520 tests =109/1232, zero failures/errors/skips, BUILD SUCCESS.

Evidence: `/tmp/pa36-final-validation-fix3-evidence/` retains logs/exits, copied class-run XML before clean, XML counts and entry-byte/source audits. Only the affected Java file, this report and the new fix3 artifact are changed; Control, V018, PA25, production, docs17, CI/POM, fixtures and other tests remain untouched. Source/report correction is complete; runtime acceptance remains blocked. No staging, commit, push, remote-state mutation, review request or final-review entry is performed.

## Code re-review fix2 addendum — 2026-11-16

Task `pa36-evidence-role-ivr-acl-code-review-fix2-gpt-20261116-01`. Read PA36 Control's current bounded fix2 gate, A re-review `docs/reviews/code-review-20261116-180600.md` and accepted plan §7.3. Controller accepted `PA36-CODE-REV-03` (A `PA36-CREREV-A-01`) after reproducing Expected17/actual18 at line489 → `history():416`. Pinning this historical fixture to017 preserves the exact17-row/288-tuple oracles; no assertion, identity, fixture semantics or current-V018 path changes. Full-class call scan finds only setup016 and this fresh017 call; the credentialed replay invokes setup016 indirectly, while the current PA25 method still uses the unchanged explicit target018 `latest()` helper. No second historical latest/null defect exists in this class.

New validation, cwd `backend`, `JAVA_HOME=/opt/jdk21`, Maven `/opt/maven-3.9.9/bin/mvn -B -ntp -o`:
- `-Pcontainer-smoke -DskipTests clean test-compile`: exit0, normal compilation of262 main/149 test sources; no narrowed test-source includes.
- `-Pcontainer-smoke -Dtest=CustomerBusinessNumberBoundaryTests -Dit.test=CustomerEvidenceReadPrivilegeIT#v017FreshAndUpgradeHaveExactSeventeenHistoryAndFrozenPredecessors verify`: exit1; Surefire4 tests/0 failures/0 errors/0 skips, Failsafe1 selected test/0 failures/1 setup error/0 skips. Failure is `CustomerEvidenceReadPrivilegeIT.start:43` → `CustomerBusinessNumbersFixture.start:61`; the corrected line489 was not reached. Subprocess logs `DOCKER_HOST unix:///var/run/docker.sock is not listening`, `java.net.SocketException: Operation not permitted`, then `Could not find a valid Docker environment`.
- `docker info`: exit1, `permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`. No socket/daemon permission workaround attempted.
- Entry-byte/source audit: exact one-line byte replacement with original mixed EOL preserved; all10 test names/count and all assertions/oracles unchanged. V018 raw SHA256 remains `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817`. Final2123-entry-file manifest audit permits only this Java file, this report and the new fix2 artifact; all other audited bytes are unchanged.

Logs, exits, copied XML, source scan and before/final manifests: `/tmp/pa36-code-review-fix2-evidence/`. Runtime completion remains blocked: rerun the exact target for expected4/4 +1/1 in a Docker-capable subprocess, then the other nine class methods/existing focus and mandatory full109-class/1232-method suite as directed by Controller. No new PostgreSQL pass is claimed; previous Controller results are historical. Control, V001–V018, PA25, production, docs17, CI/POM, other tests and read-only artifacts are untouched. Bounded source/report work is complete; no staging, commit, push, branch/PR mutation, merge, approval, mark-ready, reviewer request, comment, issue mutation or final re-review entry.

## 1. Authority and scope

Code-review fix1 addendum (2026-11-16): read Control through the bounded code-review-fix entry, accepted cumulative plan and valid reviews A3 `code-review-20261116-162504.md` and B `code-review-20261116-143119.md`. Corrected only the three existing `historicalBytes()` helpers and the new PA36 boundary test: normalize CRLF before docs17 hash/marker/body handling; remove redundant representation-specific raw pins; use the existing `lf()` helper for V018's canonical SHA256. Full-doc canonical SHA256, historical hashes, marker uniqueness and all semantic assertions remain. No helper class or JUnit identity was added. Test inventory remains 77/712 +32/520 =109/1232 (1233 annotation lines), including the same24 focused architecture methods and18 activation IT methods. All V001–V018 bytes, production/configuration, docs17/history, PA25, ACL facts53/290/18/410, topology7/9/11, CI/POM, OpenAPI, Control, plan and reviews remain unchanged relative to this task's entry. This addendum supersedes only the stale current-status statements in the historical implementation evidence below.

Current commands (cwd `backend`, `JAVA_HOME=/opt/jdk21`, Linux Maven `/opt/maven-3.9.9/bin/mvn -B -ntp -o`): focused `-Dtest=EvidenceRoleIvrAssociationAclBoundaryTests,OperationsActivationReadBoundaryTests,OperationsLifecycleBoundaryTests test` exits0 in current mixed-EOL worktree, external LF projection and external uniform-CRLF projection (24 tests each, zero failures/errors/skips). `-Pcontainer-smoke -DskipTests clean test-compile` exits0, compiling262/149 sources without narrowing test compilation. `-Pcontainer-smoke -Dtest=EvidenceRoleIvrAssociationAclBoundaryTests,OperationsActivationReadBoundaryTests,OperationsLifecycleBoundaryTests -Dit.test=OperationsActivationReadsIT verify` exits1: Surefire24/24, Failsafe1 setup error, no IT-body pass. `docker info` exits1: permission denied on `unix:///var/run/docker.sock`. External reflection probe exits0: all three compiled helpers accept LF/mixed/CRLF (9 cases), reject current/history content mutations (18 cases) and preserve unrelated bytes (3 cases), without invoking JUnit lifecycle or claiming PostgreSQL execution. CI static extraction exits0. Evidence and command logs: `/tmp/pa36-code-review-fix1-evidence/`. V018 raw current SHA256 remains `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817`. Stop at this fix; no re-review or external mutation performed.

Read Active PA36 Control, cumulative plan SHA256 `BD50534FA7EEF6E7288D6E5C1854F5C67499697EEF155A1C8E2D29567DE6555C`, fix1/fix2, and the referenced review files `plan-review-20261116-061519.md`, `-063117.md`, `-080510.md`, `-111954.md`, `-120715.md`, `-121153.md`. The accepted cumulative plan is binding; no redesign or later-gate entry is made.

The cumulative candidate has exactly **43 implementation paths**: three new (V018 and two test classes) and forty existing, plus this implementation report. Fix1 changes only the already-authorized `EvidenceRoleIvrAssociationAclIT.java`, updates this report, and adds the separately authorized fix-1 artifact. All existing method identities remain unchanged. No V001–V017, docs12/14/15, OpenAPI, production Java/configuration, PA35 application, dependencies, wrappers, plans, reviews or Control are edited by fix1. Private pre-existing `.codex` is excluded. Ignored Maven/npm/generated validation outputs are not implementation files.

## 2. Implemented boundary

- V018 contains independent literal evidence16/evidence18 oracles and a single owner-issued, non-grantable `GRANT SELECT (session_id, call_id) ON TABLE evidence.evidence_record TO evidence_role`, only after the exact clean prestate. Complete final state replays without a GRANT; partial, extra, broad, wrong-grantor and grant-option states are rejected rather than repaired.
- The migration retains actual PG16/superuser/session authority, shared `(725025,1)` advisory lock, transaction-local `pg_catalog`, exact predecessor17 metadata/checksums, finite marker/history/ACL replay states, sanitized P0001 categories, two passes, target explicit/effective ACL equality and cardinality, touching topology/attributes/settings/ownership/PUBLIC/default/database/shared/extension dependency guards. Before/after preservation includes roles/password verifiers/comments/memberships/settings/defaults/database state, owners, definitions/constraints/indexes/triggers and all non-target ACLs. Operations/IVR absolute ACL inventories are not V018 predicates.
- The optional V018 history row checks exact self metadata and non-null checksum, not a circular self checksum. Current PA25 and Flyway validation independently pin the concrete checksum. Raw replay is not claimed to authenticate its own source checksum.
- PA25 apply/check now require four inputs and exact history18 / ACL410. Independent coverage is original seven-role290 (historical288 plus two) plus unchanged Operations14 / IVR106. The four context targets do not enter410.
- Existing shared Operations datasource login LO, slot `operations_ivr_read`, has exactly seven non-inheriting SET edges; total graph eleven. Current markers are exactly `PA25_APPLY_OK logins=7 effective_roles=9 memberships=11` / `PA25_CHECK_OK logins=7 effective_roles=9 memberships=11`. Nine means monitored roles, not total reachable capabilities: capability boundary13, managed names13, full boundary17. Base roles retain true-INHERIT migration defaults where required; direct identities retain their boundaries.
- docs17/runbook/README prepend one authoritative current section, ending at the unique full-line `<!-- PA36 HISTORICAL BODY -->` delimiter. Historical bodies remain byte-identical; undated old PA25 commands are explicitly historical. The role-wide LW/LO amplification, four real production consumer paths, pre-existing JSON readability, preserved writes, marker custody and external acceptance limits are explicit. **Explicit SET is not a read-only privilege sandbox.**
- Historical PA26–PA29 tests stay at their actual prefixes and retain names/oracles/checksums. Current-only PA25/history/directory tails move to018/410; latest seven-role projections are290. PA34 manifests and docs17 historical pins remain separate from new full-document pins. Fifteen pure history-only paths change only latest counts; the unfiltered V014 classpath tail is18 while its target013/prefix13 and historical014 ACL28 remain unchanged.
- New ordinary suites have exactly8 Surefire and16 Failsafe identities, covering the accepted matrix, real unmodified Operations datasource/context, seven fixed roles including test-only IVR SET, denial enumeration, restoration/cleanup/completion faults and scalar amplification. Controller pre-fix execution passed11 of16 PA36 methods and exposed five failures; post-fix IT bodies are **not runtime-proven in this child**.
- CI preserves every historical addition/start reconstruction and the single historical comment. Current source/CI/POM inventory is77/712 +32/520 =109/1232; annotation lines1233; includes32. PA25 stays20 methods; capability pairing is91/14/77 with ten NOLOGIN password probes. Dependency/JAR allowlists remain unchanged; only the new Failsafe include and required V018 package assertion are added.

## 3. Frozen and candidate digests

| Item | Value |
| --- | --- |
| V001–V017 raw manifest | All17 match accepted-plan §2; unchanged signed predecessor checksums |
| V018 signed Flyway checksum | **1606293398** |
| V018 raw / LF SHA256 | `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817` / same (LF, no BOM, final newline) |
| docs17 full raw SHA256 | `e10aff5805e800fb1d1d78d0415690233b4dff87a110a68b0e2ca4d8dca708b7` |
| docs17 full LF SHA256 | `663b6cb4bce60c2937a8d52b4bc788595912fe1908e1847f8d7a7e152ab2eeaf` |
| docs17 historical body raw / LF | `7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e` / `8f96a69ce5309319a8c8fab1cc6cab419e79e90781945ddcd6fd2b41a311d38f` |
| Runbook historical body raw | `588e7c1aafc3f2418a8341790eaae0692919b131cf4a76f414d848eb9dc9f63a` |
| README historical body raw | `0bd1b7b17f388a603a06dc47798748ab8df16199d67d9e243332dc183350a9c6` |
| Sorted source/CI identity manifest SHA256 | `a082db1f77d93e9c76ed20500ca52c9e5c8b96fdc97b7140f0fa7d1972f58b23` (binding candidate value) |
|43-path raw implementation manifest SHA256 after fix1 | `ea68090e73c04641c6113103bd102936f709e4908a699d5c5b9e9b2df2475df3` |
| Historical initial-author XML manifest SHA256 (not fix1 evidence) | `3adc3410320aa990b264e6333c746817d16ec7e928353cfb5cf3641864945c51` |

Pinned Flyway12.4.0 `ChecksumCalculator.calculate(new StringResource(...))` independently returns1606293398, matching the UTF-8 line-by-line signed CRC32 calculation and PA25/test pins. The Controller subsequently proved fresh/upgrade migrations, Flyway validation and migration-package signature loading on PostgreSQL16.15. Those accepted pre-fix results are preserved; the post-fix author's Docker-blocked attempt does not supersede them or prove the corrected methods.

## 4. Validation history and current fix1 attempt

Environment: Temurin21.0.13+9 at `/opt/jdk21`, Maven3.9.9 from the pinned wrapper distribution. The default Windows Java launcher fails in this sandbox; an external LF wrapper shim `/tmp/pa36-tools/mvnw` uses the unchanged repository `.mvn` inputs. Maven commands use `JAVA_HOME=/opt/jdk21`, `-B -ntp -o -f backend/pom.xml`; no wrapper/toolchain/dependency change. Node22/npm10 locked dependencies install successfully offline from existing cache (265 packages).

| Validation | Observed result |
| --- | --- |
| Test compilation / enforcer | PASS:262 main and149 test sources compile; pinned toolchains/dependency rules pass |
| Exact candidate CI static extraction | PASS:77/712 +32/520;109/1232;1233 annotation lines;32 POM includes; every historical start total and identity manifest agree |
| Controller pre-fix focused run | Surefire56/56; PA25 IT20/20; PA36 IT16 total with1 failure/4 errors, all five reproduced defects are implementation/test defects, not Docker blockers |
| Fix1 focused Surefire (fresh clean run) | **PASS:9 classes /56 tests /0 failures /0 errors /0 skips** |
| Fix1 focused PostgreSQL16 suites | **BLOCKED:2 classes /36 registered cases /0 failures /36 setup errors /0 skips**;16 PA36 +20 PA25 bodies do not run in this author's sandbox; no corrected-method pass claimed |
| Fix1 CI static source/POM extraction and diff check | PASS, exit0 each;77/712 +32/520 and32 includes unchanged; only three authorized paths differ from fix1 entry |
| Broader ordinary Surefire attempt | NOT PASSED:77 classes /712 tests /5 failures /359 errors /0 skips; Mockito self-attachment and socket creation/bind restrictions are observed; retained as diagnostic evidence, not acceptance |
| Packaged resource bytes | PASS: all18 migration entries byte-equal to current resources, exactly one V018, seventeen frozen predecessors, no PA25/test/helper resources |
| Existing dependency/JAR firewall | PASS: unchanged72 resolved /65 packaged, baseline37/34, deltas35/31, dependency-only8, counterparts64, packager1; **116 unchanged adversarial probes** pass |
| A1 `npm run check:s3` | PASS, including49 operation/doc rows and media-gateway lint |
| A3 s3 / A4 s2 exact CI commands | PASS; docs17 introduces no A3 drift; A4 all113 fixture cases pass |
| A2 s3 | FAIL; reproduced against baseline inputs with byte-identical3987-byte output: A0 status drift8, registry drift6, column drift40 |
| A5 s1 / A5 s2 | FAIL; each baseline output byte-identical98 bytes: `A5_DETERMINISM_FAILURE determinism.probe two-probe` |

Required fix1 command, run once serially from `backend` (all other broader validation rows retain the initial-author results, not new fix1 executions):

```sh
JAVA_HOME=/opt/jdk21 /tmp/pa36-tools/mvnw -B -ntp -o \
  -Dtest=EvidenceRoleIvrAssociationAclBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerEvidenceBoundaryTests,CustomerSubjectActivationBoundaryTests,CustomerSubjectClaimBoundaryTests,CustomerSubjectDetailBoundaryTests,OperationsActivationReadBoundaryTests,OperationsLifecycleBoundaryTests,OperationsSubjectReadBoundaryTests \
  -Dit.test=EvidenceRoleIvrAssociationAclIT,ProductionDatabaseIdentityProvisioningIT -Pcontainer-smoke clean verify
```

That fix1 command exits1 at Failsafe verify after56 successful ordinary tests and packaging (78 seconds, finished 2026-11-16T13:20:42+08:00). `docker info` reports `permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`; Testcontainers reports no valid Docker environment and subsequent setups report cached discovery failure. No PostgreSQL server starts in this attempt: fresh/upgrade/replay/negative SQL execution, real TCP/pool probes and zero-error container evidence are **NOT EXECUTED POST-FIX**. The Controller's earlier PostgreSQL16.15/20-of20 PA25 evidence remains accepted and distinct. Tests are not disabled, skipped, relaxed or treated as passed. No duplicate PostgreSQL suite is run.

The broader unit attempt also observes `Could not self-attach to current VM using external process` / Mockito MockMaker initialization errors and `Operation not permitted` for socket creation. Its full counts are retained without claiming every failure is independently isolated or repaired. No unrelated production/test fix is attempted. Packaged launcher/HTTP transport smoke and the full1232-test zero-failure XML gate remain Controller work because these child restrictions prevent them.

Contract commands are extracted unchanged from current CI. Baseline reproduction uses physical copies of validator modules under `/tmp/pa36-contract-baseline`, frozen contract inputs and the original docs17 body; source/contract files are not edited. Symlinked executable modules were not used for the final baseline comparison because Node's entrypoint guard would not execute them. A2/A5 baseline and candidate outputs match byte-for-byte; no contract weakening is made.

Earlier diagnostic/compile/hash assertion iterations are not counted as success. The final focused verification runs after all Java/SQL/CI changes. Full Surefire diagnostics are archived separately from final focused evidence; no incomplete or mixed report set is claimed to be the full1232-case gate.

## 5. Evidence custody and Controller work

Historical initial-author evidence directory (not current fix1 evidence): `/tmp/pa36-final-evidence/`:

- `focused-surefire-reports/`: final9 XML suites,56 successful identities.
- `focused-failsafe-reports/`: final2 XML suites,36 Docker-blocked setup errors; identities still exactly match source/CI.
- `full-surefire-reports/`: separate77-suite/712-case broad-attempt diagnostic snapshot.
- `validation-counts.json`, `xml-sha256.txt`, `implementation-paths.sha256`, `source-ci-identities.txt`, `flyway-signed-checksums.json`: explicit counts/byte digests/identities and all18 signed checksum facts, not cryptographic signatures or runtime SQL proofs.
- `pa36-final-candidate-focused-verify.log`, `pa36-full-surefire-candidate.log`, `pa36-docker-info.log`, `pa36-flyway-checksum.log`, `pa36-static-final.log`, `pa36-package-firewall-final.log` and contract logs/results retain commands' actual outcomes. Temporary launchers/checksum/static extraction helpers remain under `/tmp` only.

Current fix1 evidence is `/tmp/pa36-fix1-evidence/`: exact command/exit/log, fresh XML count summary, pre-fix Controller IT XML/text preserved before clean, current43-path digest manifest, CI-static exit/log, Flyway validation bytecode and entry-snapshot/scope/diff evidence. The fix artifact explains each root cause and acceptance limit. The current required focused run remains blocked by Docker access; a working authorized environment must validate the corrected five methods before accepting fix1. No additional PostgreSQL run is launched by this author. The unchanged full CI gate (77/712 +32/520, launcher/transport and30-minute budget) and production acceptance remain unclaimed. External protected credentials/TLS/pg_hba/rotation/old-session drain, marker writers, cluster-wide dependencies and exclusive administration remain Platform/Security/Operations acceptance prerequisites. Existing docs14 drift and A2/A5 baseline failures remain owner-routed outside PA36. Production readiness is **NOT CLAIMED**; PA35 is not resumed.

No staging, commit, push, branch/PR mutation, merge, approval, mark-ready, reviewer request, external comment, issue mutation or code-review entry is performed. Stop at this implementation/report handoff.

## 6. Exact implementation write set

This list is plan §6's43 paths, excluding this separately authorized report:

1. `backend/src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql`
2. `backend/ops/postgresql/pa25-runtime-identities.sql`
3. `docs/operations/pa25-production-database-identity-provisioning-runbook.md`
4. `docs/17-production-security-design-spec.md`
5. `backend/src/test/java/com/onecall/archtests/container/EvidenceRoleIvrAssociationAclIT.java`
6. `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`
7. `backend/src/test/java/com/onecall/archtests/container/ProductionDatabaseIdentityProvisioningIT.java`
8. `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersFixture.java`
9. `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java`
10. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailReadPrivilegeIT.java`
11. `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceReadPrivilegeIT.java`
12. `backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java`
13. `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectDetailBoundaryTests.java`
14. `backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java`
15. `backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java`
16. `backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java`
17. `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java`
18. `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java`
19. `backend/src/test/java/com/onecall/archtests/container/ActivationCommandRoleLeastPrivilegeIT.java`
20. `backend/src/test/java/com/onecall/archtests/container/OperationsLifecycleRoleLeastPrivilegeIT.java`
21. `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadRoleLeastPrivilegeIT.java`
22. `.github/workflows/ci.yml`
23. `backend/pom.xml`
24. `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersIT.java`
25. `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceIT.java`
26. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimsIT.java`
27. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectsIT.java`
28. `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java`
29. `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadsIT.java`
30. `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadRoleLeastPrivilegeIT.java`
31. `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadsIT.java`
32. `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java`
33. `backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java`
34. `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java`
35. `backend/src/test/java/com/onecall/archtests/container/SubjectClaimRoleLeastPrivilegeIT.java`
36. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
37. `backend/src/test/java/com/onecall/archtests/container/DatabaseRuntimeAclRbacHardeningIT.java`
38. `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectActivationBoundaryTests.java`
39. `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectClaimBoundaryTests.java`
40. `backend/src/test/java/com/onecall/archtests/architecture/OperationsActivationReadBoundaryTests.java`
41. `backend/src/test/java/com/onecall/archtests/architecture/OperationsLifecycleBoundaryTests.java`
42. `backend/src/test/java/com/onecall/archtests/architecture/OperationsSubjectReadBoundaryTests.java`
43. `backend/README.md`
