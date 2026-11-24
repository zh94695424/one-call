# PA40B customer catalog offer read — plan fix 1 (closure record)

- Work unit: `PA40B-customer-catalog-offer-read`. Gate: `plan fix1` (documentation-only).
- Task label: `pa40b-customer-catalog-offer-read-plan-fix1-qwen4max-20261124-01`. Author route: Codex/Qianwen `qwen4max` (`qwen4-max`).
- Timestamp: `2026-11-24 10:29:58 CST` (Asia/Shanghai, local system clock).
- Base: PA40A PR #51 merged at `32cf930a16803deb6381615e197c02e73ac5c22e`; branch `codex/pa40b-customer-catalog-offer-read`.
- Inputs: Control PA40B checkpoint (`docs/gateflow/project-execution-control.md` lines 14–44, incl. the plan-review adjudication); the cumulative plan `docs/gateflow/pa40b-customer-catalog-offer-read-plan.md`; Reviewer A `docs/reviews/plan-review-20261124-095011.md` (fail; 2 High/1 Medium/1 Low); Reviewer B `docs/reviews/plan-review-20261124-101247.md` (fail; 3 High/2 Medium/1 Low); and the cited source/tests/OpenAPI/docs/CI/POM/README.
- Writes this gate: **only** update `docs/gateflow/pa40b-customer-catalog-offer-read-plan.md` and add this file. No Control/source/test/README/CI/POM/OpenAPI/design-doc/prior-review edit; no commit/push/PR/merge/approve/mark-ready/reviewer-request/comment. Stop before re-review.
- Updated plan SHA256 (this worktree, post-fix): `aadfd89d956bc8762f32a24a0d1ee5c04df45341be7d732229e05b122323d415`.

## 1. Binding scope (not reopened)

The confirmed goal and accepted design are binding and unchanged: one frozen `GET /api/v1/customer/catalog/offers` (`listCustomerCatalogOffers`) through the existing customer pool/`customer_iam_role`, merged V019 22-column grants, trusted account/workspace/session-channel authority with query-only narrowing, one REPEATABLE_READ/REQUIRES_NEW/readOnly role-asserting transaction, exact 13-field `CatalogOfferPage` projection, `productCode asc, versionNo desc, catalogOfferId asc` keyset + endpoint-isolated HMAC cursor with `CURSOR_INVALID`/`CURSOR_FILTER_MISMATCH`, zero writes, PostgreSQL16 proof. This fix touches **only plan feasibility / allowlist completeness**. No endpoint behavior, product scope, ACL, pool/role topology, projection, cursor semantics or test-class set is widened.

## 2. Global exact-guard audit (finding 1 / A-01 / B-01 / B-02)

### 2.1 Method
Repository-wide sweep of `backend/src/test/java`, `.github/workflows/ci.yml`, `backend/pom.xml`, `backend/README.md`:
- `rg` for every affected literal: `isEqualTo(298|192|320|319)`, `hasSize(47|12|15)`, `getDeclaredMethods`, `catalog_order::customer-subject-count-query`, `len(unit_expected|container_expected|includes)`, `method_totals ==`, `line_scan_total ==`, README hash pins (`9c094a00|8db84d4b|58161cad|0816cc1c`), `Code.values()`, `application-web.yml`, `NamedInterface`, `admittedPath`, `catalog/offers`.
- Reflection-equivalent inspection of `CustomerPersistenceConfiguration` (15 declared methods, all `@Bean @Lazy`) and the exact bean-method→qualifier map in `CustomerWebConfigurationTests`.
- Filesystem walk + SHA256 recompute of every baseline count/hash (all confirmed; §6).
- Cursor length arithmetic (`ceil(4N/3)`), README projection/consumer-count recompute, and `CustomerBoundaryFilter` Accept-gate location.

### 2.2 Exhaustive result — exactly 14 modified test guards
The 31-path proposal listed 6. The audit confirms **8 additional** guards (the union of A and B), and **no further guard exists**. Total modified test files = 14.

| # | File | Exact literal(s) that change | Preservation rule |
| --- | --- | --- | --- |
| 1 | `architecture/CustomerCatalogOfferReadAclBoundaryTests.java` | java 298→307; excl-V019 319→328 + hash; incl-V019 320→329 + hash; unit 1026→1071; integration 682→701; annotations 1709→1773; identity hash; CI markers `[1026, 682]`→`[1071, 701]`, `1709`→`1773`; +POM include `CustomerCatalogOffersIT` | resources 22; V019 22-grant/19-migration/432-oracle/7-login/11-edge; pa40a markers; README/runbook markers |
| 2 | `architecture/CustomerApiBoundaryTests.java` | `hasSize(47)`→52 (line 90) | fixed 8-file `@Profile` list; sharing-import clauses; per-file scan auto-covers new files |
| 3 | `architecture/ModuleBoundaryTests.java` | EXPECTED_ALLOWED customer set +`catalog_order::customer-catalog-offer-query` (line 42); both `hasSize(12)`→13 (~131/~232) + both tuple lists | 13 production edges; all other module sets |
| 4 | `architecture/CustomerEvidenceBoundaryTests.java` | exact 12-entry dep list →13 (`namedOwnerInterfacesAndTenCustomerDependenciesAreExact`) | method name; OpenAPI/V001–V016 frozen hashes |
| 5 | `architecture/CustomerBusinessNumberBoundaryTests.java` | `hasSize(12)`→13 (line 46) | all other clauses |
| 6 | `architecture/CustomerSubjectDetailBoundaryTests.java` | `hasSize(12)`→13 (line 57) | all other clauses |
| 7 | `architecture/OperationsCatalogDraftBoundaryTests.java` | 298→307 and 192→199 (line 16) | exact 5-file `operationsdraft` listing byte-identical |
| 8 | `architecture/OperationsCatalogVersionBoundaryTests.java` | 298→307 and 192→199 (line 10) | all other clauses |
| 9 | `CustomerWebConfigurationTests.java` | exact bean map 15→18 entries (+3 new methods/qualifier lists) | `application-web.yml` contains/doesNotContain; sharing/command maps |
| 10 | `core/customer_iam/internal/application/CustomerBusinessNumberQueryServiceTests.java` | `getDeclaredMethods()).hasSize(15)`→18 (line 220) | every other clause |
| 11 | `architecture/SharingQueryBoundaryTests.java` | exact 12-entry customer dep list →13 (lines 113–116) | all sharing/admission clauses |
| 12 | `architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java` | CI literals `105`→110, `41`→42 (×2), `[1026, 682]`→`[1071, 701]`, `1709`→1773 (line 144) | pa35/pa36/pa38 historical literals; UNIT/CONTAINER name lists; POM-dependency hash; `currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory` byte-frozen |
| 13 | `architecture/OperationsIvrSessionReadBoundaryTests.java` | README `currentPins` raw/LF entry + C27 aggregate | FROZEN 26; historical26 map; aggregate `ed03b90f…`; deletion logic; projected LF `0816cc1c…`; 27-cardinality; method names; negative probes |
| 14 | `container/OperationsIvrSessionReadsIT.java` | README `currentPins` raw/LF entry + C27 aggregate (Failsafe twin) | same as #13 |

### 2.3 Additional guard discovered beyond both reviews (README consumer count)
`EvidenceRoleIvrAssociationAclBoundaryTests.currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory` asserts the current README section contains **exactly 4** backtick `` `backend/src/main/java/…java` `` consumer paths (`consumers == 4`, verified by recompute). Neither review flagged this. Resolution: a binding README content rule (plan §13) forbids any new backtick java path in the PA40B section (bare class names instead), keeping `consumers == 4` and that method **byte-frozen**. Only `ciPomAndOrdinaryMethodInventoryMatchExactly` in that file changes. This avoids adding a 15th guard or weakening the consumer assertion.

### 2.4 Guards confirmed NOT affected (exhaustive negatives)
- Whole-tree 298/192 count guards: only #1/#7/#8 (no others).
- `customer_iam/internal` 47: only #2.
- customer allowed-deps 12: only #3/#4/#5/#6/#11.
- `CustomerPersistenceConfiguration` declared methods 15: only #9/#10.
- CI current literals: only #1/#12.
- README full-file pins: only #13/#14; README historical-body hash + consumer count: #12 (frozen by content rule).
- `CustomerContextException.Code` enum: **no** test pins its exact value set or iterates it by ordinal (the two `Code.values()` ordinal loops in `CustomerSubjectActivationsWebTests`/`CustomerSubjectClaimsWebTests` use `SubjectActivationException.Code`/`SubjectClaimException.Code`, not `CustomerContextException.Code`). Adding `CURSOR_FILTER_MISMATCH` is safe.
- `application-web.yml`: `CustomerWebConfigurationTests` uses `contains`/`doesNotContain`; the new key introduces no forbidden substring (`session/cookie/redis/flyway/default-property-inclusion/maximum-pool/connection-timeout`). Safe.
- Named-interface global count: no guard pins 20→21. Route/smoke admissions: no existing test/CI probe references `/api/v1/customer/catalog/offers` (grep-confirmed). POM dependencies hash: unchanged (only Failsafe includes grow). `catalog_order/internal` whole-count: no guard (only `operationsdraft` 5-file and `operationscommand` subdir, unaffected).

## 3. Issue-by-issue closure

- **A-01 / B-01 / B-02 (High, allowlist incomplete):** CLOSED. §2 audit republishes the allowlist as **39 paths** (plan §6), enumerating all 14 modified test guards with per-file literal rules and preservation constraints. The count is derived, not assumed (A's union omitted `CustomerBusinessNumberQueryServiceTests`; B's 38 omitted `EvidenceRoleIvrAssociationAclBoundaryTests`; audited union = 8 additional → 39).
- **A-02 / B-03 (High, README double-bind + placement):** CLOSED. Plan §13/§6.4 items 35–36/§6.5 item 39 authorize both IVR verifiers; pin insertion **after the PA40A heading (line 71) and before the marker (line 103)**; recompute only README current raw/LF + C27 in both; freeze historical26/F26/H26, aggregate `ed03b90f…`, projected LF `0816cc1c…`, 27-cardinality, deletion logic and negative probes; add the consumer-count content rule (§2.3).
- **B-04 (Medium, CI historical-ladder exclusion):** CLOSED. Plan §10.3 defines `pa40b_names`, merges both maps via the disjoint-union loop, and threads `pa40b_names` through the baseline `additions` union + all 20 start-total sites (ci.yml:2435–2455) + all 20 start-line-scan lambdas (ci.yml:2456–2536); adds `pa40b_start_totals==[1026,682]`/`pa40b_start_line_scan_total==1709`; keeps every historical `pa19…pa40a_start_*` byte-identical; moves only current totals/`exact_inventory`/printed ladder; adds the missing-name negative expectation.
- **A-03 (Medium, service authority):** CLOSED. Plan §5.4 makes `Request.channel/audience` non-authoritative `Optional` echoes; the service derives trusted channel from `caller.channel().name()` and audience from workspace type, rejects any present-but-different value with FORBIDDEN before any transaction/reader call, and builds the reader `Scope` only from trusted values. New negative unit test `mismatchingRequestChannelOrAudienceIsRejectedBeforeAnyReaderCall` (service tests 10→11) proves raw request authority cannot reach the reader (`verifyNoInteractions(reader)`).
- **B-05 (Medium, billing validation reaches denied column):** CLOSED. Plan §5.4 replaces "billing matrix" with the exact readable-subset rules (offer_type enum; SUBSCRIPTION/ONE_TIME/ADD_ON/QUOTE billing-cycle+installments; QUOTE↔null price↔ENTERPRISE; PERSONAL↔non-QUOTE+price≥0; currency CNY) and states `catalog_version.grant_interval_months` is NEVER selected/validated; benefit-line `grant_interval_months ≥ 0` validated separately. `readerProjectsOnlyTheTwentyTwoApprovedColumns` asserts the reader source never references `grant_interval_months` outside the benefit query.
- **A-04 / B-06 (Low, cursor API + 243 bound):** CLOSED. Plan §4.6 pins the public `Binding(accountId, organizationId, channel, audience, Optional offerType)` / `Anchor(productCode, versionNo, catalogOfferId)` records and `encode`/`static validateStructure→Anchor`/`decode` signatures (mirroring `CustomerBusinessNumberCursorCodec`, organizationId null ⇒ PERSONAL), the variable-length structural rule (bytes 120–182, chars 160–243, `codeLength == byteLen−118`, unpadded base64url only), and corrects the max from 244 to **243** (244 is the rejected padded form).
- **B open question 1 (Accept-gate precedence):** CLOSED. Plan §8.1 inserts the boundary Accept gate as explicit step 3 (after auth, before controller; `CustomerBoundaryFilter.java:139–148` → 500 INTERNAL), renumbering the rest; `errorPrecedenceIsDeterministic` asserts "incompatible Accept + invalid limit → 500".
- **B open question 2 (offerType Optional):** CLOSED. Plan §5.1 Scope comment reads "non-null channel/audience; non-null Optional offerType (empty = no narrowing)".
- **B open question 3 (same-snapshot interleaving):** CLOSED. Plan §11.6 specifies a fixture-only delegating `CustomerCatalogOfferReader` bean whose `fetchBenefits` commits a benefit mutation over a separate owner-role autocommit connection between the two port calls; the REPEATABLE_READ snapshot (established at `fetchOffers`) must return pre-mutation benefits; a fresh request then sees the mutation. No production hook.
- **B open question 4 (no invented limiter/429):** CLOSED. Plan §8.1/§14 record that `RATE_LIMITED` stays in the frozen code set but the endpoint emits no 429 and adds no limiter (Operations-read precedent); `CustomerProblemWriter`'s existing `RATE_LIMITED→429` case for other endpoints is unchanged.
- **B open question 5 (§16 stop route):** CLOSED. Plan §16 replaces "None at plan level" with a binding stop-and-report condition for any unlisted guard or unavoidable schema/contract/topology need.
- **A open question 2 (non-CNY rows):** CLOSED as documented residual. Plan §5.4/§14 keep fail-closed `currency=CNY` (non-CNY ACTIVE row → 500 INTERNAL), add an IT non-CNY canary, and assign filter-vs-fail-closed/write-path hardening to Database/Product. Not a PA40B blocker.
- **A open question 3 (22-column guard criterion):** CLOSED. Plan §5.4/§7 keep `readerProjectsOnlyTheTwentyTwoApprovedColumns` as a SELECT/JOIN/WHERE/ORDER-BY column-set assertion over the granted 22 (not naive string containment), plus the `grant_interval_months`-outside-benefit negative assertion.

## 4. Exact final allowlist (39 paths)

- **Production (15 = 9 new + 6 modified):** plan §6.1 items 1–9 (unchanged) + §6.2 items 10–15 (unchanged except item 13 now states exactly 3 new `@Bean` methods, declared methods 15→18).
- **Test (21 = 7 new + 14 modified):** plan §6.3 items 16–22 (7 new, unchanged) + §6.4 items 23–36 (14 modified, §2.2 table).
- **Infra/docs (3):** plan §6.5 items 37–39 (`backend/pom.xml`, `.github/workflows/ci.yml`, `backend/README.md`).
- **Total = 39.** No migration, OpenAPI, docs/12/14/15/17, Control, private helper or Git change.

## 5. Exact current → future counts/totals

| Quantity | Current (verified) | Future |
| --- | --- | --- |
| `src/main/java` `.java` | 298 | 307 (+9) |
| `src/main/resources` files | 22 | 22 |
| `src/main` total | 320 | 329 |
| `customer_iam/internal` files | 47 | 52 (+5) |
| `src/test/java` `.java` | 192 | 199 (+7) |
| Surefire classes / methods | 105 / 1026 | 110 / **1071** |
| Failsafe classes / methods | 41 / 682 | 42 / 701 |
| Total classes / methods | 146 / 1708 | 152 / **1772** |
| `@Test` annotation line scan | 1709 | **1773** (+64) |
| POM Failsafe includes | 41 | 42 |
| Migrations | 19 | 19 |
| customer allowed-deps | 12 | 13 |
| `CustomerPersistenceConfiguration` declared methods | 15 | 18 (+3) |
| New unit methods | — | 45 (service 11 + codec 9 + web 14 + config 6 + boundary 5) |
| New Failsafe methods | — | 19 (CustomerCatalogOffersIT) |

The +1 service method (10→11) is the new negative authority test required by A-03; it moves Surefire methods 1070→1071, total 1771→1772, scan 1772→1773 relative to the pre-fix proposal. File counts (298→307, 192→199) are unchanged by it.

## 6. Current/future hashes

Current (recomputed in this worktree, all match the plan/guards):
- Test identity SHA256: `fe582055c9a28067aa156231f5aabfece21f91510a909a56a4c189c2ba4ca55c`.
- Frozen `src/main` manifest excl V019: 319 files, `57990aa0bc0886c45a58f4e63698d05f860f94a667bc047b2072b6251f3a66fd`.
- Incl V019: 320 files, `c290aac2bc7662d978e5a516e23d706a1b5195f8601055c3e8a917c21a4065e7`.
- README raw `8db84d4bad3676dda2d48b4ecf4a89f6a57b2a8855efc50b08c299912be7d42e` / LF `9c094a00f691fd8849363355a12c174cc289ce57841a63d0f25d04f8a30faf7b` (123,653 bytes).
- C27 aggregate `58161cad19014b05911bb068d8cd120a7f4a3630c9f6c2c98b7a3883132cef98`; historical26 aggregate `ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409`; historical-projected README LF `0816cc1c1ef622eaec9d4d7545e1ee8c6dc0f35813d4748b41d44dc9d4cec20b`; README current-section java-path consumers = 4.
- OpenAPI LF `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30`; V019 LF `32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e` (frozen, unchanged).

Future (recompute at implementation over the exact new file sets; not determinable at plan time):
- Test identity hash; excl-V019 manifest hash (328 files); incl-V019 manifest hash (329 files).
- README current raw/LF pair and the C27 aggregate (in both IVR verifiers), over the exact PA40B section bytes.
- historical26/F26/H26, `ed03b90f…`, `0816cc1c…`, OpenAPI/V001–V019 hashes: **unchanged**.

## 7. CI ledger threading (exact)

- Add `pa40b_unit_additions` (5 classes; lens `[11, 9, 14, 6, 5]`) and `pa40b_container_additions` (CustomerCatalogOffersIT; lens `[19]`); merge via the disjoint-union loop (shape of ci.yml:2353) so `source_classes == set(unit_expected) | set(container_expected)`.
- Define `pa40b_names` (ci.yml:2429 shape); append to baseline `additions` (2430) + 20 start-total sites (2435–2455) + 20 start-line-scan lambdas (2456–2536); add `pa40b_start_*` accumulators/legs.
- New asserts: `pa40b_start_totals == [1026, 682]`, `pa40b_start_line_scan_total == 1709`, addition lens above.
- Current asserts only: `method_totals == [1071, 701]` sum 1772; `line_scan_total == 1773`; `len(unit_expected)==110`; `len(container_expected)==42`; `len(includes)==42`; `exact_inventory(unit_expected, 1071)` / `(container_expected, 701)`; printed ladder head `PA40B … 110/1071 + 42/701 = 152/1772 … -> PA40B-start 1026+682=1708 -> current 1071+701=1772; line scans … -> 1709 -> 1773; POM 42 includes`.
- Every historical `pa19…pa40a_start_*` constant byte-identical; `comment_hits` unchanged; jar-smoke probes added for the new route (401/503/404 + aliases).

## 8. Residual risks / owners

- Implementation-time hash/manifest/identity/CRLF pinning: Backend/Test (plan §12.9).
- Role-wide column reads without RLS; privileged concurrent mutation: Database/Platform (PA25 runbook Q17/Q23).
- Production TLS/pg_hba, credential custody/rotation (incl. the new cursor HMAC key), pool drain, rollback, bootstrap: Database/Platform/Security/Operations (PA25 runbook).
- Non-CNY/out-of-band catalog rows (fail-closed documented; canary added): Database/Product.
- No invented limiter/429: Backend/Product (no in-slice action).
- Inter-request keyset movement: Backend/Product (documented).
- Secret inventory lives only in README current section (docs/14/17/runbook freeze the old key only): Docs/Operations at next docs-17 window.
- `COLLATE "C"` sort cost outside C-collated cluster (bounded by `uq_catalog_version_active`×2 channels): Database/Platform (docs/18).
- Cursor confidentiality (authenticated plaintext anchor, HMAC; precedent-bound asymmetry vs Operations AES-GCM): Security (no in-slice action).
- Hosted CI unavailable (account billing): Platform/account owner reruns exact-head CI; hosted green NOT claimed.
- Pre-existing comment-only `@Test` convention and A2/A5 validator limitations: Test/CI maintenance work unit (unchanged inputs).

## 9. Validation plan

Full sequence in plan §12 (run from `backend/`): `-Pcontainer-smoke verify` (Surefire 110/1071 + Failsafe 42/701, Modulith verify, all guards green with recomputed hashes) → `failsafe:verify` (42 reports) → frozen-input proofs → Modulith/dep guards (14 modules, 13 edges, customer deps 13) → CI ledger (`[1071,701]`/1773/110/42/42, `exact_inventory` 1071/701, historical ladder unchanged) → targeted re-run of all 14 audited guards (§12.5a) → package/jar smokes incl. new-route probes → CLI contract checks (A1–A5 per ci.yml; A2/A5 limitations recorded) → PostgreSQL16 `CustomerCatalogOffersIT` 19/19 + `CustomerCatalogOfferReadPrivilegesIT` 16/16 → recompute/pin the three frozen manifests + identity hash + one-byte mutation rejection. Expected: zero failures/errors/skips.

## 10. Stop condition and write-boundary compliance

This fix stops after producing the updated plan and this record; it does not implement, commit, push, PR, merge, approve, mark ready, request reviewers, comment, or enter re-review. If a later gate discovers an unlisted guard or an unavoidable schema/contract/topology need, it stops and reports (plan §16) rather than widening the write set. This gate edited only the cumulative plan and added this file; no other file was touched and no forbidden Git/external action occurred.
