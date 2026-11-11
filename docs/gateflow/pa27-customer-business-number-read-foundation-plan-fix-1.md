# PA27 customer business-number read foundation: plan-fix-1

- Task: `pa27-plan-fix1-gpt-20261111-01`.
- Work unit: `PA27-customer-business-number-read-foundation`.
- Date: 2026-11-11, Asia/Shanghai.
- Snapshot: `/mnt/d/codex-temp/pa27-plan-fix1-20261111-01`, metadata-free.
- Authority: current PA27 Control adjudication, accepted findings `PA27-PLAN-REV-01..04` and accepted precisions; reviews `docs/reviews/plan-review-20261111-101806.md` and `docs/reviews/plan-review-20261111-102854.md`, both read in full.
- Input cumulative plan: 62,954 bytes / 530 LF lines; SHA256 `190e24b2ae984e3202abf01f4dece3f0356b9a118664eb172237f54f2c4108d8`.
- Control at fix intake: SHA256 `ee1657f1e286d076ed3f6ce754120d6562d0551dd75424c8623287d310e7da7b`; original planning hash remains historical provenance, not a current baseline.
- Outcome: accepted findings addressed in the cumulative plan only; not implementation, approval, observed test success or entry into re-review. Gate: **stop before re-review**.

## 1. Finding dispositions

| Accepted finding | Disposition in cumulative plan | Changed sections |
| --- | --- | --- |
| `PA27-PLAN-REV-01` High | Closed in plan text: exact `M backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java` allowlist entry and latest-chain list; the one unpinned nested-fixture `start()` migration assertion changes 14 -> 15, preserving full-chain validation, eight methods and every behavior assertion | §6.3, §7.2, §9, §10.2 full acceptance remains required |
| `PA27-PLAN-REV-02` Medium | Closed in plan text: exact bounded docs/14 and docs/17 M entries; preserve/relabel PA26 V014 section as historical and prepend PA27 V015 current-boundary supersession. Customer 26, evidence 16, global 261, delta 14, other roles 219, V001-V014 unchanged; current prerequisites and inventory agree with README/runbook, not contradictory current 12/247 claims | §7 allowlist totals, §7.3 |
| `PA27-PLAN-REV-03` Medium | Closed in plan text: exact `M backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java` entry. Specify four new bean names, types, ordered parameter qualifiers, Properties exception and all-method `@Bean`/`@Lazy`/no-`@Primary` contract; preserve original six methods/entries and reflection equality, ten declared configuration methods total | §7.1, §7.2, §9 |
| `PA27-PLAN-REV-04` Low | Closed in plan text: existing `customerDependenciesAndReadOnlySqlStayInsideBoundary` count 26 -> 32; retain full banned-string scan and narrow sharing exceptions. Add exact route/DTO/controller qualifier assertions to existing first boundary method, profile/servlet assertions to existing third method, no added test method | §8.4, §9 |

These are author dispositions against the binding fix scope, not independent verification/Controller acceptance of implementation.

## 2. Accepted precisions

- PA25 relocation (§6.3): explicitly remove the original V014-time apply/check calls near lines 127 and 323. In each credential variant finish historical assertions/replays first, then separately migrate/validate untargeted Flyway through V015 and assert fifteen applied entries. Only then execute one pair in the upgrade method and two pairs in the replay method. No historical oracle/replay follows that upgrade; no current PA25 invocation remains at V014.
- CI order (§9): construct the six-class `pa27_additions` after PA26 additions; union four unit classes/32 methods into `unit_expected` and two IT classes/26 methods into `container_expected` before existing exact source discovery/equality. Preserve historical subtraction/checkpoint logic, then exact final XML identity/zero-skip/failure/error checks; update early Surefire aggregate too.
- Public errors (§3, §8.1): preserve unconditional existing noncanonical/non-GET route 404 guard before auth; admitted GET runs authentication 401/503, workspace 503, limit 422, structural cursor 400, mandatory key 503, authenticated MAC/scope cursor 400, current lifecycle 403. Technical failures are sanitized 503 and projection/logic failures sanitized 500 at their reached stage; earlier rejection prevents downstream authority checks. Missing key wins over inactive lifecycle even for first/empty pages; structural bad cursor wins over missing key. No authority leaks.
- Role failures (§4.1, §8.1): assumption failure, absent role assertion row or effective-role mismatch is sanitized 503 `SERVICE_UNAVAILABLE`, not 500; no SQL/cause/message escapes and no protected reader runs.
- Membership fixture (§8.5): two accounts A/B and five organizations. A has one unique ACTIVE membership in each of four selected organizations, one per permitted role; B is a MEMBER in all five, including the foreign fifth organization. Each selected role organization has its own literal 48-combination visibility fixture. Preserve personal/organization and cross-selected-workspace exclusions; reset or update a unique membership for lifecycle cases rather than inserting duplicate live memberships.
- Control provenance (§10.1): retain original planning hash and separately record fix-intake hash; implementation captures its own actual intake Control hash.

## 3. Recomputed prospective arithmetic

Measured against the actual unmodified source/CI/POM tree and cumulative manifest, not guessed from review summaries:

| Inventory | Baseline | Addition | Prospective S1 target |
| --- | --- | --- | --- |
| Exact allowlist paths | 44 = 21 M + 23 N | 4 M (two existing tests, two existing docs) | 48 = 25 M + 23 N |
| Production allowlist | 18 = 4 M + 14 N | no fix expansion | 18 = 4 M + 14 N |
| Database/tests/CI allowlist | 23 = 15 M + 8 N | 2 M | 25 = 17 M + 8 N |
| Documentation allowlist | 3 = 2 M + 1 N | 2 M | 5 = 4 M + 1 N |
| Surefire source = CI classes/methods | 36 / 309 | 4 / 32 (12+8+8+4) | 40 / 341; XML must match |
| Failsafe source = CI classes/methods | 15 / 246 | 2 / 26 (18+8) | 17 / 272; XML must match |
| Total test classes/methods | 51 / 555 | 6 / 58 | 57 / 613; XML must match |
| Annotation-line scan | 556, one historical comment hit | 58 | 614 |
| Failsafe include names | 15, exactly baseline IT set | 2 | 17, exactly prospective IT set |
| Customer internal production regular files | 26 = 12 web + 5 application + 4 persistence + 5 security | 6 = 4 web + 1 application + 1 persistence | 32 = 16 web + 6 application + 5 persistence + 5 security |

The 23 N paths remain fourteen production Java files, one migration, six test classes, one test fixture and one implementation report. The newly allowlisted existing tests add no test class or method: `CustomerWebConfigurationTests` stays six methods and `EvidenceMediaReadIT` stays eight. `CustomerApiBoundaryTests` stays three. CI, POM and XML method targets therefore remain unchanged from the reviewed plan. No new XML is generated by this fix; expected XML gains six reports/58 cases only after S1 execution. Both planning artifacts are excluded from the 48-path S1 allowlist.

## 4. Preserved decisions and scope

- User-confirmed visibility matrix, all four ACTIVE roles, every binding status, all activation statuses/missing-row INACTIVE, masked seven-field projection and three-field page remain binding.
- One composed S1, two owned named query seams, endpoint-only 93-byte/124-character HMAC cursor, one read-only REPEATABLE_READ snapshot and same qualified customer connection remain unchanged. Exact wiring is now specified, not redesigned.
- V015 remains privilege-only, exactly fourteen owner-issued/no-grant-option additions: customer 12 -> 26, global 247 -> 261, evidence 16 and other-role 219 unchanged. Six logins/seven roles/four edges unchanged. No schema/index/business write, role or credential expansion.
- OpenAPI and V001-V014 remain raw-byte immutable. Preserve existing Java logical-LF/Flyway checksum policy as well as independent raw manifests; no semantic change to V014 dual raw/logical hash policy.
- No implementation/source/test/SQL/CI/Control/OpenAPI edit, Git operation, external comment, PR, approval, ready action, reviewer request or re-review entry is authorized or performed.

## 5. Validation performed and limits

- Read the cumulative plan, current Control PA27 section and both reviews; inspected the relevant configuration/reflection/boundary/media/history tests, V002 membership uniqueness, existing route/context behavior, V014/PA25 ACL/history literals, docs/14/docs/17, README/runbook and CI/POM facts.
- Recomputed baseline method sets with CI's `@Test\s+(?:public\s+)?void\s+(\w+)\s*\(` regex and compared them exactly with statically evaluated literal CI inventory construction (no CI job or XML checker execution). Measured the annotation-line scan; parsed the namespaced POM's Failsafe plugin specifically and compared its fifteen include names to the IT class set.
- Parsed the six exact manifest blocks, checked unique class/method names and disjointness from baseline, and recomputed unit/container/total targets. Parsed all 48 unique allowlist paths, checked every M exists/every N is absent, and checked section/type totals. Counted actual internal production files and the six prospective additions.
- Recomputed all fifteen raw frozen hashes in §10.1 (OpenAPI plus V001-V014); all match. Captured a 1,738-file pre-edit whole-snapshot SHA256 manifest outside the repository; final comparison permits only the cumulative plan change and this one new artifact.
- Checked strict UTF-8 without BOM, no CR, exactly one final LF and no trailing whitespace for both outputs. Final metadata is reported externally rather than embedded self-referentially.
- No Maven, Docker/PostgreSQL, API/npm validation, actual XML, full CI, deployment or live production acceptance ran or is claimed. Initial inventory extraction attempts encountered existing missing-XML guards and an incomplete literal-evaluation dependency; corrected read-only static evaluation succeeded. An initial unscoped XML include count included Surefire entries; plugin-specific Failsafe parsing confirmed fifteen. No result is presented as a passing test run.

## 6. Residual risks and stop

- Backend/Architecture own the accepted bounded cross-module DSL runtime wiring and deferred post-PA27 architectural inversion review; S1 must prove real wiring, same connection and snapshot.
- Security/Operations own mandatory cursor-key deployment availability, custody, replica consistency and rotation. Missing/malformed key intentionally makes even first/empty requests unavailable; provisioning is outside S1, not silently defaulted.
- Backend/Database own large-set sort/index performance pending representative EXPLAIN; no index/schema expansion without Controller. Security/Database retain column-grants-not-RLS risk and application tenant-filter proof.
- CI/Database own actual S1 source=CI=XML evidence, finalized V015 Flyway checksum and complete PostgreSQL 16 validation. No computed target is an observed execution result.
- Existing PA25 Q17/Q23, migration/comment custody, credential/TLS/pg_hba, pool drain and old-session invalidation remain with Platform/Database/Security/Operations; production **NOT EXECUTED / NOT CLAIMED**. Metadata-free Git/remote provenance remains Controller-owned.

Stop after these two artifacts and their metadata. No stage, commit, push, PR, merge, approval, ready action, reviewer request, external comment or entry into re-review follows.
