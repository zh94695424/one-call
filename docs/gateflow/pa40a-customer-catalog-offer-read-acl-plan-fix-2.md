# PA40A Plan Fix 2 — Frozen26 / Current27 Closure

- Task: `pa40a-customer-catalog-offer-read-acl-plan-fix2-gpt-20261123-01`.
- Date: 2026-11-23; timezone Asia/Shanghai.
- Working directory: `/mnt/c/Users/Administrator/.codex/worktrees/pa40a-customer-catalog-offer-read-acl/codex-call`.
- Cumulative plan: `docs/gateflow/pa40a-customer-catalog-offer-read-acl-plan.md`.
- Allowed writes: that cumulative plan and this new fix2 artifact only. Fix1 remains unchanged.
- Binding inputs: Control's PA40A goal and accepted re-review adjudication; cumulative plan/fix1; reviews `plan-review-20261123-094832.md`, `plan-review-20261123-100342.md`, `plan-review-20261123-103022.md`, `plan-review-20261123-111851.md` under `docs/reviews/`; exact frozen verifier source and relevant PA35 plan.
- Status: documentation-only closure proposed; no implementation, runtime validation, acceptance or implementation dispatch authority is claimed.
- Next gate: **final bounded independent A/B plan re-review**. Blocking question: **none**.

## 1. Accepted Finding Closure

`PA40A-PLAN-RR-B-01` (Medium) is **closed in the cumulative plan, pending final independent re-review**. Cumulative §5.2 specifies the unique projection/cardinality/aggregate rules and both exact PA38/39 method targets; §5.2.1 resolves all four re-review questions; §5.4's IVR consumer row and Appendix C now explicitly point to the corrected shapes. Appendix E.2 records planning-only validation.

The omitted live substitutions were PA25 SQL and PA25 runbook, not just README. Both existing IVR verifier methods must retain their original26-row literal byte-for-byte while independently executing current27 live assertions and historical26 evidence assertions. No method rename, new path, new ordinary test identity, weakened equality or historical parser change is authorized.

## 2. Exact Projection And Aggregate Contract

Let F26 be each verifier's original literal map:18 V001–V018 migration rows +5 OpenAPI/docs12/14/15/17 rows +PA25 SQL +PA25 runbook +README =26. Preserve every original row/pair byte-for-byte.

| Evidence state | Exact substitution/addition rule | Map / table / bare cardinality | Aggregate rule |
| --- | --- | --- | --- |
| F26 pre-PA40A evidence | Original literal, no substitutions, no V019 | Original map26; not another live mode | Independently recompute original `afda283795031bd226b3d48db7b9384ffacc5592798cd410769c9ec7ff8bc761` |
| H26 historical PA35 evidence | F26 with README-only historical substitution below; no other replacement; no V019 | Historical map26, original `tableCount == 26`, original `bareCount == 26` | Independently recompute `ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409` |
| C27 sole live/current projection | F26 with freshly measured PA25 SQL/runbook/README raw+LF pairs substituted, plus exact V019 row | Current map27, separately named current table27 and bare27;19 migrations +8 other paths | Independently recompute and pin a separate final current aggregate after implementation; no future hash is invented here |

Exact substituted current paths:

- `backend/ops/postgresql/pa25-runtime-identities.sql`.
- `docs/operations/pa25-production-database-identity-provisioning-runbook.md`.
- `backend/README.md`.

Exact added current path: `backend/src/main/resources/db/migration/V019__customer_catalog_offer_read_privileges.sql`.

H26's sole historical README pair is raw `fdcf813e4886d0fa68f7170207944457d331a51b79a0ae664485240f37ee5d64` / LF `bf152e7284aeeb5bc3cd9457c433607a8b91be848e9946d8549051cff1507177`. All other H26 pairs come directly from preserved FROZEN literals, especially PA25/runbook; never from the historical PA25 prefix reconstruction. V019 is absent from all historical map/table/bare/resource projections. No historical runbook byte reconstruction is assumed.

For each aggregate, sort paths by unsigned UTF-8 bytes, concatenate for each path UTF8(path) + one NUL +32 decoded raw-SHA256 bytes +32 decoded LF-SHA256 bytes, then SHA-256 the concatenation. No delimiter between hashes, no row terminator, no UTF-8 hex-text hash encoding. The two existing constants are preserved evidence; neither is repurposed as C27's aggregate.

Each existing IVR verifier method must:

- Execute F26 aggregate validation, unchanged PA35 table/bare parser and exact H26 comparisons, independently of C27 live-byte validation. Keep all original26 rows and counters unchanged.
- Pin newly measured final pairs and C27 aggregate before checking live bytes; do not calculate expected digests from the same live bytes at assertion time. Current table/bare representations are test-local in existing methods, never added to the PA35 plan or a new artifact.
- Assert exact unique map/table/bare key equality, exact pair equality and bare raw-hash equality for each projection; no `containsAll`, count-only acceptance or old/live hash fallback.
- Retain original mutation, BOM, corrupt-input, missing, duplicate, fake-plan-entry, raw/LF pin and incorrect aggregate-serialization probes. Exercise them against the appropriate historical/current projection.
- Actually copy expected V019 and all three changed files for positive current temporary checks in LF/CRLF forms without editing checkout bytes; assert exact full-basename current19/history19. Reject V019 missing/hash drift, extra V020, extra wrong-name V019 and same-count wrong-name replacement. Independently reject current PA25/runbook/README byte/pin drift and current table/bare mutations.

## 3. Explicit PA38/39 Method Targets

No existing method is renamed, including historically worded names.

| Existing method | Exact target |
| --- | --- |
| `OperationsCatalogVersionBoundaryTests.preservesFrozenInputsAndNoV019` | Preserve OpenAPI pin and historical exact18/full-basename/no-V019 evidence; retain old PA25 hash proof against §5's hash-verified V018 reconstruction. Independently require live exact19/full-basename equality to Appendix A's18 names plus exact V019, positive current success, missing V019/V020/extra wrong-name019/same-count wrong-name019 rejection, and a separate measured new shipped-PA25 pin. |
| `OperationsCatalogDraftBoundaryTests.preservesFrozenContractsEighteenMigrationsAndProvisioning` | The same independent historical18/current19 exact-basename success/rejection shape and separate old/new PA25 pin proofs, preserving original OpenAPI and historical evidence. Historical18/no-V019 is not a live predicate. |

Old PA25 raw pin remains `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9`, with the existing representation-aware raw/LF proof. New pairs are measured and pinned during implementation, not supplied by this fix. A live `isIn(oldHash,newHash)` check is not dual pinning. The reconstruction used for these method-level PA25 byte proofs never supplies H26's PA25/runbook pairs.

## 4. Four Nonblocking Question Resolutions

| Question | Binding resolution |
| --- | --- |
| Q1 — sole overlay cardinality / aggregate | Only C27 is live; exact current table27/bare27 with its own recomputed aggregate. H26 remains historical table26/bare26 with original aggregate; F26 retains the pre-change aggregate. No implementation choice remains. |
| Q2 — historical PA25/runbook hash source | Preserved original FROZEN literal values, with only historical README substitution. Never PA25 prefix reconstruction, live bytes or an invented runbook reconstruction. |
| Q3 — README/runbook consumer-count constraint | Accept explicit documentation-writing constraint: no additional backticked Java consumer path matching `` `backend/src/main/java/…java` `` before `<!-- PA36 HISTORICAL BODY -->`. Preserve current-prefix consumer count4, existing PA36 text and all historical tails. `EvidenceRoleIvrAssociationAclBoundaryTests.currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory` remains read-only and unauthorized for edits. |
| Q4 — extra/wrong-grantor fail-closed scope | Reaffirm accepted fix1 A-02: V019 owns exact customer53→75/delta22 plus invariant/preservation envelope. PA25 + independent IT own exact full410→432 across nine roles. Never expand V019 into unrelated-role ACL repair/reconciliation/broadening. |

## 5. Prior Fixes And Inventory Preservation

All five prior accepted dispositions remain closed in-plan: `PA40A-PLAN-REV-B-01..03`, `PA40A-PLAN-REV-A-01/02`. This fix changes no grant matrix, oracle-provider contract, fixture graph, capability denial scope, topology, upgrade/replay state machine, path authorization or method identity.

- Same52 implementation paths =3 new +49 modified =44 existing test/fixture paths +8 support/new paths. Existing test-side split remains12 architecture +32 container.
- Same67 existing @Test entry points +16 existing non-@Test entries +3 APPROVED fields in Appendix C. Same24 new ordinary identities =8 unit +16 IT in Appendix D.
- Same final146 ordinary classes /1708 methods /1709 annotation lines; Surefire105 classes/1026 methods +Failsafe41 classes/682 methods. Same productionJava298, resources21→22, production manifest319→320, testJava190→192 and migrations18→19.
- No inventory correction is required: changes refine four already-authorized method bodies and documentation rules, adding no implementation file or ordinary test method. The current27/historical26 map cardinalities are verifier evidence counts, not production/test inventory changes.

## 6. Validation And Remaining Gate

- Read all binding documents and exact affected verifier/PA35 evidence. Recompute both original literal26 aggregates and README-only historical26 aggregates; parse original PA35 table/bare26 and compare exact pairs/raw values. Verify unchanged source literal blocks and historical counters/probes through the full byte snapshot comparison.
- Check Appendix C path/entry counts and Appendix D24 identities against the pre-edit cumulative plan; confirm all implementation inventory arithmetic remains unchanged. Check document whitespace and exact allowed write scope using pre/post SHA256/path snapshots.
- Default read-only `git status` fails because the inherited worktree pointer references an unavailable Windows Git directory. No Git metadata is changed; byte snapshots provide the scope comparison. An initial read-only aggregate extraction matched the architecture verifier but not the IT's lowercase local `frozen` declaration; corrected extraction handles both and reruns both proofs.
- No source/test/migration/PA25/README/runbook/CI/POM/OpenAPI/Control/review/prompt/generated changes; no dependencies installed; no Maven/Docker/psql/runtime or implementation validation claimed. No commit/stage/push/PR/merge/approval/mark-ready/reviewer request/external comment/issue mutation.
- Final document SHA256 values are returned separately to avoid self-reference. Future PA25/runbook/README/V019 pairs, V019 CRC and C27 aggregate remain mandatory measured implementation pins, not unresolved design questions. Blocking question: **none**. Next gate: **final bounded A/B plan re-review**.
