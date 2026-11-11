# PA27 customer business-number read foundation: code-review-fix-1

- Task: `pa27-business-number-code-review-fix1-gpt-20261111-01`.
- Work unit: `PA27-customer-business-number-read-foundation`; author gate: `fix`.
- Date: 2026-11-11, Asia/Shanghai.
- Authority: Controller-accepted `PA27-CR-01..05` only; accepted cumulative plan, plan-fix-1, S1 implementation/fix reports and independent reviews `code-review-20261111-145158.md` / `code-review-20261111-154533.md`.
- Durable evidence: `D:\codex-temp\pa27-code-review-fix1-evidence-20261111-01`, mounted at `/mnt/d/codex-temp/pa27-code-review-fix1-evidence-20261111-01`.
- Disposition: all five accepted findings are fixed for this bounded author candidate; focused PostgreSQL execution, fresh full clean verification and unchanged exact source=CI=XML inventory pass. This author artifact is not Controller acceptance, approval, re-review entry or production acceptance.

## 1. Accepted findings and in-place evidence

| Finding | Existing method(s), unchanged names/counts | Added proof |
| --- | --- | --- |
| `PA27-CR-01` | `CustomerBusinessNumbersIT.emptySingleAndExactLimitPagesHaveExactTailSemantics` | Obtain a real signed cursor from a nonterminal one-row HTTP page; delete every binding except its anchor; independently query that the anchor still exists; continue through real HTTP and assert 200, empty items, explicitly present null nextCursor and false hasMore. The existing fixture also retains exact page property names and no-store assertions. |
| `PA27-CR-02` | `CustomerBusinessNumberQueryServiceTests.webLazyConfigurationAndSeparateReadOnlyRepeatableReadTemplateAreExact` | Invoke the actual customerQueryDsl factory with a mock datasource, assert PREPARED_STATEMENT, executeLogging=false and INDEXED, and independently assert that the service DSL parameter has the customerQueryDsl qualifier. Existing transaction/isolation/lazy/bean-count assertions remain. |
| `PA27-CR-03` | `missingActivationDefaultsToInactiveAndForeignProjectionFailsClosed`, `lookaheadUsesLastReturnedRowAndNullCursorOnTail`, `technicalAndProjectionFailuresReturnNoPartialOrSensitivePage` | Exercise the returned-value guard matrix below; retain every original missing/default activation, duplicate-row, lookahead and exception assertion. |
| `PA27-CR-04` | `CustomerBusinessNumberCursorCodecTests.keyReplacementInvalidatesOldCursorAndLimitDoesNotBindScope` | Strengthen wrong-key rejection to the exact public CURSOR_INVALID code. Execute a real composition service with reader/transaction doubles: initial limit=1 produces the tested signed cursor; the same cursor decodes into a limit=100 continuation returning the next row. Verify exact reader fetch sizes 2 and 101, unchanged trusted scope, exact returned binding and terminal-page metadata. The existing identical-scope roundtrip assertion remains. |
| `PA27-CR-05` | `CustomerBusinessNumberReadPrivilegeIT.pa25ApplyCheckRequireExactV015HistoryAndIndependent261TupleOracle` | After the existing fourteen independently removed-tuple probes, inject an owner-issued forbidden number_hmac SELECT tuple, assert exactly 262 tuples and the literal extra tuple, then independently reject both PA25 apply and check with exact widened ACL and complete catalog equality. Revoke only the injected privilege in finally, assert exact original ACL/catalog restoration, run a successful unchanged check and assert unchanged original catalog/ACL again. Existing history mutations and final successful apply/check/261 assertions remain. |

### Returned-value guard matrix (`PA27-CR-03`)

All negative service cases assert `CustomerContextException.Code.INTERNAL` with no retained cause; no partial page can be returned from the failing call.

| Guard | Independent input/evidence |
| --- | --- |
| Null collection / null row | Null reader result; singleton list containing null |
| Required IDs/time | Separately null bindingId, subjectId and subjectCreatedAt |
| Timestamp precision | Non-microsecond timestamp, otherwise valid row |
| Mask | Separately null, empty, spaces-only, 65 ASCII code points, 65 supplementary code points, embedded NUL, unpaired high surrogate and unpaired low surrogate |
| Enum values | Null and corrupt numberType, bindingStatus and realNameStatus, each independently |
| Duplicate binding | Same binding ID with otherwise valid different subject/time; original exact-duplicate-row case also retained |
| Bounded rows | Three otherwise valid ordered rows at limit=1 reject; 101 otherwise valid ordered unique-subject rows at limit=100 succeed, returning 100 with the hundredth row as anchor |
| Order / seek | Later timestamp following an older row; descending UUIDs at equal timestamps across the unsigned high-bit boundary; row newer than request anchor; exact anchor replay; lower UUID at the same anchor timestamp |
| Activation returned value | Foreign subject, corrupt status, null status map, null status and null subject key independently reject |
| Duplicate activation | Inject the real JooqCustomerActivationStatusReader with mocked SQL rows containing the same subject twice; the composition call rejects the duplicate as sanitized INTERNAL, and one exact bound query is verified |
| Unique bounded activation input | Two bindings on one subject produce exactly one single-subject activation call; 101 unique lookahead subjects produce exactly one bounded batch call with no extra interactions |
| Missing/default activation and valid mask boundary | Original absent-row INACTIVE and full literal valid-state Cartesian assertions retained; a valid 64-supplementary-code-point mask succeeds without truncation |

No invalid semantic map containing duplicate keys is fabricated. Duplicate SQL projections are exercised at the actual owned reader that rejects them, inside an existing service test, without changing its implementation or helper classes.

## 2. Exact changed paths and hashes

Exactly these six repository paths are authorized and delivered. The first four are test-only changes inside existing test methods. The implementation report receives only one appended agreed-fix-1 section; this artifact is new.

| Path | SHA256 |
| --- | --- |
| `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerBusinessNumberQueryServiceTests.java` | `bb5d702b29f02a37d3fb1bb5ec06b55abc71b2c5ad08a7e91123029477dd06aa` |
| `backend/src/test/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumberCursorCodecTests.java` | `3b5562d71f2a3e2b8ddaf85d12582c78123cb117a23f84c2f4ddda7f2f4f11b1` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersIT.java` | `17d3f1bcaec6a1970a78f89512db2fe0aa4321e4b9903101d9e78d94cd5b8a38` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java` | `5aed37155f520653d7f1efc74636df16fd89c4a282ffeba2ef67ef590f5c7552` |
| `docs/gateflow/pa27-customer-business-number-read-foundation-s1-implementation.md` | Final raw hash recorded externally in `delivery.sha256` and `scope-audit.json`; intake bytes retained in `intake/` and verified as the exact unchanged prefix |
| `docs/gateflow/pa27-customer-business-number-read-foundation-code-review-fix-1.md` | Final raw hash recorded externally in `delivery.sha256`; not self-embedded |

`baseline.json` records all 1,788 intake non-build files and modes. `audit.py` compares all non-build paths, verifies the exact six-path final write set and preserved modes, unchanged existing test names/counts/setup/helpers, and original source/assertion lines (with the sole explicitly stronger wrong-key type assertion). It checks strict UTF-8/LF, no BOM/trailing whitespace and one terminal LF for changed outputs. No historical artifact or protected line ending is rewritten. The rejected IT rename and EOL-only reviewer observations are not implemented.

## 3. Execution environment and evidence custody

Linux validation uses `/opt/jdk21` (Temurin 21.0.13+9), the checked-in Maven wrapper's pinned Maven 3.9.9 distribution and unchanged SHA256 properties, and Docker server 29.1.3. The checked-in CRLF wrapper remains byte-identical; an external LF execution copy in evidence `wrapper/` is used. Node remains the existing external v22.23.4 installation. No dependency, POM, CI, test-skipping/ignoring flag, Mockito preload or validator bypass is introduced.

The actual `postgres:16` image executes PostgreSQL 16.15 (Debian 16.15-1.pgdg13+2); repository digest `sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`. All databases and credentials are disposable test fixtures, never production.

Every required validation stem retains `.command`, `.log`, `.exit`, `.started` and `.finished`. Focused XML is copied before clean; full XML, per-class identities and zero-failure/error/skip summaries are retained separately in `reports/`, `focused-unit-xml-summary.json`, `focused-postgres-xml-summary.json` and `final-xml-summary.json`. Final source/CI identities are in `source-ci-inventory.json`; full measured XML is in `xml-measured.json`. `evidence-manifest.sha256` seals the durable evidence, including final report copies and all six delivery hashes, without self-referential document hashes.

## 4. Validation results

Validation is complete against the final test sources. Counts below are freshly observed execution/XML and exact source=CI=XML results, not expected arithmetic. Every successful stem has an actual zero exit and durable log/command/timestamps; the initial failed attempts remain separate.

- Initial `focused-unit` exited 1 during test compilation because the added anchor-existence assertion called the existing zero-parameter `one` helper with a bound parameter. No test executed in that attempt. Corrected only the new assertion to an inline prepared statement inside the same allowed IT method; no helper or production edit. The failure log and exit are retained.
- `focused-unit-rerun` and `focused-unit-xml` exit 0: two classes / 16 cases, zero failures/errors/skips.
- `focused-postgres` and `focused-postgres-xml` exit 0: Surefire two classes/16 cases plus Failsafe five classes/122 cases (endpoint 18, privilege 8, PA25 20, PA26 12, migration baseline 64), all zero failures/errors/skips, with actual PostgreSQL 16, full V001-V015 chain, PA25 apply/check and role restoration.
- `frozen-protected-preliminary` exits 0; OpenAPI/V001-V015 and 663 protected history/CI/POM/Control entries match intake. `scope-audit-preliminary` and `scope-audit-six-paths-rerun` exit 0, proving four test changes initially and exactly six paths after report creation. The intervening `scope-audit-six-paths` exit 1 was an external audit-script bookkeeping error: it newly included the unchanged `.git` pointer that intake intentionally excluded. Aligning the external scan and independently asserting the exact original pointer bytes resolves it without any repository metadata edit. The failed audit remains retained.
- `final-clean-verify` exits 0: `-B -ntp clean -Pcontainer-smoke verify`, elapsed 15:49, finished 2026-11-11 16:30:49 +08:00. Surefire 40 classes/341 cases and Failsafe 17 classes/272 cases, all zero failures/errors/skips. All 57 fresh XML reports are copied in `reports/final/` with exact method identities and summaries.
- `final-xml`, `exact-ci-inventory` and `measured-inventory` exit 0: **40/341 + 17/272 = 57/613**, annotation-line scan **614**, exactly **seventeen Failsafe includes**. The checker is the unchanged dedented current CI Python block, SHA256 `71d575bfdfae93b3000f67d005af3d04a1f801024bf9ddd2ce25e9e6cfd9292f`; all historical checkpoints, source/CI method-name sets, duplicate checks, exact XML identity and zero-skipped/error/failure requirements pass. `source-ci-inventory.json` and `xml-measured.json` retain the full identities.
- `frozen-protected` and final `frozen-final` exit 0: all fifteen plan-frozen inputs (OpenAPI plus V001-V014), V015 and 663 protected entries remain byte-identical to intake. OpenAPI SHA256 remains `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68`; V015 remains `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3`. All production Java, PA25 SQL and module wiring remain unchanged by whole-file comparison as well.
- `scope-audit-final` and `prepare-seal` exit 0: exactly five modified paths plus this one new artifact; **1,783 other intake files unchanged**, original report bytes retained as the exact prefix, preserved modes, method counts 8/8/18/8, original assertion/setup/helper preservation and changed-file UTF-8/LF checks pass. `delivery.sha256`, `scope-audit.json`, `sealing-checks.json`, final delivery copies and the externally verified `evidence-manifest.sha256` retain final hashes/results without self-reference.

Representative exact commands (the external wrapper only adapts protected CRLF for execution):

```bash
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:/opt/node-v22.23.4-linux-x64/bin:$PATH
cd backend
/mnt/d/codex-temp/pa27-code-review-fix1-evidence-20261111-01/wrapper/mvnw -B -ntp -Dtest=CustomerBusinessNumberQueryServiceTests,CustomerBusinessNumberCursorCodecTests test
/mnt/d/codex-temp/pa27-code-review-fix1-evidence-20261111-01/wrapper/mvnw -B -ntp -Pcontainer-smoke -Dtest=CustomerBusinessNumberQueryServiceTests,CustomerBusinessNumberCursorCodecTests -Dit.test=CustomerBusinessNumbersIT,CustomerBusinessNumberReadPrivilegeIT,ProductionDatabaseIdentityProvisioningIT,DatabaseRuntimeAclRbacHardeningIT,PostgresContainerIT verify
/mnt/d/codex-temp/pa27-code-review-fix1-evidence-20261111-01/wrapper/mvnw -B -ntp clean -Pcontainer-smoke verify
python3 /mnt/d/codex-temp/pa27-code-review-fix1-evidence-20261111-01/ci-exact-inventory.py
```

After final report writing, the last scope/frozen/sealing checks verify the delivered bytes and copy all six outputs under evidence `delivery/`. The evidence manifest covers commands, exits, timestamps, logs, scripts, intake, XML, summaries, inventories and delivered copies. No post-validation Java edit, re-run substitution, ignored failure, stale full XML or hosted-CI assertion is used. The archive-layout note distinguishes original focused-unit-rerun XML from the later focused-PostgreSQL unit execution; only byte-identical duplicate copies of the latter were removed.

The ordinary Linux `git status` intake command cannot resolve this worktree's Windows `D:/...` gitdir pointer; no metadata is rewritten to work around it. Bounded path/byte/mode evidence is based on the independent captured file manifest, not an invented clean Git/remote claim.

## 5. Preserved boundaries and residuals

No production Java, SQL, V001-V015, OpenAPI, module wiring, fixtures/helpers, CI/POM, plan/fix/review history, Control or unrelated documentation change is authorized or made. Original test method counts remain service 8, codec 8, endpoint IT 18 and privilege IT 8; exact acceptance targets remain 40/341 + 17/272 = 57/613, scan 614 and seventeen includes. The new tests strengthen evidence, not product semantics or privilege grants.

Existing residual ownership is unchanged: Security/Operations own mandatory cursor-key custody, distribution and rotation; Security/Database own column-grants-not-RLS/application tenant filtering; Backend/Architecture own deferred qualified-DSL inversion; Backend/Database own representative large-set sort/index EXPLAIN; Platform/Database/Security/Operations own Q17/Q23, migrator/comment custody, real credentials/TLS/pg_hba, pool drain and old-session rotation. Repeatable-read remains per request, not cross-page snapshot or linearizable revocation. Local executable evidence is not hosted CI, deployment or live production acceptance.

Stop after this bounded fix and truthful completion report. No stage, commit, branch creation, push, PR creation/mutation, merge, approval, mark-ready, reviewer request, external comment or re-review entry is performed or authorized. Controller alone owns the next gate decision. Production **NOT EXECUTED / NOT CLAIMED**.
