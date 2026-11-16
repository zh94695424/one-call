# PA36 S1 implementation fix1

- Task: `pa36-evidence-role-ivr-acl-s1-fix1-gpt-20261116-01`.
- Date: 2026-11-16 (Asia/Shanghai).
- Gate: bounded `implementation fix1`, not code review or production acceptance.
- Authority: Active PA36 Control and accepted plan SHA256 `BD50534FA7EEF6E7288D6E5C1854F5C67499697EEF155A1C8E2D29567DE6555C`, section 6's 43-path allowlist, implementation report and this expressly authorized artifact.
- Completion: scoped fixes implemented and compiled; ordinary focused tests pass; **runtime acceptance BLOCKED**, not a passed fix gate. Stop here, without entering code review.

## Scope and exact changed paths

Only these paths change relative to fix1 entry:

1. `backend/src/test/java/com/onecall/archtests/container/EvidenceRoleIvrAssociationAclIT.java`.
2. `docs/gateflow/pa36-evidence-role-ivr-association-acl-s1-implementation.md`.
3. `docs/gateflow/pa36-evidence-role-ivr-association-acl-s1-fix-1.md` (new).

The existing cumulative implementation still has 43 implementation paths; this fix adds no production path. No production datasource, pool, role, permission, auth boundary, dependency, product behavior, API or accepted semantics change. V001–V018, PA25 SQL, production Java/configuration, Control, plan, reviews, OpenAPI and private `.codex` remain byte-identical to fix1 entry. No staging, commit, branch/config mutation, push, PR/issue mutation, external comment, approval, merge or reviewer request occurs. Git's Windows pointer is not rewritten; read-only commands use `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD"` with `GIT_OPTIONAL_LOCKS=0`.

## Root causes and fixes

### Synthetic active caller identities

The PA36 seed's non-null association cases create distinct active call records but reuse `repeat('a',64)`. V005 requires lowercase 64-hex HMACs and has the valid partial unique index `uq_call_record_active_caller` for active callers. This fixture collision aborts three methods before their intended ACL/business-preservation assertions.

Each inserted call now binds its own deterministic synthetic 64-hex caller HMAC, formatted from its distinct loop index (`%064x`). Call/session/source identities and nullable association cases remain unchanged. No valid uniqueness constraint is removed, no duplicate is caught, and no call is made terminal to evade the index. The three affected methods keep all existing assertions and identities.

### Real shared datasource context

`ProductionDatabaseIdentityProvisioningIT.startApplication()` builds the real application with `persistence` and `web` profiles and server-managed datasource properties, retrieves the real Hikari pools and constrains them to one connection for restoration probes. It covers six legacy pools; it does not provide a separate Operations datasource factory to copy. `OperationsSubjectReadPersistenceConfiguration` itself owns the existing Operations datasource/DSL/transaction/context beans and is guarded by both `@Profile("persistence")` and the existing enabled property. The minimal PA36 context set only the property, so the profile condition excluded the entire configuration.

Activate only `persistence` before registering/refreshing that same production configuration. No full application/web profile, duplicate bean, fake datasource or new production configuration is needed. Constrain the resulting real pool to one connection, preserving the existing exactly-one-Hikari assertion, seven non-inheriting SET role exercises, read-only/repeatable-read probes, and normal/exception/fault restoration checks. The topology remains seven logins, nine monitored effective roles and eleven graph edges; the existing context still has no production IVR enum member.

### PA25 line-709 metadata drift

The metadata loop commits one installed-rank-18 mutation on an admin connection, then runs PA25 apply/check on separate psql connections. PA25 independently compares version, script, checksum, rank and description against its exact history18 oracle. Its rejection was already occurring; the old assertion then incorrectly required Flyway `validate()` to throw for every mutation, including `script='wrong.sql'`.

Inspection of the installed Flyway 12.4.0 `MigrationInfoImpl.validate()` bytecode confirms versioned migrations validate type/checksum/description, not equality of the applied `script` field. Thus script-only mutation does not imply a Flyway throwable. No source file was mutated or repaired between rejection and validation, and the surrounding `finally` restores the exact saved JSON history row only after the assertions.

All three mutations still must fail both PA25 modes, now specifically with `PA25_HISTORY_REJECTED`, nonzero exits, no success markers, sanitized diagnostics and unchanged post-rejection snapshots. Checksum/description drift additionally retain the Flyway-exception assertions. Script drift is rejected by the intended exact PA25 oracle, not by an invented Flyway contract. No success is accepted as a PA25 rejection and no throwable is manufactured. The shared rejection helper also excludes success markers for the existing other negative cases. Production verifier and migration behavior are unchanged.

## Required validation and evidence

Accepted Controller **pre-fix** facts remain: Surefire 56/56; `ProductionDatabaseIdentityProvisioningIT` 20/20; focused Failsafe 36 with exactly 1 failure and 4 errors in PA36 (11/16 successful). Controller PostgreSQL16.15 fresh/upgrade/Flyway/package proofs are not relabeled as Docker-blocked or post-fix evidence.

The following exact focused selection ran once, serially, from `backend`, using the existing Linux shim and offline dependencies:

```sh
JAVA_HOME=/opt/jdk21 /tmp/pa36-tools/mvnw -B -ntp -o \
  -Dtest=EvidenceRoleIvrAssociationAclBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerEvidenceBoundaryTests,CustomerSubjectActivationBoundaryTests,CustomerSubjectClaimBoundaryTests,CustomerSubjectDetailBoundaryTests,OperationsActivationReadBoundaryTests,OperationsLifecycleBoundaryTests,OperationsSubjectReadBoundaryTests \
  -Dit.test=EvidenceRoleIvrAssociationAclIT,ProductionDatabaseIdentityProvisioningIT -Pcontainer-smoke clean verify
```

- Exit **1**, Failsafe verify; 78 seconds; finished `2026-11-16T13:20:42+08:00`.
- Main/test compilation: 262/149 sources; enforcer and packaging pass.
- Fresh Surefire XML: 9 classes, **56 tests, 0 failures, 0 errors, 0 skipped**.
- Fresh Failsafe XML: 2 classes, **36 tests, 0 failures, 36 setup errors, 0 skipped**; PA36 16 errors and PA25 20 errors. No IT body executes, so none of the corrected methods is claimed to pass.
- Exact blocker: `docker info` exits 1 with `permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`; Testcontainers reports `Could not find a valid Docker environment`, followed by cached discovery failures. Sandbox approval policy is never; no daemon/socket/permission change or workaround is attempted. No duplicate PostgreSQL suite is run.
- CI source/POM static extraction: `python3 /tmp/pa36-ci-static-extracted.py`, cwd `backend`, exit **0**. Current 77/712 +32/520 =109/1232 identities, 1233 annotation lines, historical start arithmetic and 32 Failsafe includes remain unchanged. This is static evidence, not full XML acceptance.
- Read-only `git diff --check`, exit **0**, only existing LF-to-CRLF conversion warnings; no file normalization. Entry-byte snapshot/scope comparison confirms exactly the three authorized changes, no deletion or frozen-path change. Test method identities remain 16/20 and ordinary methods remain 8.

Evidence directory: `/tmp/pa36-fix1-evidence/` contains the exact command, Maven exit/log, fresh XML count JSON, archived fresh XML, archived Controller pre-fix IT XML/text, Flyway bytecode inspection, CI-static exit/log, diff check, entry-byte manifest, authorized scope report and current 43-path manifest. Current implementation-manifest SHA256: `ea68090e73c04641c6113103bd102936f709e4908a699d5c5b9e9b2df2475df3` (reports excluded). Initial-author `/tmp/pa36-final-evidence/` remains historical, not fix1 acceptance.

## Accepted and uncovered residuals

- **Uncovered/blocking fix acceptance:** a working authorized Docker/PG16 environment must run the same focused command to prove the corrected five methods and PA25 20/20 together. Static reasoning/compilation and pre-fix positives cannot substitute for post-fix runtime proof. No passed gate is claimed.
- **Unchanged accepted scope:** one real shared Operations pool, seven non-inheriting SET targets, seven/nine/eleven topology metrics, evidence eight-column/eighteen-tuple ACL and PA25 exact410/history18 remain intact. Existing role-wide read amplification and old-role writes remain accepted design residuals, not newly granted capabilities.
- **Not executed here:** full 1232-case CI/XML, launcher/transport, all production credentials/TLS/pg_hba/rotation/session-drain/marker custody and exclusive cluster administration acceptance; existing plan/report owners retain these. No production readiness or PA35 resumption is claimed.
- **Completion status:** bounded code/report fix complete, required command attempted once and transparently blocked at runtime; stop at fix1 with no code-review entry or external mutation.
