# PA11 customer authentication and API foundation — plan fix 2

## 1. Authority, inputs and write boundary

- Task label: `pa11-plan-fix2-gpt-20261101-01`; date: 2026-11-01; work unit: `PA11-customer-auth-api-foundation`; gate: planning only, not implementation or acceptance.
- Binding authority: `docs/gateflow/project-execution-control.md:33` and `docs/gateflow/project-execution-control.md:34`, accepted re-review dispositions and plan-fix-2 entry. Intake SHA256: `c6eeeaa3a1728b400f11979689d3f05750e23d011fc4a9ba600f026a92991ad6`. This is fix-time provenance, not a prediction of the acceptance-time control hash.
- Input fixed plan SHA256: `4f0ae23c863c314dbac4e98191b5266d56132fbed8cbe45e918df0100679a7db`. Unmodified fix-1: `docs/gateflow/pa11-customer-auth-api-foundation-plan-fix-1.md`, SHA256 `ffee8ff46f6b97bfa0638ce854179c366e21105c992ae93268fa33e922c89725`.
- Read all four review artifacts at their actual local paths below. Historical controller/fix-1 references under docs/reviews are not instructions to create/move files or alter review modes. Any filing/mode reconciliation belongs to the controller, not this unit.

| Input review | SHA256 |
| --- | --- |
| `docs/reviews/plan-review-20261101-213334.md` | `5b3abfbd967cbab23fafef95fcd48cafbbf683b666f113937f6c7e8320aa73bd` |
| `docs/reviews/plan-review-20261101-213337.md` | `843da4d8f3033c83f41d398f7d807009ee8e3560172ce25f0fec31f734531de5` |
| `docs/reviews/plan-review-20261101-222252.md` | `4d945384f846bd8dbd27a568b6c53452d2fed80fe9a4494d68a176bcbb52242b` |
| `docs/reviews/plan-review-20261101-222256.md` | `d420b87ffa28af223d7b80b35c130abf57aab7d1c6cdddad2007d5bf13430529` |

Only `docs/gateflow/pa11-customer-auth-api-foundation-plan.md` is modified and this fix artifact is added. No implementation, code/test/POM/CI/config/canonical contract, controller/history/review change, staging, commit, push, PR mutation or external comment is performed. One opt-in customer GET, no production verifier adapter, no login/issuer/store/Redis/provider policy, no new route/capability/mutation/schema/ACL/module edge remain binding. Compute final artifact hashes externally, not through self-referential digest fields.

## 2. Complete accepted-finding disposition map

Every location below refers to the fixed plan at `docs/gateflow/pa11-customer-auth-api-foundation-plan.md`; line numbers identify exact paragraphs and named test methods remain stable anchors.

| Accepted finding | Classification and exact plan locations | Resolution / future evidence owner |
| --- | --- | --- |
| R-B2-01 / N-05 — firewall ordering and Jackson identity | Closed in plan: §8 CI/firewall, :304, :306, :307; §4 entry gate :115 | Executable full-coordinate-tuple plus exact-JAR equality first; approved members bypass the legacy filename regex, which applies only to nonmembers; every nonmember/missing/duplicate fails regardless of regex. Jackson 2 core/databind denied by resolved group/artifact, not filename. Eight approved prefix collisions, annotations and inherited support pass without admitting legacy web, websocket/Jasper, WebFlux/Netty, alternate engines, Security/JPA/Flyway/starter/test tooling or actual reactive DB drivers. Backend/CI owns inline positive/negative fixture and entry/JAR evidence, with no additional JUnit method/file. |
| R-B2-02 — README current-state scope | Closed in plan: §3 :105, :109; §11 :358 | Explicitly edit current dependency/transitive table, firewall description, 49/84/133 inventory and CI echo, in addition to capabilities/profiles/commands. Add PA11 current preface, label former PA10 current prose historical; preserve historical PA0–PA10 results/counts. Backend/Documentation owns this bounded README edit during implementation, not now. |
| R-B2-03 / N-03 — production registration and writer | Closed in plan: §4 :131; §8 :258, :260; §7 :205 | Production scans web/servlet-gated RestControllers and configuration. Configuration defines writer and one REQUEST-only FilterRegistrationBean holding a newly constructed, non-component filter; it does not import/declare controller beans. No-scan tests import each controller once plus that configuration. Five test-provided collaborators exclude the separately configuration-provided writer/registration. Exactly-one bean/mapping assertions and BasicErrorController absence are mandatory in both contexts. Backend owns implementation proof. |
| N-06 — ERROR dispatch and dual pools | Closed in plan: §8 :262, :270, :292; §7 :205 | Existing test repository uses an in-memory one-shot flag, RequestContextHolder/ServletRequestAttributes response.sendError then fixed exception; real container ERROR, no route/filter/bypass, no prewritten body, production committed/error guards do not append. Dual-pool IT supplies OneCallApplication plus only DualPoolTestBeans, web,persistence, ephemeral port and exact customer/sharing property prefixes through test initializer; real production graphs, distinct NOINHERIT logins/roles, customer HTTP plus internal customerShareQueryService and finally cleanup. Backend owns test-only mechanics within existing seven-file test scope. |
| N-01 — control identity and actual review paths | Closed in plan: §2 :52, :67; §10 :323; §11 :358, :373 | 0022f000 is plan-fix-1-time provenance, not current. Fix-2 intake hash is also only provenance. Controller records then-current consumed control hash, accepted plan/fix hashes and actual docs/gateflow review paths at acceptance; do not pin acceptance to either historical hash or modify history here. This also classifies re-review B observation O-1 and O-2's path issue. |
| R-B2-04 / N-02 — source anchors | Closed in plan: §2 :32, :36 | PERSONAL/exact cross-schema grants/advisory rule points to docs/14:3066; issuer-side lookup remains :3064. V002 USAGE is :241, account grants :243–245, cross-schema grants from :303. Fix-1's old :3064 personal-rule claim remains historical and is superseded, not edited. |
| N-08 — accepted stack drift | Closed in plan: §2 :46; §9 :313; §11 :369 | Spring Security/filter deviation explicitly repeats accepted Boot 3.x→4.1.2 drift and points to acceptance-time control-hash recording. Backend/Architecture/Security owns both in authentication-integration ADR then docs/12 reconciliation, without canonical edit now. |
| N-04 — inactive/missing-account precedence | Closed in plan: §5 :166; §8 application #3/#4 :242, :243; §7 :211 | Empty or valid DISABLED account is 401 before other projection checks; normal ACTIVE SQL yields empty for missing/DISABLED. Null/unknown status or invalid ACTIVE projection is 500. Inactive membership/org only excludes that workspace, never a whole-account 403. No count change. |
| R-B2-05 — null displayName and Channel | Closed in plan: §3 :77; §5 :151, :154, :164; §8 application #4 :243 | Null displayName is INTERNAL because V002 NOT NULL makes omission unreachable; successful accounts always emit displayName. Remove AccountSummary NON_NULL branch; only WorkspaceSummary/Problem need targeted omission. One owner declaration for Channel in TrustedCustomerCaller, reused rather than redeclared among CustomerContext projection enums. Backend owns assertion/serialization evidence in existing methods. |
| R-B2-06 — human-readable Problem message and omitted detail | Closed decision in plan, convention work deferred with owner: §7 :199; §11 :371 | Explicit docs/17:557 reading: stable code, human-readable fixed title summary and fresh traceId meet the current message rule while detail is omitted. Backend/API/Security owns future A1/docs/15 shared convention, including whether localized text belongs in detail. No localization or canonical-contract change introduced. |
| N-07 — residual overlap | Deferred-with-owner under explicit invariant allocation: §8 :249, :267, :270, :294; §11 :372 | Backend QA -> implementation-time test-maintenance review. Empty-env baseline differs from nonblank-env nonactivation; ordinary HTTP failure/header/log evidence differs from negotiation/real ERROR replacement. §8 names surviving port/HTTP/SQL/DI evidence pairs and each of the 38 methods' invariant. If bodies cannot sustain distinctness, consolidate and recompute all exact inventories/README/CI before review, never pad or weaken SQL/ACL/lifecycle/no-write/resource evidence. Present counts remain exact. |

All six R-B2 findings and all eight N findings are classified above. No accepted item is rejected or left as an implementation-time design choice: mechanisms and precedence are decided now; implementation evidence is future work. N-07 is the sole accepted test-maintenance deferral, allowed only by its distinct-invariant condition. Re-review B O-3 is confirmed scope preservation, not an unresolved request. O-2 historical filing/mode cleanup, if desired, is controller-owned outside this allowed-write set. Original A-01–A-07 and B-F1–B-F15 dispositions remain in unchanged fix-1, with the explicit anchor/hash/null-policy refinements above superseding its historical wording.

## 3. Whole-plan consistency audit

| Evidence plane | New methods | Distinct invariant allocation |
| --- | --- | --- |
| CustomerSessionBoundaryTests | 5 | Exact verifier call, rejection/result/provider semantics and object-secret lifetime, not repeated HTTP parser matrices. |
| CustomerContextServiceTests | 6 | Trusted caller, projection/order preservation, explicit account-status precedence, transaction ordering and sanitized classification. |
| CustomerWebConfigurationTests | 6 | Environment/profile gates, production lazy/no-adapter graph, credential-before-JDBC, exact qualifiers, no profile bypass and logging/defaults. |
| CustomerMeWebTests | 8 | Real response/parser/selectors/concurrency/route denial, ordinary failure propagation, distinct negotiation/container ERROR, and lawful writer-only reserved vocabulary. |
| CustomerApiBoundaryTests | 3 | Reflection/API, SQL/source/module boundary and production artifact/profile constraints. |
| CustomerMeIT | 10 | Real role/SQL/ACL, ACTIVE and membership predicates, unsigned UUID order, lifecycle freshness, SQL binds, no writes/pool recovery, dual pools and real failure/resource redaction. |
| CustomerMeFixture | 0 | One shared class-scoped PostgreSQL 16 container, migrations once, isolated fixtures/fault restoration and bounded cleanup; no JUnit method. |

- Surefire: 5+6+6+8+3 = 28 additions; 21+28 = **49**, 6+5 = **11 classes**. Failsafe: 64+10+10 = **84**, **3 classes**. Combined: 95+38 = **133**. All 38 method names are unchanged and unique; existing methods are extended, inline CI probes/process smokes add no JUnit count.
- Keep exact source = literal expected set = XML, zero skips/errors/failures, both early/final CI aggregates, and `timeout-minutes: 30`. Runtime fit is unmeasured: Backend/CI must record suite/startup/process/total timings and return measured overruns to controller, not extend timeout or remove evidence silently.
- Wire schema stays closed: exactly account/workspaces/channel, ACTIVE account with nonnull displayName, PERSONAL omission only for organization fields, frozen roles/enums, no selectors or extra identity facts. One Channel type, no global Jackson inclusion.
- Precedence stays route/method denial 404 before authentication; parser/session denial 401 before repository; zero/multiple/unavailable/malformed verifier 503 before lazy persistence; empty/DISABLED account 401 after query; unknown status/ACTIVE invariant/negotiation failure 500; infrastructure 503. Real internal ERROR status mapping remains distinct from reserved writer-only 403/422/429. One verifier-origin 401 is retained end to end in web #4 with zero repository interaction.
- Default remains none/database-free; web servlet activation is explicit; no production adapter exists under any profile. Scanned production and imported no-DB contexts each register one writer/filter/controller pair, never both registration mechanisms. Dual-pool IT uses real production persistence, not the no-DB slice or a shares route.
- One prepared projection SELECT plus fixed transaction/role control statements, exact pre-existing grants and no writes/locks are retained. §6 :193 explicitly exempts fixed SET LOCAL ROLE/current_user/transaction control from SELECT-value parameterization; role identifiers are never client-controlled. No SQL/ACL/lifecycle/resource test is removed or diluted.

## 4. Deferred ownership and destinations

The plan §11 ledger remains binding; no deferred production input is fabricated to close a documentation finding.

| Deferred work | Owner / destination / containment |
| --- | --- |
| N-07 body-level test-maintenance overlap | Backend QA -> implementation-time test-maintenance and exact source/XML/CI/README review under §8's distinct-invariant condition; preserve database/resource evidence. |
| Acceptance-time control hash, actual review paths and any historical filing/mode reconciliation | Controller -> accepted-plan checkpoint; consume then-current control bytes and record provenance, do not rewrite inputs here. |
| Shared Problem/header and localized detail convention | Backend/API/Security -> A1/docs/15 convention; title supplies current human-readable summary, optional detail stays omitted. |
| Spring Security exception and Boot drift reconciliation | Backend/Architecture/Security -> authentication-integration ADR then docs/12 stack reconciliation; PA11-only exception, no canonical edit now. |
| Issuer/store/provider/revocation/expiry/TTL/Q1/Q19/channel provenance; denial audit acknowledgement | Identity/Backend/Security -> future authentication/security input and audit integration; missing adapter stays 503, no write/bypass; A5-IDENT-PERMISSION_DENIED-01 not absorbed. |
| Production DB logins/credentials/capacity/timeouts | Platform/Database/Security -> Q17/Q23 deployment/capacity work; no values/provisioning added, Hikari vendor defaults disclosed. |
| Large-list performance and post-snapshot races | Backend/Database -> measured EXPLAIN/index/SLO work; Backend/Security -> protected-operation authorization; list remains complete/advisory with no invented cap/lock/lease. |
| Connector/edge/TLS/header limits/external logging | Platform/Security/Observability -> deployment/edge integration; application sanitization promise remains reachable pre-commit only. |
| Verifier/account latency distinguishability | Security/Identity/Backend -> authentication integration; identical denial shape, not timing; no dummy reads/delays. |
| Exact dependency graph, firewall probes, support-JAR/image provenance and CI elapsed-time evidence | Backend/CI, with Platform/Database/Security for image/provenance -> implementation entry/CI/completion evidence; frozen managed versions, reject actual reactive drivers, retain 30-minute ceiling and controller escalation on overrun. |
| Session channel versus order channel | Backend/API/Identity -> catalog-order work unit; no order policy in PA11. |
| A5 login-provider/e2e evidence versus PA11 integration evidence | Backend/QA/Identity/Security -> implementation completion mapping to A1:OP:getCustomerMe, A0:INV:INV-01..03 and A3:TS:TS1; provider/e2e record remains missing, denial audit not completed. |
| PA10 promotion/wire cursor/policy/mutation | Backend/API/Sharing/Security -> later sharing work unit; customer HTTP never exposes PA10. |

## 5. Planning validation and stop point

Document-only validation parses six named test sections, verifies 5/6/6/8/3/10 methods, compares all 38 names to intake, checks uniqueness and 49/84/133 arithmetic, verifies the seven frozen non-control input hashes plus fix-1/four review hashes, and audits whitespace and allowed writes. A before/after SHA256 manifest covers all pre-existing checkout files: all 1,103 files other than the permitted plan retain their input bytes. The new artifact is the sole added path; tracked diff and index remain empty because the plan/reviews arrived untracked. No existing file is staged or committed.

The historical 0022f000 controller identity is deliberately not tested as current; fix-2 intake control bytes are verified unchanged and acceptance must record then-current bytes separately. Checks cover plan policy/traceability, not executable product behavior. No Maven/product tests/build, PostgreSQL/container, runtime smoke, remote CI or actual firewall implementation probe is run or claimed. Stop for fresh independent A/B re-review and controller acceptance before implementation.

status: plan-fix-2-complete
plan: docs/gateflow/pa11-customer-auth-api-foundation-plan.md
fix: docs/gateflow/pa11-customer-auth-api-foundation-plan-fix-2.md
