# PA34 Operations activation-history read foundation - S1 implementation

Current author-fix task: `pa34-operations-activation-history-pr-fix1-gpt-20261115-01`; prior author-finalization task: `pa34-operations-activation-history-code-review-rereview-fix1-finalize-gpt-20261115-02`.

Current update: 2026-11-15 (Asia/Shanghai). Historical fix1 task: `pa34-operations-activation-history-code-review-fix1-gpt-20261115-01`, reported at 2026-11-15T08:54:55+08:00.

Historical implementation task: `pa34-operations-activation-history-s1-impl-resume3-gpt-20261115-01`; reported at 2026-11-15T07:06:54+08:00. Sections1-10 retain that implementation evidence/history except the current ERROR-registration clarification; section11 records the later accepted code-review fixes and their new validation. Earlier evidence is not substituted for this fix's runs.

Status: fix2's four-test candidate is preserved at the Controller-validated hashes; section12 finalizes the missing author documentation using Controller evidence after provider recovery. Sections1-11 retain their historical results, including the then-unresolved index observation; section12 records the subsequent Controller adjudication without rewriting that history. This is author finalization for Controller intake only, not independent review, finding acceptance, gate approval, live acceptance or production readiness. No self-review or later gate is initiated.

Current PR-fix annotation: section13 records the subsequent bounded PR #45 author fix. Sections1-12 retain their historical evidence and gate boundaries, not the status of this new validation round.

## 1. Authority, checkpoint and bounded continuation

The accepted cumulative PA34 plan and Controller-owned `docs/gateflow/project-execution-control.md` govern this work. Historical implementation HEAD was `413ad7fe139fb3f08cb001c01607fbd305083f47`; current fix dispatch HEAD is `08425f79f3bb844a39093440306f41f524e4c607`. The historical cumulative plan raw SHA256 was `6afe1d7a6f14f69d628694b3bd9ef9f088f08a1e32a0378161a8449e0922ee69`.

This continuation finishes the inherited uncommitted implementation rather than replacing the approved design. Original partial production, test, POM and CI paths were audited against the accepted route, records, employee authority, shared cursor ring, one-snapshot read-only transaction, owner-local SELECT, frozen inputs and exact identities. The prior resume2 TRACE500 failure and its diagnostic evidence remain historical failures, not successful validation. This report relies on new successful runs in evidence directory `D:\codex-temp\pa34-implementation-evidence-20261115-04` (WSL `/mnt/d/codex-temp/pa34-implementation-evidence-20261115-04`).

`PA34-IMPL-CORRECTION-01` adds exactly `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsWebConfiguration.java` to the original 23 paths. Effective scope is 24 paths: 19 new / five existing; eight new production Java, ten new test/helper Java, three existing production Java, POM, CI and this sole documentation path. No other second file, controller, connector, contract, migration, grant or production verifier was added or changed.

## 2. Implementation and the narrow TRACE correction

- The sole new endpoint is `GET /api/v1/operations/activations`. Its page has exactly `items`, `nextCursor`, `hasMore`; its item has exactly `transitionId`, `subjectId`, `statusEventId`, `fromStatus`, `toStatus`, primitive `fromRevision`/`toRevision`, `sourceChannel`, `sourceActionDomain`, `reasonCode`, `occurredAt`. Tail `nextCursor` is present and null. No association/enrichment or extra endpoint is introduced.
- Existing independent employee verification, current ACTIVE ADMIN/OPS authority, exact raw-URI admission, subject GETs and lifecycle POST behavior remain intact. Authentication and authoritative lifecycle checks precede activation parsing/configuration/reader work. Only cursor/limit and the frozen error precedence are used.
- Activation continuation reuses the unchanged subject-read key configuration/ring, with endpoint-specific AEAD purpose/AAD, fixed sort and empty-filter digest. Tokens are randomized, opaque, canonical and bounded; limit is unbound. Missing/bad configuration fails closed even on first/empty pages. No separate activation secret, cursor table or production credential work is added.
- The existing qualified REQUIRES_NEW / REPEATABLE_READ / enforced read-only / five-second transaction and context enclose IAM authority and one bounded eleven-column activation-owner SELECT. Role handoffs/restoration, physical connection/backend/snapshot identity, deadlines, rollback/cleanup/eviction and no business/audit/Outbox changes are exercised. Every raw row, including lookahead, is validated; only five domain-legal shapes succeed. PostgreSQL UUID byte ordering uses unsigned word comparison. No count, offset, joins, locks, writes, row dropping or partial page is used.
- The already-added PostgreSQL space-only `23514` assertion in `allNinePersistableTransitionCombinationsReturnAllEnums` now genuinely executes in both focused and full runs. Tab-only text is preserved as valid PostgreSQL-btrim text; astral character bounds use code points. No identity was added to obtain this coverage.

The original `operationsBoundaryFilterRegistration` retains its exact `/api/v1/operations/*` wildcard, `DispatcherType.REQUEST`, `Ordered.HIGHEST_PRECEDENCE`, verifier wiring and original configuration-guard content. A separate `operationsErrorBoundaryFilterRegistration` uses `DispatcherType.ERROR` at the same highest precedence. As corrected in PR re-review2 fix3, its URL comes only from supported `spring.web.error.path` with `/error` as the only fallback; removed `server.error.path` is not effective. Malformed effective paths fail before registration with the fixed neutral message `Invalid error dispatch path`. This supersedes the earlier hard-coded default and subsequent legacy-key registration. It does not register ERROR on all Operations requests or alter the original REQUEST registration. The unchanged customer error controller's legacy-key coordination remains a Platform/Backend-owned residual outside this work unit.

At the start of `OperationsBoundaryFilter`, ERROR dispatches inspect only `RequestDispatcher.ERROR_REQUEST_URI` and `RequestDispatcher.ERROR_METHOD`. Only the exact original context-relative `/api/v1/operations/activations` with original method TRACE is written using the unchanged Operations writer and `NOT_FOUND`/404 envelope. The branch returns before normal REQUEST initialization, authentication, service, cursor/configuration or owner work. All unrelated ERROR dispatches chain the original request/response unchanged, before any filter-owned attribute/header modification. Error attributes confer no authority and supply no response data. A fresh server-generated UUID is used; no exception/message attribute is exposed.

`CustomerApiErrorController`, existing problem writers, connector settings and customer error dispatch behavior remain unchanged. TRACE is not enabled globally. Additional packaged probes demonstrate exact TRACE404 while unrelated customer/subject/trailing-slash TRACE dispatches retain their existing sanitized500 result. No new response property or problem code is introduced.

## 3. Environment and execution accommodations

Versions actually used: Java Temurin21.0.13; Maven3.9.9; Spring Boot4.1.2; embedded Tomcat11.0.27; Node22.23.4; npm10.9.9; Docker client/daemon29.1.3; PostgreSQL16.15 (`postgres:16`, image digest `sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`). Full build compiles 262 production and147 test/helper source files. Version/Docker/image evidence is in `versions.log` and `environment-details.log`; Docker info and image inspection exit0.

The Windows-linked `.git` pointer is not rewritten. Linux git inspection uses `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call18` and `GIT_WORK_TREE` equal to the real worktree. The index is unchanged and no paths are staged.

All Maven invocations source the authorized external shim rather than modifying CRLF `backend/mvnw`:

```bash
source /mnt/d/codex-temp/pa34-implementation-evidence-20261115-01/mvnw-shim.sh
export JAVA_HOME=/opt/jdk21 MAVEN_HOME=/opt/maven-3.9.9
export PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH
export BASH_ENV=/mnt/d/codex-temp/pa34-implementation-evidence-20261115-01/mvnw-shim.sh
```

`BASH_ENV` supplies that same shim to the unmodified CI body's child Bash process; pinned Linux Java/jar tools are used for packaged smokes. No wrapper, POM dependency/plugin, runtime identity or production configuration workaround is introduced.

## 4. Exact Java commands and results

Executed accepted section9.1 commands from the real repository with the shim above; Maven commands retain the listed focused-then-full order:

```bash
docker info
cd backend
./mvnw -B -ntp -v
./mvnw -B -ntp -Dtest=OperationsActivationReadWebTests,OperationsActivationReadConfigurationTests,OperationsActivationReadRequestTests,OperationsActivationCursorCodecTests,OperationsActivationReadServiceTests,OperationsActivationReadBoundaryTests,ModuleBoundaryTests,OperationsSubjectReadBoundaryTests,OperationsLifecycleBoundaryTests,OperationsSubjectReadWebTests,OperationsSubjectsWebTests test
./mvnw -B -ntp -Pcontainer-smoke -Dtest=OperationsActivationReadWebTests,OperationsActivationReadConfigurationTests,OperationsActivationReadRequestTests,OperationsActivationCursorCodecTests,OperationsActivationReadServiceTests,OperationsActivationReadBoundaryTests -Dit.test=OperationsActivationReadsIT,OperationsActivationReadRoleLeastPrivilegeIT verify
./mvnw -B -ntp -Pcontainer-smoke clean verify
cd ..
```

| Validation | Result | Exit | Evidence |
| --- | --- | --- | --- |
| Toolchain / Docker prerequisites | Correct pinned versions; real Docker and postgres16 available | 0 | versions.log; environment-details.log |
| First focused unit/boundary command | 113 tests; zero failures/errors/skips | 0 | focused-unit.log |
| Focused container-smoke verify | 60/60 Surefire +28/28 Failsafe; zero failures/errors/skips | 0 | focused-container.log |
| Focused source/XML identities | Exact eight classes /88 identities; no extras/duplicates/skips | 0 | focused-xml.log; focused-xml/ |
| Accepted plan/source/CI/focused XML | Exact same60+28 identities; two helpers have zero ordinary tests | 0 | accepted-identities.log |
| Full clean container-smoke verify | 76 suites/704 Surefire +31 suites/504 Failsafe; zero failures/errors/skips | 0 | full-clean-verify.log; full-surefire-xml/; full-failsafe-xml/ |
| Exact current CI inventory/history/POM/XML body | 107 suites/1208 identities;1209 line tokens;31 POM includes; all88 additions retained | 0 | exact-ci-bodies.log; full-xml-inventory.json |
| Exact default and web JAR smoke body | Default starts/exits without web/DB resources; web has no DB/verifier adapter; bounded processes cleaned up | 0 | exact-ci-bodies.log; boot.log; web-boot.log |
| Exact production dependency firewall body | Runtime and packaged dependency sets and adversarial probes pass without baseline changes | 0 | exact-ci-bodies.log; jar-index.txt; pa11-runtime-dependencies.txt |
| Additional packaged activation/error probes | 23/23; includes HEAD404, TRACE404 with/without bearer and unchanged unrelated ERROR dispatch behavior | 0 | activation-and-error-dispatch-probes.log; additional-web-boot.log |
| Exact A1-A5 stage block in authorized LF projection | Every command/stage succeeds; limitation retained below | 0 | contracts-a1-a5.log |
| Frozen, scope, private helpers and hygiene before report | 21/21 frozen bytes; exact17 migrations;23 implementation paths before adding report, all within24;17 private helpers unchanged | 0 | post-validation-audit.log |

Both focused and full runs execute `realHttpMethodsAndCanonicalizationVariantsStayConcealed` and the final btrim assertion. Existing `OperationsLifecycleConfigurationTests.webBoundaryUsesIndependentVerifierAndExactFilterOrder` passes unchanged in the full run. Full XML, not a focused aggregate, is authoritative for107/1208. No failing/skipped run is treated as passing.

## 5. Exact CI bodies, historical arithmetic and XML inventory

The accepted section9.2 extractor below executes the current CI bodies verbatim. It runs only after full clean verify succeeds; every `subprocess.run(..., check=True)` returns0. The complete run and full XML copies are retained externally, including a per-suite JSON inventory and equality of every focused identity with its full XML identity.

```bash
python3 - <<'PY'
from pathlib import Path
import subprocess
lines=Path('.github/workflows/ci.yml').read_text().splitlines()
for name in ('assert container smoke actually ran',
             'default and opt-in web jar smokes',
             'production jar dependency firewall'):
    marker='      - name: '+name
    found=[i for i,line in enumerate(lines) if line==marker]
    assert len(found)==1,name
    start=found[0]+1
    assert lines[start]=='        run: |',(name,lines[start])
    body=[]
    for line in lines[start+1:]:
        if line and not line.startswith('          '): break
        body.append(line[10:] if line else '')
    assert body,name
    subprocess.run(['bash','-e','-o','pipefail','-c','\n'.join(body)],cwd='backend',check=True)
PY
```

PA34 exact source=CI=POM=XML inventory passed: 76/704 + 31/504 = 107/1208; historical 112+178=290 -> PA19-start 128+192=320 -> PA20-start 162+198=360 -> PA21-start 196+204=400 -> PA22-start 231+214=445 -> PA23-start 266+214=480 -> PA25-start 309+214=523 -> PA26-start 309+234=543 -> PA27-start 309+246=555 -> PA28-start 341+272=613 -> PA29-start 375+302=677 -> PA30-start 417+336=753 -> PA31-start 468+370=838 -> PA32-start 520+404=924 -> PA33-start 574+440=1014 -> PA34-start 644+476=1120 -> current 704+504=1208; line scans 291 -> 321 -> 361 -> 401 -> 446 -> 481 -> 524 -> 544 -> 556 -> 614 -> 678 -> 754 -> 839 -> 925 -> 1015 -> 1121 -> 1209; POM 31 includes; zero skips/errors/failures

Firewall output (original historical label retained, not a weaker replacement):

```text
PA11 firewall passed: baseline37/34, ssm72/65, counterparts64, dependency-only8, packager1; deltas35/0 and31/0; 116 adversarial probes
```

Packaged application JAR SHA256: `374c660a18595e6fa3198d3112f0050e49f26d6e8ff2320c5a2a4c97c6ebdadc`. No test fixture or production employee verifier is packaged. The exact firewall checks the resolved runtime dependencies, packaged libraries/classes, test-only absence and existing adversarial cases; its baselines and POM dependencies/plugins are unchanged.

PA34 additions remain exactly six ordinary unit classes (12,8,8,10,14,8 =60) and two IT classes (18,10 =28). Names with historical references to nine shapes or endpoint properties are retained; their corrected assertions exercise five legal/four corrupt shapes and the shared ring. `accepted-identities.log` enumerates all88 fully-qualified identities and proves accepted plan = source = CI = focused XML. The current CI body additionally proves full source = CI = POM = full XML, preserving every preexisting identity and all historical subtraction checks.

Complete full-run XML suite inventory follows. Every row has failures=0, errors=0, skips=0; the107 original XML documents and method names are in the two external XML directories.

| Runner | Fully-qualified suite | Tests | Failures | Errors | Skips |
| --- | --- | --- | --- | --- | --- |
| surefire | `com.onecall.archtests.CustomerBusinessNumbersWebTests` | 12 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.CustomerEvidenceConfigurationTests` | 6 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.CustomerEvidenceWebTests` | 12 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.CustomerMeWebTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.CustomerSharesCommandWebTests` | 4 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.CustomerSharesWebTests` | 10 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.CustomerSubjectActivationConfigurationTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.CustomerSubjectActivationsWebTests` | 12 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.CustomerSubjectClaimsWebTests` | 12 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.CustomerSubjectDetailConfigurationTests` | 6 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.CustomerSubjectsWebTests` | 12 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.CustomerWebConfigurationTests` | 6 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.EvidenceMediaReadPersistenceConfigurationTests` | 4 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.MediaGatewayConfigurationTests` | 4 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.MediaGatewayWebTests` | 18 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.MediaPlaybackConfigurationTests` | 19 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.OneCallApplicationTests` | 1 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.OperationsActivationReadConfigurationTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.OperationsActivationReadWebTests` | 12 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.OperationsLifecycleConfigurationTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.OperationsSubjectReadConfigurationTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.OperationsSubjectReadWebTests` | 16 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.OperationsSubjectsWebTests` | 12 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.PublicSharePersistenceConfigurationTests` | 2 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.PublicShareWebTests` | 12 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.SharingCommandPersistenceConfigurationTests` | 2 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.SharingPersistenceConfigurationTests` | 2 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.SubjectClaimPersistenceConfigurationTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.activation.CustomerSubjectActivationServiceTests` | 18 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.activation.OperationsSubjectLifecycleServiceTests` | 18 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests` | 1 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.architecture.CustomerApiBoundaryTests` | 3 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.architecture.CustomerBusinessNumberBoundaryTests` | 4 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.architecture.CustomerEvidenceBoundaryTests` | 4 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.architecture.CustomerSubjectActivationBoundaryTests` | 6 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.architecture.CustomerSubjectClaimBoundaryTests` | 6 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.architecture.CustomerSubjectDetailBoundaryTests` | 4 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.architecture.ModuleBoundaryTests` | 9 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.architecture.OperationsActivationReadBoundaryTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.architecture.OperationsLifecycleBoundaryTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.architecture.OperationsSubjectReadBoundaryTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.architecture.SharingQueryBoundaryTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.entitlement.ActivationTrialServiceTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.evidence.ControlledMediaByteSourceTests` | 4 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.evidence.EvidenceMediaObjectReaderTests` | 4 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.sharing.CustomerShareCommandServiceTests` | 6 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.sharing.MediaPlaybackCredentialAuthorityTests` | 24 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.sharing.MediaPlaybackServiceTests` | 21 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.sharing.PublicShareQueryServiceTests` | 4 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.sharing.ShareAdmissionServiceTests` | 26 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.sharing.SharingPlaybackStateReaderTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.sharing.SingleByteRangeTests` | 6 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.subjectnumber.CustomerSubjectClaimServiceTests` | 17 | 0 | 0 | 0 |
| surefire | `com.onecall.archtests.subjectnumber.SubjectClaimVerificationReferenceTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryServiceTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.core.customer_iam.internal.application.CustomerContextServiceTests` | 6 | 0 | 0 | 0 |
| surefire | `com.onecall.core.customer_iam.internal.application.CustomerEvidenceQueryServiceTests` | 12 | 0 | 0 | 0 |
| surefire | `com.onecall.core.customer_iam.internal.application.CustomerSubjectDetailQueryServiceTests` | 12 | 0 | 0 | 0 |
| surefire | `com.onecall.core.customer_iam.internal.security.CustomerSessionBoundaryTests` | 7 | 0 | 0 | 0 |
| surefire | `com.onecall.core.customer_iam.internal.web.CustomerBusinessNumberCursorCodecTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.core.customer_iam.internal.web.CustomerEvidenceCursorCodecTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.core.customer_iam.internal.web.CustomerShareCursorCodecTests` | 6 | 0 | 0 | 0 |
| surefire | `com.onecall.core.operations_iam.internal.activationread.OperationsActivationCursorCodecTests` | 10 | 0 | 0 | 0 |
| surefire | `com.onecall.core.operations_iam.internal.activationread.OperationsActivationReadRequestTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.core.operations_iam.internal.activationread.OperationsActivationReadServiceTests` | 14 | 0 | 0 | 0 |
| surefire | `com.onecall.core.operations_iam.internal.security.EmployeeSessionBoundaryTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectCursorCodecTests` | 12 | 0 | 0 | 0 |
| surefire | `com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadRequestTests` | 10 | 0 | 0 | 0 |
| surefire | `com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests` | 16 | 0 | 0 | 0 |
| surefire | `com.onecall.core.sharing.internal.application.CustomerShareQueryServiceTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.core.sharing.internal.keysource.FileMediaPlaybackSigningKeyProviderTests` | 10 | 0 | 0 | 0 |
| surefire | `com.onecall.core.sharing.internal.keysource.MediaPlaybackFileKeySourceIntegrationTests` | 8 | 0 | 0 | 0 |
| surefire | `com.onecall.core.sharing.internal.keysource.MediaPlaybackKeyBundleParserTests` | 12 | 0 | 0 | 0 |
| surefire | `com.onecall.core.sharing.internal.keysource.MediaPlaybackTencentKeySourceIntegrationTests` | 10 | 0 | 0 | 0 |
| surefire | `com.onecall.core.sharing.internal.keysource.TencentSsmMediaPlaybackSigningKeyProviderTests` | 18 | 0 | 0 | 0 |
| surefire | `com.onecall.core.sharing.internal.keysource.TencentSsmSecretStringClientTests` | 8 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.ActivationCommandRoleLeastPrivilegeIT` | 10 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.CustomerBusinessNumberReadPrivilegeIT` | 8 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.CustomerBusinessNumbersIT` | 18 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.CustomerEvidenceIT` | 24 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.CustomerEvidenceReadPrivilegeIT` | 10 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.CustomerMeIT` | 10 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.CustomerShareCommandsIT` | 18 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.CustomerSharesIT` | 8 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.CustomerSubjectActivationsIT` | 24 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.CustomerSubjectClaimsIT` | 24 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.CustomerSubjectDetailReadPrivilegeIT` | 10 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.CustomerSubjectsIT` | 20 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.DatabaseRuntimeAclRbacHardeningIT` | 12 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.EvidenceMediaReadIT` | 8 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.EvidenceMediaReadRoleLeastPrivilegeIT` | 8 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.MediaGatewayTransportIT` | 8 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.MediaPlaybackIT` | 9 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.OperationsActivationReadRoleLeastPrivilegeIT` | 10 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.OperationsActivationReadsIT` | 18 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.OperationsLifecycleRoleLeastPrivilegeIT` | 12 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.OperationsSubjectLifecycleIT` | 24 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT` | 12 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.OperationsSubjectReadsIT` | 24 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.PostgresContainerIT` | 64 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.ProductionDatabaseIdentityProvisioningIT` | 20 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.PublicShareAdmissionIT` | 42 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.PublicSharesIT` | 8 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.SharingAdmissionRoleLeastPrivilegeIT` | 9 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.SharingQueryServiceIT` | 10 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.SharingRoleLeastPrivilegeIT` | 12 | 0 | 0 | 0 |
| failsafe | `com.onecall.archtests.container.SubjectClaimRoleLeastPrivilegeIT` | 10 | 0 | 0 | 0 |

## 6. Exact packaged activation and ERROR-dispatch probes

The exact CI web smoke checks the exact five-key problem envelope, fixed type/title/status/code, fresh canonical UUID, no-store/no-cache/nosniff, 401 challenge only, no cookies and no raw diagnostics. Activation probes are:

| Method / exact path | Authorization | Expected and observed |
| --- | --- | --- |
| GET /api/v1/operations/activations | absent | 401 UNAUTHENTICATED |
| GET /api/v1/operations/activations | opaque bearer, no packaged verifier | 503 SERVICE_UNAVAILABLE |
| GET /api/v1/operations/activations?unknown=x | absent | 401 UNAUTHENTICATED |
| POST, PUT, PATCH, DELETE, OPTIONS, TRACE /api/v1/operations/activations | absent | each404 NOT_FOUND |
| GET /api/v1/operations/activations/ | absent | 404 NOT_FOUND |
| GET /api/v1/operations/activations;x | absent | 404 NOT_FOUND |
| GET /api/v1/operations/activations/extra | absent | 404 NOT_FOUND |
| GET /api/v1/operations//activations | absent | 404 NOT_FOUND |
| GET /api/v1/operations/%61ctivations | absent | 404 NOT_FOUND |
| GET /api/v1/operations/activations%3b | absent | 404 NOT_FOUND |
| GET /api/v1/operations/activations%25 | absent | 404 NOT_FOUND |

The separate23-probe packaged run repeats these16 probes, adds HEAD exact activation404 (correctly no HTTP body), TRACE exact activation with opaque bearer404, TRACE customer/me500 INTERNAL, TRACE Operations subjects500 INTERNAL, TRACE activation trailing slash500 INTERNAL, GET /error404 NOT_FOUND and GET customer/me401 UNAUTHENTICATED. Unrelated TRACE500 is the unchanged shared error-controller result, not an activation success or a newly introduced error policy. This intentionally proves the correction is not a broad ERROR concealment rule or global TRACE switch. Its JSON-bearing responses also satisfy exact safe envelope/headers and unique traces; no Hikari pool starts. Its Java command is:

```bash
cd backend
java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar --spring.profiles.active=web --spring.main.web-application-type=servlet --server.port=0
```

Startup/request/terminate/kill limits are60/10/15/5 seconds respectively. The additional JVM is always cleaned up in finally; full probe rows, assertions' successful completion and process return code are in `activation-and-error-dispatch-probes.log`. Authenticated real-PG16 concealment, including HEAD/OPTIONS/TRACE and all accepted aliases, also passes in the unchanged IT identity.

## 7. Exact A1-A5 commands and the LF-projection limitation

Run root: authorized external `D:\codex-temp\pa34-a1a5-lf-20261115-01` (WSL `/mnt/d/codex-temp/pa34-a1a5-lf-20261115-01`), not a normalized main worktree. The projection's docs/14 LF SHA256 is `bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58`; verified again before execution. Main worktree docs/14 raw SHA256 remains `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31`. Existing Windows raw CRLF bytes were not rewritten to satisfy LF-sensitive validators.

Executed exact accepted section9.3 block with Bash errexit/pipefail and command tracing; aggregate exit0 means every command returned0:

```bash
node --version | grep -Fx v22.23.4
(cd api/openapi && npm ci && npm run check:s3)
node protocol/ivr/validate.mjs --stage s3 \
  --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures \
  --spec docs/16-production-ivr-protocol-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s3 \
  --model security/v1/security-design-v1.json --fixtures security/v1/fixtures \
  --spec docs/17-production-security-design-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures
node deployment/capacity/validate.mjs --stage s2 \
  --template deployment/capacity/inputs.template.json --fixtures deployment/capacity/fixtures
for stage in s1 s2; do
  extra=()
  if [ "$stage" = s2 ]; then extra+=(--check-doc-links); fi
  node acceptance/validate.mjs --stage "$stage" \
    --matrix acceptance/v1/acceptance-matrix-v1.json \
    --doc docs/19-production-acceptance-matrix.md --fixtures acceptance/fixtures \
    --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
    --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
    --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures \
    --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures \
    --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures \
    "${extra[@]}"
done
```

A1: lint, bundle, client generation/typecheck, document maps, media-gateway contract checks and S3 source map pass. A2 S3:25/25 fixtures, zero failures. A3 S3:23/23 fixtures, zero failures,23 deferred inputs retained. A4 S2:113/113 fixture cases,471 nonfinite probes, deterministic checks pass; productionReady/procurementReady/cloudVerified/a3RiskClosed remain false. A5 S1:3/3 fixtures; A5 S2:4/4 fixtures including doc links; zero unresolved/ambiguous/drift references, runtime_evidence=NOT_RUN and production_accepted=false. These are fixture/contract validators, not live acceptance or hosted-CI proof.

`npm ci` reports17 existing development-tool dependency vulnerabilities (one low, nine moderate, five high, two critical). No package or lockfile was changed and no audit-fix was attempted. Existing toolchain/dependency owners retain remediation separately; local validator success does not close that residual or establish production safety.

## 8. Frozen byte proofs, migration names, scope and hygiene

Before validation and again after full backend/JAR/firewall/contract/probe validation, the exact accepted section8 checker is executed against the real worktree, not the LF projection:

```bash
python3 - <<'PY'
import hashlib,re
from pathlib import Path
plan=Path('docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md').read_text()
entries=re.findall(r'^([0-9a-f]{64})  (.+)$',plan,re.M)
assert len(entries)==21 and len({p for _,p in entries})==21
for expected,name in entries:
    assert hashlib.sha256(Path(name).read_bytes()).hexdigest()==expected,name
actual={p.name for p in Path('backend/src/main/resources/db/migration').glob('*.sql')}
expected={Path(p).name for _,p in entries if '/migration/' in p}
assert actual==expected and len(actual)==17
print('21 frozen raw-byte hashes; exactly V001-V017; no V018')
PY
```

Results:21/21 raw SHA256 values match; migration basename set is exactly the17 accepted V001-V017 names, with no V018 or additional SQL file. `frozen-before.log`, `pre-report-audit.log` and `post-validation-audit.log` contain the complete hashes; `frozen-after.log` records the exact accepted checker again after all other validation, exit0. The architecture and real PostgreSQL tests independently execute the frozen checker as well. No migration, production role/ACL, public contract, shared read/cursor configuration or production identity file changed.

Exact effective correction allowlist and final implementation paths:

| Row | State | Complete path |
| --- | --- | --- |
| 01 | new | `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsActivationReadService.java` |
| 02 | new | `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsActivationReadPorts.java` |
| 03 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadRequest.java` |
| 04 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationCursorCodec.java` |
| 05 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadConfiguration.java` |
| 06 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/activationread/DefaultOperationsActivationReadService.java` |
| 07 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsActivationReadController.java` |
| 08 | new | `backend/src/main/java/com/onecall/core/activation_consent/internal/operationsread/JooqOperationsActivationHistoryReader.java` |
| 09 | existing | `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java` |
| 10 | existing | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` |
| 11 | new | `backend/src/test/java/com/onecall/archtests/OperationsActivationReadWebTests.java` |
| 12 | new | `backend/src/test/java/com/onecall/archtests/OperationsActivationReadConfigurationTests.java` |
| 13 | new | `backend/src/test/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadRequestTests.java` |
| 14 | new | `backend/src/test/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationCursorCodecTests.java` |
| 15 | new | `backend/src/test/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadServiceTests.java` |
| 16 | new | `backend/src/test/java/com/onecall/archtests/architecture/OperationsActivationReadBoundaryTests.java` |
| 17 | new | `backend/src/test/java/com/onecall/archtests/operations/OperationsActivationReadTestProviders.java` |
| 18 | new | `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadsIT.java` |
| 19 | new | `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadRoleLeastPrivilegeIT.java` |
| 20 | new | `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadFixture.java` |
| 21 | existing | `backend/pom.xml` |
| 22 | existing | `.github/workflows/ci.yml` |
| 23 | new | `docs/gateflow/pa34-operations-activation-history-read-foundation-implementation.md` |
| 24 | existing | `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsWebConfiguration.java` |

The external audit uses the23 literal accepted rows plus the single Controller-authorized configuration path, not package globs. Before report creation exactly23 implementation paths are present; this report supplies the24th. The17 preexisting private prompt/helper files (including `.codex`) are byte-identical to the continuation snapshot and are not counted as implementation paths or deleted. No new private helper is placed in the worktree; validation helpers and generated evidence stay in the authorized external directory.

Executed checks include strict UTF-8, no BOM/U+FFFD, final LF, no conflict markers, no trailing whitespace, tracked `git diff --check`, each untracked implementation file's `git diff --no-index --check /dev/null`, unchanged index bytes and no staged paths. Pre-report checks pass exit0 in `post-validation-audit.log`; report-inclusive checks are recorded in `final-audit.log` using:

```bash
python3 /mnt/d/codex-temp/pa34-implementation-evidence-20261115-04/final-audit.py
python3 /mnt/d/codex-temp/pa34-implementation-evidence-20261115-04/final-audit.py --require-report
```

## 9. Residuals and ownership

| Residual | Classification / owner | Boundary / destination |
| --- | --- | --- |
| PA34-PLAN-RISK-01: persisted domain-impossible/corrupt transition blocks any containing page, including corrupt lookahead | deferred-with-owner; Backend/DBA + Operations | Remains sanitized503, no row dropping/partial page. Immutable V007 and this read-only slice provide no repair. Separately authorized data-integrity/remediation runbook; no writes, trigger bypass, grant, migration or V018 here. |
| Production independent employee verifier, session issuance/MFA and provisioning | External prerequisite; Security/Operations IAM | Absent verifier fails503. No production fake adapter or credential/provisioning acceptance is supplied. |
| Runtime read-login role capability and credentials | External prerequisite; DBA/Backend | Existing explicit NOINHERIT/SET membership semantics only; missing capability fails503 without repair. Fixture provisioning is test-only, not production grants. |
| Shared read ring custody/rotation and nonce uniqueness | External prerequisite; Security/Backend | Existing subject-read key custody; endpoint-specific AAD/purpose, no separate operator secret. Missing/bad ring fails503 even first/empty pages. |
| Global occurredAt/id ordering may sort many rows with the subject-leading V007 index | Bounded performance residual; Backend/DBA | Existing deployment-capacity/readiness assessment; prepared limit+1 and timeouts may fail503 at scale. No index/migration or throughput/SLO claim. |
| RR revocation and continuation semantics | Existing semantics; Backend/Security | Current request uses its original snapshot; next request sees committed disable/append. No linearizable revocation or pinned cross-page snapshot. |
| Transport rejection before REQUEST filters | Existing connector/environment nuance; Backend | Only accepted exact activation TRACE is corrected through ERROR dispatch. Other malformed encodings may be connector400; unrelated error behavior is unchanged. |
| LF-only validator execution accommodation and hosted environment parity | Validation limitation; Controller/Backend CI | A1-A5 passed only in the authorized LF projection; frozen raw bytes pass in the real worktree. No raw Windows execution or hosted-CI result is invented. |
|17 existing npm development-tool vulnerabilities and existing validator deferred inputs | Existing residual; Contract/CI toolchain maintainers + Security | Reported, not fixed or waived by PA34; separate dependency remediation ownership remains. Do not confuse fixture success with production readiness. |

## 10. Controller handoff and stop

All mandatory local runtime/contract/inventory/probe validation succeeds before this authorized implementation report is created. Complete command output, exit codes, all107 XML suite documents, exact88-identity manifest, dependency/JAR evidence, source hashes and frozen/scope/hygiene evidence are in the external04 directory. Earlier01-03 evidence is retained as history and is not substituted for current passing results.

This continuation changes only the two authorized correction production files and adds this report to the inherited partial implementation; the final cumulative implementation set is exactly the24 paths above. The Controller checkpoint, accepted plan, previous reports/reviews, private helpers, existing tests/fixtures, shared customer error controller and frozen inputs remain unchanged.

No staging, commit, branch creation, push, PR/comment, approval, review request, self-review or code-review entry is performed. Stop for Controller intake. No production readiness is asserted.


## 11. Code-review fix1 evidence (2026-11-15)

Task: `pa34-operations-activation-history-code-review-fix1-gpt-20261115-01`; dispatch HEAD `08425f79f3bb844a39093440306f41f524e4c607`. Authority is the unchanged Controller PA34 section, accepted plan and reviews A `docs/reviews/code-review-20261115-071731.md` and B2 `docs/reviews/code-review-20261115-075611.md`. Only accepted PA34-CODE-REV-01 through04 are addressed; B-F04 remains deferred. The dedicated author-fix artifact is `docs/gateflow/pa34-code-review-fix-1.md`, including exact changes, commands, failure provenance, runtime results and residuals. This is not independent review or finding acceptance.

### 11.1 Exact changes and bounded scope

- REV01: the existing architecture/PostgreSQL frozen identities each accept their own21 literal CRLF/LF hash pairs using `isIn(...)`. Plan raw-byte self-pinning is removed; manifest path/constant/count checks remain. Actual migration listings explicitly require17 exact V001-V017 filenames. No frozen input is normalized or modified.
- REV02: the existing ERROR registration obtains the effective Boot property/default from `Environment` (now `spring.web.error.path` with only `/error`, corrected in PR re-review2 fix3), refuses malformed servlet paths with a fixed safe exception and retains ERROR-only precedence. Existing configuration/Web/IT identities and packaged CI probes cover default/custom paths and preserve unrelated errors; only exact activation TRACE is concealed. REQUEST wildcard, verifier and production filter predicate/logic remain unchanged.
- REV03: existing exact-projection Web/IT methods assert every one of the eleven JSON values against distinguishable valid source facts, including independent subject UUID, event ID, statuses, revisions, channel/domain/reason and timestamp. Both ADMIN/OPS exercise the persistence projection.
- REV04: existing methods assert exact safe failure codes and route statuses, actual production transaction-template semantics and real file-list counts. No `@Test` method/class is added.

There are nine changed existing code/CI files, plus this existing report update and the sole new fix artifact:11 changed paths relative to the dispatch baseline. The ten changed existing paths are all inside the effective24-path allowlist. The exact list and hashes are retained in the fix artifact and `final-audit.log`. POM/dependencies, controllers/readers/services/filter production logic, contracts/migrations, application configuration and all unrelated files remain unchanged by this fix.

### 11.2 Current validation (not historical results)

Evidence root: `D:\codex-temp\pa34-code-review-fix1-evidence-20261115-01` (WSL `/mnt/d/codex-temp/pa34-code-review-fix1-evidence-20261115-01`). Maven uses the unchanged external01 shim; repository `backend/mvnw` is not edited. Versions are remeasured in `versions.log`.

| Validation | Actual result | Evidence in current fix directory |
| --- | --- | --- |
| Focused unit/boundary | 60/60, zero failures/errors/skips | `focused-unit-final.log` |
| Focused two-IT container profile | 60 Surefire +28 Failsafe, zero failures/errors/skips, exit0 | `focused-container-aligned.log`, `.exit` |
| Exact focused XML identities | Six unit classes60 +two IT classes28, exact accepted names | `focused-xml-summary.log`; `focused-surefire-xml/`, `focused-failsafe-xml/` |
| Accepted focused plus adjacent boundaries | 113 tests, zero failures/errors/skips | `focused-adjacent-boundaries.log` |
| Frozen guards in canonical LF projection | Exact selected architecture1 +real PostgreSQL1, zero failures/errors/skips, exit0 | `lf-frozen-final.log`, `.exit`; `linux-lf/` |
| Full clean PostgreSQL16 container-smoke verify | 76/704 Surefire +31/504 Failsafe =107/1208, zero failures/errors/skips, exit0 | `full-clean-verify.log`, `.exit`; all107 XML files and `full-xml-summary.json` |
| Exact current CI inventory body | Source=CI=POM=XML; token1209/POM31; all88 PA34 identities unchanged | `exact-ci-bodies.log`, `.exit`; `accepted-identities.json` |
| Exact current CI packaged default/web body | Default non-web startup/exit; 60 probes each default/custom error path (120 total), no pool; bounded process cleanup | `exact-ci-bodies.log`; `boot.log`, `web-boot.log`, `web-boot-custom-error.log` |
| Exact current CI dependency firewall body | 72 runtime/65 packaged,64 counterparts,8 dependency-only,1 packager; all116 adversarial probes pass | `exact-ci-bodies.log`; `jar-index.txt`, `pa11-runtime-dependencies.txt` |
| Exact A1-A5 accepted section9.3 commands | Every command exit0 in authorized LF projection | `contracts-a1-a5.log` |
| Main raw frozen checks and final report-inclusive content audit | 21/21 original hashes; exact17 migrations/no V018; effective24-path scope plus sole fix artifact; source hygiene/private checks pass | Timestamped `final-audit.log`, `baseline.json`, `final-audit-complete.py` |
| Git hygiene | Dispatch HEAD/worktree pointer preserved; no staged delta; raw index-byte preservation unresolved, not all-green audit | `final-audit.log`, `.exit`; `final-audit-metadata-failure.log`; `index-read-only-investigation.log` |

The exact Maven commands and CI-body execution method are recorded in the fix artifact section3; the accepted section9.1 focused113, focused60+28 and full-clean commands all run unchanged. Full clean verify began at 2026-11-15T08:23:06+08:00 and finished at 08:52:31+08:00 (29:24 minutes). Final canonical LF selected guards finished at 08:27:38+08:00. Both guards also run in the mixed-EOL main focused/full suites. All21 main frozen raw hashes remain unchanged, while LF expectations are direct dispatch Git blobs, not runtime-derived oracles. A1-A5 retain the separately authorized `D:\codex-temp\pa34-a1a5-lf-20261115-01` execution boundary.

The final main JAR SHA-256 is `f6b49f1ea912f8aa68600fe7b29669c81cd1c7ef98b0b8754f7ff476ab34b296`. For both error destinations the packaged body proves exact activation TRACE404/NOT_FOUND, HEAD404/empty body and unchanged customer TRACE, subject TRACE and activation trailing-slash TRACE500/INTERNAL. Safe envelopes/headers and no data/authentication success remain asserted. Only this local CI-body execution is claimed, not hosted Actions or live deployment acceptance.

Earlier failed attempts and their fixes are retained in the dedicated artifact/logs; none is counted as passing. In particular, the non-default container error destination initially failed when only the controller property was changed, exposing the pinned framework distinction below. Read-only index measurements expose an unresolved byte discrepancy, not repaired or suppressed. Some intermediate measurements match baseline, but others do not; the final audit records this separately rather than claiming continuous metadata preservation. Baseline index SHA-256 is `e23cd2e7c9d2ed4bec9dd0442c9bf48057bc0ec88fa33833a9b554dd90fe86c4`; another observed SHA-256 is `6927b28ee7ccdf4bdff73bc8e7d113dfc67791e4b6aac872bda13b94e6ab7e17`. Read-only staging comparison remains empty; no baseline rewrite or index restoration occurs. Cause/actor is not established and Controller/tooling adjudication is required.

### 11.3 Residuals and Controller stop

Spring Boot4.1.2 obtains its container error-page destination from supported `spring.web.error.path` (`WebProperties`), default `/error`; its metadata marks removed `server.error.path` at deprecation level `error` with replacement `spring.web.error.path`. PR re-review2 fix3 corrects the production ERROR registration to follow the supported effective property, never the removed key. The real-container fixture now sets only the Spring key, and its existing IT asserts agreement with `WebProperties.Error#getPath()` and exact activation TRACE404 at the custom path. That same identity separately starts a default-path web context to retain exact activation TRACE404 and sibling TRACE500/INTERNAL observations without setting the removed key. The unchanged `CustomerApiErrorController` still uses `${server.error.path:/error}`; broader legacy-key/controller/deployment coordination remains an honest Platform/Backend-owned residual outside this work unit. The first Spring-only custom-path run exposed this residual: the sibling error dispatch reached an unmapped custom error-controller path and returned404, not the former dual-key500. That failing attempt is retained, not counted or concealed; sibling500 is proven only in the explicit default-path context, not claimed for Spring-only custom deployments. Historical packaged CI probes coordinate both keys; their passing is not evidence that the removed key controls Boot. No production application configuration, customer error controller or extra registrar/customizer is authorized or changed. Earlier bytecode evidence remains historical: `boot-error-path-bytecode.log`.

B-F04 remains deferred to later Operations route-boundary/contract work. All section9 prerequisites/residuals remain, including external production verifier/credentials/key custody, global-sort capacity, corrupt-fact fail-closed remediation and snapshot-relative revocation. No production-readiness claim is made.

Final audit compares the full dispatch snapshot, including inherited untracked implementation files and all21 actual private-helper files; it does not rely only on tracked `git diff`. It proves preserved Controller/accepted artifacts/reviews/frozen content, worktree `.git` pointer, dispatch HEAD and empty staging; it does not establish raw index-byte preservation. That hygiene residual remains explicitly open for Controller/tooling, without weakening the failed metadata check or restoring Git state. Exactly one new fix artifact is added; no dependency, test identity, migration/grant, endpoint, field or production verifier is added. No stage/commit/branch/push/PR/comment/approval/review request/self-review/re-review is performed. Stop for Controller intake.

## 12. Controller-validated fix2 author finalization (2026-11-15)

Task: `pa34-operations-activation-history-code-review-rereview-fix1-finalize-gpt-20261115-02`; work unit: `PA34-operations-activation-history-read-foundation`; starting/ending checkpoint HEAD: `6dd4aa79edb95a93023f219cffa6b1a0366d7fde`. Authority is the active PA34 Control section, accepted cumulative plan, historical fix1 artifact and independent re-reviews `docs/reviews/code-review-20261115-091434.md` / `docs/reviews/code-review-20261115-093905.md`. The dedicated author artifact is `docs/gateflow/pa34-code-review-rereview-fix-1.md`. This continuation changes only this report and creates that artifact; it does not edit the Controller-validated candidate or independently accept the findings.

### 12.1 Repairs already present; exact candidate preservation

- `PA34-REREVIEW-01` (accepted Low): the existing service and real PostgreSQL/HTTP methods now compare all eleven projection values for all five domain-legal shapes. The HTTP method checks the exact eleven item keys, all expected IDs once, string/integral types and exact page keys/tail semantics; service record types and boundary record-name checks protect the corresponding service shape. The four DB-shape-only combinations still fail the whole page with sanitized503, including returned-row and lookahead cases. No production mapping or V007 constraint changed.
- `PA34-REREVIEW-02` (accepted Low): the existing two boundary and two configuration methods assert exact `Anchor(Instant occurredAt, UUID transitionId)`, exact `ActivationPage` names/types including `List<ActivationSummary>`, both exact eleven-column SELECT lists, ignored coexisting activation-prefix keys with a valid shared ring, failure rather than fallback for an invalid shared active ID, and the real production manager/template wiring. A mocked qualified datasource plus unrelated datasource prove selection without opening a pool; production manager enforced-read-only/rollback-on-commit-failure and template read-only/RR/REQUIRES_NEW/5s settings are asserted.

Exactly four test content paths changed in fix2, as recorded by Controller `pre-document-scope.log`, `baseline.json`, `baseline-files/` and `test-only.patch`:

1. `backend/src/test/java/com/onecall/archtests/OperationsActivationReadConfigurationTests.java`
2. `backend/src/test/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadServiceTests.java`
3. `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadsIT.java`
4. `backend/src/test/java/com/onecall/archtests/architecture/OperationsActivationReadBoundaryTests.java`

| Candidate file | Starting SHA-256 = ending SHA-256 = Controller full-run candidate |
| --- | --- |
| `OperationsActivationReadConfigurationTests.java` | `A3B36AB4367D18AE404A67063CA93ECA0358613DB8D5B6E7F5E48F17DD022086` |
| `OperationsActivationReadServiceTests.java` | `5D9B5A5095931F3C500895089878579C26C7031FC998A06B5E651DA24CBA92F6` |
| `OperationsActivationReadsIT.java` | `831B566DAC88BC8AA0A48C790BCD4C31EC518A6CCD42CD47EDB718ED38025C9F` |
| `OperationsActivationReadBoundaryTests.java` | `C5314D39603500F6C4A21018D61DF9CB9D98B7E13127C358431ECCAA3D788AE1` |

These are raw whole-file hashes, not normalized substitutes; the four LF-projection copies also match. Finalization reconstructs `test-only.patch` exactly from the saved baseline files and current tests. Production, CI, POM/dependencies, fixtures, configuration, contracts, V001-V017, accepted artifacts/reviews and private helpers receive no fix2/finalization edit. The later Controller Control update and supplied finalization prompt already differ from the older fix2 baseline; they are separately identified in `starting-baseline-delta.log`, preserved unchanged here and not miscounted as candidate changes. Relative to this finalization's own starting content snapshot, exactly the two authorized documentation paths change.

### 12.2 Controller evidence reused; no new test run

Evidence root E: `D:\codex-temp\pa34-code-review-rereview-fix1-evidence-20261115-01` (WSL `/mnt/d/codex-temp/pa34-code-review-rereview-fix1-evidence-20261115-01`). All paths in this table are relative to E. No new Maven, PostgreSQL, JAR/firewall or A1-A5 execution is claimed by this documentation task.

| Validation | Result / provenance | Evidence under E |
| --- | --- | --- |
| Focused unit/boundary | 60/60; zero failures/errors/skips; exit0 | `focused-unit.log`, `focused-unit.exit`; `focused-surefire-xml/` |
| Focused PostgreSQL Failsafe | 28/28; zero failures/errors/skips; exit0 | `focused-container.log`, `focused-container.exit`; `focused-failsafe-xml/` |
| Controller full clean PostgreSQL16 rerun | 76/704 Surefire +31/504 Failsafe =107/1208; zero failures/errors/skips; exit0; finished 2026-11-15T10:47:39+08:00 | `full-clean-verify-controller-rerun.log`, `full-clean-verify-controller-rerun.exit`; `full-surefire-xml/`, `full-failsafe-xml/` |
| Exact full/focused identity inventory | Accepted plan=source=CI=focused PA34 XML; full XML=all1208 source names=previous accepted full XML; exactly60+28 PA34 identities; token1209/POM31 | `accepted-identities.json`, `inventory.py`, `focused-inventory.log`; full/focused XML directories; unchanged CI dictionaries/POM |
| Canonical LF selected frozen guards | Architecture1 +PostgreSQL1; zero failures/errors/skips; exit0; exact V001-V017/no V018 | `lf-frozen-controller.log`, `lf-frozen-controller.exit`; `lf-surefire-xml/`, `lf-failsafe-xml/`, `lf-projection.log`, `linux-lf/` |
| Raw frozen/scope/candidate proof | 21/21 main frozen hashes,21 LF Git-blob matches, exact17 migrations; four-test-only candidate delta; raw/full candidate hashes above | `baseline.json`, `baseline-files/`, `test-only.patch`, `pre-document-scope.log` |

Exactly six Surefire classes with counts12/8/8/10/14/8 and two Failsafe classes18/10 retain all accepted names: no test added, removed, renamed, duplicated, disabled or skipped. Finalization re-reads the saved XML through `inventory.py --full`, checks the CI addition dictionaries without invoking the build, and verifies frozen bytes and scope. Generated finalization-only evidence is under `D:\codex-temp\pa34-code-review-rereview-fix2-finalize-20261115-02`: `starting-content.json`, `starting-baseline-delta.log`, `evidence-verification.log`, `identity-verification.log`, `provenance-verification.log`, and `final-audit.log`. These checks are not a new runtime validation or independent review. Earlier skipped/interrupted attempts remain historical, not replacements for the named passing Controller runs.

### 12.3 Provider recovery, residuals and stop

The preceding fix2 run `D:\codex-temp\pa34-code-review-rereview-fix1-run-20261115-01` has161 valid JSONL lines in `fix2-gpt.jsonl`, six provider403 insufficient-prepaid-balance errors, one `turn.failed`, no `turn.completed`, runner exit1 per Control, empty `fix2-gpt.stderr` and no last-message/new rereview-fix artifact. The four test edits survived; author documentation did not complete. This remains an external provider-account failure, not a test failure or successful author handoff. The later recovery probe `D:\codex-temp\pa34-gpt-recovery-probe-20261115-01` returned `GPT_RESTORED` in `probe.last.md` and a completed `probe.jsonl` turn; it establishes access recovery only, not retroactive success of the failed run. Controller independently supplied the later full/LF validation above.

Control subsequently classified `PA34-FIX-HYGIENE-01` as `rejected-with-reason` as a blocking finding after content/HEAD/staging verification. Section11's historical raw-index discrepancy is retained, not silently relabeled as a passing byte check; no Git-state repair is performed. Remaining accepted risks/open questions are unchanged: Platform/Backend error-path coordination and untested root/trailing-slash error mappings; deferred B-F04; Backend/DBA global-sort/timeouts and corrupt-fact remediation; snapshot-relative revocation/fresh continuation snapshots; external verifier/credentials/shared-ring custody; LF-only A1-A5 evidence, no hosted/live acceptance and existing development-tool vulnerabilities. Shared PA33 wiring and the standalone customer-filter test limitation retain the review's existing real-HTTP coverage/ownership, without a new finding or scope expansion. No new code/test defect or blocking hash mismatch was found in this finalization inspection.

Finish checks require the four hashes unchanged, exactly these two documentation deltas from the starting snapshot, preserved HEAD/pointer and empty staging, plus strict UTF-8/no BOM, whitespace/conflict/U+FFFD/final-newline checks on both documents. No stage, commit, push, branch/PR mutation, approval, review request, comment, merge, mark-ready or re-review entry is performed. Stop for Controller intake and fresh independent A/B re-review of the same candidate. This is author finalization, not independent acceptance or production readiness.

## 13. PR #45 bounded author fix 1 (2026-11-15)

Task: `pa34-operations-activation-history-pr-fix1-gpt-20261115-01`; verified checkpoint HEAD `2e8ad0cde168d7c7d74c2963e02b58d19d010b6e`; Controller-supplied reviewed remote head `a12d9bae70a864af8a72b2cda657a33c4674c069`. Authority is active PA34 Control and PR reviews `pr-45-review-20261115-142238.md` / `pr-45-review-20261115-152516.md`. Full dispositions, command provenance and owner risks are in `docs/gateflow/pa34-pr-review-fix-1.md`. This is author fix, not independent re-review or acceptance.

- `PA34-PR-REV-01`: preserve malformed/tag/wrong-key/purpose/authenticated-invalid-anchor400; map remaining checked JCE decode failures to sanitized503. Scoped Cipher/SHA-256 static seams extend one existing codec identity, with fixed-message/null-cause and post-scope restoration assertions; no identity/dependency change. PA33's unchanged subject codec divergence is a later Backend/Security owner risk.
- `PA34-PR-REV-02` / `PA34-PLAN-CORRECTION-02`: permission-only plan reconciliation explicitly includes `OperationsWebConfiguration.java` under accepted `PA34-IMPL-CORRECTION-01`, exactly24 rows=19 new/5 existing. New raw plan SHA256 `bfd33265dfd0df5f4df4e72017266a9ce6a28055877449d5372cfea6f7ae4c62`, 85,784 bytes /574 lines; section5 line197, row11 line222, arithmetic line237, section8 line418. The section8 body excluding the '## 8.' heading marker remains4,419 identical bytes before/after /SHA256 `BAB64BF545FA209C705056BE2E896C88E21170D6CB90ED0F12A6ECDF0878E135`; the complete section8 block including the '## 8.' heading marker remains4,424 identical bytes before/after /SHA256 `D114E71C79BA44F56F76E0DF9767D926E5E9A250E71F92B6C0EBBC396BA23E86`. Both definitions contain exactly the same21 unchanged hash/path rows; only the five-byte heading marker distinguishes their lengths/hashes (`PA34-PR-FIX-EVIDENCE-01`). These are current Controller handoff references; older plan identities remain historical. Control is untouched.
- `PA34-PR-REV-03`: rejected-with-reason retained. Accepted `PA34-CODE-REV-01` intentionally preserves the plan's21-row manifest while removing only its byte self-pin; neither frozen test or21-row expectation changes. Later oracle relocation is separately owned maintainability work.
- `PA34-PR-REV-04`: readiness explicitly labels the dated pre-push checkpoint and separately annotates Controller's later OPEN Draft PR #45 against `main`, without future knowledge or remote action.

Current validation evidence is under `D:\codex-temp\pa34-pr-fix1-evidence-20261115-01`; the unchanged authorized Maven shim is retained. Focused unit/IT pass exactly60/60 and28/28; authoritative rerun full clean passes exactly704/704 Surefire +504/504 Failsafe with zero failures/errors/skips; exact CI inventory/body, packaged JAR/firewall, frozen raw/LF, permission-only plan, scope/hygiene and accepted A1-A5 evidence all pass. The first full-clean attempt's eight environment `Cannot allocate memory` errors are retained and explicitly not counted; the later clean rerun is authoritative. Only the six dispatched paths changed; no new test identity/class, contract, endpoint, field, migration, grant or fallback. No staging, commit, push, PR mutation or re-review entry; stop for Controller. Production readiness is NOT CLAIMED.

## 14. PR #45 bounded author fix2 (2026-11-15)

Task: `pa34-operations-activation-history-pr-fix2-gpt-20261115-01`; verified local HEAD `5934d6665757f791e725d7c89df5982ec056d47a`; Controller-supplied remote reviewed head remains `a12d9bae70a864af8a72b2cda657a33c4674c069`. Authority is active PA34 Control and independent A/B PR re-reviews `pr-45-rereview-20261115-170900.md` / `pr-45-rereview-20261115-172042.md`. Full dispositions, runtime metadata, commands, retained attempts and residual owners are in `docs/gateflow/pa34-pr-rereview-fix-1.md`. This is author remediation, not independent re-review or acceptance; section13's plan/evidence identities remain historical.

- `PA34-PR-REREVIEW-01`: only plan line251's permission sentence now includes existing production rows09–11. Current raw plan SHA256 `1d99885939bcc0d6536334366ee6108b43b0b4506c724cbba9ce68f17647b4d4`, 85,788 bytes /574 lines (before: `bfd33265dfd0df5f4df4e72017266a9ce6a28055877449d5372cfea6f7ae4c62`, 85,784 bytes /574 lines). Section5 line197 remains exactly24 rows=19 new/5 existing; section8 line418 remains exactly21 unchanged rows. Body excluding the five-byte heading marker remains4,419 bytes /SHA256 `bab64bf545fa209c705056be2e896c88e21170d6cb90ed0f12a6ecdf0878e135`; complete block including it remains4,424 bytes /SHA256 `d114e71c79ba44f56f76e0df9767d926e5e9a250e71f92b6c0ebbc396ba23e86`.
- `PA34-PR-REREVIEW-02/03`: decode catches plain `BadPaddingException` and its AEAD subclass as sanitized400, other checked JCE failures remain503, and encode's checked-security failures remain503. The same existing codec identity adds a plain-padding provider seam proving decode400/encode503; both static scopes use `CALLS_REAL_METHODS`, retaining targeted algorithm-failure503, malformed400, fixed-message/null-cause and post-scope restoration checks. No test identity/class/dependency changes.
- `PA34-PR-REREVIEW-04`: rejected-with-reason retained. Actual candidate bytes are LF, the Git diff is surgical and hosted Linux uses LF; no CRLF rewrite, wholesale historical-evidence relabel or frozen dual-EOL guard change is performed.

New evidence root: `D:\codex-temp\pa34-pr-fix2-evidence-20261115-01`; Maven wrapper/shim unchanged. Focused six-unit and two-PostgreSQL16-IT validation passes exactly60/60 and28/28, zero failures/errors/skips. Serial full clean verification passes exactly704/704 Surefire +504/504 Failsafe with zero failures/errors/skips and BUILD SUCCESS; exact source=CI=POM=XML inventory, packaged JAR/firewall, all21 raw/LF frozen inputs, exact V001-V017/no V018, accepted A1-A5 commands, section5 exactly24 and section8 exactly21, and exact five-path scope/hygiene all pass. The early wrong-selection 26/26 unit attempt, the prior fix1 concurrent memory failure, and the adapted permission-script boundary assertion are retained and explicitly excluded from authoritative results. Production readiness remains NOT CLAIMED.

Fix2 permits exactly five content deltas from its starting candidate: codec, existing test, one plan sentence, this report and one new artifact. Readiness, prior PR-fix artifact, Control, reviews, POM/CI/migrations, other code/tests and private helpers remain unchanged. No staging, commit, push, PR update, reviewer request, approval, mark-ready action, merge, external comment or next-gate entry. Stop for Controller and fresh independent A/B PR re-review. Production readiness is NOT CLAIMED.

## 15. Bounded PR re-review2 fix3 — effective Spring error path (2026-11-15)

Task `pa34-operations-activation-history-pr-rereview2-fix3-gpt-20261115-01` begins at verified HEAD `8380f8523f87bb287bca01fbcc7a65a96ca64c25`. Current authority is the PA34 Control fix3 checkpoint and independent review artifacts `pr-45-rereview2-20261115-182500-a4.md` / `pr-45-rereview2-20261115-182500-b.md`; section14 describes the preceding fix2, not the current candidate. Full commands, candidate hashes, attempt history and author-validation results belong to `docs/gateflow/pa34-pr-rereview-fix-2.md` and external evidence `/mnt/d/codex-temp/pa34-pr-fix3-evidence-20261115-01`.

- `PA34-PR-REREVIEW2-A-M01`: ERROR registration now uses only `spring.web.error.path`, default `/error`; removed `server.error.path` has no effect. Existing fail-closed path checks and exact ERROR/HIGHEST_PRECEDENCE are retained; validation throws the neutral fixed message before registration. REQUEST wiring is unchanged. Existing configuration tests add supported-key custom, conflicting-key precedence, valid/malformed legacy-only non-effect and twelve malformed-effective-path checks without new identities. The PostgreSQL fixture sets only the supported key and its existing IT checks agreement with `WebProperties.Error#getPath()`; custom activation TRACE404 and separate default-path sibling500 observations are distinguished explicitly in section11.
- `PA34-PR-REREVIEW2-A-L02`: the generic read-only config catch-all explicitly excepts authorized row11. `PA34-PR-REREVIEW2-B-L01`: mutable permission text attributes row11 registration assertions to the configuration test and updates exactly the two existing assertion descriptions, including provider/plain-padding seams. Only five plan lines change; section5 remains24 contiguous rows=19 new/5 existing and section8 remains the exact21 frozen hash/path rows with both original block identities.
- Prior `PA34-PR-REREVIEW-01..03` remain fixed and `04` remains rejected; codec/test/readiness/prior-fix bytes are preserved. The unchanged customer error controller's legacy-key mapping remains Platform/Backend-owned outside this work unit, including the observed custom-path sibling error dispatch mismatch. No broader TRACE concealment or customer-controller repair is claimed.

Serial author validation passes: focused60/60 unit and28/28 PostgreSQL16 IT; full clean76/704 Surefire +31/504 Failsafe =107/1208, zero failures/errors/skips, BUILD SUCCESS (31:44). Exact source=CI=POM=XML preserves88 PA34 identities, source1208, token1209, POM31 and all previous accepted full1208 XML names. All three exact CI bodies pass, including default/web/custom-error JAR probes and PA11's116 firewall adversarial probes. All21 frozen raw/LF hashes, exact V001–V017/no V018, both section8 block identities, selected LF guards and the exact accepted A1–A5 block pass. Six additional supported-key-only packaged activation probes pass. The final four production/test source hashes equal the pre-validation snapshot; only documentation is finalized afterward. The real initial container assertion failure and informational Git/script/toolchain attempts remain retained and excluded from authoritative totals in the fix artifact and external evidence. The unchanged customer-controller coordination residual is not claimed fixed; sibling500 evidence is default-path only unless historical CI explicitly coordinates both keys.

Exactly six existing paths and the new fix artifact are task-owned; the seven-path baseline comparison, frozen/permission/source proofs and final hygiene audit cover the final documents as well as code. No new test identity or POM/CI/contract/migration/Control/review/private-helper delta is introduced. Author remediation is complete, not independently accepted. Stop for Controller intake and mandatory fresh independent A/B re-review. No staging, commit, push, PR mutation or later gate. Production readiness is NOT CLAIMED.
