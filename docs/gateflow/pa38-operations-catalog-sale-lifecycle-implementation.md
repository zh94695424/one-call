# PA38-S1 implementation — current evidence and Controller trace

## Current aggregate evidence and Controller trace

- Task: `pa38-operations-catalog-sale-lifecycle-aggregate-rereview2-fix1-qwen38flash-20261119-01`; bounded documentation-only closure of accepted `PA38-AGG-RR2-02` (Low) and `PA38-AGG-RR2-03` (Low). Only this report, the supplemental aggregate fix2 artifact, the aggregate re-review fix1 artifact and the new `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview2-fix-1.md` change. Product/test/migration/OpenAPI/POM/CI/review/evidence and Control bytes remain untouched, and historical aggregate fix1 is not rewritten.
- RR3/Controller trace: aggregate re-review3 findings `PA38-AGG-RR3-01..04` and re-review4 findings `PA38-AGG-RR4-01..06` were resolved through documentation-only edits to this report, the aggregate fix2/re-review fix1/re-review2 fix1 artifacts, Control, the new `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview3-intake.md`, and new review artifacts. Product, test, migration, OpenAPI, POM, CI and evidence bytes remain unchanged; this report and fix2 carry the final candidate re-pin. The current final aggregate re-review gate is the one named as current in Control, which also names the intake path and hash-chain endpoint.
- Currency pointer: this section and the final original 32-path raw-byte total plus ordinal path+raw-SHA256+LF aggregate recorded in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-2.md` govern future state. Every heading below the two current sections of this report is historical evidence for its own gate: either the gate is named in the heading (the renamed `Fix1-gate historical …`, `Fix2-gate …`, `Fix3-gate …` and `Aggregate-fix1-gate …` labels, previously worded as `Current …`), or the heading sits inside a region whose introductory paragraph names the gate — as the `## Historical fix3 implementation evidence` lead now states for every heading from `## Gate and bindings` through `## Residuals and disposition`.
- No historical measurement value is rewritten in this gate. Renames and pointers only: heading levels, label prefixes and supersession pointers. Superseded manifest pins, XML hashes, per-path hash tables, validation totals and runtime versions keep their exact recorded bytes and remain readable as the evidence of their own gate.
- Documentation truth from the aggregate re-review fix1 gate is retained below in substance, with the `PA38-AGG-RR3-04` precision correction: `platform.outbox_append` returns the existing event id for an identical-payload duplicate with no error or rollback; the collision path raises `P0001` for same-key/different-payload, and the function also has a defensive `P0001` when the post-conflict row cannot be re-read plus explicit `P0001` validation branches. PA38 replay safety remains the advisory transaction lock + post-lock state short-circuit + revision/current-state CAS. The extended supersession pointer covering historical aggregate fix1 lines 38 and 40 is in the aggregate re-review fix1 artifact.
- Runtime currency is unchanged and every PostgreSQL 16.15 statement below is now unmistakably a historical gate measurement; current executed LifecycleIT evidence remains PostgreSQL **16.16** from the floating **`postgres:16`** image with no pinned digest.
- Structure: this report has exactly one top-level H1 (this heading). The duplicate historical fix3 H1 is demoted inside `## Historical fix3 implementation evidence`, so the fix3 narrative headings beneath it are read as that historical region.
- Not a rerun: no Maven, container, PostgreSQL, oracle or test execution occurs here. Focused 86/86, guards 17/17, Failsafe 38/38, observer 1/1 three times, clean full 97/934 + 38/630 and shipped oracle/135-report custody remain previously recorded green results. Java remains **290/180**; production manifest remains **310 / `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045`**; migrations remain V001–V018 with no V019.

## Aggregate re-review fix1 gate — retained supporting truth

- Task: `pa38-operations-catalog-sale-lifecycle-aggregate-rereview-fix1-gpt-20261119-01`; bounded documentation-only closure of accepted `PA38-AGG-RR-01` (Medium) and `PA38-AGG-RR-02` (Low). Only this report, the supplemental aggregate fix2 artifact and new `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview-fix-1.md` change. Product/test/migration/OpenAPI/POM/CI/evidence and Control bytes remain untouched; historical aggregate fix1 is not rewritten.
- Corrected replay truth: PA38 product replay safety comes from the advisory transaction lock + post-lock state short-circuit + revision/current-state CAS. `audit.audit_fact.request_id` is only a correlator. Frozen `platform.outbox_append` uses `ON CONFLICT (event_domain, dedupe_key) DO NOTHING`: an identical duplicate `payload_sha256` returns the existing event id idempotently, with no error or rollback; the collision path raises `OUTBOX_DEDUPE_CONFLICT`, SQLSTATE `P0001`, for same-key/different-payload and rolls back the mutation transaction, including its audit append. The function also has a defensive `P0001` at lines 363-364 when the post-conflict row cannot be re-read, and explicit validation `P0001` branches. Its dedupe index cannot surface an uncaught `23505`; Java's `23505` recovery is driven by the catalog active unique index, not Outbox dedupe. SQL/source evidence is in the fix artifacts.
- Under a hypothetical bypass of the product protections, an identical-payload Outbox skip could leave an unpaired audit fact with no Java-visible signal. Owner Platform/Operations + Backend; destination later pairing/detection hardening. Both publication events still commit atomically; delivery order is not guaranteed by `(available_at,id)`, deferred to Platform/Operations' later dispatcher/consumer contract (`PA38-AGG-RR-03`).
- Retained runtime truth (re-confirmed by aggregate re-review2 fix1): executed LifecycleIT evidence records PostgreSQL **16.16** from the floating **`postgres:16`** image, not a pinned digest. Existing Controller validation remains focused 86/86, guards 17/17, Failsafe 38/38, observer 1/1 three times, clean full 97/934 + 38/630 and shipped oracle/135-report custody PASS. These are prior green results, not reruns. Every PostgreSQL 16.15 run below is an explicitly gate-labeled historical measurement; image pinning is later test-infrastructure work.
- Controller dispositions remain `PA38-AGG-RR-04/06` rejected-with-reason and `PA38-AGG-RR-03/05/07` deferred-with-owner; rationale, residual owners/destinations and final raw custody are in the new fix artifact. Historical static candidate files are not overwritten. Original 32-path custody is recomputed after this report's final bytes and held in fix2, outside the candidate set, avoiding self-reference; all aggregate fix artifacts are supplemental. Java remains 290/180; manifest remains 310 / `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045`; migrations remain V001–V018, no V019.
- Aggregate re-review fix1 gate completion: accepted documentation corrections complete, not aggregate acceptance or production readiness. The next gate named there (**fresh independent A/B aggregate re-review**) was subsequently delivered through the current aggregate gate; the current gate is the one explicitly recorded at the top of Control. No staging, commit, push, PR mutation, merge, approval, mark-ready, reviewer request, external comment or issue mutation.

## Historical aggregate deepreview fix2 final evidence — November 19, 2026

- Final evidence task: `pa38-operations-catalog-sale-lifecycle-aggregate-fix2-final-gpt-20261119-01`; complete the existing bounded fix2 gate and close Controller finding `PA38-AGG-FIX1-PIN-01`. The previously authored, test-only carrier change synchronizes the existing 310-file manifest digest literal to `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045`. Count 310, every other assertion/hash, every `@Test` identity and every other carrier byte remain unchanged. This finalization edits only this report and the supplemental fix2 artifact; no Java/test, evidence, fixture or Control bytes are edited.
- Fix2-gate production manifest and guard pin, unchanged through aggregate re-review2 fix1: **310 / `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045`**. Prior `3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0` is historical. Aggregate fix1's scope blocker and failed validation below are retained as historical evidence, not the current pin/status.
- Controller's canonical validation on `/tmp/pa38-lf-validation.hgmbb4/candidate` is complete: focused **86/86**, guards **17/17**, Failsafe **38/38**, exact observer **1/1 three sequential times**, and clean full **97 reports / 934 tests + 38 reports / 630 tests**, all zero failures/errors/skips. The smoke class is **8/8** in the canonical XML; the supplied evidence root has no separate smoke transcript. The shipped CI/XML oracle and 135-report custody pass. Independent `xml-audit.py` method-identity/totals checks agree with `test-summary.json` and `full-xml135.sha256`; final XML aggregate is `316abf43769a81c897ab2c96733af20f3e35acf7e22983bf188ad7c3e505f422`. These are Controller's existing results, verified read-only here, not newly executed Maven or oracle results.
- Fix2-gate closure evidence, exact recorded commands/exits, source/CI/XML identities, frozen inputs and the original **32-path final raw-byte total and ordinal-path aggregate** are recorded in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-2.md`, whose 32-row listing is the current authority and is recomputed after every later documentation edit to this report. That binding is recomputed only after this report reaches its final bytes; it includes this report's final raw SHA256. Its value is deliberately held in the supplemental artifact, outside the original 32 paths, avoiding a report self-hash cycle. Java remains **290/180**; V001–V018/OpenAPI/docs12/14/15/17/PA25 stay frozen, with no V019. Both aggregate artifacts are supplemental, not additional original implementation paths; Control is unchanged.
- Accepted aggregate remediations remain intact: both POST handlers authenticate before reading, use `readNBytes(8193)` before strict parsing and provider lookup, accept valid exactly **8192-byte** bodies, and reject **8193-byte / 2 MiB / missing / zero-byte / truncated** bodies with sanitized `422 VALIDATION_FAILED`. Oversized reads stop at 8193 bytes; truncated reads map `IOException` without parsing the partial body. Invalid bodies cause no provider construction, command invocation, authority/database/lock work or facts. Authentication/verifier failures consume no body bytes. Audit request IDs are correlators, not audit-side uniqueness constraints; product replay safety relies on advisory locking, post-lock state short-circuit and revision/current-state CAS, not an error on identical Outbox duplicates.
- Residual owners remain Platform/Security/Operations for deployment, pools and secrets; Database/Security/Catalog-order for existing broad ACLs; Platform for audit/Outbox retention and dispatch; Security for verifier policy; API governance/Catalog-order for future slices; Backend test-boundary owner/Controller for primary PA35 CRLF and the accepted LF projection. No production readiness or later acceptance is asserted.
- That historical fix2 gate's next step was a fresh independent A/B aggregate re-review, not entered by that author. Bounded fix2 final evidence was complete at that gate; this is not aggregate acceptance or production readiness. No staging, commit, push, PR, merge, approval, readiness mark, reviewer request, external comment or issue mutation.

## Historical aggregate deepreview fix1 status — November 19, 2026

- Task: `pa38-operations-catalog-sale-lifecycle-aggregate-fix1-gpt-20261119-01`; implement only `PA38-AGG-REV-01/02/03` in the four explicitly authorized paths. **Code/documentation remediations implemented; required aggregate validation is blocked, not implementation-pass or gate acceptance.** All sections below the historical fix3 heading describe prior gates, not the current validation outcome.
- Both exact catalog POST handlers retain `caller(request)` first, then locally acquire the servlet input stream and call `readNBytes(8193)`. Framework `@RequestBody byte[]` buffering is removed. No more than 8193 body bytes are consumed; the unchanged strict parser retains `body.length > 8192` and rejects the sentinel byte with sanitized `422 VALIDATION_FAILED`. Exactly 8192 bytes of padded valid JSON succeed. Missing/explicit zero-length bodies and a stream throwing `IOException` after seven bytes now produce the same sanitized `422`, not `500 INTERNAL`.
- Existing Web identities contain count-preserving probes for both routes, including declared/unknown-length 8192-byte acceptance and 8193-byte/2 MiB rejection. Oversize consumes exactly 8193 bytes, zero length consumes zero, and truncated I/O consumes seven; invalid input resolves no command provider and invokes no command, so it cannot enter command-side authority, database, lock, audit or Outbox work. Fail-if-read streams prove `401` (missing/invalid/wrong-audience credential) and verifier `503` precede even input-stream acquisition. After authenticated bounded parsing of valid 8192-byte input, lazy provider construction still fails as sanitized `503`.
- Audit truth (`PA38-AGG-REV-03`, corrected by `PA38-AGG-RR-01`): **`audit.audit_fact.request_id` is an identity correlator, not an enforced uniqueness key.** Product replay safety is the advisory transaction lock + post-lock state short-circuit + revision/current-state CAS. Audit and Outbox appends share the mutation transaction. Frozen `platform.outbox_append` suppresses the `(event_domain, dedupe_key)` insert conflict and returns the existing id for an identical `payload_sha256`, with no error or rollback; same-key/different-payload raises `OUTBOX_DEDUPE_CONFLICT` / `P0001` and rolls back. The dedupe index does not produce an uncaught `23505` through this function; Java `23505` recovery concerns the catalog active unique index. A hypothetical bypass could commit an unpaired audit fact after a silent identical-payload skip, with no Java-visible signal: Platform/Operations + Backend own later pairing/detection hardening. No schema, index or migration is added. Historical aggregate fix1 remains unchanged; the new fix artifact explicitly corrects its earlier overclaim.
- Environment/preflight: `/opt/maven-3.9.9/bin/mvn -B -ntp`, `JAVA_HOME=/opt/jdk21`, Java 21.0.13, Docker Client/Server 29.1.3, real PostgreSQL **16.16**. Accepted LF projection `/tmp/pa38-lf-validation.oYmOvd/candidate`; all tested Java inputs equal primary bytes. Only the projection's previously authorized PA35 document materialization differs from primary frozen bytes.
- Executed commands/exits: focused eight-class `-Dtest=EmployeeAuthorityReadPortTests,CatalogSaleLifecycleSemanticsTests,CatalogSaleLifecycleRepositoryTests,DefaultOperationsCatalogSaleCommandTests,CatalogVersionRequestBinderTests,OperationsCatalogVersionWebTests,OperationsCatalogVersionConfigurationTests,OperationsCatalogVersionBoundaryTests test` **0 / 86 of 86 pass**; guards `-Dtest=ModuleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests test` **1 / 16 of 17 pass**; two-class `-Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT failsafe:integration-test failsafe:verify` **0 / 24+14=38 of 38 pass**; exact `-Pcontainer-smoke '-Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner' failsafe:integration-test failsafe:verify` **three sequential exits 0 / 1 of 1 each**. Direct Failsafe goals are separate live evidence, not a replacement for lifecycle `verify`.
- Canonical unchanged `clean -Pcontainer-smoke verify` **exits 1**: 97 Surefire reports / 934 tests, **one failure**, zero errors/skips; the same manifest-pin assertion is the sole failure. Failsafe 38/630, the success-gated same-shell shipped CI/XML oracle and accepted 135-file custody are **not reached**. Do not reuse historical XML, invoke the oracle against focused reports, waive the assertion or describe this as a green full gate.
- Historical aggregate fix1 manifest recomputed to **310 / `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045`**, while its then-out-of-scope carrier `EvidenceRoleIvrAssociationAclBoundaryTests.v001ThroughV017AndPublicContractsRemainByteFrozen` pinned `3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0`, raw SHA256 `02931eff38612241830a3e4d99f936c42e2c4d673ae865d7a9bd8325104b3520`. That four-path scope blocker was subsequently authorized for lockstep correction by Controller finding `PA38-AGG-FIX1-PIN-01`; the current fix2 pin is recorded above.
- Aggregate-fix1-gate recorded raw controller SHA256 (both values remain byte-identical to the current fix2 32-row listing): `5e04236630271a447783ea294252bc063cb146f7c434211113533d5ad120b365`; Web SHA256: `07608529343964ed58da9f608cb68847a64bff0bc31e920d91f2ea2455768093`. The original 32-path implementation/evidence candidate set remains unchanged; that gate's final report hash, all 32 hashes, raw byte total and ordinal path/hash/LF aggregate were recorded in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-1.md` to avoid a self-hash cycle, and are superseded for current bytes by the recomputed listing in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-2.md`. The new fix artifact is supplemental evidence, not a 33rd implementation path.
- All **14 Web identities**, eight-class **86** and two PostgreSQL-class **38** identities are preserved; Java files remain **290/180**. Source identities equal the unchanged shipped CI maps. V001–V018/OpenAPI/docs12/14/15/17/PA25 remain unchanged, no V019; raw migration inventory aggregate remains `53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70`. No new production class/dependency, Java test class, test identity, POM/CI/ACL/OpenAPI/schema change, unapproved session write or tracked deletion occurs.
- Evidence recorded at this historical aggregate-fix1 gate: expanded commands/exits, body assertions and XML hashes, three observer transcripts, failed clean-full transcript, immutable manifest mismatch and static/path custody. **That gate stopped after its fix artifact and completion report.** Its next step was a fresh independent aggregate re-review, subject to then-pending Controller disposition; it is historical. The current gate is the one explicitly named in Control.

## Historical bounded fix3 status — November 19, 2026

- Task: `pa38-operations-catalog-sale-lifecycle-code-review-fix3-gpt-20261119-01`; close only Controller finding `PA38-FIX2-PIN-01`. Controller authorized synchronizing the existing carrier after fix2's accepted dead-qualifier removal; no product behavior changes in fix3.
- Fix3-gate historical production manifest (superseded by the fix2-gate pin recorded above and in aggregate fix2): **310**, SHA256 **`3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0`**, recomputed at that gate by the unchanged backend-relative path / CRLF-to-LF file-hash / ordinal rows / final LF algorithm. The guard changes exactly that digest literal; a raw byte replacement assertion preserves every other carrier byte, hash, assertion and test identity. `e9de000fabce960d271e819e870390bcb151e8b03b1d7292c43b5c0b391e65d8` is the historical fix1/before-fix3 pin only.
- Fresh accepted LF validation root `/tmp/pa38-lf-validation.SzsubC/candidate`; all Java inputs byte-identical to primary. Only the existing PA35 plan's 1,344 CRLF lines are normalized in the projection; no primary frozen input changes. Final report/artifact completion follows execution and is covered by final raw custody, not presented as the setup inventory.
- Historical fix3-gate preflight and runtime: `/opt/maven-3.9.9/bin/mvn -B -ntp`, `JAVA_HOME=/opt/jdk21`, `/opt/jdk21/bin` first on PATH; Maven 3.9.9, Java 21.0.13, Docker Client and separately observed Server field 29.1.3; all four preflight commands exit 0. Real PostgreSQL 16.15 measured at that fix3 gate, historical only; current execution is PostgreSQL 16.16 from the floating `postgres:16` image.
- **Executed fix3 gates, each exit 0, zero failures/errors/skips:** smoke **1/8**; guards **2/17**; focused **8/86**; canonical two-class PostgreSQL verify **Surefire 97/934 + Failsafe 2/38 = 24+14**; three sequential exact `OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner` executions **1/1 each**; canonical `clean -Pcontainer-smoke verify` **97/934 + 38/630**; its same-shell unchanged shipped CI/XML oracle and **135-file custody**; XML and final static/frozen/path audits.
- Clean full XML ordinal filename/raw-SHA256/LF aggregate: **`f187461a6e0d3d1ac4e29be8fc2e0cff45b9e738e317012ceb6192d4a0549cb0`**. Source=CI=POM=XML identities remain exact, POM Failsafe includes **38**, Java source counts **290/180**; all guard and PA38 method identities are unchanged. Frozen V001–V018/OpenAPI/docs12/14/15/17/PA25 remain unchanged, no V019; raw migration aggregate **`53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70`**.
- Fix3-gate historical raw candidate bindings (not production-manifest row hashes; superseded by the current fix2 32-row listing):

```text
backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java 02931eff38612241830a3e4d99f936c42e2c4d673ae865d7a9bd8325104b3520
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java c8815c97a2185e2c92153def48f58b8790a4fe728d18132643fe9c18655f8cee
.github/workflows/ci.yml 11dfd54c9c0c797ea30142d2a0a614df6385c724f54a343f6bfc5a2b8677209a
backend/pom.xml f8fc1389d46a0af34ebb8b538947c6571211b44a0e19be7875ab3923dfee0fb8
```

- Fix3-gate final **32-path** raw byte total, ordinal aggregate and all per-path hashes then current (including this report's hash at that gate), exact commands/exits, test identities, XML filenames/hashes and residual owners are recorded in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-code-review-fix-3.md`, with raw evidence in `/tmp/pa38-fix3-evidence/`. Those fix3-gate values are historical and are superseded by the recomputed listing in aggregate fix2. Keeping this report's hash outside the report avoids a self-hash cycle.
- Public work-unit audit: **32 original + fix1/fix2/fix3 artifacts + Controller Control = 36**; zero tracked deletions, zero staged paths, no private `.pa38-*`/`.codex` inclusion. Four pre-existing unchanged review artifacts and one pre-existing ignored unchanged generated build JSON are separately reconciled in the raw status audit, not counted as new candidate edits.
- **Status at the historical fix3 gate: bounded fix3 complete; `PA38-FIX2-PIN-01` closed in the then-candidate, all requested gates then green. Its next step was `re-review`.** No re-review, acceptance, readiness, repository publication or external mutation was performed at that gate. The PA35 raw-CRLF issue remains deferred-with-owner; deployment/operations/scheduling risks retain their existing owners.

## Historical bounded fix2 status — November 19, 2026

- Task: `pa38-operations-catalog-sale-lifecycle-code-review-fix2-gpt-20261119-01`; bounded author, not acceptance authority.
- Accepted code corrections `PA38-RR2-02..04` are implemented. **Validation is blocked by an out-of-allowlist production-manifest carrier; this candidate does not have a green 17/17 guard gate or green canonical full verify.** The successful fix1 results below are historical and must not be attributed to fix2.
- `PA38-RR2-02`: each fixture reset initializes a lock-attempt latch and backend PID. Immediately before executing the product advisory-lock SQL, the jOOQ listener reads `pg_backend_pid()` from the creating command connection and releases that latch. A separately started observer proves readiness before command submission, awaits that exact PID, and polls only `pid = ? AND wait_event_type = 'Lock'`. Its observer-started and wait-observed latches are separate; another same-login backend cannot satisfy the witness. The 250ms product timeout, real 55P03/409, zero facts/writes, concurrent same-pool read, lifecycle byte equality and session-restoration assertions remain intact.
- `PA38-RR2-03`: only the manually constructed command's inert constructor `@Qualifier` and import are removed. The actual qualifier on `CatalogSaleLifecycleConfiguration`'s bean-method parameter is unchanged.
- `PA38-RR2-04`: the existing non-test harness now owns UUID/hash/time constants, the valid-row factory and mocked-record factory; neither harness nor repository test depends on the semantics test class. Semantics depends only on the harness. `readsAuthorityWithoutLocking` asserts nonempty statements before its negative SQL assertion and identifies `DefaultOperationsCatalogSaleCommandTests.requiresActiveAdminWitness` as the ordered authority-port delegation owner.
- `PA38-RR2-01` remains deferred to the PA35 guard owner. No PA35 guard or primary CRLF input was changed, and no raw CRLF reproduction was used as a PA38 blocker.

### Bounded-code-fix2 gate historical validation — superseded for current bytes

All commands use `/opt/maven-3.9.9/bin/mvn -B -ntp`, `JAVA_HOME=/opt/jdk21` and `/opt/jdk21/bin` first on PATH. Preflight: Maven 3.9.9, Java 21.0.13, Docker Client 29.1.3 and **Server field 29.1.3**, reachable server-backed info; all four individual preflight commands exit 0. Tests at this bounded-code-fix2 gate used live Testcontainers PostgreSQL 16.15 — a historical fix2-gate measurement only; current accepted execution is PostgreSQL 16.16 from the floating `postgres:16` image.

| Gate | Actual exit / totals |
| --- | --- |
| Focused eight-class suite | 0; **8/86**, zero failures/errors/skips |
| Existing guards | **1; 2/17, one failure**, zero errors/skips; manifest literal only |
| Canonical focused Failsafe `verify` | **1; Surefire 97/934, one failure**, zero errors/skips; same manifest failure prevents reaching Failsafe |
| Direct existing Failsafe goals, same profile and two-class selector | 0; **2/38 = 24+14**, zero failures/errors/skips |
| Exact `OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner`, three sequential Maven invocations | **0 / 0 / 0; 1/1 each**, zero failures/errors/skips |
| Canonical `clean -Pcontainer-smoke verify` | **1; Surefire 97/934, one failure**, zero errors/skips; same manifest failure prevents Failsafe/full CI-oracle completion |
| Static/frozen audit | 0; **8/86 + 2/38** equal unchanged source/CI/POM and preserved fix1 source; Java files **290/180**; frozen boundaries unchanged |

One Git-less validation root: `/tmp/pa38-lf-validation.y9rnvg1o/candidate`, initial materialization inventory `0c4fc052a0d3adf376114595dcbdfbbd435a6047d44c16ca4551bc39528805d5`. Only the prescribed PA35 plan's 1,344 CRLF lines are LF-normalized there. Every changed Java input is byte-identical to primary; no projection-only code/guard patch, ignored failure, skipped test, changed identity or spliced acceptance result is used. Raw logs, exit records, immutable per-run XML snapshots and test summaries are under `/tmp/pa38-fix2-evidence/`.

### Manifest conflict and disposition

- Recomputed production manifest: **310**, SHA256 **`3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0`**. Algorithm remains backend-relative path/raw-content-normalized-CRLF-to-LF SHA256 rows, ordinal sorting and one final LF, excluding only V018's filename.
- Historical fix2 blocker: required metadata removal changed this digest while the then-forbidden carrier retained `e9de000fabce960d271e819e870390bcb151e8b03b1d7292c43b5c0b391e65d8`. Fix3 now pins **`3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0`** in `EvidenceRoleIvrAssociationAclBoundaryTests.v001ThroughV017AndPublicContractsRemainByteFrozen`; the 310 count, other assertions/hashes and test identities remain unchanged. The historical failure was not PA35 line-ending behavior or a PostgreSQL regression.
- Controller explicitly authorized this one-literal synchronization under `PA38-FIX2-PIN-01`. CI, POM and Control remain unedited by this author; fix3 validation supersedes the historical blocked results above.
- Candidate remains the original **32 paths plus fix1/fix2 artifacts and Controller-owned Control = 35**, with zero tracked deletions or staging. Four already present, unchanged independent review artifacts are separately identified in raw Git public status, not silently counted as candidate changes. Only five allowed Java paths, this report and the new fix2 artifact change in this task. No Java file is added.
- V001–V018/OpenAPI/docs12/14/15/17/PA25 remain byte-frozen; no V019. Raw migration path/hash aggregate remains `53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70`. All other prior deployment residuals remain owned as recorded below.
- Finding-by-finding code lines, exact commands/exits, XML/log hashes, test identities, that bounded-code-fix2 gate's 32-path bytes/ordinal aggregate and full candidate inventory: `docs/gateflow/pa38-operations-catalog-sale-lifecycle-code-review-fix-2.md` (the report's hash at that gate is recorded there to avoid self-reference). Those fix2-gate values are historical; the current candidate binding is the recomputed listing in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-2.md`.
- **Historical completion at the then-blocked fix2 gate: accepted bounded code corrections implemented; validation-blocked, not implementation-pass. Its next step was re-review**, after Controller disposition of the manifest-carrier scope conflict and required green validation. No stage, commit, push, PR, merge, approval, mark-ready, reviewer request, external comment or issue mutation occurred.

## Historical fix1 report — retained evidence, not current aggregate fix2 acceptance

The remaining report is preserved historical evidence. Its “current”, green-completion, old manifest, per-path hash and XML-custody statements apply only to bounded fix1 or their explicitly labeled earlier implementation runs; the current aggregate fix2 status above supersedes them. Historical fix2 blocked results are retained as provenance, not current validation. Currency pointer (aggregate re-review2 fix1): the two sections at the top of this report and the final 32-path custody recorded in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-2.md` govern future state; nothing below this point is a current claim.

## Historical bounded fix1 status — November 19, 2026

- Task: `pa38-operations-catalog-sale-lifecycle-code-review-fix1-gpt-20261119-02`.
- Scope: only `PA38-CODE-REV-01..11` and `PA38-CODE-REV-CTRL-01`; the accepted two-route contract, migrations, OpenAPI, public DTO/error vocabulary, POM, CI identities and all test method identities remain unchanged.
- Production corrections: canonical semantics-owned advisory-lock and Outbox dedupe binds; provider-only construction failures become closed sanitized 503; remove ineffective command annotations/import and unreachable authority `FORBIDDEN`; retain only a validated SQLSTATE witness, never the raw database cause/message.
- Test corrections: full SQL/bind/payload evidence, actual Spring condition/profile matrix and provider construction/ambiguity, literal success JSON bytes, behavioral authority/no-lock assertions, explicit non-test repository harness, both candidate active IDs, prestarted PostgreSQL lock observer, and fresh recovery transaction/role/no-retry/single-competitor assertions. No test method or Java source file is added, deleted, renamed, disabled or skipped.
- Closures, exact evidence lines, commands, public path audit, post-fix 32-path aggregate and final XML custody belong to `docs/gateflow/pa38-operations-catalog-sale-lifecycle-code-review-fix-1.md`.
- Fix1-gate historical validation (bounded-code-fix1 gate; superseded for current bytes by the two current sections above and the aggregate fix2 custody): smoke **8/8**, focused **8/86**, existing guards **2/17**, frozen regressions **6/52**, focused PostgreSQL **16.15** as a historical fix1-gate measurement **2/38 = 24+14**, its full Surefire **97/934**, all zero failures/errors/skips and exit 0. Clean full sweep and its own shipped CI/XML oracle: **PASS, exit 0; clean Surefire 97/934 + Failsafe 38/630, zero failures/errors/skips; shipped CI/XML oracle exit 0**.
- Canonical final validation root: `/tmp/pa38-lf-validation.G2fHfN/candidate`, materialization inventory `d536aa3fd7fd1d511c81d833194c21bcbfcff87d825d0ecb58723fe9bd2a95fe`. Only the frozen PA35 plan is LF-normalized in that Git-less projection; no primary frozen byte changes. Earlier `/tmp/pa38-lf-validation.QZulK9/candidate` (`440f7098561db3b2827c89379884e8a8cf1a79382e5d9790dc48184b5a2ce193`) was retired before canonical gates because exact PostgreSQL argument assertions changed the two allowed fixture/IT files. No acceptance is spliced from it.
- Fix1-gate historical runtime: `/opt/maven-3.9.9/bin/mvn -B -ntp`, `JAVA_HOME=/opt/jdk21`, PATH prefixed `/opt/jdk21/bin`; Maven 3.9.9, Temurin Java 21.0.13, Docker Client 29.1.3 and **Server field** 29.1.3, PostgreSQL 16.15 measured at that fix1 gate. That gate's preflight and smoke passed; raw diagnostics are preserved in the fix artifact. Current execution truth remains PostgreSQL 16.16 from the floating `postgres:16` image.
- Historical bounded fix1 recomputed production manifest: **310**, LF-normalized path/hash/LF SHA256 **`3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0`**, then pinned by the existing guard. This value, the earlier fix1 pin `e9de000fabce960d271e819e870390bcb151e8b03b1d7292c43b5c0b391e65d8` and implementation-fix3 `216538c1...` are historical, not the current aggregate fix2 guard value.
- V001–V018 remain exactly 18 unchanged files, no V019; raw migration aggregate `53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70`; six frozen OpenAPI/specification/PA25 pins below remain current. Controller-owned Control remains untouched by this author.
- Fix1-gate completion: **bounded-code-fix-complete**, with all twelve accepted findings closed and all required gates passing at that gate. Next gate after that successful bounded fix completion: **re-review**, not entered by this author. No commit, staging, push, branch, PR, merge, approval, reviewer request, external comment or issue mutation.

### Fix1-gate historical full-sweep XML custody — superseded by aggregate fix2 XML custody

- Clean full verify: **27:57**, finished **2026-11-19T09:57:57+08:00**, exit 0. Its own unchanged shipped CI/XML oracle exits 0: **97/934 + 38/630 = 135/1,564**, scan 1,565, POM includes 38, zero failures/errors/skips.
- Clean-sweep XML: **135 files**, filename/raw-SHA256/LF aggregate **`fb061f40eafd4e13a22e426fba4e38ca046acadbb562ef8b00bd3bf3a56748ef`**; complete inventory in `/tmp/pa38-code-fix1-final-clean-full-oracle.log`.
- `target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT.xml`: **24/24**, SHA256 `6b0d06d2581d0c8d03eba439243cbdf63378973b0475d11bbc4be0944351a06f`.
- `target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT.xml`: **14/14**, SHA256 `dd55e0332072873f548aaaa71b1d6bc7904e0cdefa92c221b3d8401b139ec9e8`.

### Fix1-gate historical implementation path hashes — superseded by the final 32-row listing in aggregate fix2

Raw SHA256 for all 31 original implementation paths as measured at the bounded-code-fix1 gate follows. These are historical fix1-gate values: eight of these 31 paths were changed by later accepted gates, and the current per-path hashes are exactly the 32 rows in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-2.md`. The report's own hash is never embedded in the report; it is recorded in that same fix2 listing, avoiding a self-referential report hash. Tables and claims after the next historical heading describe the earlier fix3 gate only.

```text
.github/workflows/ci.yml 11dfd54c9c0c797ea30142d2a0a614df6385c724f54a343f6bfc5a2b8677209a
backend/pom.xml f8fc1389d46a0af34ebb8b538947c6571211b44a0e19be7875ab3923dfee0fb8
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleConfiguration.java 5ae7933e32a200e1efc6c66c0d79c1568e382f8767eef9f3eebaff24f8b2dc40
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepository.java 360290131c6d32200bb725627fb3268222ec755012b8dd8b8ffef44c60ae9f29
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemantics.java fb4b3f0834bb55f15f230b916aaea0a40b34943de71040d0ce0af2cceb001640
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java 0939a2ce0ffdcef1947794dcf9a5c5fcc324e83cb9119354148a0ded9592032e
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java a6cb5b1a1afe682b3b539db04a922f6213c95992291e3319e04448c950be3141
backend/src/main/java/com/onecall/core/catalog_order/package-info.java 26aade7834fac4dcc4c0cb8986b0cf73baf79eec92feb7d62583807738f50f6a
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java 8478a8ce9f605422d81521aec94625a035641dbe4d3fe8b36eaa82486dfb34d4
backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPort.java b47cba36fb7caf349fb93b6b934be69f2f842624d88976f94377835173995042
backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/package-info.java 0f660b9b5b2bb67aabcd493175cdb04fc34496655886750f8ef122e28334ed03
backend/src/main/java/com/onecall/core/operations_iam/internal/employeeauthority/DefaultEmployeeAuthorityReadPort.java f76b0db87646b0a8fad62256164e26d0805c454a07b2ec88e9990c220904bd61
backend/src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java 01ff82d510de08bf02da5d9737ad75710f998973faacbc883cc31cbfe95cf0fb
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java e32deefb17b395682188612c60fffb4da95b690e7e9b676176a063c1bdd78e1d
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogVersionController.java ef26754fbea7b7b24a313ff138c176f508ed3c189b0f4e758c69d2aa337adf18
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsProblemWriter.java d46fb4056ae3edc4489e8b71b0787b8ae6adf0c7710ad9771e1def99ed5cdb2b
backend/src/main/java/com/onecall/core/operations_iam/operationscommand/OperationsCatalogSaleCommand.java a99c6c711644fe4e9dcdae32b77e256108ec86a66d11dc47a73f36c5b8985b91
backend/src/main/java/com/onecall/core/operations_iam/operationscommand/package-info.java fbfa75f83eef307f9458a2202b80bbf2cebd91335f34f4ed7d512ac18c762c6d
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java 4f204661ca4512d7112dca3d1043249114b1ce84c3b6876591b91cb16e0acb72
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java 4584f7aba6a8c11cff0aa446b78a1a96c839cfceb99ddc102cc9e91e3990de62
backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java f2fcebd2dc1f38096ed1b60969b6b1fe29fdcc975540659204ffa289d8265a60
backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java 9accf48cef6775f83a0c2f4d808eeb58c8ad534c44e393fa010fb28167967bdc
backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java 811cf9aecd74f4f3b6180faba42dcc66c4e29fe596d3919d6df5218579786d31
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java 631a13be9a2ba6fd9f2fb79eb9b240190cadf83bb6dff0bd4970fa693a5329cc
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java bc3c6ccbd7d0426690b78e9ec1fe775687a9fd4562324ba941e3bb4ef0cd614f
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java 4646b6de23b81353a65feff6fed469486b4ee6c245eec2246b7f79e9da71096a
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java 2177e6913a5b1308b72457edfe242788dd353f240d7dcb96d54087071b4d859e
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java 9d32f9b2a2a90f2101273936536c6760d10579fd1b7c8844ecd5310c2ec1ea86
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommandTests.java c83930bcfda5728defc18cb08b437a48ed274d0fc6dee1bf54e98ec32ce8ddc4
backend/src/test/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPortTests.java 0c53ae76efda5b34dda851bd93a88e720644e767d5270758df327dace6a4f4a9
backend/src/test/java/com/onecall/core/operations_iam/internal/web/CatalogVersionRequestBinderTests.java 06519b1ac4a2fa46ffb37585990e4c3aa11cf35f6be393344bea17ad47be5c21
```

## Historical fix3 implementation evidence — superseded for current bytes

The following unchanged historical narrative records the preceding bounded-fix3 gate. In particular its test-byte identity, production hashes, 32-path digest, validation-root and completion statements do not replace the current evidence above and in the aggregate fix2 artifact. Currency pointer (aggregate re-review2 fix1): every second-level heading from `## Gate and bindings` through `## Residuals and disposition` below belongs to this historical bounded-fix3 gate narrative; none of them is a current statement, and the only current authority in this report is its two top sections plus the final 32-path custody in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-2.md`. Historical measurement values in this region are deliberately preserved byte-for-byte.

## Bounded-fix3 gate historical evidence — PA38-S1 implementation gate fix3 final evidence (demoted from a second H1; superseded for current bytes)

## Gate and bindings

- Result: **implementation-pass** for the bounded PA38-S1 two-route implementation; no deployment, approval, commit, push, PR, merge, reviewer request or external comment is claimed.
- Task: `pa38-operations-catalog-sale-lifecycle-implementation-s1-fix3-gpt-20261118-07`; date: November 18, 2026, Asia/Shanghai.
- Primary candidate: `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call`.
- Resolved branch: `codex/pa38-next-work-unit`; unchanged HEAD: `ca90b0793f615a370ec66334fd196653beb358f4`; tree: `f716066ac00f19bf6874060be343a99b2be55aa3`.
- Binding plan SHA256: `337e27f7cbb7a1f31c0acba474fbe375ad1e587b88e34a3b6089c04c137fd8de`; fix8 SHA256: `e157371e946958fd765254210a382aac6d2eb05fb8c72aa7c361a7663b6248f1`.
- Only this report and the accepted 31 implementation paths are candidate changes. This fix3 turn edits exactly the lifecycle repository, its no-test container fixture, the authorized manifest guard literal, the CI source-inventory check ordering, and this report; all ten PA38 test classes remain byte-identical to their dispatch versions.

## Reproduction and minimal fix

Inspected `/tmp/pa38-fix1-it3.log` (latest), then reproduced the failure before editing. The exact diagnostic command, from the primary `backend`, was:

```bash
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke '-Dit.test=OperationsCatalogVersionLifecycleIT#adminPublishesWithoutPreviousActive' -Dtest=OperationsSubjectReadConfigurationTests verify
```

Historical bounded-fix3 gate reproduction. Exit **1**: Surefire 8/8 passed; targeted Failsafe 1 test / 1 failure, HTTP 503 instead of 200, SQLSTATE **42804**. That gate's PostgreSQL **16.15** server logs (a historical fix3-gate measurement; current execution is 16.16) captured the actual failing statement:

```sql
UPDATE catalog_order.catalog_version SET published_at=$1, sale_status='ACTIVE', revision=revision+1 WHERE id=$2 AND sale_status='DRAFT' AND published_at IS NULL AND revision=$3
```

The server error was `column "published_at" is of type timestamp with time zone but expression is of type character varying`, with the explicit cast hint. The trace completed the benefit SELECT and did not complete activation. Raw evidence: `/tmp/pa38-fix3-before.log` and `/tmp/pa38-fix3-postgres-before.log`.

The production change is only explicit parameter typing:

- Activation: `published_at=?` → `published_at=?::timestamptz`.
- First audit bind: `audit.append_fact(?, ...)` → `audit.append_fact(?::timestamptz, ...)`.
- All new production SQL was inspected: audit before/after snapshots and Outbox payload already use `?::jsonb`; IDs are typed UUID parameters; revisions are typed integers; advisory-lock input is explicitly cast to text; the literal nullable Outbox ciphertext `NULL` resolves against its existing unique function signature. No unrelated casts, schema changes or behavior changes were introduced.

Historical bounded-fix3 gate diagnostics, retaining their own 16.15 measurement: independent PostgreSQL 16.15 diagnostic commands used a disposable `postgres:16-alpine` container (`docker run -d --rm --name pa38-fix3-sql -e POSTGRES_PASSWORD=pa38 postgres:16-alpine`) and `docker exec -i pa38-fix3-sql psql -U postgres -v VERBOSITY=verbose`. Prepared parameter types were `(varchar,uuid,integer)` for the exact activation statement above: before-cast PREPARE returned **42804**; the identical statement with `$1::timestamptz` prepared and executed **UPDATE 1**, retaining the expected ACTIVE/revision-2 transition. Transcript: `/tmp/pa38-fix3-sql-reproduction.log`.

Loaded the unchanged V001 in a single transaction using `docker exec -i pa38-fix3-sql psql -U postgres -1 -v ON_ERROR_STOP=1 < backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql` (exit 0). For the exact audit function call with a varchar first bind, PREPARE before the cast returned **42883** (no matching timestamptz function); `$1::timestamptz` prepared and appended one fact successfully. Transcripts: `/tmp/pa38-fix3-audit-setup.log` and `/tmp/pa38-fix3-audit-reproduction.log`. The two diagnostic psql transcripts intentionally continue after their expected before-cast errors and exit 0; those exits alone are not acceptance. The actual pre-fix HTTP test exits 1, and the post-fix HTTP test exits 0. The disposable diagnostic container was stopped after evidence capture.

### Fault-seam correction, not an acceptance change

After the timestamp fix, `adminPublishesWithoutPreviousActive` returned HTTP **200**. The first complete 24+14 diagnostic run returned exit 1: all 14 RoleLeastPrivilegeIT methods passed, while LifecycleIT had exactly one failure, `outboxFailureRollsBackLifecycle`, on its final offsale subcase (expected 503, observed 200).

The exact contradiction was in the no-test fixture, not the expected rollback behavior: `AFTER_OFFSALE_OUTBOX` classification searched every historical `entries` row for `SET published_at=?`. Earlier faulted publication requests in the same test had rolled back, but their trace rows remained; therefore the later, separate offsale transaction was wrongly classified as `AFTER_RETIREMENT_OUTBOX`, and its armed offsale fault was never injected. Plan §9.3 requires injection after the current offsale transaction’s real Outbox fact. The fixture now adds only `entry.transaction() == transaction` to that existing predicate. Every fault stage and every status/rollback/fact assertion remains unchanged. No test identity, test method, expected response, expected count or acceptance criterion was rewritten or weakened.

The final-root rerun passed the target (1/1), all 24 LifecycleIT methods and all 14 RoleLeastPrivilegeIT methods. `/tmp/pa38-fix3-identity-preservation.log` proves all eight PA38 unit classes, both PA38 IT classes and all six frozen regression classes retain identical raw bytes; the fixture contains no test methods.

### CI inventory ordering correction

Retired root `/tmp/pa38-lf-validation.FwM0kE/candidate`, candidate inventory `b446d6e0371fd0c5b881585e3a6b23c827b227a6c1c1d96712b7320b5a6184d5`, completed its clean full Maven sweep successfully: 97/934 plus 38/630, zero failures/errors/skips. Its chained shipped CI oracle then exited 1 with `unexpected/missing source test class`; canonical custody was not reached and this root is not final acceptance evidence. Raw transcript: `/tmp/pa38-fix3-final-clean-full-oracle-custody.log`.

The strict source-class equality was evaluated before the eight PA38 unit classes and two PA38 container classes had been unioned into the expected maps. The sole CI correction moves the identical equality/error block after the PA38 union. It removes no assertion and changes no method map/count, historical subtraction or expected test behavior. A separate in-memory diagnostic against the retired sweep passed the entire shipped oracle with only this ordering correction (`/tmp/pa38-fix3-oracle-diagnostic.log`, exit 0); it is diagnostic, not a substitute for the required clean sweep. Changed CI bytes justified the new final root, and every preflight/smoke/target/canonical gate was rerun there, including clean full Maven plus its own shipped oracle and custody in one shell.

## Projection custody and host preflight

- Initial preflight and static/frozen/path checks: `/tmp/pa38-fix3-preflight-initial.log`, exits 0. The previously recorded `/tmp/pa38-lf-validation.QAgamM/candidate` was used only for the initial host diagnostic, not final acceptance evidence.
- After the two production casts, diagnostic root `/tmp/pa38-lf-validation.Eg5I4N/candidate` was created by the exact pinned-plan §11.0 fresh-parent/nonexistent-child setup. Candidate inventory SHA256: `d99aba82c5f9c176e93428aa9e38eefa9cbeba6fb240b546fe1c0f14222f4065`. Its target passed; its 38-test run exposed the fault-seam issue. This root is retired and contributes no final gate/XML totals.
- A new root was justified by changed candidate bytes: the transaction-scoped fault seam, the recomputed manifest digest literal and finally the corrected CI union/check ordering. Final root: `/tmp/pa38-lf-validation.JTdoCw/candidate`; candidate inventory SHA256: `228f4c7fd630b75b73c5f20c45d7ecbca4f65a731175562ed1f3df02d9633965`. Setup transcript: `/tmp/pa38-fix3-setup-oracle-final.log`, exit 0.
- Final preflight, smoke, target, 38-test rerun, every canonical gate, shipped CI oracle and final XML custody all use that single final root and inventory. No evidence is spliced across roots; no environment waiver, alternate launcher, report copy, source normalization or primary-checkout canonical gate is used.
- Only the projection’s frozen PA35 plan was materialized from 1,344 CRLF to LF. Raw primary SHA256 `d5ba84e34124e0cb44fd5a6c8fdc008a8894c3dee472268997d6a4f02500de55`; canonical projection SHA256 `58ec889fe812cd1e0c439a7d3c1054aa41bfe8bd44bcd27183512fbe3ffd85d3`. All other projected bytes and all primary bytes were unchanged at setup. Primary frozen tests/documents, `.git`, repository configuration and private helpers were not edited.
- Immediately before writing this final evidence report, the complete candidate inventory was recomputed and equalled the final-root binding above; the full projection inventory matched it except for the single prescribed PA35 LF materialization. This report is the final evidence-only update required after execution by task step 6; executable/source/test/build/CI bytes remain exactly the validated final-root bytes. No final-report self-hash or post-report whole-tree inventory is misrepresented as the pre-run inventory.

All canonical blocks were extracted verbatim from the SHA-bound plan using `re.findall(r'```bash\n(.*?)\n```', plan, re.S)` and executed as `subprocess.run(["bash", "-c", blocks[index]])`, with fail-fast nonzero exits. Fence indices: setup 1; preflight 2; smoke 3; regression 6; focused 5; guards 7; focused Failsafe 8; clean full + oracle + custody 9; final static 11; manifest 0. Each block independently re-declares/asserts its launcher, JDK, candidate/projection roots and required Controller Git bindings. Exact expanded command records and actual exits are in `/tmp/pa38-fix3-r2-gates.log`.

Restore bindings (unchanged for all final gates):

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_VALIDATION_ROOT=/tmp/pa38-lf-validation.JTdoCw/candidate
export PA38_EXPECTED_HEAD=ca90b0793f615a370ec66334fd196653beb358f4
export PA38_EXPECTED_TREE=f716066ac00f19bf6874060be343a99b2be55aa3
export PA38_EXPECTED_BRANCH=codex/pa38-next-work-unit
```

Final guarded preflight raw observations (`/tmp/pa38-fix3-r2-final-preflight.log`):

| Command | Actual exit | Raw observation |
| --- | --- | --- |
| `/opt/maven-3.9.9/bin/mvn -B -ntp -v` | 0 | Apache Maven 3.9.9; Java version 21.0.13; Eclipse Adoptium; runtime `/opt/jdk21` |
| `docker version --format '{{.Client.Version}}'` | 0 | `29.1.3` |
| `docker version --format '{{.Server.Version}}'` | 0 | `29.1.3`, explicitly from the Server field, not a client banner |
| `docker info --format '{{.ServerVersion}}\|{{.OperatingSystem}}\|{{.KernelVersion}}'` | 0 | `29.1.3\|Ubuntu 22.04.5 LTS\|6.18.33.2-microsoft-standard-WSL2` |

Every stdout/stderr capture uses the plan’s guarded `if output=$(command 2>&1); then rc=0; else rc=$?; fi` pattern, prints the raw exit before assertions, and enforces reachable server-backed nonempty fields. All final gates retain `set -euo pipefail`.

## Executed commands and actual totals

All following Maven commands run from `$PA38_VALIDATION_ROOT/backend` with the exact launcher/environment above, in the listed order. Each final command exits **0**; every executed suite has zero failures, errors and skips.

| Gate | Exact Maven arguments after `/opt/maven-3.9.9/bin/mvn -B -ntp` | Classes/tests | Raw log |
| --- | --- | --- | --- |
| Smoke | `-Dtest=OperationsSubjectReadConfigurationTests test` | 1/8 | `/tmp/pa38-fix3-r2-final-smoke.log` |
| Target | `-Pcontainer-smoke -Dtest=OperationsSubjectReadConfigurationTests -Dit.test=OperationsCatalogVersionLifecycleIT#adminPublishesWithoutPreviousActive verify` | Surefire 1/8; Failsafe 1/1, HTTP 200 | `/tmp/pa38-fix3-r2-final-target.log` |
| Full PA38 diagnostic | `-Pcontainer-smoke -Dtest=OperationsSubjectReadConfigurationTests -Dit.test=OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT verify` | Surefire 1/8; Failsafe 2/38 (24+14) | `/tmp/pa38-fix3-r2-final-all38.log` |
| Regression | `-Dtest='OperationsSubjectReadConfigurationTests,OperationsLifecycleBoundaryTests,OperationsActivationReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectReadBoundaryTests' test` | 6/52 | `/tmp/pa38-fix3-r2-final-regression52.log` |
| Focused Surefire | `-Dtest='EmployeeAuthorityReadPortTests,CatalogSaleLifecycleSemanticsTests,CatalogSaleLifecycleRepositoryTests,DefaultOperationsCatalogSaleCommandTests,CatalogVersionRequestBinderTests,OperationsCatalogVersionWebTests,OperationsCatalogVersionConfigurationTests,OperationsCatalogVersionBoundaryTests' test` | 8/86 | `/tmp/pa38-fix3-r2-final-focused86.log` |
| Existing guards | `-Dtest='ModuleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests' test` | 2/17 (9+8) | `/tmp/pa38-fix3-r2-final-guards17.log` |
| Focused Failsafe | `-Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT verify` | Surefire 97/934; Failsafe 2/38 | `/tmp/pa38-fix3-r2-final-failsafe38.log` |
| Clean full + CI/XML custody | `clean -Pcontainer-smoke verify` followed by the exact chained Python block below | Surefire 97/934; Failsafe 38/630 | `/tmp/pa38-fix3-r2-final-clean-full-oracle-custody.log` |

The clean full command and the shipped oracle/custody Python are executed in the **same shell invocation**, chained by `&&`, exactly as plan §11.3. No intervening Maven selector or report copy occurs. The oracle is extracted from the final projection’s `.github/workflows/ci.yml` first `python3` heredoc via `textwrap.dedent`, then executed with `compile(..., "ci-container-inventory", "exec")`; no replacement oracle or YAML package is installed. Final totals below come only from that clean sweep’s own XML.

Focused selectors can leave earlier reports in the directory. Their acceptance totals are selector-scoped and match Maven’s executed-suite totals, not an indiscriminate stale-directory sum. For each selector, the exact execution-time XML hashes were captured in `/tmp/pa38-fix3-r2-gates.log`. The following aggregate hashes use only the named selector’s own reports, sorted by backend-relative filename and serialized as `filename + " " + lowercase SHA256 + LF`:

| Selector snapshot | Selected XML files | Inventory SHA256 |
| --- | --- | --- |
| target | 1 | `9be4ed019aa9382b686ed5d83e14d42f710e06979e7576f3aca2f86d956076dd` |
| all38 | 2 | `9d946b15b0cb27de6887d914b4bb15ab2a43bb6dca5918bb2bc7190f85c4b24b` |
| regression52 | 6 | `7983e6f3f8c141f02ff96c4f80ceb59e8b9bd4a19f8dcb39f39667a4193f3d52` |
| focused86 | 8 | `d47bba31f3d6031b404adfa4284036b075d575231fe49e662e1bb95ddfcd2f03` |
| guards17 | 2 | `1d796135c6c4dfbd40f1a2cbddccf4782f11f244429e7c9cd756bd3ffa4bdd5b` |
| failsafe38 | 2 | `85cddc66d87a9af40b99893b779c918b28cfc8e141c2dba37ed92793d0cbb939` |

CI discovery now performs its unchanged exact source-class-set equality only after the full PA38 maps are unioned; this ordering correction is part of the final candidate and is validated by the shipped oracle, not by a relaxed expected class set.

## Exact clean sweep, oracle and custody command

The following is the complete unabridged plan §11.3 block executed against the final restored JTdoCw bindings above, not a replacement oracle or a post-hoc selector. The Bash invocation exited 0.

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
export PA38_VALIDATION_ROOT="${PA38_VALIDATION_ROOT:?restore the recorded root; never recreate it for an environment retry}"
case "$PA38_VALIDATION_ROOT" in /tmp/pa38-lf-validation.*/candidate) ;; *) exit 1 ;; esac
test "$PA38_VALIDATION_ROOT" != "$PA38_CANDIDATE_ROOT"
test -d "$PA38_VALIDATION_ROOT/backend"
(cd "$PA38_VALIDATION_ROOT/backend" && "${PA38_MAVEN[@]}" clean -Pcontainer-smoke verify && python3 - <<'PY'
from pathlib import Path
from textwrap import dedent
workflow = Path('../.github/workflows/ci.yml').read_text()
marker = "          python3 - <<'PY'\n"
start = workflow.index(marker) + len(marker)
end = workflow.index('\n          PY', start)
exec(compile(dedent(workflow[start:end]), 'ci-container-inventory', 'exec'))
from hashlib import sha256
report_paths = sorted(
    [*Path('target/surefire-reports').glob('TEST-*.xml'),
     *Path('target/failsafe-reports').glob('TEST-*.xml')],
    key=lambda path: path.as_posix())
assert sum(path.parent.name == 'surefire-reports' for path in report_paths) == 97
assert sum(path.parent.name == 'failsafe-reports' for path in report_paths) == 38
report_inventory = {path.as_posix(): sha256(path.read_bytes()).hexdigest()
                    for path in report_paths}
report_binding = ''.join(filename + ' ' + digest + '\n'
                         for filename, digest in report_inventory.items())
print(report_binding, end='')
print('final XML files', len(report_inventory))
print('final XML inventory SHA256', sha256(report_binding.encode('utf-8')).hexdigest())
for class_name in ('OperationsCatalogVersionLifecycleIT',
                   'OperationsCatalogVersionRoleLeastPrivilegeIT'):
    filename = ('target/failsafe-reports/TEST-com.onecall.archtests.container.'
                + class_name + '.xml')
    assert filename in report_inventory
    print('PA38 Failsafe XML', filename, report_inventory[filename])
PY
)
```

## Bounded-fix3 gate historical runtime and boundary proof — only the 16.16 sentence is current

Currency pointer (aggregate re-review2 fix1): the first sentence below is the current runtime truth (PostgreSQL 16.16 from the floating `postgres:16` image); the remainder of this section is the bounded-fix3 gate narrative, and the earlier 16.15 sentences in this report are all gate-labeled historical measurements.

The unmodified 24 LifecycleIT identities executed on PostgreSQL **16.16** in the current accepted aggregate fix2 evidence, using the floating **`postgres:16`** image, not a pinned digest. This current execution statement supersedes the earlier 16.15 attribution; explicitly historical diagnostic runs above retain their original versions. They cover ADMIN publication with/without an old ACTIVE, exact seven response fields/currentActiveVersionId, both zero-write current-state replays, pre-transition stale retries, wrong-revision/expected-active conflicts, zero-benefit publication, persisted content/history byte identity, exact eleven/eight audit/Outbox arguments, retirement-before-activation, 1/1 versus 1/2 fact cardinalities, and all real-fact rollback hooks. The race subcases prove one winner/one ACTIVE, direct catalog-active-index 23505 and injected 40001 recovery in a new transaction, real 55P03 lock conflict with a successful concurrent existing read on the same LO pool, and real 57014 statement cancellation with atomic rollback and a clean subsequent read. Actual local limits remain 2000ms statement / 250ms lock / 5000ms idle-in-transaction.

The unmodified 14 RoleLeastPrivilegeIT identities prove disposable login membership flags ADMIN=false/INHERIT=false/SET=true, both existing role switches, only the authority-column read, PID/transaction identity, clean role restoration and failed-connection eviction, frozen V004 **42-tuple** effective matrix and negative probes, audit/Outbox EXECUTE rather than direct writes, and exactly V001–V018/no V019. Broad frozen catalog SELECT/INSERT/UPDATE privileges are not described as a newly hardened least-privilege design.

The 8/86 unit group and 6/52 shipped regression group prove unchanged Operations authentication precedence, exact source-literal/prefix guards, provider unavailable/ambiguity behavior, strict binders and problem envelopes, OPS and disabled authority 403, both 409 codes, shipped unacceptable-Accept 500, leak canaries, Content-Length/no-store, and the actual Operations-then-Customer chain. The binding Customer guard is the behavioral zero-customer-verifier/object-identity sentinel: both accepted commands reach MVC, unsafe alternatives never reach it, no catalog Idempotency-Key requirement is added, and Operations status/problem/body/headers are untouched. No employee lock or reverse Operations-IAM→catalog dependency is introduced.

The 17 existing guard methods remain exact: 14 modules, unchanged 13-entry discovered-edge multiset, catalog dependency allow set 5, the three IAM named-interface tuples, frozen Operations IAM allow-map, and all historical negative fixtures. Java production count is 290 (279+11); test Java count is 180 (169+11, including one no-test fixture). New paths 22 plus existing modified paths 9 = 31 implementation paths; 16 changed production paths (11 new/5 modified), 13 changed test paths (11 new/2 modified), 2 build/CI paths.

## Shipped inventory oracle and manifest lockstep

Executed result: **97/934 + 38/630 = 135/1564**, source = CI = POM = XML; source line scan **1565**, ordinary method total **1564**, exactly one historical comment hit; POM exactly **38** includes. No method identity or historical subtraction family was discarded. The complete historical chains printed by the shipped oracle are:

```text
PA38 exact source=CI=POM=XML inventory passed: 97/934 + 38/630 = 135/1564; historical 112+178=290 -> PA19-start 128+192=320 -> PA20-start 162+198=360 -> PA21-start 196+204=400 -> PA22-start 231+214=445 -> PA23-start 266+214=480 -> PA25-start 309+214=523 -> PA26-start 309+234=543 -> PA27-start 309+246=555 -> PA28-start 341+272=613 -> PA29-start 375+302=677 -> PA30-start 417+336=753 -> PA31-start 468+370=838 -> PA32-start 520+404=924 -> PA33-start 574+440=1014 -> PA34-start 644+476=1120 -> PA36-start 704+504=1208 -> PA35-start 712+520=1232 -> PA37-start 788+556=1344 -> PA38-start 848+592=1440 -> current 934+630=1564; line scans 291 -> 321 -> 361 -> 401 -> 446 -> 481 -> 524 -> 544 -> 556 -> 614 -> 678 -> 754 -> 839 -> 925 -> 1015 -> 1121 -> 1209 -> 1233 -> 1345 -> 1441 -> 1565; POM 38 includes; zero skips/errors/failures
```

Executed old/new literal reconciliation (the plan’s context-specific ledger; historical fix1 mistakes are not treated as shipped baseline values):

| Carrier / context | Previous literal or diagnostic | Final checked value / reason |
| --- | --- | --- |
| New bean methods/injection/configuration tests/diagnostics (§2.3/§6.1/§9.3) | unimplemented fix2 manager/template names | `operationsSubjectReadCatalogSaleTransactionManager` / `operationsSubjectReadCatalogSaleTransactionTemplate`; shipped prefix oracle stays unchanged, no extra path |
| OperationsBoundaryFilter source and its three shipped source-literal oracles | exact current §5.1 literals and context-path URI equality | unchanged; add only non-breaking catalog classification, no extra existing test edit |
| CustomerBoundaryFilter / authorized scope closure | existing subject POST/Operations GET early branches; catalog POST obstruction | add only §5.4's exact early POST/servlet-path/context+URI branch; preserve all existing literal/equality tests and customer behavior; fifth modified production file, 31 total paths (22 new + 9 modified) |
| CI early `assert container smoke actually ran`, sum of Surefire XML tests (currently line 162) | `!= 848` | `!= 934` = 848+86 |
| Same early diagnostic | `expected exactly 848 Surefire unit/module tests` | `expected exactly 934 Surefire unit/module tests` |
| CI current line assertion | `line_scan_total == 1441` | `line_scan_total == 1565` = 1441+124 |
| CI current method assertion | `[848,592]`, sum `1440` | `[934,630]`, sum `1564` = +86/+38 |
| CI historical tuple | `(291,321,361,401,446,481,1441)` | `(291,321,361,401,446,481,1441,1565)`, adding PA38-start before current |
| Fix1 erroneous historical tuple tail (not shipped CI) | `1557` | `1565` |
| CI expected-class literals | `len(unit_expected) == 89`, `len(container_expected) == 36` | `97`, `38` |
| CI POM include count | `len(includes) == 36` | `38` |
| POM include set | existing 36 exact includes | add exactly `**/OperationsCatalogVersionLifecycleIT.java` and `**/OperationsCatalogVersionRoleLeastPrivilegeIT.java`; final 38, no fixture include |
| CI Surefire exact_inventory argument | `848` (fix1 error `926`) | `934` |
| CI Failsafe exact_inventory argument | `592` | `630` |
| CI summary label and leading inventory | `PA37 ... 89/848 + 36/592 = 125/1440` | `PA38 ... 97/934 + 38/630 = 135/1564` |
| CI method-chain diagnostic tail | `PA37-start 788+556=1344 -> current 848+592=1440` | `PA37-start 788+556=1344 -> PA38-start 848+592=1440 -> current 934+630=1564` |
| CI line-chain diagnostic tail | `... -> 1233 -> 1345 -> 1441; POM 36 includes` | `... -> 1233 -> 1345 -> 1441 -> 1565; POM 38 includes` |
| CI PA38 map/union/subtraction diagnostic | absent | added 8/86 and 2/38 maps, PA38-start `[848,592]` / `1441`, complete §10.1 union and subtraction families; preserve `zero skips/errors/failures` suffix |
| ModuleBoundaryTests catalog_order allow-map | 3 entries | 5 entries; add exactly the two §3 named interfaces |
| Fix1 erroneous discovered-edge instruction | `13 -> 14` | **13 -> 13**, preserve entire discovered-edge multiset and diagnostic |
| Evidence guard CI-token expectations | `89`, `36`, `36` in the three `len(...)` tokens | `97`, `38`, `38`, plus PA38 map/start/current tokens; retain every historical PA36/35/37 token |
| Evidence guard product manifest count | `299` | `310` = (279+11)+21-1 |
| Evidence guard product manifest digest | `12e70c031e42bcbca806cc1983335408d2c52fe830bd7a2ea77ddb2f1435b8c1` | recomputed 64-hex LF digest after all 16 production changed paths, including CustomerBoundaryFilter; procedure §10.3, no fabricated future hash |
| Fix1 Java inventory ledger | historical test `179`, new test files `10`, modified production `2` | test `180`, new test files `11` (fixture no tests), modified production `5`; main `290` unchanged |
| Fix1 manifest/report carrier and aggregate claim | CI carrier / count plus nonexistent byte aggregate | Java guard carrier / count and SHA256 only; remove the nonexistent field, add no replacement size aggregate |

Actual final manifest carrier is `EvidenceRoleIvrAssociationAclBoundaryTests.v001ThroughV017AndPublicContractsRemainByteFrozen`, not CI. The exact guard algorithm computes all regular `src/main/java` and `src/main/resources` files, excluding only V018’s filename, normalizing CRLF to LF, sorting path/hash lines and hashing their LF-terminated UTF-8 serialization. **310** files, SHA256 **`216538c10bf3a86925cc5db4a59da1473504a67b3ec31025d05ce00c62730f5e`**. Historical baseline: 299 / `12e70c031e42bcbca806cc1983335408d2c52fe830bd7a2ea77ddb2f1435b8c1`; this fix3 updates only the intermediate accepted-candidate digest literal `41572011f1615fbcfcec48892a80179ce95fe22a79a91a052d2b50263510930e` → `216538c10bf3a86925cc5db4a59da1473504a67b3ec31025d05ce00c62730f5e`, recomputed after the two production casts. The assertion is retained; no manifest byte-size aggregate exists or is claimed. §10.3 recomputation and the 17 guards exit 0.

## Frozen inputs and exact path custody

Plan §11.4 ran before the production fix and after the final full/oracle sequence, both exit 0. The `.git` Windows pointer is read without editing it, converted with `wslpath -u`, and used process-locally through the exact `PA38_GIT` array with `GIT_DIR`, `GIT_WORK_TREE`, `GIT_OPTIONAL_LOCKS=0` and `-c core.safecrlf=false`. Resolved Git status/HEAD/tree/branch assertions, `diff --check`, frozen `diff --exit-code HEAD -- ...`, migration aggregate and exact tracked+untracked allowlist all pass. No `.git` or repository-configuration repair is made.

Six raw pins verified before and after (all unchanged):

```text
0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68  api/openapi/onecall-v1.openapi.yaml
9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db  docs/12-technology-stack-and-system-architecture.md
0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31  docs/14-production-data-model-and-flyway-schema-spec.md
db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0  docs/15-production-openapi-surface-spec.md
b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205  docs/17-production-security-design-spec.md
b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9  backend/ops/postgresql/pa25-runtime-identities.sql
```

Exactly 18 migration files in numeric V001–V018 order, no `V019__*.sql`; raw-byte path/hash/LF aggregate **`53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70`** before and after. No migration, OpenAPI, public specification, ACL/role, credential, pool, Control/plan/fix/review artifact or private helper was edited. Pre-existing `.codex`/`.pa*-*` and generated `target`/`tmp`/`node_modules` are excluded exactly by the plan’s algorithm, not silently counted as implementation.

Exact changed-path set proved by the resolved-Git tracked/untracked union (31 implementation paths plus report, no missing or extra):

```text
.github/workflows/ci.yml
backend/pom.xml
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleConfiguration.java
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepository.java
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemantics.java
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java
backend/src/main/java/com/onecall/core/catalog_order/package-info.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java
backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPort.java
backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/package-info.java
backend/src/main/java/com/onecall/core/operations_iam/internal/employeeauthority/DefaultEmployeeAuthorityReadPort.java
backend/src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogVersionController.java
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsProblemWriter.java
backend/src/main/java/com/onecall/core/operations_iam/operationscommand/OperationsCatalogSaleCommand.java
backend/src/main/java/com/onecall/core/operations_iam/operationscommand/package-info.java
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java
backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommandTests.java
backend/src/test/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPortTests.java
backend/src/test/java/com/onecall/core/operations_iam/internal/web/CatalogVersionRequestBinderTests.java
docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md
```

## Exact PA38 test identities

The following are the final clean-sweep XML identities, not synthesized replacements. All ten test source files are byte-identical to the diagnostic snapshot taken before the fault-seam correction; no new or renamed method is introduced.

### EmployeeAuthorityReadPortTests — 8

```text
doesNotUseForUpdateOrAdvisoryLock
isTransactionMandatory
mapsMissingEmployeeToUnavailable
marksOuterTransactionRollbackOnlyOnFailure
preservesCommandConnectionOnSuccess
readsOnlyStatusRoleRevision
returnsImmutableSnapshot
usesOperationsIamRole
```

### CatalogSaleLifecycleSemanticsTests — 10

```text
canonicalLockKeyUsesUtf8ProductId
dedupeKeysUseCanonicalVersionId
nullActiveExpectationIsSyntaxOnlyForActiveIdentity
offsaleReasonIsActionDerived
offsaleSnapshotsContainOnlyFrozenFields
preTransitionExpectationIsNotReplay
publicationReasonIsActionDerived
publicationSnapshotsContainOnlyFrozenFields
rejectsMalformedPersistedState
rejectsRevisionOverflow
```

### CatalogSaleLifecycleRepositoryTests — 12

```text
acquiresProductLockBeforeDecisionTime
entersExactCatalogOrderRoleForMutation
entersExactOperationsIamRole
offsaleUsesConditionalRevisionUpdate
publicationReadsCandidateAndCurrentActiveUnderLock
readsAuthorityWithoutLocking
restoresRoleOnSuccessAndFailure
retiresOldBeforeActivatingCandidate
updatesOnlyLifecycleColumns
usesOneOffsaleAuditAndOutbox
usesOnePublishAudit
usesTwoPublishOutboxesWhenOldActiveExists
```

### DefaultOperationsCatalogSaleCommandTests — 16

```text
acceptsNullableExpectedActiveOnly
disabledMapsToForbidden
doesNotExposeDatabaseTypes
doesNotExposeRequestReason
mapsStaleExpectation
mapsUnmatchableStateToConflict
missingAuthorityMapsToServiceUnavailable
offsaleCommandHasFrozenFields
opsMapsToForbidden
publishCommandHasFrozenFields
rejectsNonPositiveExpectedRevision
replayPerformsZeroWritesAndFacts
requiresActiveAdminWitness
returnsExactCatalogVersionState
rollsBackAllFactsOnRepositoryFailure
usesClosedFailureCodes
```

### CatalogVersionRequestBinderTests — 10

```text
offsaleRequiresExactlyOneField
publicationAcceptsExplicitNullActive
publicationRequiresExactlyTwoFields
rejectsDuplicateFields
rejectsInvalidUtf8AndOversizedBody
rejectsNonCanonicalUuid
rejectsQuotedFractionalOverflowAndZeroRevision
rejectsReasonAndIdempotencyFields
rejectsTrailingTokens
rejectsUnknownFields
```

### OperationsCatalogVersionWebTests — 14

```text
admitsExactOffsalePost
admitsExactPublicationPost
authenticationPrecedesStructuralParsing
catalogCommandsDoNotRequireIdempotencyKey
classifiesReadsSubjectsAndCatalogCommandsSeparately
concealsOtherCatalogMethodsAndUris
disabledEmployeeReturnsForbiddenAfterValidParsing
opsReturnsForbidden
publicationConflictHasExact409ProblemHeadersAndLength
rejectsInvalidContentTypeAndShippedAcceptOutcome
staleViewHasExact409ProblemHeadersAndLength
structuralValidationPrecedesAuthorityWitness
subjectCommandsRetainExactIdempotencyRule
successHasExactStateAndHeaders
```

### OperationsCatalogVersionConfigurationTests — 8

```text
ambiguousProviderFailsClosed
defaultRuntimeHasNoWriteBeans
doesNotCreateSecondPoolOrCredentialFamily
missingProviderFailsClosed
resolvesExactlyOneCommandProvider
reusesExistingOperationsDatasource
reusesSamePhysicalDslBinding
usesNewWritableRequiresNewReadCommittedTemplate
```

### OperationsCatalogVersionBoundaryTests — 8

```text
catalogOrderImplementsNamedPortWithoutReverseEdge
catalogSqlNeverReadsEmployeeAccount
commandPortIsOperationsIamOwned
employeeAuthorityPortIsOperationsIamOwned
preservesFrozenInputsAndNoV019
rejectsSelectStarForbiddenTablesAndContentWrites
requiresOneTransactionAndPublicationOrder
scansExactProductionFiles
```

### OperationsCatalogVersionLifecycleIT — 24

```text
adminPublishesAndRetiresPreviousActive
adminPublishesWithoutPreviousActive
auditFailureRollsBackLifecycle
concurrentOffsalesYieldOneWinner
concurrentPublishersYieldOneActiveWinner
offsaleAuditAndOutboxArgumentsAreExact
offsaleCurrentStateReplayIsZeroWrite
offsaleTransitionsActive
oneActiveInvariantHoldsAfterEveryCommit
outboxFailureRollsBackLifecycle
preTransitionOffsaleRetryIsStale
preTransitionPublicationRetryIsStale
publicationAuditArgumentsAreExact
publicationCurrentStateReplayIsZeroWrite
publicationIncrementsBothRevisions
publicationOutboxArgumentsAreExact
publicationUsesOneDecisionTimestamp
publicationWithOldActiveHasOneAuditAndTwoOutboxes
publishedContentAndHistoryRemainByteIdentical
retirementStatementPrecedesActivation
routeMutationOutsideTwoTemplatesIsConcealed
staleOffsaleMakesNoChange
stalePublicationMakesNoChange
unexpectedStatesMapToConflict
```

### OperationsCatalogVersionRoleLeastPrivilegeIT — 14

```text
activePartialUniqueIndexHolds
catalogRoleCannotReadEmployeeAccount
catalogRoleMatchesExactV004GrantMatrixAndNegativeProbes
disposableLoginHasAdminFalseInheritFalseSetTrueRoles
historicalRowsRemainUnchangedAfterRepeatedCommands
loginCanSetCatalogOrderRole
loginCanSetOperationsIamRole
noV019IsNeededOrPresent
operationsIamRoleReadsOnlyAuthorityColumns
publishedTriggerRejectsContentMutation
readOnlyOrWrongRoleCannotMutateCatalog
restorationFailureEvictsDirtyConnection
setLocalRoleIsExactAndRestored
v001ThroughV018ProvideAllRequiredObjects
```

## Final clean-sweep XML custody

Custody and shipped oracle executed only after successful `clean -Pcontainer-smoke verify`, in that same shell. Enumerated exactly 97 Surefire + 38 Failsafe = **135 XML files**, with **934+630=1564 tests** and zero failures/errors/skips. Sorted backend-relative POSIX filename + single ASCII space + lowercase raw-byte SHA256 + LF, including the terminal LF, UTF-8 encoded and SHA256 hashed:

- **135-file XML inventory SHA256: `560b5ea82e5e75b2a393c7a243acd1d759fe2cf6ea045d1c9c2c86b79d79b588`.**
- `target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT.xml`: **`cbf382926ed3ea4ed1cc7d11c9893ca76543b7c6699f33218942849daf339c85`**.
- `target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT.xml`: **`496428f6c9730303dc4c83d598526590beff3c43f199e5950515f24a505ef234`**.

Complete raw-byte filename/hash inventory from that sweep:

```text
target/failsafe-reports/TEST-com.onecall.archtests.container.ActivationCommandRoleLeastPrivilegeIT.xml 21a82a189adc023326f2569993d420d044458fafcc643a08c7628d07d632a178
target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerBusinessNumberReadPrivilegeIT.xml 06c4faafb3e45736b599198a716e412863e79eff89f07bc1ad879b674120d410
target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerBusinessNumbersIT.xml 947cb5315c0fb00d2a50fe578fcac5618ca6b33e6b231c2604bf47470d0942d0
target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerEvidenceIT.xml ed259dd7324ddb03f0ff764649352718c6feee0b9866cbc353bd5baaad57bcf1
target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerEvidenceReadPrivilegeIT.xml fde0fb48dcf5b91ca510b12fef889d43796ecdfe1d8f70b108b579cd162d44eb
target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerMeIT.xml 99a27698fffc2d8e116e2a99b675de031a30839e3a18faa62dc7f4c2f32590b3
target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerShareCommandsIT.xml 91bb8554ab5c21dfd3d8dfbb2c93602fbe545a33d8708ab62bf2d100fcfd81ab
target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerSharesIT.xml 18041daed15bc19be3d60a71061cacc98fcd8773cf66d69fc353ebc2a5dbf8ae
target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerSubjectActivationsIT.xml dc64de2158fd81931ae36508a5ebecceb9ec9d986a96e17360626c229175e5c3
target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerSubjectClaimsIT.xml c3771e3a19a251bcbb7a4f1d8b2fecd3bf7917304cef5ced3e2ccd2ed06514cc
target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerSubjectDetailReadPrivilegeIT.xml e46c8d3d53c0ff753ccaaf5a11d817a8c7147fe90160c7e2c0b7733cc89104a2
target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerSubjectsIT.xml a3058444149bb8cc33ac9733ec2421d31b3b36ce388e15711936c2a7ba870824
target/failsafe-reports/TEST-com.onecall.archtests.container.DatabaseRuntimeAclRbacHardeningIT.xml 39b5601eb7a4af97db6c12b37085e92f836c0bf50452087f1b46bc47d84343a0
target/failsafe-reports/TEST-com.onecall.archtests.container.EvidenceMediaReadIT.xml 1703fbee37299438b1be82216830a007abed9985e1d36c4f397700acadf375f1
target/failsafe-reports/TEST-com.onecall.archtests.container.EvidenceMediaReadRoleLeastPrivilegeIT.xml 75afca63ee46edd1de8d2c2c4c9d50e01868c90d82b758a7db40bd5fa99fb609
target/failsafe-reports/TEST-com.onecall.archtests.container.EvidenceRoleIvrAssociationAclIT.xml fc7da1b19da962d3a75d43cff61f1cccbe58ef548acbbb3e1870bb0805ad01bb
target/failsafe-reports/TEST-com.onecall.archtests.container.MediaGatewayTransportIT.xml 32049d6d6625975298d1a2690fb46654b5ea2356c793aaf6ded2d9750089b26d
target/failsafe-reports/TEST-com.onecall.archtests.container.MediaPlaybackIT.xml f57a913db2104a112c69557af60fc081c65a349d392a85b9b432e66e006bcdce
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsActivationReadRoleLeastPrivilegeIT.xml 8d34ed03c6f9a6ee082fb93b4fe76d7d3aeb5c89d77c84e606bbb90c9cc81ec1
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsActivationReadsIT.xml ac0ab03ff8a0782330757ede958836895bbeb03b21e9ec8e90b17302c8b39fff
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogProductReadRoleLeastPrivilegeIT.xml 519c7e983c595b6f818a23f7961c4d27121042d846d8b7d7f579728d369f9c7e
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogProductReadsIT.xml 943a59738d991da3aff8e673e93f15193c7c874f344e9b3ed61c97e5e6dbdad4
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT.xml cbf382926ed3ea4ed1cc7d11c9893ca76543b7c6699f33218942849daf339c85
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT.xml 496428f6c9730303dc4c83d598526590beff3c43f199e5950515f24a505ef234
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT.xml 409c9930363818e9d1f969a1f5dc4aff93ab365e24da51d98fe553fe43302173
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsIvrSessionReadsIT.xml e58572fe89e55b70e1455f6932be2ed1ecc05115511d26535b1a041d676f4af4
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsLifecycleRoleLeastPrivilegeIT.xml 25014b5e0e651ab097c33e61bc49ba8112c52917acd767ad81702b143db331fc
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsSubjectLifecycleIT.xml 4aefdd9fee8914c37a62f55d85d96afc5ef6a9eb8701b55cf9e5a7ec60f6e21c
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT.xml d07b876f3d5f7adc8c1bcd8e3a297e755d5ab1130e5d3ff68e42f2eb009275d8
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsSubjectReadsIT.xml 34fc344d7e8ab52f60e512494162723f4a9c37c97242d291099deab5063123d6
target/failsafe-reports/TEST-com.onecall.archtests.container.PostgresContainerIT.xml 7bfea456111b515bd996e038a33b7f486c8b197f20f74442c50b6bf8a695c477
target/failsafe-reports/TEST-com.onecall.archtests.container.ProductionDatabaseIdentityProvisioningIT.xml 89f0654a10de0233e14b3051f5473d4640ad4475c4695faa197b2a657a9417c7
target/failsafe-reports/TEST-com.onecall.archtests.container.PublicShareAdmissionIT.xml 67fe0ab95737ae74af6eeff4cc5f7d4062668e805bff0c1ef8d36d1f7f0c38fd
target/failsafe-reports/TEST-com.onecall.archtests.container.PublicSharesIT.xml a1477a281a4a46caeb0bff54d3d001f3d305331738a679f3a30eb6fd0fde7d05
target/failsafe-reports/TEST-com.onecall.archtests.container.SharingAdmissionRoleLeastPrivilegeIT.xml 54ce7bb4007099ad99c76c41c9ae60e722f604f45915835e9098b1d70cbba79e
target/failsafe-reports/TEST-com.onecall.archtests.container.SharingQueryServiceIT.xml 0fae689763040905a5f35242c372da564af53f50f9ebc7073649a6cd98588304
target/failsafe-reports/TEST-com.onecall.archtests.container.SharingRoleLeastPrivilegeIT.xml daa0199974166a36fed8de3a5714597f7b9e0c25f4accd43ad77acdcfed63dde
target/failsafe-reports/TEST-com.onecall.archtests.container.SubjectClaimRoleLeastPrivilegeIT.xml 2bb29cd6d0817845be0d7492ab77e93d49fc4a19817dadeadc7d1e05411cfe75
target/surefire-reports/TEST-com.onecall.archtests.CustomerBusinessNumbersWebTests.xml ab416f922661b53d51125db48a0780db018e8afddb5455f34e47433406151129
target/surefire-reports/TEST-com.onecall.archtests.CustomerEvidenceConfigurationTests.xml c5ec8810cc8780150f5667927dd0b2c707056e72595986a22c5246678cb9d86d
target/surefire-reports/TEST-com.onecall.archtests.CustomerEvidenceWebTests.xml ab20e42805dd7762f81f863289fbac8d776ec32b0147d53673b003e245ae813b
target/surefire-reports/TEST-com.onecall.archtests.CustomerMeWebTests.xml c83fd2b8929c92d830800fd48927165eedbdc1d1e487637385d3732f5ca198cc
target/surefire-reports/TEST-com.onecall.archtests.CustomerSharesCommandWebTests.xml d11064465161e2cba887565fef1573615f185c2c89590d60937a0c1b25705a03
target/surefire-reports/TEST-com.onecall.archtests.CustomerSharesWebTests.xml 8e265183f7a2c197d3cb866e2ae4079b32e61f4a894f0694f0a7ecd4b8516ff4
target/surefire-reports/TEST-com.onecall.archtests.CustomerSubjectActivationConfigurationTests.xml 7f25d03f206938b7d14525ec42bb15b3a616425cbed2ab8c49cbe5cbbad333f5
target/surefire-reports/TEST-com.onecall.archtests.CustomerSubjectActivationsWebTests.xml c6572e45581ac1246fe0cc6ef6afc7eec635eb542028e6e18399a45220af444e
target/surefire-reports/TEST-com.onecall.archtests.CustomerSubjectClaimsWebTests.xml fb8e447e5feee5618bb80c853b75342834504aa7dd99903313844b3bb33511f8
target/surefire-reports/TEST-com.onecall.archtests.CustomerSubjectDetailConfigurationTests.xml e9d1e72a8cceebc4b87f6f0d7d2a7b1eb453205a7ff2b2ab31788c06a6a2ec78
target/surefire-reports/TEST-com.onecall.archtests.CustomerSubjectsWebTests.xml 8532d20a1dbb50d5603319949d4672ffb1739ad2e118013b067fd42c7cae007a
target/surefire-reports/TEST-com.onecall.archtests.CustomerWebConfigurationTests.xml d3220d91c1ac4887fe46aaaba29042bbac045ba6eb590f80435898dc2a8d9a44
target/surefire-reports/TEST-com.onecall.archtests.EvidenceMediaReadPersistenceConfigurationTests.xml 10c0b567fdd9b2f78bc5bff2e8dd6a92e57751ed91e663882b8e987fb4fad40d
target/surefire-reports/TEST-com.onecall.archtests.MediaGatewayConfigurationTests.xml 5227f99dff61beec2a6410b2226c79aae756e42ba99d36a23a8b62ce7641c2b6
target/surefire-reports/TEST-com.onecall.archtests.MediaGatewayWebTests.xml f16dfbbe5f0a95ced3437d55bb708d7e662ce98b249d9f8d8d032854efaa5c05
target/surefire-reports/TEST-com.onecall.archtests.MediaPlaybackConfigurationTests.xml 1b2c779e49a014146c5d6a61810c6b1d93e69b70d91fa994dbfa68e414396977
target/surefire-reports/TEST-com.onecall.archtests.OneCallApplicationTests.xml 9bfe0946ff90cf694bb0389b67a8c4b7f7d8bcf7f11cceb5c1a015727454cd5c
target/surefire-reports/TEST-com.onecall.archtests.OperationsActivationReadConfigurationTests.xml bf16b9e5b7d623fba947b1fb8fc97ba1362de46190c4e3be394e186d9a602151
target/surefire-reports/TEST-com.onecall.archtests.OperationsActivationReadWebTests.xml e278fefe8b116329f1b63d6e408249e4d5a27bf641acc77bd21c94e3fefbe1d7
target/surefire-reports/TEST-com.onecall.archtests.OperationsCatalogProductReadConfigurationTests.xml 8d7e1ccef9cb630b4851584dfe330d9704f9cc7c1677dc4c948522c5aeb456b7
target/surefire-reports/TEST-com.onecall.archtests.OperationsCatalogProductReadWebTests.xml 26b8ddea497e4634a9bd33c193abfcf1371e01ad4b935d09bd37836a5062a8d4
target/surefire-reports/TEST-com.onecall.archtests.OperationsCatalogVersionConfigurationTests.xml 6b9d48aaded857c97ccdc2c5877ff170eeca62f6c6fc7341e9625d98fbd374d6
target/surefire-reports/TEST-com.onecall.archtests.OperationsCatalogVersionWebTests.xml 7197c646206970f16ce628a5bb4dbb274b7a1ac7bc560e8e9afc91cb9b3a3c80
target/surefire-reports/TEST-com.onecall.archtests.OperationsIvrSessionReadConfigurationTests.xml 1f88a37bc8d6233901e6954660cda984058e6c9902a1b9fd664aaf0c8fd8b7d5
target/surefire-reports/TEST-com.onecall.archtests.OperationsIvrSessionReadWebTests.xml 0652eda78c01e54d1f62879daeb7c98d2840d92f93f93cd1fce6cc01e0508e99
target/surefire-reports/TEST-com.onecall.archtests.OperationsLifecycleConfigurationTests.xml 2ebec9d1b102eae28acf9980994015a0be82d19850a4ea515bc43cffe325127b
target/surefire-reports/TEST-com.onecall.archtests.OperationsSubjectReadConfigurationTests.xml 7937d0f5e2740647667e027f4630b310ed0788fba4e2c6c66015b6f9631a6640
target/surefire-reports/TEST-com.onecall.archtests.OperationsSubjectReadWebTests.xml 88a41104ffe7eaf13fd6f1c4d2bcfa16e8e639ff1af779ac686743dc2cfe3484
target/surefire-reports/TEST-com.onecall.archtests.OperationsSubjectsWebTests.xml 92171d0255af9b09cb737e3f424e8a778f0bcb0d8c093cde24fb5f3a5f7d3fda
target/surefire-reports/TEST-com.onecall.archtests.PublicSharePersistenceConfigurationTests.xml 18da258506c99cdaa08b48626cf2e5100757a08157b7ccf46dec3809319aeef0
target/surefire-reports/TEST-com.onecall.archtests.PublicShareWebTests.xml 7f9a2a8f13852f72e6226998a4bdde36c4ce151123e7c2511dd93652e7f8d552
target/surefire-reports/TEST-com.onecall.archtests.SharingCommandPersistenceConfigurationTests.xml 3739a3dab9ae1c8960d6991cf3ae2f25e3df8120f48ba10d8e3e54c870c4fe61
target/surefire-reports/TEST-com.onecall.archtests.SharingPersistenceConfigurationTests.xml a6a6d4cb43a7928bdc583f74cdc1a4d8940bb986b8628bc19d4ec8ce93cf4fec
target/surefire-reports/TEST-com.onecall.archtests.SubjectClaimPersistenceConfigurationTests.xml c842edc5c31115fd7eee46e30df20cfe1068212a43b31ddc6211616cceebacf5
target/surefire-reports/TEST-com.onecall.archtests.activation.CustomerSubjectActivationServiceTests.xml a0eb9a5c5789590d34b94925be7d0357eb40e1a3140bc9820e001ae156f4062d
target/surefire-reports/TEST-com.onecall.archtests.activation.OperationsSubjectLifecycleServiceTests.xml ad062662417408c8fc89ce8896d1074f0ffe963f4ef7fecb4a55a40981f047cc
target/surefire-reports/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml 5e5d2cd184844259709a3c1eb4aa8301d48dc2637c1eeef4b181c45cc328ea23
target/surefire-reports/TEST-com.onecall.archtests.architecture.CustomerApiBoundaryTests.xml d2133f855ca1a3aab3f30e81a4b1ef8df0650617d42dada4c47a186ebb646616
target/surefire-reports/TEST-com.onecall.archtests.architecture.CustomerBusinessNumberBoundaryTests.xml bfc0532dd2240d306bd08e944055c92e632cd3cb0878f4c74691c80930760bbe
target/surefire-reports/TEST-com.onecall.archtests.architecture.CustomerEvidenceBoundaryTests.xml 0ef8e6ab0c0b8dc8940a65bbf85ea804917a38581e56eb11885b05a8d7866954
target/surefire-reports/TEST-com.onecall.archtests.architecture.CustomerSubjectActivationBoundaryTests.xml 1808d6c6394436deba8a61efb38a2e0e1055a3c362c0fa211b6a9d528dec282b
target/surefire-reports/TEST-com.onecall.archtests.architecture.CustomerSubjectClaimBoundaryTests.xml 724daf8d532257e5df8f1a6dd7b4689b79e3f6af4b8901930c5e524beaa905a6
target/surefire-reports/TEST-com.onecall.archtests.architecture.CustomerSubjectDetailBoundaryTests.xml 461bf4a85c350d6bbc33462b3c45fea3b9d1d2b56fde0e32e2ba9324775019cf
target/surefire-reports/TEST-com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests.xml 3e0f31b9021543bf32eff99631bec5620719cd8e930f0f681639f6a5b0aebb35
target/surefire-reports/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml a4d6712c0b6b3523591ab1ab9e5429a996ec480f1b7f8ac785675f7f2ceee822
target/surefire-reports/TEST-com.onecall.archtests.architecture.OperationsActivationReadBoundaryTests.xml 0c3650df866feb77856bec3b0cdaa67e988a6fa70bb20907733a224f20c8bd7c
target/surefire-reports/TEST-com.onecall.archtests.architecture.OperationsCatalogProductReadBoundaryTests.xml cd51966cd1d533e65f56052cfe0ff9ccf9ddd2fb1a37f9847ac77bd32023562c
target/surefire-reports/TEST-com.onecall.archtests.architecture.OperationsCatalogVersionBoundaryTests.xml 1e0504b5c581f3372309f85a6e65a5c9137e1b1e6e5f7a7635722d0b52b86399
target/surefire-reports/TEST-com.onecall.archtests.architecture.OperationsIvrSessionReadBoundaryTests.xml 48d4b9c815270f559fa80e44e428a34874a571484e8a8a0e19a647ec1118d2ad
target/surefire-reports/TEST-com.onecall.archtests.architecture.OperationsLifecycleBoundaryTests.xml fb029809d443b02eb3a71477ef44f7dcec791ddca032d3cff5b14b83a9ad2dea
target/surefire-reports/TEST-com.onecall.archtests.architecture.OperationsSubjectReadBoundaryTests.xml 0571b2eea0ca712ca09f5e343e026992c00d4479fb4344522ee0c5f35e060b11
target/surefire-reports/TEST-com.onecall.archtests.architecture.SharingQueryBoundaryTests.xml a52013e6424a5c3dbe1a4d42a2c17fb6dd74dd326103a8878f0380930c14270e
target/surefire-reports/TEST-com.onecall.archtests.entitlement.ActivationTrialServiceTests.xml b4b20582386a6239ffb8c2d97e54497db60005bd24439ee599d4c3fec1a13998
target/surefire-reports/TEST-com.onecall.archtests.evidence.ControlledMediaByteSourceTests.xml f2c164c3bbe1a22e4916f336ff6dabc0cf996af58d8dce5263feddd55a83ae30
target/surefire-reports/TEST-com.onecall.archtests.evidence.EvidenceMediaObjectReaderTests.xml a875dc80498114645104065b938683a36f2b01b17cbfe0ab81c7d8ea27502248
target/surefire-reports/TEST-com.onecall.archtests.sharing.CustomerShareCommandServiceTests.xml fe8a9f1950a0868cb38c881d61088d3a36601570987a85c0626b307fffd1c234
target/surefire-reports/TEST-com.onecall.archtests.sharing.MediaPlaybackCredentialAuthorityTests.xml 4eb0125e9bb54634e06636a60071a3355d1e7488c216fa319a9e07e791fb8a0a
target/surefire-reports/TEST-com.onecall.archtests.sharing.MediaPlaybackServiceTests.xml c0a58bc8240aa9ba37940ac4a6bbe36f7942c4a47056dfcb81845e87e5ec242f
target/surefire-reports/TEST-com.onecall.archtests.sharing.PublicShareQueryServiceTests.xml e31d3b70b2e2c2c11e91f688090a1810ecc9218ac292853e3140eb1a71ce98fe
target/surefire-reports/TEST-com.onecall.archtests.sharing.ShareAdmissionServiceTests.xml a782c53d3dc2cccd882e79dab7e814a7320df109bf16cb4870d66853559bcd96
target/surefire-reports/TEST-com.onecall.archtests.sharing.SharingPlaybackStateReaderTests.xml e7c234e1346642f0b5c96f2a68f77b774530022e0e858209d96c716ae4cf30b3
target/surefire-reports/TEST-com.onecall.archtests.sharing.SingleByteRangeTests.xml 03375bfa548dc7c458fe360be7ea9c785add82a8a1cb55339f299d97fd6ceb88
target/surefire-reports/TEST-com.onecall.archtests.subjectnumber.CustomerSubjectClaimServiceTests.xml 99ca575bb60c0d5d6eb4cfb4d9f5d83110db77f8386e950d5b870c4cb3727606
target/surefire-reports/TEST-com.onecall.archtests.subjectnumber.SubjectClaimVerificationReferenceTests.xml 038c9ef021b72f350b6b289e00c3d8ff470b386cf9986fec156590b3ef5d8c6a
target/surefire-reports/TEST-com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleRepositoryTests.xml 18710675d00c182cd30b8bf6ba815b028797fc979690d3858e7e1232a00c3e68
target/surefire-reports/TEST-com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleSemanticsTests.xml cdd94639fd4b5da4f6ecad2155035afded9b3e61a30576cb14552a183d1442c9
target/surefire-reports/TEST-com.onecall.core.catalog_order.internal.operationscommand.DefaultOperationsCatalogSaleCommandTests.xml 88e67003949dcac59165706ea68b701430579cf83244b716de78bfa450bea538
target/surefire-reports/TEST-com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryServiceTests.xml 68d0f9871a8556e0380f5e9db47ae59631308a695dbb6d2e39c321e948960917
target/surefire-reports/TEST-com.onecall.core.customer_iam.internal.application.CustomerContextServiceTests.xml d1187108a6e62208a5181e70b8cf7ca31db123c247446b1b37e4d1264645b123
target/surefire-reports/TEST-com.onecall.core.customer_iam.internal.application.CustomerEvidenceQueryServiceTests.xml 2db678c3e10c3df2c13029c63104de38b198b4065eb0c0b071738669b01707e1
target/surefire-reports/TEST-com.onecall.core.customer_iam.internal.application.CustomerSubjectDetailQueryServiceTests.xml 694fdccb7f66cf71022a78bc05a71d584fed69bd04478f0ab8fdee5b5baf5122
target/surefire-reports/TEST-com.onecall.core.customer_iam.internal.security.CustomerSessionBoundaryTests.xml 6eb22907da524943421cacb37c77b9a661b5df005debc825e58365cc8c63b97b
target/surefire-reports/TEST-com.onecall.core.customer_iam.internal.web.CustomerBusinessNumberCursorCodecTests.xml 28974bbcb6d485b3b3d94357006e315ddfc67c47ab85c7986ee8b97718fb7309
target/surefire-reports/TEST-com.onecall.core.customer_iam.internal.web.CustomerEvidenceCursorCodecTests.xml 221c7c1c3dbb05955c6355a9a5457a182276d0ff074cb12fd3a51ac920e226e8
target/surefire-reports/TEST-com.onecall.core.customer_iam.internal.web.CustomerShareCursorCodecTests.xml 15a8adba30d37a1815a9ec0a102746c2daf650b297cb63e81e2b5f41945ee867
target/surefire-reports/TEST-com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPortTests.xml 4fe393499bb2249de024818bc2894b26ce41cd4b4b4761b8f1795abf99054808
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.activationread.OperationsActivationCursorCodecTests.xml 09f0226a78d2594f9d4cda5f35e6a3e90a646f881a4c11861951bea4056d6ab5
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.activationread.OperationsActivationReadRequestTests.xml 4d59336dcd55e03db88b087380f361ae912f08fcbd262928ec27d79249084bf0
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.activationread.OperationsActivationReadServiceTests.xml fdcce401fb00f56c53fb8d4a911b0ae158f5dc106eb5f2e7e67a9d500ace5bb4
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductCursorCodecTests.xml e11817c9b28dc9a942c64aed57a3321eb0fc8babdbb3c438fcec0c2ee647c545
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadRequestTests.xml c52806ace30e4574bab596409b3f17cd53218a7c61eb10afb02737b77d5d2c13
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadServiceTests.xml 1624495b1b25ad1fa937a245c4ca78bd482baf833a7f06e259170fb8d9ece125
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests.xml 59a7ecb64457bf3e4273c9b9877df020ef41574db576687ead24163b0d3e9841
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests.xml acff196b0397c657fb4c7f955a03f6bc9489065e2521ce099412c4279f9afce3
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests.xml dabd7a3c13417b7911a012fd494ba98d0e6364d09d4c3c42e8cd5d6c0656ea90
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.security.EmployeeSessionBoundaryTests.xml adab74df45abbe5a70a9cbb43c433ff40d9033ab9dfde9f07c41fcd9a7b19e16
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectCursorCodecTests.xml 53c9cf8a8c5c8695118ec4a2b8c05f3613c643d7ceb2b6f45004d6b082fb2c5e
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadRequestTests.xml dc08b9f7f23fecf01cec6327ba3f50f38dc4e1acd584ae53cda035261116f4d6
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests.xml 79bd069cbd53fdff42d83fe89c265007d23fdce993737186a348c64aece991b3
target/surefire-reports/TEST-com.onecall.core.operations_iam.internal.web.CatalogVersionRequestBinderTests.xml c6993bf58212193947b45b6ae7116d1175b531bafe1f8949367f5625e96d26ba
target/surefire-reports/TEST-com.onecall.core.sharing.internal.application.CustomerShareQueryServiceTests.xml 5573089577040172c015c5a296b04313c55de8b2ab252fbdb308799b2d9b5a0f
target/surefire-reports/TEST-com.onecall.core.sharing.internal.keysource.FileMediaPlaybackSigningKeyProviderTests.xml 1e254acebcb772344c0a4a13ede1e4cfe1922424d5f13313fae3a242fe598bf8
target/surefire-reports/TEST-com.onecall.core.sharing.internal.keysource.MediaPlaybackFileKeySourceIntegrationTests.xml 8eddf0b5366b96b17ffa8e067bc20b381eb89f669487aa38ded5d9a7ae6fb7a4
target/surefire-reports/TEST-com.onecall.core.sharing.internal.keysource.MediaPlaybackKeyBundleParserTests.xml dbb091ea3af073fa86b9953a14cdf76f76a3cacb6674b62161aff2b8969a98d1
target/surefire-reports/TEST-com.onecall.core.sharing.internal.keysource.MediaPlaybackTencentKeySourceIntegrationTests.xml 2c0f121fb97ee8acb8cfd2d0a09d2ab72e46fdf5a1114b23580aa2d3682532ef
target/surefire-reports/TEST-com.onecall.core.sharing.internal.keysource.TencentSsmMediaPlaybackSigningKeyProviderTests.xml be88345a4236a0b4d20801d96e930c58131a6e6163d919fac7e6d7fc0db16174
target/surefire-reports/TEST-com.onecall.core.sharing.internal.keysource.TencentSsmSecretStringClientTests.xml 551c0f1bb8bda1f0351dc0cc824a3d1ea8a23dbbb4c1ed46c6fb42572ccd8fce
```

## Raw evidence log custody

All logs below are generated `/tmp` evidence, not repository paths or replacement acceptance oracles. Exit records, expanded commands and per-selector XML hashes are in the gate transcript. SHA256 values bind the completed raw transcripts:

```text
/tmp/pa38-fix3-r2-gates.log 35c93f80360f0a351e74b6e62a02e4a7f3b959b759b33e664d3cd69a4c3e55a7
/tmp/pa38-fix3-setup.log 4848c732194c2c5a03987a139364f115bad211a9bf9f2926bdc5a9b37264beca
/tmp/pa38-fix3-setup-oracle-final.log 478b20abfd969f4a514a9fd33dfe6875509a9af06abb579492ba11a199d840ac
/tmp/pa38-fix3-preflight-initial.log 6f9ef53140fce2923ca88f7bbb6f4d576969e708d433cacd8e6fbd4c003e309c
/tmp/pa38-fix3-before.log 6f52c518f49b5403d70029ead7a3c023f1c52e052f223468f6d76f51c46f8772
/tmp/pa38-fix3-postgres-before.log 52a3121db6c5947789cfe1ea20be87e56802297cbda47fe49ec14d217b5f591c
/tmp/pa38-fix3-sql-reproduction.log c71b619e23cc1070b90f7eb952194ed9078e04bc3d963c9c871548297f04fc95
/tmp/pa38-fix3-audit-setup.log 306045f64e649f23ccc6ea93141e97885821d85957004c3df0599de5a2012dc7
/tmp/pa38-fix3-audit-reproduction.log 9430c8336214d501716e5a27bf68a93cece497fb6157c414b6cfbd07c7bfdb7d
/tmp/pa38-fix3-target-sequence.log 36f810c665d084853755793622c3ab3823dabd5fb3d63ed4b96ce61759fad431
/tmp/pa38-fix3-target.log b8d8507748b9228ed6e4d69d226f6880a2a7cd3e26a0af170c9b3529e15c1f02
/tmp/pa38-fix3-all38.log 403916ade68f950efe7081dfdda5b5525c2bc6abefee29590d6bf0603fecd8a1
/tmp/pa38-fix3-identity-preservation.log 6ddcbe676ac7e055186873bb9cbb1be043561567797a631b35b14769083b3081
/tmp/pa38-fix3-r2-final-all38.log 237b4351354ec65f46591c2653c21b5d22990c9826d95627ac7db4bcd9a8954f
/tmp/pa38-fix3-r2-final-clean-full-oracle-custody.log e414e9e7d663a1123af9155079ddf70b4bbcdde866b4c526ce4f3daf2daecf67
/tmp/pa38-fix3-r2-final-failsafe38.log 5bc3ff78cbf65b01dd10025e87c3f11c8e64d4eb4403fc234326f25afdc934fd
/tmp/pa38-fix3-r2-final-focused86.log 54f45aef8d25486bc5d12f78eca5d1f7036ddb37f4f29a3ec621708ff6a2d093
/tmp/pa38-fix3-r2-final-guards17.log 6f22152df22a98beaa884b189354d0772261be50a70258f714186b5091a2a92a
/tmp/pa38-fix3-r2-final-manifest-final.log ee907f28fa2ea4758a8a131709b5e3f5b733b0d4593cf0ed0799311f52599429
/tmp/pa38-fix3-r2-final-preflight.log 626a908c129c93aabeb704293b7c21b70b744b7a6bf59c654db2a882c1b2b704
/tmp/pa38-fix3-r2-final-regression52.log 76d573ccc32b4a6c4044be158bd0e5828cd8f825ba3457cc6770d9b67b0033f6
/tmp/pa38-fix3-r2-final-smoke.log 306c48fc7c8d80cba031461104bed227c66adbe10e1153c8ea21e4defc65882a
/tmp/pa38-fix3-r2-final-static-final.log 17577d47af49cbb6c96f3d1c4828ec8fdaa0f3c6058b977cf83c749e7fba1b65
/tmp/pa38-fix3-r2-final-target.log 6c5cdab63acd1a258433af9e40af15c98f6249bd9ad5263a14c3682403d2f331
/tmp/pa38-fix3-final-clean-full-oracle-custody.log 4d6006ebd3ac11814a7b2702ece58453eaea03fdfe5f856ec3ca5a3baaf20a92
/tmp/pa38-fix3-oracle-diagnostic.log bdc3091ee01765a156d7a9348672cb3f9e34a91e45cbe7ea4aad0c911488d283
/tmp/pa38-fix3-setup-final.log 9994a6d19b268ce2596db23bdfb473e9105f62ca580117c4a62e6c71b2f3b007
```

## Residuals and disposition

No implementation or validation blocker remains for these two commands; no environment failure is waived. The accepted plan’s production/future-slice residuals remain explicitly owned and are not implementation acceptance claims:

| Owner | Deferred boundary / destination |
| --- | --- |
| Platform / Security / Operations | Production LO enablement, secret delivery, seven existing PA25 SET edges and pool health: deployment readiness, not secret provisioning here. |
| Backend + Platform / Operations / Security | Mandatory bounded shared-pool lock/cancel/concurrent-read cases passed; sustained saturation/eviction/load capacity and read-503 recovery evidence remain deployment readiness for the unchanged four-slot/3000ms LO pool. No SLO or pool resize is claimed. |
| Database / Security + Catalog-order | Frozen broad catalog privileges remain; exact V004 42-tuple matrix and negatives passed. No ACL/migration hardening is performed. |
| Platform | Existing audit/Outbox retention, dispatch, monitoring and retries remain platform operations. |
| Security | Existing employee verifier availability/sanitized controls and admission-at-authority-read-instant concurrency boundary remain owned policy; no employee lock or new audit fields. |
| API governance + Catalog-order | UI second confirmation, draft authoring/replacement, trial policy and customer catalog filtering remain separate future slices. |
| Backend test-boundary owner + Controller | Primary PA35 CRLF remains frozen; only the prescribed LF validation projection establishes runtime acceptance. No historical drift harness is edited or waived. |
| Controller | Subsequent review/acceptance or Control bookkeeping is outside this task; no reviewer request, approval, commit, push, PR or external mutation is made. |

**Completion: implementation-pass.** The final-root canonical gates, clean full sweep, shipped source/CI/POM/XML oracle, deterministic 135-file custody, frozen pins/migration aggregate, 310-file computed manifest and exact 31+report path set all executed successfully. This report replaces the stale scaffold/residual narrative with the measured evidence above; it does not claim production deployment or later-gate acceptance.
