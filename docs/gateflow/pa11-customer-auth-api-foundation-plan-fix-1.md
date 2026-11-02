# PA11 customer authentication and API foundation — plan fix 1

## 1. Authority and scope

- Task label: `pa11-plan-fix1-gpt-20261101-01`; date: 2026-11-01; work unit: `PA11-customer-auth-api-foundation`; gate: planning fix only.
- Binding authority: `docs/gateflow/project-execution-control.md`, accepted goal/non-goals and plan-fix adjudication at §Active resume checkpoint / PA11. Current control SHA256: `0022f000cecccfe292fb584cc7e7372d8672a5b69c647cf400a19502273a6d1c`.
- Original reviewed plan SHA256: `bf8073dd2189c4b568e0e068a3b9503bb9909d56a9fbe9006f48c29be1ecc22d`. Fixed plan: `docs/gateflow/pa11-customer-auth-api-foundation-plan.md`. Compute final artifact hashes externally; no self-referential digest.
- Review A: `docs/reviews/plan-review-20261101-213334.md`, SHA256 `5b3abfbd967cbab23fafef95fcd48cafbbf683b666f113937f6c7e8320aa73bd`.
- Review B: `docs/reviews/plan-review-20261101-213337.md`, SHA256 `843da4d8f3033c83f41d398f7d807009ee8e3560172ce25f0fec31f734531de5`.
- Only the plan and this new fix artifact are written. One behavioral S1 remains: opt-in web runtime, trusted provider-neutral opaque-session verifier boundary and only GET /api/v1/customer/me. Default non-web/database-free runtime, frozen contracts, no provider/issuer/Redis/login, no new API, no mutation/schema/ACL/module change remain binding. No implementation, staging, commit, push, PR, external comment or controller/historical artifact change is authorized or performed.

## 2. Review A dispositions

All section references below identify sections of the fixed plan; named methods uniquely identify the exact §8 change.

| Finding | Disposition and exact plan change |
| --- | --- |
| A-01 — reserved errors lack lawful HTTP producers | Accepted, §§7–8. FORBIDDEN/VALIDATION_FAILED/RATE_LIMITED are reserved 403/422/429 writer vocabulary without HTTP producers. `reservedProblemVocabularyIsWriterOnly` directly tests the writer with a mock servlet response; no fake controller/filter/validator/limiter. `reachableProblemsAndResponseHeadersAreClosedAndSanitized` exercises only reachable HTTP failures. |
| A-02 — web test collides with lazy persistence graph | Accepted, §8 / CustomerMeWebTests exact no-PostgreSQL slice. Explicit WebTestApplication, no OneCallApplication/component scan; import only three production web configurations/controllers and WebTestBeans. Neither production persistence configuration is imported/scanned; assert their bean and pool/manager absence. Five eager, unique beans are named: customerSessionVerifier, customerContextRepository, customerQueryTransactionTemplate, customerQueryDsl, customerContextService. Real service uses test repository plus mocked transaction/DSL collaborators, no DB or mock service; exact qualifiers, no overriding/@Primary/global lazy initialization. Separate production-scanned configuration test and real PostgreSQL IT prove the excluded graphs actually work. |
| A-03 — PERSONAL synthesis underspecified | Accepted, §§5–6, §8 web #1 and IT #2–4. Repository synthesizes one PERSONAL from the ACTIVE account once, then appends only DB-ordered usable ORGANIZATION rows. NULL organization is a no-organization LEFT JOIN sentinel, not a PERSONAL row. No rows means no successful context. Both no-org and multi-org responses assert exactly one PERSONAL. Removed NULLS FIRST from SQL. |
| A-04 — service order validator lacks DB-equivalent comparator | Accepted with the preserve-order option, §§5 and 8. Service never validates/re-sorts organization order; it defensively copies repository order, validating shape/uniqueness/PERSONAL position only. Application #2 and IT #4 cover both high- and low-long UUID sign boundaries; no UUID.compareTo rejection. Removed bad organization-order failure case. |
| A-05 — incomplete JAR allowlist | Accepted, §§4 and 8 / CI and dependency firewall. Freeze positive exact coordinates from baseline-plus-starter entry probe, encode literals in existing CI script, compare full resolved/runtime/BOOT-INF/lib sets one-to-one with versions/no duplicates; retain negative firewall, no prefix loosening. Explicitly account for Micrometer, Jakarta annotation, Jackson annotations and inherited r2dbc-spi/reactive-streams/checker-qual support. |
| A-06 — over-broad global Jackson inclusion | Accepted, §§3, 5 and 8. Remove global inclusion property; targeted NON_NULL on PA11 AccountSummary/WorkspaceSummary/Problem records only. Application #2 proves serialization outside the profile; web #1/#8 prove wire/writer omission. Future nullable endpoint contracts are not changed. |
| A-07 — duplicate test budget | Accepted only as consolidation per controller, §8 exact named sets and CI totals. New Surefire reduced from 34 to 28, IT retained at 10 for distinct SQL/ACL/lifecycle/resource evidence. See §4 below for removed overlap and recomputed totals; no assertion is retained merely to meet the previous count. |

## 3. Review B dispositions

| Finding | Disposition and exact plan change |
| --- | --- |
| B-F1 — exact Boot /error replacement | Accepted, §§3–4, 7–8. Web bean implements org.springframework.boot.webmvc.error.ErrorController in the same context; Boot CURRENT missing-bean condition backs off. Marker interface has no getErrorPath method. Unconstrained /error mapping handles ERROR only, REQUEST denied by filter. Configuration #2 and web #7 assert BasicErrorController bean/handler absence and no Boot JSON/HTML. |
| B-F2 — application versus connector errors | Accepted with controller transport split, §§5, 7–8 and 11. Explicit produces=application/json; incompatible Accept reaching MVC normalizes to 500 INTERNAL Problem written outside negotiation. Missing bearer remains 401 first. Internal dispatch 401/404/500/503 maps explicitly; all unsupported/missing/malformed dispatch statuses, including 400/405/406/415 and reserved 403/422/429, normalize to 500 INTERNAL. Web #7 exercises hostile Accept and real sendError ERROR dispatch without another route/filter. CI smoke rejects Boot map/HTML for reachable Problems. Connector malformed/oversized heads/targets/authority and header limits are Platform/Security edge residuals, not an application guarantee. |
| B-F3 — denial audit conflict | Accepted as explicit deferral, not a write, §§2, 6, 8 IT #8 and 11. SECURITY_AUTH_DENIED / SECURITY_AUDIENCE_MISMATCH acknowledgement via append_fact belongs to Identity/Backend/Security authentication/audit integration. PA11 writes no durable domain/session/audit/idempotency/outbox data and calls no write functions; transaction control is not a domain write. A5-IDENT-PERMISSION-DENIED-01 (canonical A5-IDENT-PERMISSION_DENIED-01) is not absorbed. No-write tests do not imply future denials need no audit. |
| B-F4 — Spring Security stack deviation | Accepted, §§2, 9 and 11. Bounded filter/verifier-port plus existing enforcer ban explicitly supersede docs/12 §2's general stack recommendation only for PA11. Backend/Architecture/Security owns authentication-integration ADR then docs/12 reconciliation; docs/12 unchanged. |
| B-F5 — PERSONAL/NULLS FIRST reasoning | Accepted, §§5–6 and §8 web #1 / IT #2–4. Exact once-only synthesis and sentinel skip; SQL orders organization UUID ASC without NULLS FIRST. |
| B-F6 — UUID comparator | Accepted via no service order validator, §§5 and 8 application #2/#4 and IT #4. Repository sequence preserved; sign-boundary unit/DB evidence retained, no signed UUID comparison. |
| B-F7 — firewall must not loosen prefixes | Accepted, §8 CI/firewall and §4 entry gate. Exact positive full-runtime enumeration plus negative firewall; known 18-coordinate addition recorded as reviewer result requiring entry verification, not a wildcard or assumed entire runtime set. Unapproved legacy web, websocket, Jasper, WebFlux, servlet API and Jackson 2 core/databind remain rejected. |
| B-F8 — omission tied to global property | Accepted, §§3, 5, 8. Targeted record annotations; global property removed, with no contract-wide defense-in-depth setting retained. |
| B-F9 — overclaimed indistinguishability | Accepted, §§7, 8 security #2 and 11. Identical status/code/title/body shape only, fresh trace IDs differ; no timing claim. Verifier-before-DB versus missing/disabled-account-after-DB latency is bounded Security/Identity/Backend authentication-integration residual; no dummy DB queries/delays. |
| B-F10 — new titles/headers portrayed as frozen | Accepted, §§7, 9 and 11. Seven titles, about:blank, challenge, cache/security headers and path/method denial are labelled PA11-owned implementation decisions. Backend/API/Security consolidates shared API Problem/header convention through A1/docs/15 reconciliation later. No canonical edit now. |
| B-F11 — citations/spacing/counts | Accepted, §§1–2, 6, 8, 10–11. Correct docs/14 personal-workspace anchor to 3064, PA10 method/role anchors to 28/33, add POM scope/ban anchors; heading anchors explicitly labelled. Refresh current adjudication hash while preserving original control provenance. Fix malformed numeric spacing and recompute inventories throughout. |
| B-F12 — CI timeout/container lifetime | Accepted, §8 fixture and CI. One class-scoped CustomerMeIT PostgreSQL container across all ten methods, migrations once, isolated reset/restore and deterministic teardown. Retain 30-minute CI ceiling; record per-suite/startup/process/full elapsed measurements during implementation. No runtime-fit claim or silent increase; measured failure returns to controller for authorization. |
| B-F13 — Hikari defaults implicit | Accepted, §§4, 8 configuration #6 and 11. Unspecified production sizing/timeouts/lifetimes explicitly use Hikari vendor defaults, no invented capacity/SLO; Q17/Q23 owns measured tuning. Test-only resource bounds remain evidence bounds. |
| B-F14 — unreachable six-code coverage | Accepted, §§7–8. Reserved 403/422/429 only at lawful writer boundary, no HTTP producer or fake security/validation/rate-limit mechanism. Six operation codes plus separate global NOT_FOUND remain distinct. |
| B-F15 — bearer lifetime/header stripping | Accepted, §§3, 5, 7–8 security #5. Transient parsing/verifier material is never logged, serialized or retained by PA11 in principal/attributes/cache/diagnostics. Servlet wrapper does not physically strip the underlying Authorization header; no JVM string-zeroization claim. |

### Technical precision note

Local read-only javap inspection confirmed Boot 4.1.2 ErrorController is an empty marker and ErrorMvcAutoConfiguration.basicErrorController has ConditionalOnMissingBean(ErrorController, search=CURRENT). Cached JAR SHA256 is recorded in plan §4. Review B's incidental getErrorPath/nested BasicErrorControllerConfiguration wording is not copied: neither is required by the inspected API. The accepted replacement finding is closed by the actual marker condition and runtime absence tests, not by inventing an obsolete method. The web retrieval attempt returned no usable source; this fix relies on repository artifacts, reviewer evidence and locally inspected primary bytecode, not an asserted online verification or fresh build.

## 4. Final evidence inventory and consolidation

| New class | Ordinary test methods | Distinct evidence |
| --- | ---: | --- |
| CustomerSessionBoundaryTests | 5 | Verifier contract, denial/result/provider classification, secret object/principal lifetime |
| CustomerContextServiceTests | 6 | Trusted caller, projection invariants/order preservation, transaction/role ordering, internal failure classification |
| CustomerWebConfigurationTests | 6 | Production scanning/profile/lazy graph, exact DI, credentials-before-JDBC, no bypass/log policy |
| CustomerMeWebTests | 8 | Real HTTP parser, JSON/omission, spoof resistance, concurrency, reachable Problem/negotiation/dispatch; one direct writer-only vocabulary method |
| CustomerApiBoundaryTests | 3 | Signatures/module map/read-only SQL/source/profile/product boundary |
| CustomerMeIT | 10 | Real PostgreSQL role/ACL/SQL binds, PERSONAL synthesis/order, lifecycle freshness, no writes, dual pools, failure recovery/cleanup |
| CustomerMeFixture | 0 | Shared disposable container/fixtures/capture/cleanup only |

- Surefire: 5+6+6+8+3 = **28 new**; 21 baseline + 28 = **49**, across 6+5 = **11 classes**.
- Failsafe: **10 new**; 64 PostgresContainerIT + 10 SharingQueryServiceIT + 10 CustomerMeIT = **84**, across **3 classes**.
- Combined: 95 baseline + 38 new = **133**. Existing methods extended in place add no count. Process/JAR smokes, fixture support and matrices add no JUnit methods. Both early/final CI aggregates and literal source/XML sets use 49/84, never minimums.
- Drop security's duplicate exhaustive parser method; real HTTP #2 owns grammar/header multiplicity. Keep exact verifier argument/opaque-byte unit evidence because it is a different contract.
- Fold web PERSONAL-only case into frozen-response #1; fold cache/header/redaction assertions into reachable-response #4. Wrapper serialization/lifetime remains a separate unit assertion; JDBC/role/fault canaries remain IT #10 because they require the real persistence boundary.
- Remove repeated HTTP session rejection and unavailable-provider matrices; security #2–4 own those. Production configuration #2 still proves missing adapter cannot initialize the real graph, and IT #10 proves no real checkout on outage.
- Remove HTTP inactive-account stub test; application #3 owns defensive mapping, IT #2 owns real ACTIVE SQL filtering. IT #7 retains only the distinct SQL-bound selector/account isolation proof, not another parser/session-denial matrix.
- Replace old all-six-codes HTTP injection with reachable-only HTTP #4 plus lawful writer-only #8. Add HTTP #7 for the previously missing negotiation/internal ERROR boundary. No fake HTTP producer for reserved codes.
- Retain all ten IT methods: role/grant errors (42501), read-only write rejection (25006), snapshot/lifecycle changes, stable unsigned DB order across sign boundaries, no-write row snapshots, dual-pool coexistence and failure resource recovery remain distinct and non-negotiable.

## 5. Deferred/rejected scope and remaining owners

- Denial append_fact integration is deferred, not rejected as unnecessary; Identity/Backend/Security owns authentication/audit integration and the still-uncovered A5 denial scenario. PA11 adds no write transaction or audit acknowledgment claim.
- Spring Security adoption/framework expansion is rejected for this bounded slice; its accepted exception goes to Backend/Architecture/Security's authentication-integration ADR and later docs/12 reconciliation. No docs/12 edit now.
- Connector/edge transport normalization is deferred to Platform/Security/Observability deployment integration. Reachable pre-commit Problems are covered; connector/committed/socket failures are not claimed to be rewriteable.
- Channel reconciliation for createCustomerOrder belongs to Backend/API/Identity's catalog-order work unit; /me is session-attested only and imposes no order-channel policy.
- Completion report must trace web/CustomerMeIT integration evidence to A1:OP:getCustomerMe, A0:INV:INV-01..03 and A3:TS:TS1, while explicitly leaving login-provider/e2e A5 evidence missing and not absorbing the denial-audit scenario.
- Provider/store/revocation/expiry/TTL inputs, production credentials/provisioning/capacity, large-list EXPLAIN/index/SLO measurement, post-snapshot authorization races, external logging/TLS, image provenance and PA10 promotion remain in plan §11 with named owners/destinations. No values or additional capability are invented.
- No accepted material finding is rejected or left as an implementation-time design choice. Entry probe/build/HTTP/PostgreSQL/remote CI evidence is future implementation work, not evidence produced by this planning fix. Fresh independent A/B Planreview and controller acceptance are still required before implementation.

## 6. Planning validation

Validate the final named sets, count arithmetic, duplicate-method absence, frozen-input/review hashes, whitespace and allowed-write scope without running product builds/tests or modifying historical artifacts. This is document consistency validation, not successful application/CI/database verification. The plan retains exact future file ownership, validation commands and implementation completion evidence requirements in §§3 and 8–11.

Executed planning checks: parsed all six named test-class sections and verified 5/6/6/8/3/10 methods, 38 unique names, 28/10 additions and final 49/84/133 arithmetic; verified all eight plan input hashes and both review hashes; checked A-01–A-07 and B-F1–B-F15 disposition coverage, removed global inclusion/SQL NULLS FIRST and malformed numeric spacing, final new-file whitespace and `git diff --check`. All passed. `git diff --name-only` and `git diff --cached --name-only` are empty: plan/reviews arrived untracked, and the final status differs from intake only by the permitted new fix artifact; the existing untracked plan was edited in place. Existing `.codex` and review files were untouched. No product tests/build, PostgreSQL, runtime smoke or remote CI was executed or claimed.

status: plan-fix-complete
plan: docs/gateflow/pa11-customer-auth-api-foundation-plan.md
fix: docs/gateflow/pa11-customer-auth-api-foundation-plan-fix-1.md
