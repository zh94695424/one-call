# PA40A Plan Fix 1 — Accepted Finding Closure And Audit

- Task: `pa40a-customer-catalog-offer-read-acl-plan-fix1-gpt-20261123-01`.
- Date: 2026-11-23; timezone Asia/Shanghai.
- Working directory: `/mnt/c/Users/Administrator/.codex/worktrees/pa40a-customer-catalog-offer-read-acl/codex-call`.
- Cumulative plan: `docs/gateflow/pa40a-customer-catalog-offer-read-acl-plan.md`.
- Binding sources: PA40A Goal Confirmation and current accepted adjudication in `docs/gateflow/project-execution-control.md`; `docs/reviews/plan-review-20261123-094832.md`; `docs/reviews/plan-review-20261123-100342.md`; affected source, migration, fixture, PA25, README/runbook, CI and POM inputs.
- Writes: the cumulative plan and this new fix artifact only. No implementation, runtime validation, approval or production acceptance is claimed. No source/test/migration/PA25/README/runbook/CI/POM/OpenAPI/Control/review/helper/generated file was edited.
- Next gate: **fresh independent A/B plan re-review**. This document records proposed-plan closure, not reviewer acceptance or permission to dispatch implementation.

## 1. Closure Ledger

| Finding | Plan-fix status | Exact closure location / obligation |
| --- | --- | --- |
| `PA40A-PLAN-REV-B-01` High | Closed in plan; re-review pending | §5.3 and Appendix C explicitly authorize all three exhaustive methods and their `APPROVED` fields. Each map gains exactly four catalog entries/22 columns. Keep all existing enumeration, denial, lock, write/DDL/routine/sequence/role and ACL-preservation assertions. Explicit historical/current phases prevent accidentally allowing V019 columns on a database still targeted015/017. |
| `PA40A-PLAN-REV-B-02` High | Closed in plan; re-review pending | §5.5/Appendix C add the missing Operations subject boundary method; current exact migration set18→19 uses full basenames and rejects extra020, extra wrong-name019 and same-count wrong-name019 replacement. §9 focused selector includes the class. Audited implementation set is52, not51. |
| `PA40A-PLAN-REV-B-03` Medium | Closed in plan; re-review pending | §3.1 fixes one package-private Java delta provider/access pattern. All current consumers compose historical410+22 through it; historical `EvidenceRoleIvrAssociationAclIT.currentOracle()` remains exactly410. No extra provider/fixture file or duplicated Java delta literals. |
| `PA40A-PLAN-REV-A-01` Medium | Closed in plan; re-review pending | §5.4 enumerates all11 direct null-start call sites and full34-path composed graph, exact affected terminal methods/pins, and explicit read-only dispositions. §5.5 extends the independent-fixture/global-pin audit. Appendix C is44 paths; §6 is52=3new+49modified. |
| `PA40A-PLAN-REV-A-02` clarification | Closed in plan; re-review pending | §3 explicitly resolves Control wording: V019 owns exact customer53→75/delta22 and preservation/invariant envelope; PA25 + independent IT own exact full410→432. No unrelated-role ACL repair/broadening. |

Blocking open question: **none**. V019 final bytes/checksum and full320 manifest remain scheduled implementation measurements, not unresolved scope choices or fabricated planning results. Existing environmental/operational residuals remain in cumulative §10.

## 2. Exact Newly Authorized Existing Test Methods

All paths below are under `backend/src/test/java/com/onecall/archtests/`; all six existing @Test identities remain unchanged. These are assertion-body authorizations, not six new tests.

| Exact path | Exact method addition to Appendix C |
| --- | --- |
| `container/CustomerBusinessNumberReadPrivilegeIT.java` | `customerRoleReadsOnlyApprovedColumnsAndDeniesEveryOtherCapabilityWith42501` |
| `container/CustomerSubjectDetailReadPrivilegeIT.java` | `customerRoleReadsOnlyApprovedColumnsAndDeniesAllOtherCapabilitiesWith42501` |
| `container/CustomerEvidenceReadPrivilegeIT.java` | `customerRoleReadsOnlyApprovedFactsAndDeniesAllSensitiveCapabilitiesWith42501` |
| `architecture/OperationsSubjectReadBoundaryTests.java` | `v001ThroughV017AndFrozenOpenApiAndDesignDocsRemainUnchanged` |
| `container/EvidenceRoleIvrAssociationAclIT.java` | `evidenceRoleReadsAllEightColumnsAndDeniesEveryUngrantableColumn` |
| `container/EvidenceRoleIvrAssociationAclIT.java` | `v018ReplaysOnlyExactCleanOrCompleteStatesWithoutDrift` |

The last two arise from the complete audit, beyond the four directly named in B-01/B-02: the evidence capability method's final410 equality must follow changed current provisioning to432; the historical V018 replay method's topology==2 apply must use the hash-verified historical PA25 prefix, not current19-only provisioning. Its replay/oracle remains018/410.

Also explicitly authorize the three `APPROVED` fields. Qualify the existing `EvidenceMediaReadIT` helper entry as `Fixture.start`, not its unchanged outer delegating @BeforeAll. Appendix C now names67 existing @Test entry points (previously61),16 existing non-@Test entries and3 fields. No existing method is renamed/deleted; no extra ordinary method beyond the original planned24 is added.

### Historical/Current Capability Phases

- Business-number map currently has six tables/22 columns (customer26 includes four schema tuples), not47 columns. Append exactly catalog22 to that map:10 tables/44 columns. Preserve its original015/261/customer26 loop; current019 capability loop overlays historical410's47 customer columns plus delta22=69. The intervening25 V016/V017 columns are an explicit baseline overlay, not additional V019 grants or unrelated literal-map changes.
- Subject-detail map already has12 tables/47 columns; final16 tables/69 columns. Its exhaustive method already migrates015→018; change that tail3→4 to019, currentHistory19.
- Evidence map already has12 tables/47 columns; final16 tables/69 columns. Preserve its017/288 loop with original projection and seed/canary assertions, then migrate017→019 (2 resources), provision and repeat at current312 seven-role oracle. Do not change historical288.
- Catalog delta on each map is product2/version10/offer5/benefit5; adjacent12 columns remain denied. Original noncatalog map entries, table/column enumeration and savepoint denial helpers remain intact. No catalog-table exclusion or full-row/read/write/lock shortcut.

## 3. Audited Path Arithmetic

| Path inventory | Audited current / existing | Future final | Delta / composition |
| --- | ---: | ---: | --- |
| Existing test/fixture write paths, Appendix C | 44 | 44 modified | 12 architecture +32 container |
| Existing support write paths | 5 | 5 modified | PA25 SQL, backend README, PA25 runbook, CI, POM |
| Existing implementation path set | 49 | 49 modified | 44+5 |
| New implementation paths | 0 | 3 new | V019 +one boundary class +one IT class |
| Complete implementation allowlist | 49 existing | 52 | 49 modified +3 new |
| Test-side implementation paths | 44 existing | 46 | +2 new tests; no new fixture/provider file |
| Non-test-side implementation paths | 5 existing | 6 | +1 migration; no application Java |
| Actual plan-fix writes | 1 existing plan | 2 documents | 1 modified +1 new; not implementation paths |

Relative to the superseded proposal: Appendix C43→44; modified implementation paths48→49; total implementation paths51→52. Only `OperationsSubjectReadBoundaryTests.java` adds an implementation path. The three maps and two newly recognized V018 method bodies reside in already listed files. All current/final path references in the cumulative plan use the audited values, not the old proposed arithmetic.

### Fixture Graph Results

- `start(null)`:11 direct call sites in11 files; seven direct wrapper fixtures, business-number IT, V018 IT, Operations activation role IT's baseline, and draft fixture's independent provisioning oracle. Cumulative §5.4 lists every exact method and edge.
- Full composed null graph:34 distinct paths =12 fixtures including root +22 terminal IT classes. Four composed wrappers are subject lifecycle, catalog product, catalog version lifecycle and catalog draft. Draft has both composed startup and an independent direct-null oracle.
- Modified graph subset: root fixture +17 terminal IT paths =18. Eleven non-root fixtures and five terminal ITs remain read-only because they have no changing assertion/helper.
- The five read-only terminals are `CustomerSubjectActivationsIT`, `OperationsSubjectLifecycleIT`, `OperationsIvrSessionReadRoleLeastPrivilegeIT`, `OperationsCatalogProductReadsIT` and `OperationsCatalogVersionLifecycleIT`; §5.4 records their exact startup routing and no-changing-pin result.
- Three additional root-fixture clients currently use explicit historical014/015/016 targets, not null: the business-number, subject-detail and evidence ACL ITs. Including these gives37 root-related paths. §5.3 supplies explicit current overlays without rewriting their historical proofs.
- `OperationsCatalogDraftFixture.provisioningOracle()` actually constructs an independent root fixture, runs null-start then PA25 check; it does not delegate to the catalog product ACL IT. Corrected that prior factual error without authorizing a fixture edit.
- Separate current-chain roots and transitive fixtures (`CustomerMeFixture`, `SharingQueryFixture`, public/customer shares, playback and nested evidence-media fixture) were audited in §5.5. Only exact changing assertions enter Appendix C.

## 4. Full Inventory Recalculation

Values are measured current source and future expectations, not executed Maven/XML totals. The fix itself changes none of these source inventories.

| Inventory | Current | Future final | Delta |
| --- | ---: | ---: | ---: |
| Production Java files | 298 | 298 | 0 |
| Production resources | 21 | 22 | +1 |
| Migration resources / successful full history | 18 | 19 | +1 |
| Production Java +resources manifest | 319 | 320 | +1 |
| Test Java files including fixtures | 190 | 192 | +2 |
| Surefire classes | 104 | 105 | +1 |
| Surefire ordinary methods | 1018 | 1026 | +8 |
| Failsafe classes | 40 | 41 | +1 |
| Failsafe ordinary methods | 666 | 682 | +16 |
| All ordinary-test classes | 144 | 146 | +2 |
| All ordinary-test methods / identities | 1684 | 1708 | +24 |
| Annotation-containing lines (`@Test\b`) | 1685 | 1709 | +24 |
| Known historical comment annotation | 1 | 1 | 0 |
| Non-ordinary-test Java fixture/helper files | 46 | 46 | 0 |
| Failsafe-specific POM includes | 40 | 41 | +1 |
| PA25 provisioning IT ordinary methods | 20 | 20 | 0 |
| Frozen old migration files / pin rows | 18 | 18 | 0 |
| Customer effective ACL tuples | 53 | 75 | +22 |
| Customer schema / column tuples | 6 /47 | 6 /69 | 0 /+22 |
| Seven-role ACL subtotal | 290 | 312 | +22 |
| Full nine-role effective ACL tuples | 410 | 432 | +22 |
| Noncustomer ACL subtotal | 357 | 357 | 0 |
| Current logins / monitored roles / memberships | 7 /9 /11 | 7 /9 /11 | 0 |
| Catalog runtime role's own tuples | 42 | 42 | 0 |

The POM has42 `<include>` entries across all plugins, but exactly40 belong to Failsafe; the two dependency-unpack entries are not test includes and must not advance. Annotation counting uses the CI word-boundary expression, not substring matching @TestConfiguration. No identity-count increase is caused by authorizing more existing assertion bodies or adding non-@Test oracle helpers.

### Identity And Manifest Digests

- Current1684 ordinary identities: `6041250383865a75ff49037c1f736bb87db552561206b9990aedaf8604a51770`.
- Future1708 identities, with precisely cumulative Appendix D's24 additions: `fe582055c9a28067aa156231f5aabfece21f91510a909a56a4c189c2ba4ca55c`.
- Format: sorted LF `FQCN#method` records, terminal LF; duplicate rejection. Existing identities remain exactly equal to the baseline subset. New8/16 class identities remain unchanged by this fix.
- Current full319-file LF production manifest: `57990aa0bc0886c45a58f4e63698d05f860f94a667bc047b2072b6251f3a66fd`. Future320 minus precisely V019 must reproduce this319 manifest.
- Historical318 manifest remains `26a10794d635d28bb402a4c88ee27b4c486ac09175b637f90faa122b38114f4a`; do not rewrite318 to319/320 or change its hash. Historical18/410 and PA35 frozen26 evidence stay historical, with distinct current overlays.
- Final V019 CRC/raw/LF SHA256 and full320 manifest digest cannot be computed before authorized implementation. They remain mandatory implementation measurements/pins, not guessed values.

## 5. Oracle Ownership And Replay Scope

Choose `CustomerCatalogOfferReadPrivilegesIT` (the already planned new IT) as the only Java delta provider, with package-private `static Set<String> catalogDeltaOracle()` and package-private `static Set<String> current432Oracle()`. Immutable delta22 equals Appendix B.2; current accessor composes historical410 +that delta. Current same-package PA25/ACL consumers use it; seven-role consumers project exactly312. Cross-package architecture consumers may reflectively read these accessors without public visibility or duplicate tuple literals. SQL target/oracle literals remain independent comparison surfaces.

Historical `EvidenceRoleIvrAssociationAclIT.currentOracle()` stays exactly410;122 additional and2 association accessors stay unchanged. Reconstructed historical PA25 text belongs to a separate package-private non-@Test accessor on the new IT, `historicalV018Pa25Script()`, hash-verified against the original raw/LF pins and used only for historical provisioning/replay. Normal PA25 apply/check stays exact19/432 and must reject18; no operational fallback.

V019's exact replay target is customer53→75/delta22 plus its invariant/preservation envelope. PA25 +independent IT prove full410→432. V019 never repairs or broadens another role; end-to-end full-graph drift rejection is not weakened by keeping migration authority target-scoped. This explicitly implements Controller's accepted A-02 clarification rather than silently interpreting older Control wording as permission for unrelated-role reconciliation.

## 6. Validation And Boundaries

- Read binding adjudication/reviews and affected migration/source/test/fixture/support inputs; global test-tree numeric/range/oracle/resource/identity searches found no further affected path outside the44-path test/fixture allowlist.
- Recomputed source class/method identities, annotation scan, Failsafe-scoped XML POM includes, production/test file counts,319 manifest, PA25 JSON410/history18/customer53 and22 delta identities; validated every Appendix C method/field against its source path.
- Verified complete null-start graph, historical-only root clients, direct/composed independent fixture consumers, current-versus-historical pins and extra-name/V020 negative-proof obligations. No source/test method name or planned new method identity changes.
- Historical rank16/17/18 corruptions remain; current rank19/duplicate019/future020 corruptions are explicit. Current rank relocation uses a noncolliding sentinel rather than occupied19; all432 missing-tuple tests must execute instead of V018's selective skip.
- Default Git status was unavailable because the checkout's `.git` worktree pointer references an unavailable Windows path. No `.git` mutation or alternative index operation; before/after file hashes and exact path-set comparison enforce the two-document write boundary instead.
- No Maven, Docker, PostgreSQL/psql, CI/JAR, application, contract or production validation ran. No dependency installation, staging, commit, push, PR, approval, reviewer request, external comment or issue mutation. Future validation commands remain proposals in cumulative §9.
- Final artifact lines/bytes/SHA256 and actual changed-file validation are reported in the completion message; hashes are external to avoid self-reference.

**Next gate: fresh independent A/B plan re-review; stop before implementation.**
