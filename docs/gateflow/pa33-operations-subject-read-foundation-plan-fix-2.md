# PA33 Operations subject read foundation — bounded plan fix 2

## 1. Authority and evidence

- Task: `pa33-operations-subject-read-plan-correction1-gpt-20261114-01`; date: 2026-11-14; workspace HEAD: `185a91f3`.
- Gate: bounded plan-correction/test-oracle fix inside approved S1, under binding Controller decision `PA33-PLAN-CORRECTION-01` in `docs/gateflow/project-execution-control.md`.
- Required dispatch: Codex / GPT `gpt-6.1-sol`, reasoning high. This artifact does not independently attest to runtime/model metadata; Controller verifies dispatch.
- Controller full clean exposed the retained `CustomerEvidenceBoundaryTests.namedOwnerInterfacesAndTenCustomerDependenciesAreExact` assertions: subject_number still expected no allowed dependencies, and evidence still expected only entitlement_metering. These conflict with the already-approved production module metadata, not a newly requested behavior.
- Direct inspection confirms subject_number declares `operations_iam::operations-subject-read`; evidence declares `entitlement_metering` plus `operations_iam::operations-subject-read`. Production metadata is not changed by this correction.

## 2. Exact correction and scope

The added existing implementation path is exactly:

`backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java`

Retain the class, all four test identities, and specifically `namedOwnerInterfacesAndTenCustomerDependenciesAreExact`. Update only its two dependency assertions: subject_number must have exactly the named Operations read interface; evidence must have exactly entitlement_metering's unnamed interface and the named Operations read interface. Identity comparison includes target module and named interface; an unnamed interface is represented by its module alone. Exact assertions remain; no containsAll/subset weakening.

Cumulative plan section 8 gains this path. Corrected arithmetic is **37 = 25 new + 12 existing (8 production + 2 existing tests + POM + CI)**. The 25 new paths still comprise 15 production paths, eight test classes and two fixtures. Plan artifacts remain separate from the implementation manifest.

Only this new artifact, the cumulative plan and the exact existing test path may change in this task. Historical fix-1, production, POM, CI, fixtures, implementation artifacts, all other tests, Control, reviews and frozen files are preserved. Pre-task tracked/unignored file hashes are captured outside the repository in `/tmp/pa33-correction1-before.json`; generated Maven output is not a source write.

## 3. Unchanged behavior and inventory

One approved **S1** slice remains. No endpoint, behavior, public contract, schema, grant, production path, test class or method identity is added. The **106 new identities**, **70/644 + 29/476 = 99/1120**, token **1121**, and POM include count **29** remain unchanged. The frozen files and Appendix A/B inventories are not edited. Historical fix-1 arithmetic remains historical evidence, not a live manifest.

## 4. Focused validation and residual state

Run from `backend` exactly:

```bash
JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH mvn \
  -Dmaven.repo.local=/root/.m2/repository -B -ntp \
  -DargLine=-javaagent:/root/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.15/byte-buddy-agent-1.18.15.jar \
  -Dtest=CustomerEvidenceBoundaryTests,ModuleBoundaryTests,OperationsSubjectReadBoundaryTests test
```

Result: **BUILD SUCCESS**, exit **0**, completed 2026-11-14 at 19:18:37 +08:00. Surefire XML confirms **CustomerEvidenceBoundaryTests 4/4**, **ModuleBoundaryTests 9/9**, **OperationsSubjectReadBoundaryTests 8/8**: **21 tests, zero failures, errors or skips**. Command output is retained outside the repository at `/tmp/pa33-correction1-boundary-tests.log`.

Static checks confirm section 8 has exactly **37** paths (**25 new / 12 existing**); only the three authorized paths differ from the pre-task tracked/unignored SHA256 inventory. Every other recorded file, including historical fix-1 and frozen files, is unchanged. Appendix A/B remain unchanged against HEAD. Word-boundary `@Test` token count is **1121**. An initial overly broad substring scan counted `@Testcontainers` too (1147); the corrected `@Test\b` scan passes. Scoped `git diff --check` passes with only existing LF-to-CRLF advisories. Git uses the existing explicit Linux git-dir/work-tree override; metadata is not repaired or changed.

This correction does not rerun or claim full clean, PostgreSQL/Failsafe, JAR, CI or full inventory acceptance. Resumed Controller full validation remains required; existing deployment, role-membership, key-custody and capacity residuals are unchanged. No staging, commit, push, PR, merge, approval, ready action, reviewer request, comment, issue mutation or review initiation is authorized or performed.

plan correction complete; stop for resumed full validation
