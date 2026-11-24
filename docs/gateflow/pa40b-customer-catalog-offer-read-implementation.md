# PA40B-S1 customer catalog offer read — implementation report

- Task label: `pa40b-customer-catalog-offer-read-implementation-qwen4max-20261124-03`
- Date: 2026-11-24 (Asia/Shanghai)
- Binding inputs: `docs/gateflow/pa40b-customer-catalog-offer-read-plan.md` (+ plan-fix-1, plan-fix-2, six review artifacts, active PA40B Control checkpoint).
- Slice: the sole accepted `PA40B-S1` — frozen `GET /api/v1/customer/catalog/offers` through the existing customer pool and `customer_iam_role`. No other endpoint, field, filter, credential, pool, schema, migration, write, cache/RLS/front-end or future-lifecycle work.
- Stop condition honored: this artifact is produced before code review. No commit, push, PR, merge, approval, mark-ready, reviewer request, external comment or issue mutation occurred.

## 1. Planned slice confirmation

Implemented exactly one read capability mirroring the accepted customer business-number read seam:

- Owner port `catalog_order::customer-catalog-offer-query` (`CustomerCatalogOfferReader`) + internal jOOQ reader (`JooqCustomerCatalogOfferReader`) on the qualified customer DSL; one owner query for offers (mixed keyset, `limit+1` lookahead) and one for benefits.
- Customer application `CustomerCatalogOfferQueryService`: trusted-caller/workspace verification, lifecycle recheck via the existing `customerBusinessNumberAccessRepository`, read-only `REPEATABLE_READ`/`REQUIRES_NEW` transaction on the existing customer query pool, ACTIVE version + ACTIVE offer + caller channel + derived audience, optional `offerType`, fail-closed projection validation (CNY, QUOTE null price, canonical benefits, monotonic keyset, no duplicate/corrupt facts).
- Customer web `CustomerCatalogOffersController` + endpoint-isolated HMAC `CustomerCatalogOfferCursorCodec`/`CustomerCatalogOfferCursorProperties` + `CustomerCatalogOfferPage` DTO (13-field item / 4-field benefit line).
- `CustomerContextException.Code.CURSOR_FILTER_MISMATCH` + `CustomerProblemWriter` 400 mapping ("Cursor filter mismatch"); `CustomerBoundaryFilter` GET-only route admission; `CustomerPersistenceConfiguration` +3 `@Bean @Lazy` methods (15→18) and `CustomerCatalogOfferCursorProperties` registration; `customer_iam/package-info.java` allowed-deps 12→13; `application-web.yml` `onecall.customer.catalog-offers.cursor-hmac-key: ${ONECALL_CUSTOMER_CATALOG_OFFER_CURSOR_HMAC_KEY:}`.
- Deterministic error precedence; no 429 and no limiter; zero business writes on success/denial/failure.

## 2. Changed paths (exact 39-path allowlist, plan §6)

SHA256 is over the raw checkout bytes; line counts are newline-delimited. New = added; Mod = modified.

| # | Path | Lines | Bytes | SHA256 |
| --- | --- | --- | --- | --- |
| 1 | `backend/src/main/java/com/onecall/core/catalog_order/customercatalogquery/CustomerCatalogOfferReader.java` | 39L | 1779B | `d87b9285487b6e31a31985e6a1a3661795eb27d119ddd8f5528d0843e5476c68` |
| 2 | `backend/src/main/java/com/onecall/core/catalog_order/customercatalogquery/package-info.java` | 2L | 138B | `b9feac2f4cc2ff12ebeea43647400645085430a928bd0354afdb4a3c1e9bc566` |
| 3 | `backend/src/main/java/com/onecall/core/catalog_order/internal/customercatalogquery/JooqCustomerCatalogOfferReader.java` | 83L | 4897B | `c6429cbfa3df14d1d96c707a6ff3e4f2f2695b5bac17d116c4b2b1cf3a065654` |
| 4 | `backend/src/main/java/com/onecall/core/catalog_order/internal/customercatalogquery/CustomerCatalogOfferQueryConfiguration.java` | 19L | 780B | `54f24aa04b267607001cedd1a0c078543b1e0e401ea475cda5a640020c97ca64` |
| 5 | `backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerCatalogOfferQueryService.java` | 173L | 10622B | `13367d77083104e6ded45234a8f2b18541b1f3a383afe567bd96c9cfaa6570ef` |
| 6 | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerCatalogOffersController.java` | 109L | 6591B | `9f7cee465d2d6979cc483c97249fe5ce556ed00cb5eb43545de27eea91dde740` |
| 7 | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerCatalogOfferCursorCodec.java` | 137L | 7164B | `e9862cc54671495b6a6ebcbdbddafe8dee996c6cd88d27c6538104e3acb4aa3c` |
| 8 | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerCatalogOfferCursorProperties.java` | 11L | 427B | `0b538db3982beeccd816ae4cf055df12ec4f2eb03fcb9610b2fe54e3070b4282` |
| 9 | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerCatalogOfferPage.java` | 31L | 2084B | `6819fbffaf7f5c07f1e87bdfbaed9a84f6e7c4b5af6df02ac17b7b6920e57ec1` |
| 10 | `backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerContextException.java` | 13L | 502B | `ef15fe8abaf6695d13adba6a8db416dfee73390a1c2d092e85257a6133d7b5da` |
| 11 | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerProblemWriter.java` | 151L | 7878B | `45076bf597de545a0bd2edaf19bca17cc053e5566b7b3d4da5b22c7f41b3f68f` |
| 12 | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` | 211L | 12639B | `b6febd1938024fbb680e16ecab2b06490cf48e4768883ab3802616c47c06a96b` |
| 13 | `backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerPersistenceConfiguration.java` | 212L | 11108B | `714b68640f1018692cf36169bbaf78b94f3e365e31819a106c5289b0cbd223a1` |
| 14 | `backend/src/main/java/com/onecall/core/customer_iam/package-info.java` | 2L | 645B | `2d5b989c1550ed29744c29a5a59f622ffee54ea3fefe159365e1d7a280be9cda` |
| 15 | `backend/src/main/resources/application-web.yml` | 26L | 710B | `688e6ec58de29f3ba43d9b59013791d5d1ad895bb7d29bb04e790476d5d4b205` |
| 16 | `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerCatalogOfferQueryServiceTests.java` | 239L | 17281B | `aa48cfbf44c8fda2bb9092a897bfa73971dc700503c927c8fe4b814869cdb66d` |
| 17 | `backend/src/test/java/com/onecall/core/customer_iam/internal/web/CustomerCatalogOfferCursorCodecTests.java` | 189L | 13183B | `2c8284fa1b15eefbf02c8c76550ccfd27a99890e438eb51e5268eb4f16f398cd` |
| 18 | `backend/src/test/java/com/onecall/archtests/CustomerCatalogOffersWebTests.java` | 270L | 16018B | `81906dfe035c00b3035f3e191323b0ff20cc28492ce565b2acf42b26255960f3` |
| 19 | `backend/src/test/java/com/onecall/archtests/CustomerCatalogOfferConfigurationTests.java` | 231L | 17438B | `8fc1fea512b93c07bf72dc589f46d8e56ab12b60220fb804d6a446b70406d267` |
| 20 | `backend/src/test/java/com/onecall/archtests/architecture/CustomerCatalogOfferBoundaryTests.java` | 135L | 11232B | `04a13dc2e65f6ae2cae04d41d45b5fdd9031afaa675360bfb4e6ae805b21dad1` |
| 21 | `backend/src/test/java/com/onecall/archtests/container/CustomerCatalogOffersIT.java` | 373L | 24519B | `f6f1d4ba408cf4610da151ab24aef325b210b32af7f95cec6a7fac8d21ab28aa` |
| 22 | `backend/src/test/java/com/onecall/archtests/container/CustomerCatalogOffersFixture.java` | 228L | 16660B | `31d517247d017f9fd62fd44eea92976512da6571f5cd306f9ca9b5f5f2e06816` |
| 23 | `backend/src/test/java/com/onecall/archtests/architecture/CustomerCatalogOfferReadAclBoundaryTests.java` | 142L | 12720B | `17763257cdbed5bd6df33b9f68c79f06915beb914245e0672908baec3f6e54f8` |
| 24 | `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java` | 187L | 17616B | `161671772f54ef01ba4667050440cf573f63bc3a97cef1cff3ea7c3e982e0172` |
| 25 | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` | 245L | 17696B | `21f05a8cac5ee8b9c4dbc19ce863460c88f4b9ed3d58f897a831e2b526566851` |
| 26 | `backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java` | 265L | 24181B | `af317aa4e3bab6123f8a18fdd723d920c5eb4845726523b4eafc2f4824846d62` |
| 27 | `backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java` | 132L | 10107B | `f912f7c0f308962a601a7c91983deff29a699ef9065295d024c33d9f5b67a5ac` |
| 28 | `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectDetailBoundaryTests.java` | 135L | 11881B | `b0f2ae40d7aee54ceaf39da2c473da9ba0cadd40b5cb404bc724a180eec184de` |
| 29 | `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogDraftBoundaryTests.java` | 32L | 6892B | `3e9fc169bd046bd4087c794a8f3a999703fb0beda622ba00daa07fca959f92c7` |
| 30 | `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java` | 28L | 5070B | `8fec51b2828745c7a62953517be804e30ec26f7d691e74235fd39edce1ac13cb` |
| 31 | `backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java` | 297L | 24530B | `38683f4d8501c4bb411666fb887c0719d7c63ede8a00f7666b49b91342875c10` |
| 32 | `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerBusinessNumberQueryServiceTests.java` | 243L | 20654B | `a99fee6ed6c36f29dcce9e0282a11ef3ef8971c1c704a4313ef3283236a2f4a1` |
| 33 | `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` | 594L | 66668B | `01f8683c230928215166e0dfc1b2e32edbd5c3e5756199fdeb7f23c4f2593711` |
| 34 | `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java` | 150L | 23300B | `db71cdae1cf5c311e8d506f1c767d99de19d75f66df71491421eb2209ce24699` |
| 35 | `backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java` | 324L | 35205B | `468658859bd87b99cea444142e532c45650683da25ea0d4ca02710da22d0805f` |
| 36 | `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java` | 447L | 50341B | `e5b2d05ca779c07d7c0fb7d494b9286fd57623a7700c82c53138ddceafc8a8f3` |
| 37 | `backend/pom.xml` | 241L | 10763B | `35da4637f2ab2ffa728af836f914c4121e9c48c4c298d80c0c80a126780332cb` |
| 38 | `.github/workflows/ci.yml` | 3383L | 234095B | `7c1f2308a6b6f5abcef5b8e29e5b1a1bc3b1c40d6fd1de0eba34b97ec45d9c20` |
| 39 | `backend/README.md` | 591L | 125920B | `d8065005190dfecc1bc4809f936777927e3581c29a346cf709a5dcef37d912ee` |
Write-set audit: `git status --porcelain` shows exactly 23 modified + 16 new = 39 allowlisted paths. The untracked `.pa40b-*` private helper scripts are pre-existing and were not created or edited by this implementation. No V001–V019, PA25 SQL/runbook, OpenAPI, docs12/14/15/17, Control, private helper or unlisted path was touched. This report is the only additional artifact.

## 3. Current → future identity and inventory deltas (verified)

| Metric | Before | After |
| --- | --- | --- |
| Surefire classes / ordinary methods | 105 / 1026 | 110 / 1071 |
| Failsafe classes / ordinary methods | 41 / 682 | 42 / 701 |
| Total classes / methods | 146 / 1708 | 152 / 1772 |
| `@Test` annotation line scan | 1709 | 1773 |
| POM Failsafe includes | 41 | 42 |
| Successful migrations | 19 | 19 (unchanged) |
| `src/main/java` `.java` files | 298 | 307 |
| `src/main` files (java+resources) | 320 | 329 |
| `customer_iam/internal` files | 47 | 52 |
| `src/test/java` `.java` files | 192 | 199 |
| `customer_iam` named-interface allowed-deps | 12 | 13 |
| `CustomerPersistenceConfiguration` declared methods | 15 | 18 |

Recomputed frozen pins (LF-normalized):
- excl-V019 production manifest: 328 files, SHA256 `27d4a25ea4e6f792565ea638a1d9a89c3b04c2d4bd978806d15827cbc36373a9`.
- incl-V019 production manifest: 329 files, SHA256 `3be935b02c6c2a6622880e2d59274e386c6f17649ce712d7d2b728dcfc7b0f97`.
- excl-V018+V019 production manifest (`EvidenceRoleIvrAssociationAclBoundaryTests`): 327 files, SHA256 `ecc30b8b411debc208cca8415a9a2cdb576ee2e9b1ee54fee129b6d792ca206d`.
- Test-identity hash (1772 sorted `fqcn#method` lines): `dd3d1c5e93baa9d9cd5e362cfb3ea2493589b5a3ef73f4a5dace1da16738cab8`.
- README canonical-LF SHA256 (both `currentPins` slots): `95e6e5e042792ddd5878817f4536b5901fc74749a0bd49095ec2161c49a2f70d`.
- C27 aggregate (both IVR verifiers): `738652e487145167e32c07fc315ed0bb2dbdf4f4f4e2295712eb11a4b57cb75e`.
- README historical-body hash `fb6ab282…3beb`, projected README LF `0816cc1c…c20b`, historical aggregate `ed03b90f…54409`, and the 4-consumer backtick-java-path count all remain unchanged (PA40B section inserted inside the deleted PA40A→marker window with zero new backtick java paths).
- Frozen V001–V019 / OpenAPI hashes verified unchanged by `CustomerCatalogOfferBoundaryTests.openApiAndV001ThroughV019RemainFrozenWithoutContractExpansion`.

## 4. New test identities (exact)

- `CustomerCatalogOfferQueryServiceTests` (11): defaultBoundsAndLookaheadRequestAreExact; trustedWorkspaceDerivesAudienceAndLifecycleIsRequiredBeforeReaders; activeChannelAudienceAndOfferTypeNarrowingReachTheOwnerReader; mismatchingRequestChannelOrAudienceIsRejectedBeforeAnyReaderCall; quoteProjectsExplicitNullPriceAndOnlyStoredVersionToken; lookaheadUsesLastReturnedOfferAndNullCursorOnTail; emptySingleAndMultiPageProjectionIsExact; emptyBenefitLinesAreValidAndCanonicalOrderIsEntitlementTypeAscending; nonMonotonicDuplicateOrCorruptOfferFactsFailClosed; duplicateOrphanOrOutOfRangeBenefitFactsFailClosed; technicalAndProjectionFailuresReturnNoPartialOrSensitivePage.
- `CustomerCatalogOfferCursorCodecTests` (9): personalAndOrganizationRoundTripsAreCanonicalAndDeterministic; payloadVersionLengthDomainAndAnchorAreExact; invalidAlphabetPaddingWhitespaceAndAlternateEncodingAreRejected; truncatedExtendedUnknownVersionAndInvalidAnchorAreRejected; everyPayloadOrMacByteMutationIsRejected; accountWorkspaceKindAndOrganizationReplayAreRejected; normalizedFilterChangeIsFilterMismatchNotInvalid; missingMalformedAndWrongLengthKeysAreUnavailableWithoutDisclosure; keyReplacementInvalidatesOldCursorAndLimitDoesNotBindScope.
- `CustomerCatalogOffersWebTests` (14): successPageHasExactFieldsBytesHeadersAndNullPriceForQuote; tailHasExplicitNullNextCursorAndHasMoreFalse; unauthenticatedAndVerifierOutageMap401And503; unsupportedMethodsAliasesTrailingAndEncodedVariantsAre404BeforeAuth; malformedOrRepeatedLimitIs422; malformedOrRepeatedEnumFiltersAre422; conflictingChannelOrAudienceIs403Forbidden; offerTypeNarrowingAndEmptyIntersectionAre200; malformedTamperedAndCrossScopeCursorsAre400CursorInvalid; normalizedFilterChangeIs400CursorFilterMismatch; missingCursorKeyIs503WithoutEagerPool; errorPrecedenceIsDeterministic; problemEnvelopesCarryTraceIdsAndNoSecrets; acceptAndContentTypeNegotiationStayJson.
- `CustomerCatalogOfferConfigurationTests` (6): defaultAndNonWebProfilesRemainDatabaseFree; webWiringIsLazyAndInvalidRequestsDoNotCreatePools; catalogOfferTemplateIsIndependentReadOnlyRepeatableRequiresNew; ownerReaderUsesOnlyTheQualifiedCustomerDsl; missingCredentialsReaderAndCursorKeyFailClosedWithoutFallback; cursorKeyResolutionStaysAfterStructuralInputValidation.
- `CustomerCatalogOfferBoundaryTests` (5): catalogOrderExposesOnlyTheNamedCustomerCatalogOfferQueryInterface; customerImportsOnlyThePublicCatalogSeamAndNoForeignInternalImplementation; controllerServiceAndOwnerSqlStayInsideTheirSemanticOwners; readerProjectsOnlyTheTwentyTwoApprovedColumns; openApiAndV001ThroughV019RemainFrozenWithoutContractExpansion.
- `CustomerCatalogOffersIT` (19) + `CustomerCatalogOffersFixture` (no `@Test`): postgres16FullChainAndCustomerRuntimeRoleAreUsed; personalScopeReturnsOnlyActivePersonalChannelOffers; organizationScopeReturnsOnlyActiveEnterpriseQuoteOffers; draftOffsaleWithdrawnWrongChannelAndWrongAudienceFactsNeverAppear; offerTypeNarrowingAndEmptyIntersectionAreExact; quotePriceIsNullAndCurrencyIsCnyAndTokenIsStored; benefitLinesAreCanonicalAndEmptyWhenAbsent; productCodeAscVersionNoDescCatalogOfferIdAscTiesAreExact; emptySingleAndExactLimitPagesHaveExactTailSemantics; firstMiddleAndLastPagesEnumerateEveryOfferOnce; defaultOneAndHundredLimitsAndChangedContinuationLimitAreExact; malformedTamperedAndCrossScopeCursorsCannotWidenResults; normalizedFilterChangeIsCursorFilterMismatch; committedLifecycleChangesAreRecheckedForEveryPage; lifecyclePageAndBenefitsShareOneRepeatableReadSnapshot; concurrentInsertAndDeletedAnchorFollowDocumentedKeysetSemantics; successDenialAndFailurePerformZeroBusinessWrites; roleIsolationReadOnlyAndAutoCommitRestoreOnPooledConnectionReuse; responsesAndCapturedLogsNeverContainSecretOrOwnershipSentinels.

## 5. Validation results

Environment: WSL2 Linux JDK 21.0.13 (`/opt/jdk21`) + Maven 3.9.9 (offline `-o`; Windows `java`/`mvn` on PATH are non-functional and were bypassed). PostgreSQL 16 via Testcontainers (`postgres:16`). Hosted CI is externally unavailable (account billing); all evidence below is local and exact. No pass is fabricated.

### 5a. Focused evidence (run first)

- New Surefire methods: 45/45 green (service 11, codec 9, web 14, configuration 6, boundary 5).
- Audited guard batch (13 guard classes + the 5 new unit/web classes): 131 methods green after the §6 manifest-literal correction; targeted re-run of `EvidenceRoleIvrAssociationAclBoundaryTests` + `CustomerCatalogOfferReadAclBoundaryTests` + `OperationsIvrSessionReadBoundaryTests` = 24/24 green.
- `CustomerCatalogOffersIT`: 19/19 green on postgres:16 (role restoration, zero-write snapshot equality, same-snapshot interleaving, 42501 adjacent-column denial via the customer runtime role).
- `CustomerCatalogOfferReadPrivilegesIT`: 16/16 green on postgres:16.
- Modulith `modules.verify()` green via `ModuleBoundaryTests` (9/9) / `CustomerApiBoundaryTests` (14 modules, 13 edges, customer allowed-deps 13, no cycle).

### 5b. Full plan §12 sequence

1. `./mvnw -B -ntp -Pcontainer-smoke verify` (full build, authoritative clean re-run, total time 01:08 h): Surefire **1071/1071** (0 failures/errors/skips), Modulith green, all guards green with recomputed hashes. Failsafe aggregate: **701/701** (0 failures/errors/skips), single clean run.
2. `./mvnw -B -ntp -Pcontainer-smoke failsafe:verify`: **BUILD SUCCESS** over all 42 reports (701/701, 0 failures/errors/skips).
3. Frozen-input proof: `CustomerCatalogOfferBoundaryTests.openApiAndV001ThroughV019RemainFrozenWithoutContractExpansion` and `CustomerEvidenceBoundaryTests.openApiAndV001ThroughV016RemainFrozenWithoutContractExpansion` green (OpenAPI `ce8de66d…7e30`, V001–V019 unchanged); `CustomerCatalogOfferReadAclBoundaryTests` green (V019 22 grants, 19 migrations, 432 oracle, 7-login/11-edge graph, README/runbook markers).
4. Modulith: `ModuleBoundaryTests`, `CustomerApiBoundaryTests.customerDependenciesAndReadOnlySqlStayInsideBoundary`, `CustomerCatalogOfferBoundaryTests` green (14 modules, 13 edges, allowed-deps 13, no internal import, no cycle).
5. CI Python ledger (`assert container smoke actually ran`, run from `backend/`): **PASS** — `PA40B exact source=CI=POM=XML inventory passed: 110/1071 + 42/701 = 152/1772`; full historical ladder 290→320→360→400→445→480→523→543→555→613→677→753→838→924→1014→1120→1208→1232→1344→1440→1564→PA40A-start 1684→PA40B-start 1708→current 1772; line scans 291→…→1685→1709→1773; `method_totals==[1071,701]`, `line_scan_total==1773`, `len(unit_expected)==110`, `len(container_expected)==42`, POM includes 42, `exact_inventory` Surefire 1071 / Failsafe 701, `pa40b_start_totals==[1026,682]`, `pa40b_start_line_scan_total==1709`, every historical `pa19…pa40a_start_*` unchanged; zero skips/errors/failures.
6. Package/jar: jar `backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar` built by the full verify. CI `default and opt-in web jar smokes` block run verbatim: **EXIT=0**, 166 JAR probes across `/error` and `/internal/pa34-error` variants; default jar starts with no Tomcat/Netty/Jetty/Undertow/HikariPool; web jar PA40B probes exact — GET 401 UNAUTHENTICATED, Bearer smoke-opaque 503 SERVICE_UNAVAILABLE, POST/PUT/PATCH/DELETE/OPTIONS 404, HEAD 404 empty body, TRACE 500 INTERNAL (deviation 1), aliases `/`,`/extra`,`;x`,`//offers`,`%61ffers`,`%2F` all 404; no HikariPool, no secret/ownership leakage, JVMs cleaned up. CI `production jar dependency firewall` block run verbatim: **EXIT=0** — baseline37/34, ssm72/65, counterparts64, dependency-only8, packager1, deltas35/0 and31/0, 116 adversarial probes; approved legacy-regex collisions and packager branch unchanged.
7. CLI contract checks (repo root, `api/openapi`, node v22.23.4, `npm ci` done): **A1 `check:s3` PASS** (49 operations; customer 18 / operations 21 / ivr 6 / public-share 3 / provider-callback 1); **A3 security s3 PASS** (drift=0; a0_tables=44 a1_operations=49); **A4 deployment/capacity s2 PASS** (groups=9 fixtureCases=113 passed=113 failed=0; productionReady=false). **A2 protocol/ivr s3 FAIL** and **A5 acceptance s1 & s2 (`--check-doc-links`) FAIL** — both pre-existing source/document input limitations on UNTOUCHED inputs (A2: model references A0 columns absent from docs/14; A5: docs/19 §7.1 state-table header). Recorded as limitations, NOT claimed as passes; git confirms those inputs unmodified.
8. PostgreSQL16 specifics: `CustomerCatalogOffersIT` 19/19 and `CustomerCatalogOfferReadPrivilegesIT` 16/16 on `postgres:16` (role restoration, zero-write snapshot equality, 42501 adjacent-column denial) — green standalone and within the full run.
9. Frozen pins recomputed and pinned (§3). One-byte mutation rejection verified empirically per existing guard style: appended a single space byte to `CustomerCatalogOfferPage.java`, ran `CustomerCatalogOfferReadAclBoundaryTests#migrationOnlyAddsOnePackagedResource` → **rejected** (manifest SHA256 `3be935b0…` vs observed `14b21b38…`); restored byte-exactly (SHA256 `6819fbff…` unchanged) and re-ran → **green**. The test-identity guard uses the same exact-hash-equality style and is exercised green by the full ledger/guard run. (Process note: the probe's `-Dsurefire.reportsDirectory` override was not honored by the POM, so the probe wrote the default `CustomerCatalogOfferReadAclBoundaryTests` Surefire XML with a single method; that one report was regenerated by a full green 8/8 re-run of the class before the XML inventory. No other Surefire/Failsafe report was affected.)

### 5c. Write-set audit

`git status --porcelain -uall` (excluding pre-existing `.pa40b-*` helper scripts, which were neither created nor edited) shows exactly **23 modified + 16 new = 39** allowlisted paths (§6). No V001–V019, PA25 SQL/runbook, OpenAPI, docs12/14/15/17, Control, private helper or unlisted path was touched. This report is the only additional artifact.

### 5d. Full-run transparency (intermittent pre-existing failure)

The first full `-Pcontainer-smoke verify` completed Surefire **1071/1071** and Failsafe **700/701**, with a single intermittent failure in the pre-existing `PublicSharesIT.publicProjectionUsesDedicatedRoleOnSameReadOnlyConnection` (expected 200, got 404 at `PublicSharesIT.java:134`). That class is outside the PA40B 39-path allowlist and was not modified; the PA40B `CustomerBoundaryFilter` diff is additive-only (`|| "/api/v1/customer/catalog/offers".equals(servletPath)`) and cannot affect `/api/v1/public/shares` admission. `PublicSharesIT` starts its own per-class container with per-test reset, so this is an internal intermittent race, not cross-IT ordering. An isolated re-run of `PublicSharesIT` was **8/8 green**. A clean full re-run (no concurrent smoke/probe JVMs) was performed for the authoritative single-run result reported in §5b.

## 6. Deviations and implementation notes (all recorded, none fabricated)

1. TRACE on the exact path returns 500 INTERNAL, not the plan §8.3 "404". This is the existing `CustomerBoundaryFilter` behavior for customer routes (identical to the frozen `customer/me` jar-smoke probe `TRACE → 500`); TRACE is not admitted by the GET-only gate and the boundary's TRACE handling yields a sanitized 500. Changing it to 404 would require altering shared customer-boundary TRACE handling (out of scope, would affect `customer/me`). The web test and jar probe assert the actual 500. POST/PUT/PATCH/DELETE/HEAD/OPTIONS and all aliases (`/`, `/extra`, `;x`, `//offers`, `%61ffers`, `%2F`) are 404 before auth, as planned.
2. Append-only canary/lifecycle mutations: V004 `trg_catalog_product_immutable` (BEFORE UPDATE OR DELETE) and `trg_catalog_benefit_line_immutable` (BEFORE UPDATE OR DELETE) reject the plan's canary product DELETE and benefit-quantity UPDATE. Following the accepted `CustomerSubjectActivationsIT` precedent, the fixture admin (superuser) temporarily `DISABLE TRIGGER`, performs the exact mutation, and `ENABLE TRIGGER` in `finally`. The non-CNY canary baseline is snapshotted AFTER insert and the exact product/version/offer rows are deleted in `finally` (Low instruction 1); the lifecycle method keeps the plan's exact `quantity+1` UPDATE and pre/post assertions. No production trigger/schema is changed.
3. `concurrentInsertAndDeletedAnchorFollowDocumentedKeysetSemantics` assertion corrected: `fixture.insert(...)` appends AAA/ZZZ to the seed list, so the original `fixture.expected("PERSONAL").subList(1,6)` was stale. The test now captures the expected list before the inserts and asserts the continuation equals (original minus deleted anchor) + ZZZ. The reader's keyset behavior was already correct (deleted anchor and before-anchor AAA excluded, after-anchor ZZZ included).
4. Plan §6.4 item 34 under-specified one literal: `EvidenceRoleIvrAssociationAclBoundaryTests.v001ThroughV017AndPublicContractsRemainByteFrozen` also pins the excl-V018+V019 production manifest (`hasSize(318)`→327 + hash). The file is allowlisted (#34); this necessary mechanical literal was updated (no checker weakened, exact count+hash still pinned). Recorded as a plan-audit omission resolved within the allowlist, not a write-set widening.
5. CI early current-total check `sum(Surefire) != 1026` updated to 1071 (within "update only current totals"); all historical `pa19…pa40a_start_*` constants kept byte-identical.
6. Same-`productCode` sort ties are constraint-impossible at rest: V004 `uq_catalog_version_active` permits one ACTIVE version per product, so the `(productCode, versionNo DESC, catalogOfferId ASC)` tie-break on equal productCode cannot occur among visible rows; the ordering is still implemented and asserted exactly.
7. Non-CNY canary channel equals the caller channel (WEB) per Low instruction 2; the canary is ENTERPRISE/QUOTE/USD and reachable only from the enterprise QUOTE scope inside its owning method.

8. CI ledger `pa40a` historical computation lines corrected: when PA40B additions were merged, `pa40a_start_totals[suite_index] += len(names) - len(pa40a_names)` and the `pa40a_start_source` lambda (`in pa40a_names`) each needed `| pa40b_names` appended (exactly as every `pa19…pa39` start-total/start-source line already received). Without this, the historical invariant `pa40a_start_totals == [1018, 666]` failed (computed `[1063, 685]`). Both computation lines were updated within allowlisted `ci.yml` (#38); the asserted historical constants (`[1018, 666]`, line-scan `1685`, and all `pa19…pa40a` constants) are byte-identical. The three ci.yml-reading guards (`CustomerCatalogOfferReadAclBoundaryTests`, `EvidenceRoleIvrAssociationAclBoundaryTests`, `SharingQueryBoundaryTests`) re-ran 24/24 green and the full ledger passes after the fix.

## 7. Residual risks (owner / destination)

- Role-wide column reads without RLS: endpoint ACTIVE/channel/audience predicates are the application control; privileged-administrator concurrent mutation remains Database/Platform operational control (PA25 runbook Q17/Q23).
- Production TLS/pg_hba, credential custody/rotation (including `ONECALL_CUSTOMER_CATALOG_OFFER_CURSOR_HMAC_KEY`), pool drain, live rollback-by-backup, bootstrap ordering: Database/Platform/Security/Operations (PA25 Q17/Q23). Production acceptance NOT EXECUTED.
- Hosted CI externally unavailable (account billing): Platform/account owner must rerun exact-head CI after recovery; hosted green NOT claimed.
- Pre-existing comment-only `@Test` convention and A2/A5 source/document validator limitations: Test/CI maintenance (unchanged inputs).
- Inter-request moving-row keyset semantics (no global no-skip/no-duplicate): documented product behavior, Backend/Product.
- No invented limiter / no 429: `RATE_LIMITED` stays in the frozen error-code set; this endpoint emits no 429 (accepted Operations-read precedent), Backend/Product.
- Non-CNY / out-of-band `catalog_version` rows: fail-closed `currency=CNY` validation (non-CNY ACTIVE row → sanitized 500; a bad row before the anchor can stall keyset paging). Decision filter-vs-fail-closed and write-path hardening: Database/Product; documented, not a PA40B blocker.

## 8. Allowlist / forbidden-action confirmation

- Only the 39 §6 paths plus this report were written. No migration, OpenAPI, docs12/14/15/17, PA25 SQL/runbook, Control, private helper or unlisted path edited.
- No commit, push, PR, merge, approval, mark-ready, reviewer request, external comment or issue mutation. Stopped before code review.

## 9. Bounded code-review fix 1 — 2026-11-24

- Task `pa40b-code-review-fix1-gpt-20261124-02`; gate: `code review fix`, not re-review. The implementation-stage evidence above remains historical; the four changed source/document table rows and two current aggregate pins now describe this fix candidate.
- Closed accepted Low `PA40B-CR-B-02`: README now distinguishes account/workspace/scope/endpoint/sort replay (`CURSOR_INVALID`, 400) from normalized channel/audience/offerType changes after valid scope/MAC (`CURSOR_FILTER_MISMATCH`, 400). Secret/no-default/fail-closed behavior and historical body are unchanged; no new backtick Java paths were introduced.
- Closed accepted Low `PA40B-CR-B-04`: the existing frozen-input method reads actual bytes, hashes without normalization, checks the original canonical LF digests and permits only canonical raw LF/CRLF. Its existing method now proves both supported representations pass and CR-only and the retained one-byte `X` prefix mutation fail for all 21 frozen inputs. No test identities, frozen hashes or files were added/removed.
- Focused Surefire final run: `CustomerCatalogOfferBoundaryTests` 5/5 and `OperationsIvrSessionReadBoundaryTests` 8/8, zero failures/errors/skips, BUILD SUCCESS. An earlier overlapping run compiled before pin recomputation and correctly rejected the old README pin; the final rerun supersedes it.
- Failsafe/PostgreSQL16 rerun attempted for `OperationsIvrSessionReadsIT`, but Testcontainers failed before tests in `@BeforeAll`: Docker Unix socket connection returned `Operation not permitted`. Current Failsafe XML therefore contains a setup error; no current container green or full fresh 152/1772 pass is claimed.
- Extracted unmodified CI ledger fails on current reports (`wrong aggregate test inventory`) after that setup failure. A separately labelled `/tmp/pa40b-fix1-02/ledger-evidence` projection uses current source/POM/Surefire and preserved pre-attempt Failsafe reports; it passes 110/1071 + 42/701 = 152/1772, scan 1773, POM 42 and the complete historical ladder. Repository reports were not restored or falsified. This is inventory/historical evidence, not a successful post-fix PostgreSQL rerun.
- `git diff --check` passes. Only the six authorized fix paths are changed relative to this task's dirty starting candidate; pre-existing implementation paths, Control, review artifacts and excluded private helpers remain untouched. Deferred TRACE and plan-manifest findings and rejected trusted-derivation consolidation were not implemented.
- Full fix scope, evidence and environment blocker: `docs/gateflow/pa40b-customer-catalog-offer-read-code-review-fix-1.md`. Stop here; no re-review or remote/repository mutation actions.

## 10. Post-fix exact-head validation

- The earlier Docker blocker in §9 was resolved by running the candidate outside the restricted Codex sandbox in WSL2/JDK21/Maven 3.9.9/PostgreSQL16.
- Exact accepted PR candidate before final documentation bookkeeping: `6f77d66c971d8424df237d1e09d238d71d3384bf`.
- Final uninterrupted `mvn -B -ntp -o -Pcontainer-smoke verify`: `BUILD SUCCESS`, total time 01:08 h, `EXIT=0`; Surefire 110 classes / 1071 methods and Failsafe 42 classes / 701 methods, all zero failures/errors/skips. Log: `/tmp/pa40b-pr52-final-verify.log`, 1,702,560 bytes / SHA256 `bab7a465b9a909e9a462e4cc6374e4ac2952c1d45f434d350248e531fa656df0`.
- The unchanged CI inventory ledger run against those final reports ended `EXIT=0`: 110/1071 + 42/701 = 152/1772, annotation scan 1773, POM 42, complete historical ladder intact. Log: `/tmp/pa40b-pr52-final-ledger.log`, 950 bytes / SHA256 `e001b7801fcb0cb6094f20bfbe9af59473a700b33ac2843af8f910fff20166eb`.
- This supersedes only the point-in-time sandbox limitation recorded in §9; the earlier failed/preserved-report logs remain honest historical evidence. Hosted exact-head CI remains unavailable and is not claimed as passing.
