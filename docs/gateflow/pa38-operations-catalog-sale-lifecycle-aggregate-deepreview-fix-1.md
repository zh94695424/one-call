# PA38 aggregate deepreview fix 1 — bounded catalog request ingestion

## Task and disposition

- Task: `pa38-operations-catalog-sale-lifecycle-aggregate-fix1-gpt-20261119-01`; November 19, 2026, Asia/Shanghai. Bounded fix author, not gate-acceptance authority.
- Primary: `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call`; unchanged HEAD `e3e49c4cf20c80078aa833a69efd3a9a2a832bfa`, tree `bdfe4736b08ff425747b4fb1e9a4c6cc56adb850`, branch `codex/pa38-next-work-unit`.
- Implement only accepted `PA38-AGG-REV-01`, `PA38-AGG-REV-02`, `PA38-AGG-REV-03`; no other review residual is changed or declared closed. Read the current Control PA38 section, aggregate A/B reviews, code-review fix1–3, implementation report, cumulative accepted plan, private-parser binder tests and existing controller Web tests.
- **Status: all three remediations are implemented in the candidate; aggregate validation is BLOCKED, not implementation-pass or gate acceptance.** Focused 86, live PostgreSQL 38 and three observer runs pass. Guards and clean full fail solely because the authorized controller change invalidates an out-of-allowlist manifest literal. The literal is not changed or bypassed, including in the validation projection.
- Next intended gate is **fresh independent aggregate re-review**, subject to Controller disposition of that scope conflict and completion of required green validation. This artifact does not request reviewers, advance the gate, waive a failure or assert readiness.

## Authorized writes

Exactly these session writes; pre-existing Control/review/private-helper changes remain untouched:

```text
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogVersionController.java
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java
docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md
docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-1.md
```

No new production class/dependency, Java test class, test identity, migration, index, ACL, DTO, route, OpenAPI, plan, Control, review, POM or CI change. No tracked deletion or staging. Git's existing Windows worktree pointer is resolved process-locally with `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call25`, `GIT_WORK_TREE` equal to primary, `GIT_OPTIONAL_LOCKS=0` and `-c core.safecrlf=false`; `.git` and Git configuration are not repaired or edited.

## Candidate remediation

### PA38-AGG-REV-01 — preventive transport-level bound

Both exact POST mappings remove `@RequestBody byte[]` argument binding. Each handler evaluates `caller(request)` first, then calls the local body reader, then the unchanged strict private `parse`, and only then resolves the command provider. The reader is exactly `request.getInputStream().readNBytes(8193)` and consumes no more than 8193 bytes, regardless of request length metadata. The existing `body.length > 8192` parser check is retained as the sentinel rejection. Valid UTF-8 JSON plus whitespace totaling exactly 8192 bytes is admitted; 8193 bytes and a 2 MiB request are rejected after exactly 8193 bytes, not full entity buffering.

Canonical UUID, duplicate/trailing/unknown-field rejection, JSON object and integral positive revision checks, UTF-8 validation and parser reflection identity are unchanged. Authentication `401/503`, exact routes, DTOs, success and problem bodies, trace, headers/content length, media handling, shipped unacceptable-Accept `500`, both `409` codes and all other error codes are preserved.

### PA38-AGG-REV-02 — absent and truncated client bodies

The local reader catches `IOException` and throws the existing sanitized `Failure(VALIDATION_FAILED)`. Missing and explicit zero-byte bodies now reach structural parsing rather than framework-required-body binding, and return `422 VALIDATION_FAILED`. A stream throwing after seven bytes follows the same mapping. No partial JSON is parsed or provider resolved after a read failure; no raw exception or body text appears in the five-field problem envelope.

### PA38-AGG-REV-03 — real durable replay topology

**`audit.audit_fact.request_id` is an identity correlator, not an independently enforced uniqueness key.** No audit-side uniqueness constraint exists or is claimed. The durable replay backstop is the product-scoped advisory transaction lock + post-lock state-machine short-circuit + unique `platform.outbox_event(event_domain, dedupe_key)` constraint `uq_outbox_domain_dedupe`. For PA38 the event domain is fixed to `OPERATIONS`, so the dedupe key is unique within that domain, not globally by `dedupe_key` alone.

The ordinary replay branch returns before append. A real transition appends audit and paired Outbox in one transaction; a duplicate paired Outbox raises a uniqueness error and rolls back the transaction, including its audit append. This is not an independent audit-side constraint, and does not promise standalone audit deduplication for future unpaired writers or pruned Outbox identities. Retention/dispatch and any future audit-only writer remain separately owned. **No schema change, migration or V019 is introduced.**

## Executed body evidence — both routes

These are assertions executed inside existing Web identities, not a new test class or an extra `@Test`. The servlet wrapper counts input-stream acquisitions and delivered bytes; its partial-read stream throws `IOException`, and its authentication sentinel throws `AssertionError` if read. Declared-length and unknown-length request metadata are exercised for both exact-bound acceptance and oversize rejection.

| Input / failure | publications | offsales | Consumption / downstream assertion |
| --- | --- | --- | --- |
| Valid JSON padded to 8192 bytes, declared and unknown length | 200 | 200 | exactly 8192; normal success headers |
| Valid prefix padded to 8193 bytes, declared and unknown length | 422 VALIDATION_FAILED | 422 VALIDATION_FAILED | exactly 8193; provider construction 0; command calls 0 |
| Valid prefix padded to 2 MiB, declared and unknown length | 422 VALIDATION_FAILED | 422 VALIDATION_FAILED | exactly 8193, not 2 MiB; provider construction 0; command calls 0 |
| No content supplied | 422 VALIDATION_FAILED | 422 VALIDATION_FAILED | empty native servlet body; provider construction 0; command calls 0 |
| Explicit `content(new byte[0])` with instrumented stream | 422 VALIDATION_FAILED | 422 VALIDATION_FAILED | one acquisition, 0 bytes; provider construction 0; command calls 0 |
| `IOException` after partial body | 422 VALIDATION_FAILED | 422 VALIDATION_FAILED | one acquisition, 7 bytes; provider construction 0; command calls 0 |
| Missing/invalid/wrong-audience credential, fail-if-read stream | 401 UNAUTHENTICATED | 401 UNAUTHENTICATED | 0 acquisitions, 0 bytes; command calls 0 |
| Verifier exception, fail-if-read stream | 503 SERVICE_UNAVAILABLE | 503 SERVICE_UNAVAILABLE | 0 acquisitions, 0 bytes; command calls 0 |
| Lazy command-provider construction throws after authenticated valid 8192-byte parsing | 503 SERVICE_UNAVAILABLE | 503 SERVICE_UNAVAILABLE | one acquisition, 8192 bytes; one provider attempt per route; command calls 0 |

Zero provider construction and `verifyNoInteractions(command)` establish that invalid bodies cannot reach command-side authority/database/locks/facts. The lazy supplier counter is asserted zero after all invalid cases before the two valid provider-503 attempts. The original malformed-body authentication checks, missing/ambiguous provider checks, command exception `500`, `403`, response and route-concealment coverage remain. `problem(...)` checks exact status/code/trace, five fields, content type, no-store/no-cache/nosniff, content length, 401 challenge and leak canaries for each negative subcase.

Evidence: `body-probes.json` records 24 asserted rows (each auth-401 row includes three credential cases); `focused86-xml/TEST-com.onecall.archtests.OperationsCatalogVersionWebTests.xml` has 14 tests, zero failures/errors/skips. Raw SHA256 `881b6e47c4cf8151e073066dd18df32e339674fbca7e23c2525e6dc8f0c58cbb`.

## Validation environment and exact outcomes

Evidence root: `/tmp/pa38-aggregate-fix1-evidence/`. All Maven invocations use `/opt/maven-3.9.9/bin/mvn -B -ntp`, `JAVA_HOME=/opt/jdk21`, `/opt/jdk21/bin` first on PATH. Preflight executes Maven/JDK version, Docker Client, Docker Server and server-backed `docker info`, all exit 0: Maven 3.9.9, Java 21.0.13, Docker Client/Server 29.1.3. Live fixtures report **PostgreSQL 16.16**, not the historical 16.15 version.

Accepted-plan setup runs once, exit 0, creating `/tmp/pa38-lf-validation.oYmOvd/candidate`. All projected inputs initially equal primary bytes, then only the previously authorized PA35 document's 1,344 CRLF lines are materialized as LF in the projection. No primary frozen input is edited. Final static audit proves all PA38 test Java and production Java equal tested bytes. The manifest carrier is byte-identical and stale in both roots.

| Execution | Exit | Actual result |
| --- | --- | --- |
| Focused eight-class Surefire | 0 | 8 classes / 86 tests pass; 0 failures/errors/skips |
| Two unchanged guards | 1 | 2 classes / 17 tests; 16 pass, 1 stale-manifest failure; 0 errors/skips |
| Both live PostgreSQL Failsafe classes, direct goals | 0 | 2 classes / 38 tests = 24+14 pass; 0 failures/errors/skips |
| Exact observer method, three sequential Maven processes | 0, 0, 0 | 1/1 each, no failures/errors/skips |
| Canonical `clean -Pcontainer-smoke verify` | 1 | 97 Surefire classes / 934 tests; 933 pass, same 1 manifest failure; 0 errors/skips; Failsafe not reached |
| Success-gated same-shell unchanged CI/XML oracle | not executed | preceding clean verify failed; no alternate/edited oracle or focused-report substitution |
| Accepted 135-file XML custody | not reached | canonical full Failsafe 38/630 did not execute; no green 135/1564 claim |
| Independent focused/guard/live XML audit and body-evidence serialization | 0 | inventories and unchanged identities verified; failure explicitly retained |
| Static/frozen/path audit | 0 | 32 original paths, Java 290/180, manifest 310 recomputed; stale pin reported, not waived |

The direct Failsafe goals are supplemental live behavior evidence after compilation, not a successful lifecycle `verify` or a substitute for the required full sweep. The canonical plan script keeps `verify && python3` in the same shell, so the unchanged shipped oracle/custody block correctly does not run after failure. There are only 97 failed-full Surefire XML files after clean, no Failsafe XML. Their diagnostic ordinal filename/raw-hash/LF aggregate is `8728a93ff14a5dc51ae95937f7fc4f86e00ce46cff10a00c9f42d2189c387266`; this is **not** accepted 135-file custody and is not used as an acceptance oracle.

### Commands

Run from the recorded projection's `backend` with the environment above:

```bash
/opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=EmployeeAuthorityReadPortTests\,CatalogSaleLifecycleSemanticsTests\,CatalogSaleLifecycleRepositoryTests\,DefaultOperationsCatalogSaleCommandTests\,CatalogVersionRequestBinderTests\,OperationsCatalogVersionWebTests\,OperationsCatalogVersionConfigurationTests\,OperationsCatalogVersionBoundaryTests test
/opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=ModuleBoundaryTests\,EvidenceRoleIvrAssociationAclBoundaryTests test
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT\,OperationsCatalogVersionRoleLeastPrivilegeIT failsafe:integration-test failsafe:verify
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner failsafe:integration-test failsafe:verify
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner failsafe:integration-test failsafe:verify
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner failsafe:integration-test failsafe:verify
/opt/maven-3.9.9/bin/mvn -B -ntp clean -Pcontainer-smoke verify
```

Expanded per-gate commands, separate exits and raw transcripts are retained in the evidence root. The clean/full command is executed by the unchanged accepted-plan `clean-full-oracle.sh`; its entire success-gated oracle remains unchanged. No skip flags, test-identity edits, POM changes or failing-assertion bypasses are used.

## Manifest scope conflict — unresolved validation blocker

The accepted production modification changes the 310-file normalized manifest to:

```text
count 310
actual 1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045
pinned 3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0
```

The unchanged carrier is `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`, method `v001ThroughV017AndPublicContractsRemainByteFrozen`, line 102; raw SHA256 `02931eff38612241830a3e4d99f936c42e2c4d673ae865d7a9bd8325104b3520`. Both guards and clean full identify exactly this assertion, with the actual digest above. Its frozen-file assertions before the manifest assertion pass. This is not the historical PA35 CRLF deferral.

The four-path task allowlist forbids editing that carrier. Therefore required green guards, canonical full and downstream oracle/custody cannot all be delivered within current authorization. **Controller must disposition the scope conflict before a digest-only carrier synchronization and fresh validation can occur.** No carrier edit is made, proposed bytes are not substituted in the projection, and the original assertion is not relaxed. No claim of all-green completion is made.

Manifest algorithm: all regular files beneath backend `src/main/java` and `src/main/resources`, excluding only V018's filename; normalize each file's CRLF to LF, SHA256; sort backend-relative POSIX `path + single space + lowercase digest` rows ordinally, append LF to every row including the last, UTF-8 encode and SHA256. Count remains 310; no manifest byte-size field is invented.

## Preserved identities and boundaries

All original 14 `OperationsCatalogVersionWebTests` identities, in unchanged source order:

```text
admitsExactPublicationPost
admitsExactOffsalePost
classifiesReadsSubjectsAndCatalogCommandsSeparately
catalogCommandsDoNotRequireIdempotencyKey
subjectCommandsRetainExactIdempotencyRule
authenticationPrecedesStructuralParsing
structuralValidationPrecedesAuthorityWitness
disabledEmployeeReturnsForbiddenAfterValidParsing
opsReturnsForbidden
rejectsInvalidContentTypeAndShippedAcceptOutcome
successHasExactStateAndHeaders
staleViewHasExact409ProblemHeadersAndLength
concealsOtherCatalogMethodsAndUris
publicationConflictHasExact409ProblemHeadersAndLength
```

Eight focused classes remain 86; the two PostgreSQL classes remain 38. Exact method sets are re-derived from source, matched to unchanged CI maps and passing focused/live XML, and serialized in `pa38-test-identities.json`. No source method is added, removed, renamed, disabled or skipped. Java source file counts remain **290 production / 180 test**; POM and CI hashes are unchanged. Full-suite identity acceptance is not inferred from a failed canonical sweep.

| Class | Preserved tests |
| --- | --- |
| `com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPortTests` | 8 |
| `com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleSemanticsTests` | 10 |
| `com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleRepositoryTests` | 12 |
| `com.onecall.core.catalog_order.internal.operationscommand.DefaultOperationsCatalogSaleCommandTests` | 16 |
| `com.onecall.core.operations_iam.internal.web.CatalogVersionRequestBinderTests` | 10 |
| `com.onecall.archtests.OperationsCatalogVersionWebTests` | 14 |
| `com.onecall.archtests.OperationsCatalogVersionConfigurationTests` | 8 |
| `com.onecall.archtests.architecture.OperationsCatalogVersionBoundaryTests` | 8 |
| `com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT` | 24 |
| `com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT` | 14 |

Frozen primary raw hashes:

```text
api/openapi/onecall-v1.openapi.yaml 0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68
backend/ops/postgresql/pa25-runtime-identities.sql b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9
docs/12-technology-stack-and-system-architecture.md 9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db
docs/14-production-data-model-and-flyway-schema-spec.md 0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31
docs/15-production-openapi-surface-spec.md db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0
docs/17-production-security-design-spec.md b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205
V001-V018 raw migration inventory SHA256 53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70
```

All 18 migrations remain byte-identical; no V019 exists. Final nonmutating Git checks retain HEAD/tree/branch, no tracked deletion, no staging and clean `diff --check`; full baseline custody permits only the four authorized session paths and preserves pre-existing Control/review/private bytes. The original implementation/evidence candidate is still exactly **32 paths** (16 production Java + POM + CI + 13 test Java + implementation report), with this supplemental fix artifact outside that set.

## Final original 32-path candidate custody

Raw byte total: **556,654**. Ordinal `path + single space + raw SHA256 + LF` aggregate (terminal LF, UTF-8): **`7c14c50e503927d2a997042ab8c4d63901cc6bb312294b5089b89db7b2e868b4`**. This is separate from the LF-normalized production manifest and the failed-full XML diagnostic. The report hash is bound here, not inside itself; this artifact is not included in its own hash.

```text
.github/workflows/ci.yml 11dfd54c9c0c797ea30142d2a0a614df6385c724f54a343f6bfc5a2b8677209a
backend/pom.xml f8fc1389d46a0af34ebb8b538947c6571211b44a0e19be7875ab3923dfee0fb8
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleConfiguration.java 5ae7933e32a200e1efc6c66c0d79c1568e382f8767eef9f3eebaff24f8b2dc40
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepository.java 360290131c6d32200bb725627fb3268222ec755012b8dd8b8ffef44c60ae9f29
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemantics.java fb4b3f0834bb55f15f230b916aaea0a40b34943de71040d0ce0af2cceb001640
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java c8815c97a2185e2c92153def48f58b8790a4fe728d18132643fe9c18655f8cee
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java a6cb5b1a1afe682b3b539db04a922f6213c95992291e3319e04448c950be3141
backend/src/main/java/com/onecall/core/catalog_order/package-info.java 26aade7834fac4dcc4c0cb8986b0cf73baf79eec92feb7d62583807738f50f6a
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java 8478a8ce9f605422d81521aec94625a035641dbe4d3fe8b36eaa82486dfb34d4
backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPort.java b47cba36fb7caf349fb93b6b934be69f2f842624d88976f94377835173995042
backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/package-info.java 0f660b9b5b2bb67aabcd493175cdb04fc34496655886750f8ef122e28334ed03
backend/src/main/java/com/onecall/core/operations_iam/internal/employeeauthority/DefaultEmployeeAuthorityReadPort.java f76b0db87646b0a8fad62256164e26d0805c454a07b2ec88e9990c220904bd61
backend/src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java 01ff82d510de08bf02da5d9737ad75710f998973faacbc883cc31cbfe95cf0fb
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java e32deefb17b395682188612c60fffb4da95b690e7e9b676176a063c1bdd78e1d
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogVersionController.java 5e04236630271a447783ea294252bc063cb146f7c434211113533d5ad120b365
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsProblemWriter.java d46fb4056ae3edc4489e8b71b0787b8ae6adf0c7710ad9771e1def99ed5cdb2b
backend/src/main/java/com/onecall/core/operations_iam/operationscommand/OperationsCatalogSaleCommand.java a99c6c711644fe4e9dcdae32b77e256108ec86a66d11dc47a73f36c5b8985b91
backend/src/main/java/com/onecall/core/operations_iam/operationscommand/package-info.java fbfa75f83eef307f9458a2202b80bbf2cebd91335f34f4ed7d512ac18c762c6d
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java 4f204661ca4512d7112dca3d1043249114b1ce84c3b6876591b91cb16e0acb72
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java 07608529343964ed58da9f608cb68847a64bff0bc31e920d91f2ea2455768093
backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java 02931eff38612241830a3e4d99f936c42e2c4d673ae865d7a9bd8325104b3520
backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java 9accf48cef6775f83a0c2f4d808eeb58c8ad534c44e393fa010fb28167967bdc
backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java 811cf9aecd74f4f3b6180faba42dcc66c4e29fe596d3919d6df5218579786d31
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java 7c5c4a8a654cc187eeee6f3f3bdfa047b46056de2b605680891c6e615a5c4f40
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java 10d01e19451caa78479791f357c6c7e0d04314420a843f02c0f14d839f14b432
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java 4646b6de23b81353a65feff6fed469486b4ee6c245eec2246b7f79e9da71096a
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java acfc4ae711725606643f98ac544918871c4330abd056ae92e1bc7def24418eb1
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java d3a355aa6218620d1ec00d145fbe5567ec04a4aee595d50173a77f0e0b91ac0e
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommandTests.java c83930bcfda5728defc18cb08b437a48ed274d0fc6dee1bf54e98ec32ce8ddc4
backend/src/test/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPortTests.java 0c53ae76efda5b34dda851bd93a88e720644e767d5270758df327dace6a4f4a9
backend/src/test/java/com/onecall/core/operations_iam/internal/web/CatalogVersionRequestBinderTests.java 06519b1ac4a2fa46ffb37585990e4c3aa11cf35f6be393344bea17ad47be5c21
docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md a99ab10664d2b221682df96c863bbc6b127fb38ede90d1a9b769261ea2f9be07
```

## Input and transcript bindings

Input raw SHA256 (unchanged during this task):

```text
docs/gateflow/pa38-operations-catalog-sale-lifecycle-code-review-fix-1.md 356852b572409570a06c9701b23e4ff559e3bc38830c3224dc601ea61030dcff
docs/gateflow/pa38-operations-catalog-sale-lifecycle-code-review-fix-2.md 530946083685fad6c9448d49e7f4f487fffae0d96b49c4b80adda7d737dd110e
docs/gateflow/pa38-operations-catalog-sale-lifecycle-code-review-fix-3.md 7eb7bde5c9d5aaf306b51e5f0a62cc81f4a0d6ada0c335134f9d4f707df729a3
docs/gateflow/pa38-operations-catalog-sale-lifecycle-plan.md 337e27f7cbb7a1f31c0acba474fbe375ad1e587b88e34a3b6089c04c137fd8de
docs/gateflow/project-execution-control.md cf3cb303ed85d543ea5912875ab11c495f8eb1b776cae98fddecd1b188ef0a59
docs/reviews/code-review-20261119-123719.md 55c7b33ef342f52dfa00954b1b7e51543e1be7199b57a99bb4e27131b0705a49
docs/reviews/code-review-20261119-132415.md 9ee93c3b840d9552dd57198a3b1f641133d7a97b60ae220e15f4da7abbe27600
```

Raw evidence log SHA256, relative to the evidence root:

```text
setup.log 1935609ce61c64391d656ce186a52f6f24ef7f4c5f27e7b72f57f0e574903853
preflight.log b8faf0957790e84cd1807975c0930b91ae186f17755af06b4d106773c58693ad
focused86.log 6d7de4e79719f7148a893e5be5955d43c6c0722b43a4aae23bee4939b50cc6cb
guards17.log 890df1c4e15fef3b531a418a34c36d22d6b1401c100bd102e8aba4b0117e9a65
failsafe38.log 3c7b49c1fac554bc48050c47ad420bd3271e5d064535e4b9ea8195d65b4db109
observer-repeat-1.log b7dcef424e3e3d6e0289b05bd0333d89c06c38872abb367497a8c39bec9207b9
observer-repeat-2.log 00f6d8576c6c6f3c1a25c817f82d5425e0f1c87aac4d985797c688e1279d0da4
observer-repeat-3.log 93184c91591dfc15e9e2d8191eac1a2bf116e007739b0a60e0652e07c865a1df
clean-full-oracle.log 594af643d9df492d3b76e67f53f72f3183b004b98a029c1995e19c5eab79ca5b
xml-audit.log e0e0d3d1dfc0706f88e28060d6f165c72ec109c5d3a5183fd778af12b7245f4c
static-final.log da36760d6726d209fcf203f8b9e1c9d250b0316f2c1f15e2038a92ba020da63b
```

Final post-artifact path custody is recorded externally in `handoff-static.log` and `handoff-custody.json`, avoiding an artifact self-hash cycle. `/tmp` scripts and XML are evidence only, not candidate paths or replacement acceptance oracles.

## Residuals and stop boundary

- **Controller / Gateflow validation owner:** manifest-carrier scope conflict blocks required green validation. An authorized digest-only update followed by guards, clean full and its same-shell unchanged oracle/135-file custody is still required; this author does not expand the allowlist.
- **Fresh independent aggregate reviewers / Controller:** assess candidate remediation of only the three accepted aggregate findings; no independent re-review or gate decision is performed here.
- **Existing residual owners unchanged:** Backend test-hardening retains PA35 CRLF, filtered nonempty witnesses and observer scheduling; Security retains lock-free authority/admission races; Platform/Operations retain shared-pool capacity, commit-then-cleanup semantics, audit/Outbox retention/dispatch and absent separate retirement audit; future structural-recovery/filter/limiter work remains with its already classified owners. This bounded patch does not silently resolve or reclassify them.

**Stop:** aggregate-fix artifact and completion report only. No stage, commit, branch creation, push, PR, merge, approval, mark-ready, reviewer request, external comment or issue mutation. No production deployment or readiness claim. Next intended gate remains fresh independent aggregate re-review, after Controller disposition of the validation blocker.
