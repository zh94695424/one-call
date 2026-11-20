# PA38 plan fix5 — accepted re-review4 closure

## 1. Identity and binding inputs

- Task: `pa38-operations-catalog-sale-lifecycle-plan-fix5-gpt-20261118-01`.
- Date: November 18, 2026, Asia/Shanghai.
- Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call`.
- Branch: `codex/pa38-next-work-unit`.
- Full HEAD: `49d42b2a154e9a445a34ef0a0801076a6601e208`, equal to the expected Controller bookkeeping HEAD; branch/HEAD unchanged.
- Status: **`plan-fix-complete`**. RR4-01..03 are closed at the plan/specification level only. No implementation, runtime acceptance, Controller acceptance or fresh review conclusion is claimed. Stop before fresh independent A/B plan re-review.

| Binding artifact | SHA256 before | SHA256 after / disposition |
| --- | --- | --- |
| Cumulative plan | `4b1edeb9f19d12471e6d034332939fcd44e3d6df7f1a37883159ed367e49f88d` | `fb58fcc1d398de1211f534fec9640b340c62963ec1b23a8d51c5c7e6b9d47677`; 115,385 bytes |
| Frozen fix4 | `cb8f4ceae417a622b084961748c02addb83e0426fde31665c490e1968df14eae` | unchanged |
| Frozen Review A, `docs/reviews/plan-review-20261118-135749.md` | `a9c04ae6ddf2775ef4a6ad95d08a75e578aad712f6681d0fbf5e4a77b9ccaf19` | unchanged; accepted Low Operations literal/concealment finding |
| Frozen Review B, `docs/reviews/plan-review-20261118-140719.md` | `79470ce113b2d6b16ad32431a9606d4c5955514004dec5c2cdcaea50d5862c13` | unchanged; accepted Low oracle enumeration and Medium CRLF-precondition findings, plus specified open-question clarifications; no waiver accepted |
| Control, `docs/gateflow/project-execution-control.md` | `67aff86c889738bea0a91b5807f3c5c2833de61048e2853426bd2e3620ed639a` | unchanged |
| Frozen blocked implementation evidence | `c45b3ad3769aab00f1b3f52f2b83246ba98e00c878d4fef4c0741b886252e16d` | unchanged; historical failure/blocker evidence, not rewritten as successful |

Only these two documentation paths are task changes:

1. `docs/gateflow/pa38-operations-catalog-sale-lifecycle-plan.md`.
2. `docs/gateflow/pa38-operations-catalog-sale-lifecycle-plan-fix-5.md`.

At intake, the cumulative plan already differed from HEAD (the bound fix4 content); fix4 and both supplied reviews were already untracked, alongside private helpers. Those are pre-existing state, not fix5 additions or permission to change them. Final fix5 size/SHA256 are returned externally, avoiding a self-referential digest.

## 2. Exact closure matrix

| Accepted finding | Plan destination | Exact closure / remaining executable obligation |
| --- | --- | --- |
| `PA38-PLAN-RR4-01` | Status; §4.2; §5.1; §9.3; §13 | **Closed at plan level.** Pin the complete Operations POST/servlet-path/regex/context+URI literal below; require the Customer predicate's identical shape. Preserve all shipped GET/subject/equality/authentication literals. Within existing `OperationsCatalogVersionWebTests.concealsOtherCatalogMethodsAndUris`, test the malformed `publications;x` alternate with missing and invalid employee credentials, exact Operations sanitized404 and zero employee-verifier/MVC/controller/command/authority/customer-authentication work. No test identity is added or renamed. |
| `PA38-PLAN-RR4-02` | Status; §5.1; §11.1; §13 | **Closed at plan level.** Add frozen `OperationsSubjectReadBoundaryTests.employeeVerifierIsReusedWithoutPackagedProductionImplementation:89` to the source-oracle list and its class to the regression selector; also name the duplicate IVR authentication literal at `:228`. The class has exactly eight current methods, verified from real source. Six frozen classes total52; no global inventory change, test edit or identity addition. |
| `PA38-PLAN-RR4-03` | Status; new §11.0; §11.1–§11.3; §12; §13 | **Closed at plan level, no waiver.** Establish the 1,344-CRLF/zero-bare-LF checkout root cause; prescribe one exact-candidate Git-less test-only LF projection, one-document normalization, unchanged primary/frozen/test/source/schema/hash bytes and core.autocrlf. Require all regression/focused/full/CI XML gates there; record raw results separately. Only exact raw IVR `:207` is materialization, any other or canonical failure is a real gate failure. |
| B Open Question 1 | §4.2; §5.1; §5.4 | Same exact route shape in both filters, not two independently designed regexes. |
| B Open Question 2 | §9.3 positive web identities | Fail-on-call `CustomerSessionVerifier` is registered through the existing fixture registry and `ObjectProvider<CustomerSessionVerifier>` / `beans.getBeanProvider(CustomerSessionVerifier.class)`. No production seam/dependency. |
| B Open Question 3 | §5.4; §13 | Both command-chain registrations are `DispatcherType.REQUEST`; separate Operations ERROR registration is irrelevant to these commands and unchanged. No ERROR behavior changes. |
| Historical Control dispatch | §1; §12; §13 | Control `:120` is superseded by `:123-128` and this fix authorization. Controller's renewed implementation dispatch must bind the final fixed plan SHA256 above and the **31-path** allowlist after fresh A/B re-review; historical text is not dispatch authority. Preserve `PA38-RR3-OPS-01..05`. |

Changed plan sections: **identity/status, §1, §4.2, §5.1, §5.4, §9 introduction, §9.3, new §11.0, §11.1, §11.2, §11.3, §12 and §13**. §9.1/§9.2 exact class/method identity lists remain identical to HEAD's prior cumulative plan, as verified non-mutatingly. §2/§3/§6–§8/§10 contracts and inventories remain unchanged.

## 3. Operations and Customer literals

Operations (§4.2 and §5.1):

```java
"POST".equals(request.getMethod()) && path != null
        && path.matches("/api/v1/operations/catalog/versions/[^/;%]+/(publications|offsales)")
        && (request.getContextPath() + path).equals(request.getRequestURI())
```

Customer (§5.4, existing local variable only):

```java
"POST".equals(request.getMethod()) && operationsPath != null
        && operationsPath.matches("/api/v1/operations/catalog/versions/[^/;%]+/(publications|offsales)")
        && (request.getContextPath() + operationsPath).equals(request.getRequestURI())
```

Both must use the same exact shape, with Operations first. Customer's branch stays before trace/headers/authentication/buffering and only continues the chain with the original request/response objects. Canonical UUID/body parsing is separate. Existing GET/subject literals, negated Operations URI rejection, customer equality, all customer protection, filter registrations/order and shipped source oracles remain frozen. The malformed alternate is `POST /api/v1/operations/catalog/versions/00000000-0000-0000-0000-000000000038/publications;x`, with matching servlet path/raw URI, tested both without credentials and with invalid credentials inside the existing concealment identity.

## 4. Regression arithmetic and unchanged inventories

| Frozen existing regression class | Current methods |
| --- | ---: |
| OperationsSubjectReadConfigurationTests | 8 |
| OperationsLifecycleBoundaryTests | 8 |
| OperationsActivationReadBoundaryTests | 8 |
| OperationsIvrSessionReadBoundaryTests | 8 |
| OperationsSubjectsWebTests | 12 |
| OperationsSubjectReadBoundaryTests | 8 |
| **6 existing classes** | **52 = 8+8+8+8+12+8 = 44+8** |

This adds coverage of an existing class, not a class or method to source inventory. All six files remain frozen/unmodified. Keep **31 = 22 new + 9 modified** implementation paths: production **11 new + 5 modified = 16**, tests **11 new + 2 modified = 13**, build/CI **2 modified**; 16+13+2=31. No allowlist expansion occurs in fix5.

Final Java **290/180**, Surefire **97/934**, Failsafe **38/630**, combined focused **135/1,564**, scan **1,565** (one historical comment hit), POM includes **38**, modules/discovered edges **14/13**, catalog allowed dependencies **5**, resources **21/1**, migrations **18/no-V019**, manifest count **310 = 290+21-1** remain binding. Manifest digest is deferred until actual implementation bytes exist, then recomputed by §10.3 in the existing Java carrier; no guessed digest or nonexistent byte aggregate.

## 5. Exact LF-projection validation contract

The primary PA35 plan's observed raw SHA256 is `d5ba84e34124e0cb44fd5a6c8fdc008a8894c3dee472268997d6a4f02500de55`, with **1,344 CRLF / zero bare LF**. The two LF-terminated negative removal strings in the frozen IVR test do not occur in the raw CRLF document, so they do not mutate it; `assertThatThrownBy` fails at `frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact:207`. Product bytes and the frozen independent verifier are not thereby defective or changed.

Precondition: candidate stable; **do not change core.autocrlf, normalize/re-checkout any frozen primary file, edit/skip/weaken a test, change expected hashes, or grant a waiver**. Execute the complete setup block in plan **§11.0** exactly once from the primary candidate root. Its commands export `PA38_CANDIDATE_ROOT="$PWD"` and `PA38_VALIDATION_ROOT="$(mktemp -d /tmp/pa38-lf-validation.XXXXXX)"`, copy the exact candidate with Python `copytree(..., ignore=ignore_patterns('.git', '.codex', '.pa38-*', 'target', 'tmp', 'node_modules'), symlinks=True, dirs_exist_ok=True)`, and assert equal path/hash inventories before normalization. Then, only in that projection, replace CRLF with LF for the single PA35 plan. Assert that every other projected hash and every primary hash is unchanged; print candidate inventory binding and raw/LF document hashes. No Git checkout, config change, source/test/schema/hash rewrite or committed projection path is involved. Regenerate and rerun after any candidate change; the primary candidate remains the review/commit source.

The exact regression selector is the same for raw diagnostics and canonical validation; only the root differs:

```bash
(cd "$PA38_CANDIDATE_ROOT/backend" && ./mvnw -B -ntp -Dtest='OperationsSubjectReadConfigurationTests,OperationsLifecycleBoundaryTests,OperationsActivationReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectReadBoundaryTests' test)
(cd "$PA38_VALIDATION_ROOT/backend" && ./mvnw -B -ntp -Dtest='OperationsSubjectReadConfigurationTests,OperationsLifecycleBoundaryTests,OperationsActivationReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectReadBoundaryTests' test)
(cd "$PA38_VALIDATION_ROOT/backend" && ./mvnw -B -ntp -Dtest='EmployeeAuthorityReadPortTests,CatalogSaleLifecycleSemanticsTests,CatalogSaleLifecycleRepositoryTests,DefaultOperationsCatalogSaleCommandTests,CatalogVersionRequestBinderTests,OperationsCatalogVersionWebTests,OperationsCatalogVersionConfigurationTests,OperationsCatalogVersionBoundaryTests' test)
(cd "$PA38_VALIDATION_ROOT/backend" && ./mvnw -B -ntp -Dtest='ModuleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests' test)
(cd "$PA38_VALIDATION_ROOT/backend" && ./mvnw -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT verify)
(cd "$PA38_VALIDATION_ROOT/backend" && ./mvnw -B -ntp -Pcontainer-smoke verify)
```

After the full verify, run the shipped oracle in that same projection against its own XML, not partial focused reports:

```bash
(cd "$PA38_VALIDATION_ROOT/backend" && python3 - <<'PY'
from pathlib import Path
from textwrap import dedent
workflow = Path('../.github/workflows/ci.yml').read_text()
marker = "          python3 - <<'PY'\n"
start = workflow.index(marker) + len(marker)
end = workflow.index('\n          PY', start)
exec(compile(dedent(workflow[start:end]), 'ci-container-inventory', 'exec'))
PY
)
```

Record actual commands/launchers, exits, report totals and exact failure identities. If the unchanged wrapper cannot launch on the host, record that and use the unchanged supported host launcher (e.g. `cmd.exe /c mvnw.cmd` from the same backend directory); never normalize a wrapper. Raw failure exclusively at IVR `:207` is a **checkout materialization issue**, neither product failure nor waiver. Any other raw failure, or any canonical failure including `:207`, is a real gate failure. Canonical required exits are all0: regression **6/52**, focused Surefire **8/86**, guards **2/17**, focused Failsafe **2/38** with complete Surefire **97/934**, full **97/934 + 38/630**, and shipped source=CI=POM=XML oracle. No unconditional raw-focused/full-green claim remains. Record provenance/exclusions, one-document normalization/hashes and unchanged primary/frozen bytes. All these runtime commands are **planned, not executed in fix5**; no validation projection or generated test output was created here.

## 6. Non-mutating validation and command exits

Use process-local `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call25`, `GIT_WORK_TREE` equal to this task worktree and `GIT_OPTIONAL_LOCKS=0` for Git checks. No `.git`/config repair is performed; existing normalization remains unchanged. `-c core.safecrlf=false` suppresses advisory warnings only.

| Actual command/check family | Exit / result |
| --- | --- |
| Initial ordinary `git branch --show-current`, `git rev-parse HEAD`, `git status --short` | Each128: Windows `D:/...` gitdir cannot resolve as a native Linux path. Recovered with explicit read-only Linux gitdir/worktree overrides, not by editing metadata. The enclosing exploratory shell continued and exited0. |
| Explicit-gitdir branch/HEAD/status; read-only input/source searches (`rg`, `sed`, `cat`, `wc`, `sha256sum`) | 0; correct branch/full HEAD, original plan/fix4/review hashes match, shipped registration/provider/authentication/drift sources inspected. |
| Read-only Python source/line-ending/protected-byte inventory | 0; six method counts8/8/8/8/12/8 total52, PA35 CRLF1344/bareLF0. Protected inventory before/after excludes exactly the two task docs and private/generated entries; **2,252 files**, sorted `path + space + raw SHA256 + LF` aggregate `eaec293c1b4171bfe6006cd4e5dbe4e1e7a69ea36a9851fee9b960f739ad9a84`. |
| Read-only Python allowlist/identity/stale-reference/syntax assertions | 0;31 unique paths/22 absent new/9 existing modified; production11+5, tests11+2, build2; §9.1/§9.2 identical to HEAD; no stale active regression arithmetic/unknown-root-cause claim; embedded Bash (`bash -n`) and Python (`compile`) parse without execution. |
| `git -c core.safecrlf=false diff --check` | 0; no whitespace errors. |
| Final read-only hash/size, branch/HEAD, protected-path and diff checks | 0; final plan/fix5 size/SHA256 in hand-back; only the two authorized task docs changed from intake. Frozen source/test/POM/CI/OpenAPI/migration/Control/review/implementation/fix1–4/wrapper bytes unchanged. |

No Maven, Docker, PostgreSQL, CI oracle, validation-projection generation, source edit, staging, commit, branch change, push, PR/issue mutation, merge, approval, readiness mark, reviewer request or external comment occurred. Private `.pa38-*`/`.codex` and all existing generated output remain untouched.

## 7. Residual owners and next destinations

| Owner | Residual / destination |
| --- | --- |
| Controller | Fresh peer-isolated A/B plan re-review, then a separately authorized implementation dispatch bound to the final plan hash and31 paths; do not reuse historical Control `:120`. Preserve RR3-OPS-01..05. This task stops before those gates. |
| Backend test-boundary owner + Controller | Execute §11.0 canonical projection and every §11 gate; record exact raw materialization behavior separately. No waiver; any additional or canonical failure blocks acceptance. |
| Backend Catalog-order + Operations IAM | Implement the two commands, exact filter literals, existing-identity adversarial/full-chain proof, authority/role/rollback/fact/concurrency assertions and recomputed310-entry manifest digest; plan closure is not runtime closure. |
| Backend; Platform/Operations + Security | Mandatory bounded same-pool read/lock/statement-cancel proof at implementation; sustained saturation, read503/recovery and capacity before deployment. No SLO, new pool or credential family. |
| Platform/Security/Operations | Production LO enablement, secret delivery, exact seven PA25 SET memberships and pool health at deployment readiness, not proven by local tests. |
| Database/Security + Catalog-order | Frozen catalog-role over-privilege and effective ACL drift; exact V00442-tuple matrix/negative probes at implementation, no ACL/migration repair here. |
| Security / Operations IAM | Lock-free authority-read timing and existing verifier availability/sanitization remain accepted boundaries; no new employee lock or policy. |
| Platform/Operations | Audit/Outbox retention, dispatch, monitoring and retry remain A3 operational work. |
| API governance / Catalog-order / future-slice owners | Draft authoring/replacement, trial policy, customer filtering and UI second confirmation remain outside the confirmed two-route goal; require future goal confirmation. |

**Final status: `plan-fix-complete`; stop before fresh A/B re-review.**
