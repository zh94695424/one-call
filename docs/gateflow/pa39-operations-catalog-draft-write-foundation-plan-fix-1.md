# PA39 Operations Catalog Draft Write Foundation — Bounded Plan Fix 1

Task: `pa39-operations-catalog-draft-write-foundation-plan-fix1-gpt-20261120-01`.
Date: 2026-11-20. Gate: plan fix only; no product implementation or runtime acceptance.
Working directory: `/mnt/c/Users/Administrator/.codex/worktrees/pa39-next-work-unit/codex-call`.
Read-only verified branch: `codex/pa39-next-work-unit`; HEAD `69e9c14f2929ab9c47e1d48c11fd17fc608a8c28`; tree `bfcf00bb5edf1110f850cd901f19848b155bf93d`.

## 1. Authority And Write Boundary

Binding inputs were the top PA39 Goal Confirmation in `docs/gateflow/project-execution-control.md`, including the user-confirmed 2026-11-20 unknown-field422 clarification; the cumulative plan; reviews `docs/reviews/plan-review-20261120-160606.md` and `docs/reviews/plan-review-20261120-170922.md`; and the exact local OpenAPI routes/schemas, docs12/14/15/17, relevant V001–V018 definitions/effective grants, PA37/PA38 code/tests, architecture guards, POM, CI, README and PostgreSQL fixtures.

Only the cumulative plan is updated and this bounded artifact is added. Two frozen routes and **one S1 behavioral slice** remain. No product code, test, POM, CI, README, OpenAPI, migration, semantic specification, review, Control, private helper, generated artifact or Git metadata was edited. No dependency installation, staging, commit, branch creation, remote/PR/issue action or later gate occurred. Sub-agents were not spawned.

Control raw SHA256 at fix entry and completion: `d5c2a90523f577f9aa1609c6131c43401ecdb2df83389124e7894a160adc0ee9`.
The historical review-freeze Control hash is not substituted for this clarified current input.

## 2. Accepted Findings — Plan Closure

All eight findings are **closed at the plan level**. This is not a claim that future implementation tests passed.

| Finding | Exact closure / cumulative-plan destination | Residual evidence owner / destination |
| --- | --- | --- |
| PA39-PLAN-REV-A-01 | §7.3 expressly authorizes five edits in `EvidenceRoleIvrAssociationAclBoundaryTests.ciPomAndOrdinaryMethodInventoryMatchExactly`: unit classes97→104, container classes38→40, second38 (POM includes)→40, current methods `[934, 630]`→`[1018, 666]`, current annotation lines1565→1685. Manifest310→318/digest remains authorized separately. All eight identities and every PA36/PA35/PA38 historical literal stay unchanged. | Backend: exact focused1/1 guard, complete3/25 architecture selector, then full source=CI=POM=XML. |
| PA39-PLAN-REV-A-02 | §7.3 restricts README edits to the current section before the actual HTML marker. Preserve marker and every following raw byte; retain the same four backticked Java consumer paths and `consumers == 4`. Refresh only the current inventory paragraph and add bounded runtime documentation. No guard historical hash/consumer-count edit. §9.2 pins `currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory`. | Backend: exact focused1/1 method and raw historical suffix comparison, not only normalized hashes. |
| PA39-PLAN-REV-B-01 | §6.4/§6.5: both commands map55P02/55P03 to `CATALOG_DRAFT_CONFLICT`409 after rollback, including recovery-lock expiry; no automatic retry/recovery from a lock failure. Keep250ms. Statement57014/connection failures remain503. §7 separates actual within-budget replay from an expired409 loser and a distinct later retry. Assert actual durable version/children/tokens/revisions and audit cardinality/content, with no loser writes. | Backend/Database: real PG1655P03 on create and replace, unit coverage of both states, observer-before-dispatch wait evidence, repeated2/8 race/fault selector. Harness/environment failures still block. |
| PA39-PLAN-REV-B-02 | §1/§4/§6.5/§7 apply the user-confirmed rule, not B's superseded proposed409 input handling: every member absent from frozen replace schema, including product/version IDs, benefits/channels, status/timestamps/tokens/creator/hash, is strict422 before provider/state checks. Semantic409 means a grammar-valid command against persisted immutable mismatch/state. Existing binder/web/live HTTP identities carry all subcases. | Backend/Contract: exact422 envelope and zero-state/audit-change assertions against missing/stale/published targets; no OpenAPI/docs15/Control edit. |
| PA39-PLAN-REV-B-03 | §5.1 recursively sorts keys by UTF-16 code units and canonicalizes benefit/channel arrays before UTF-8 serialization. docs15's twelve semantic members map exactly to eleven P members after omitting only self-referential snapshotSha256. Response metadata is outside that list, not extra hash exclusions. Corrected golden lengths/hashes are below; jsonb order cannot control the digest. | Catalog owner: sorted-key/null/large-exact-integer vectors, jsonb key-reorder acceptance and former ordered-key-digest503 detection. No alternate canonical convention or silent repair. |
| PA39-PLAN-REV-B-04 | §5.1/§6.3/§6.5: valid never-published DRAFT with unrecognized snapshot/member set→409 on create replay, replace or recovery, before revision/application. Recognized malformed/inconsistent snapshot, row/child disagreement or bad hash→503. Valid non-DRAFT state→409 without draft-format recognition; malformed primitive/lifecycle shape→503. No overwrite/backfill. Valid WITHDRAWN offer after integrity checks→409; malformed offer→503. | Catalog/Data: semantics fixture loops and live create/replace/replay before/after durable-state proofs. Legacy repair is a separate operational unit. |
| PA39-PLAN-REV-B-05 | §3.2: Operations binder alone owns closed wire grammar; catalog-local CatalogDraftSemantics alone owns commercial matrix and projection. Command calls owner validation before transaction/authority/SQL and throws the shared typed VALIDATION_FAILED mapped422 by web. Public records remain data carriers; no reverse import, validation SPI or new module edge. Exact command identity `portCraftedInvalidMatrixReturns422BeforeTransactionOrAuthority`. | Backend/Architecture: direct-port invalid matrix tests, zero repository/transaction/authority calls, unchanged14modules/13edges/5catalog allowed dependencies. |
| PA39-PLAN-REV-B-06 | §4.1 removes the invented query-string422 rule and corresponding planned subcase. Match shipped sibling write handling: no query inspection/rejection, parameter-derived input/authority or new branch. No new test identity is needed or retained for the removed rule. | Backend: source/admission/binder boundary review; no new query policy or future work added. |

## 3. Material Open Questions Resolved

- Binder returns only local nested `ParsedCreate` / `ParsedReplace`, with no caller. Controller adds trusted Caller to public commands. `create` returns CreateResult; `replace` returns CatalogVersionDraft directly. No ReplaceResult or additional top-level type/file.
- The new draft Code has eight reachable values, not dead RATE_LIMITED vocabulary. Frozen OpenAPI429 remains unchanged, but no in-scope producer exists: no limiter, artificial provider result or fabricated429 test is introduced. Existing shared overloads remain untouched.
- Grammar422 precedes provider resolution; missing provider503 precedes owner policy validation. With a usable provider, owner matrix422 precedes transaction/current authority/resource checks. Stored malformed matrices are503, not client422.
- `Instant.toString()` consumes the PostgreSQL microsecond read-back value; zero/three/six fractional digits are intentional. The475-byte constant applies only to the pinned six-digit golden vector, never all live bodies. Container HTTP assertions derive expected time independently from durable JDBC values and compare literal ordered bytes/Content-Length/replay stability without production mapper or clock seeding.
- The existing `operations-catalog-sale-command` package/named interface is the same Operations catalog command family. Adding this port neither exports catalog internals nor changes package-info, allowed dependency sets or discovered edges.
- Positive versionNo is the confirmed natural-key input, not a new MAX/monotonic-admission branch. docs14's descriptive monotonic wording is not an additional frozen-schema/DB check. Live non-owner create must prove the creator FK insert without employee SELECT; any ACL failure stops for Controller.
- The42-tuple catalog-owned oracle is14 table SELECT/INSERT +23 column UPDATE +4 granted trigger-function EXECUTE +1 schema USAGE. Cross-schema and audit grants are separate capabilities, not extra members of42; provisioning's independent410-tuple oracle stays intact.
- All18 pre-existing CI start-total subtraction unions and all18 source substitutions are explicitly enumerated in §8, with additions/baseline union and the new PA39-start mirrors. No old historical count/map/name is rewritten.

Blocking open questions: **none**. Unexecuted runtime/environment obligations retain their existing owners and are future acceptance blockers, not unspecified behavior.

## 4. Corrected Byte Vectors

The empty-array P vector's exact UTF-8 bytes, without trailing LF, are:

```json
{"audience":"PERSONAL","benefitLines":[],"billingCycle":"ONCE","channels":[],"currency":"CNY","grantIntervalMonths":0,"installments":1,"offerType":"ONE_TIME","priceCents":0,"productId":"00000000-0000-0000-0000-000000000001","versionNo":1}
```

| Vector | Bytes | SHA256 |
| --- | --- | --- |
| Sorted empty-array P | 238 | `eecbe2e59df56e661efa42fb92262cc6a8d507646ecf964b7aa1820f6ed7a35a` |
| Same P plus AI_SECONDS (quantity1, validityDays1, grantIntervalMonths0), channels MINIAPP/WEB; benefit keys sorted | 339 | `88a88dbd9303d10a4e58fb39778feccc19d0cf5bcf2d176bd7c725ec4b8cdec0` |
| Ordered response for empty P; version UUID ending0002, DRAFT/revision1, null publishedAt, createdAt `2026-11-20T00:00:00.123456Z`, corrected hash | 475 | `7f759e65fa0db683941cf207f7d3aee6a15a3b9a7a70cb9459bf799f0a3868e9` |
| Same response with createdAt `2026-11-20T00:00:00Z` | 468 | `f749c92277d39f199d689fa931477c17f410c283858d247603fcbebbed7370b0` |
| Same response with createdAt `2026-11-20T00:00:00.123Z` | 472 | `f432f913c3ef690f0f7ddbe431fb9d43c0d90b902678be89c7f2cf1d783d6f6f` |
| Same response with createdAt `2026-11-20T00:00:00.123400Z` (not .1234Z) | 475 | `c39ceb166d38ca7cd5da855ea25dd26d77d91089faa2db5df8e53866e47f14f6` |

Length238/475 did not change just because keys were sorted; their digest did. The former238-byte ordered-key digest is no longer a valid stored hash for recognized P. P uses exact integer decimal across the frozen long domain without lossy double conversion or invented safe-integer cap; response order is intentionally separate from canonical hash-key order.

## 5. Inventory And Exact Proposed Identity Changes

| Inventory | Current source | Planned final implementation |
| --- | --- | --- |
| Behavioral slices / routes | No PA39 implementation / two frozen commands | One S1 / same two commands |
| Production Java | 290 | 298 (+8) |
| Test Java including fixtures | 180 | 190 (+10) |
| Surefire classes / methods | 97 / 934 | 104 / 1018 (+7 / +84) |
| Failsafe classes / methods | 38 / 630 | 40 / 666 (+2 / +36) |
| Executable classes / methods | 135 / 1564 | 144 / 1684 (+9 / +120) |
| Annotation-bearing lines | 1565 | 1685 (same one historical comment hit) |
| Failsafe explicit POM includes | 38 | 40, set-exact |
| Resources / migrations | 21 / 18 | 21 / 18 (V001–V018) |
| Governed manifest | 310 | 318 (298+21−1) |
| Modules / discovered edges / catalog allowed dependencies | 14 / 13 / 5 | 14 / 13 / 5 |
| Future implementation paths | None changed at this gate | 19 new +8 modified =27 |
| Existing modified tests / identities changed | None actually edited | Two files / zero shipped identities changed |
| New unit / live family sizes | None yet | `[12,14,12,14,8,16,8]` / `[24,12]` |

The27-path inventory is eight new/three modified production Java, ten new/two modified test Java, modified POM/CI/README, and one new implementation report. No new path or method is required by fix1. The cumulative plan and this fix artifact are not implementation allowlist additions. The final observable matrix has29 rows, including explicit legacy/integrity/lock/contention outcomes.

Only these ten **proposed** identities change; all1,564 shipped identities stay byte-unchanged:

| Proposed class | Pre-fix name → final name |
| --- | --- |
| CatalogDraftRequestBinderTests | `enforcesEveryOfferAudiencePriceBillingMatrixCell` → `passesGrammarValidMatrixCombinationsToOwnerWithoutPolicy` |
| CatalogDraftRequestBinderTests | `canonicalizesUniqueBenefitLinesAndChannels` → `acceptsUniqueBenefitLinesAndChannelsInAnyOrder` |
| CatalogDraftSemanticsTests | `projectionHasExactMemberOrderTypesAndNulls` → `projectionHasRecursivelySortedKeysExactTypesAndNulls` |
| CatalogDraftSemanticsTests | `rejectsMalformedOrInconsistentPersistedFacts` → `validatesCommercialMatrixAndClassifiesLegacyAndCorruptStoredFacts` |
| DefaultOperationsCatalogDraftCommandTests | `closedPortAndFailuresExposeNoPersistenceOrCredentialTypes` → `portCraftedInvalidMatrixReturns422BeforeTransactionOrAuthority` |
| OperationsCatalogDraftWebTests | `unexpectedFailuresAndDeclaredRateLimitUseFrozenProblems` → `unexpectedFailuresUseSanitizedInternalProblemWithoutInventedLimiter` |
| OperationsCatalogDraftLifecycleIT | `concurrentIdenticalCreatesYieldOne201AndOne200` → `concurrentIdenticalCreatesReplayOrTimeOutWithoutDuplicateFacts` |
| OperationsCatalogDraftLifecycleIT | `concurrentChangedCreatesYieldOne201AndOne409` → `concurrentChangedCreatesConflictWithoutDuplicateFacts` |
| OperationsCatalogDraftLifecycleIT | `concurrentEqualReplacesYieldOneApplicationAndOneReplay` → `concurrentEqualReplacesReplayOrTimeOutAfterOneApplication` |
| OperationsCatalogDraftLifecycleIT | `concurrentDifferentReplacesYieldOneApplicationAndOneStaleView` → `concurrentDifferentReplacesReturnStaleOrLockConflictAfterOneApplication` |

Canonical sorted FQCN#method manifests, LF plus terminal LF: current1564 SHA256 `a6b67db97e2aa7b4f765de7e5d1320e9226302da4a3e63c22c50d93e761dff7c`; planned final1684 SHA256 `6041250383865a75ff49037c1f736bb87db552561206b9990aedaf8604a51770`. §7 is the complete final proposed identity list; both future literal CI maps and source/XML must use it. Current governed manifest310 independently rehashes to `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045`; final production digest/physical LOC must be measured after implementation, never fabricated at a plan gate.

Future selectors remain7/84 new unit,3/25 architecture,6/52 frozen regression,13/138 PA37/PA38 unit,2/36 draft live and4/74 PA37/PA38 live. Two additional focused selectors each run one existing EvidenceRole guard method (1/1 each) without adding identities. Repeated race/fault selector is exactly2classes/8methods in three separate processes. Full clean XML inventory remains104/1018+40/666=144/1684 with zero skips/errors/failures; runtime oracle has not been executed here.

## 6. Validation Actually Performed And Custody

- Verified the existing Windows .git pointer mapping, back-pointer and commondir, then used only read-only `git --no-optional-locks` commands with verified GIT_DIR/GIT_WORK_TREE overrides. No .git edit or index write.
- Captured raw SHA256 for all2,354 tracked input files before editing. After edits, the only changed pre-existing raw file is the allowed cumulative plan. The only new public artifact is this fix file; pre-existing private helpers remain untouched. Control and both reviews retain their entry bytes.
- Recomputed all23 frozen-input raw hashes in the plan (OpenAPI, docs12/14/15/17 and V001–V018): all match. Source/test/POM/CI/README/provisioning bytes are unchanged. Frozen contracts and Control are not merely semantically unchanged: they are byte-unchanged.
- Reconstructed shipped CI literal maps with their additive merges, without executing report parsing or the CI/XML oracle; compared every FQCN/method set to source: exact97/934 and38/630, zero differences. Parsed POM: exactly38 includes, set-equal to current container classes. Recomputed310-entry governed manifest/digest and1565 annotation lines. Parsed the revised plan's nine proposed executable families:84+36=120 distinct additions and the final identity digest above.
- Independently recomputed the sorted-key canonical vectors and ordered response timestamp variants using compact JSON plus SHA256; all byte lengths/digests agree with the revised plan. Exact integer domains, sorted nested benefit keys and semantic array order are specified, not left to mapper defaults.
- Located the actual standalone README marker, not its backticked mention in the opening paragraph. Raw bytes strictly after that actual marker:110,835 bytes, SHA256 `a1142fc745ca60ed058b92c951ef1f413ff6deb678546eecb045485f910678d8`. Exactly four current backticked Java consumer paths remain. Future implementation must retain those identities and that raw suffix.
- Ran read-only Git whitespace/diff/allowed-path checks; no unrelated tracked change. Git's CRLF materialization warnings do not authorize normalization or changing any frozen byte. Final artifact line/byte counts and raw SHA256 are reported at completion, outside this self-referential file.

No Maven test/build, PostgreSQL/Testcontainers, Docker, full CI/XML, JAR smoke, frontend/contract generation or dependency-install command ran. Existing Platform/Controller environment constraints and live privilege/concurrency/audit acceptance obligations remain recorded in cumulative-plan §11; none is waived by static validation.

Stop here. Exact next gate: **fresh peer-isolated A/B plan re-review**. No implementation, approval or later gate is entered.
