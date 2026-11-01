# PA10 bounded plan fix 1

## 1. Identity, authority and result

- Task: `pa10-planfix1-gpt-20261101-01`; work unit: `PA10-sharing-application-service-foundation`; date: 2026-11-01; current gate: **fix**.
- Isolated detached checkout: `b65019b23e52c03cc6eeed8af10b05e30acadbbc`; product base remains `bdfafc706ce7c1f9f515fa27434139e31769576f`.
- Binding authority: active PA10 Goal Confirmation and controller adjudication in `docs/gateflow/project-execution-control.md`. No goal amendment, implementation, acceptance or gate advancement is claimed.
- Plan: `docs/gateflow/pa10-sharing-application-service-plan.md`.
- Prior plan SHA256: `b6266475fd027286e806cee299755c7610e9a107fa10534e443aad8bfbd5bd15` (261 lines).
- Fixed plan SHA256: `b323f5368ed215c389b7448b8c90643cfe1cd22c55597e3419e65568cb54ec86` (286 lines).
- Result: bounded plan-text repairs complete, retaining one behavioral slice, PA10-S1. This artifact reports repairs, not an accepted-plan or re-review verdict. Stop at this gate; controller owns fresh A/B re-review.

Both review artifacts were read in full: `docs/reviews/plan-review-20261101-132834.md` (A) and `docs/reviews/plan-review-20261101-134327.md` (B). A's artifact is complete but its run ended `turn.failed`; it is evidence, not valid independent review completion. B completed normally. The controller's dispositions, not voting or either review verdict alone, control this fix.

## 2. Controller input-manifest correction

The previous dispatch supplied Windows CRLF auxiliary hashes. Both reviewers actually inspected the same canonical Linux checkout at `add92fd260a2ab8b70ac4874e6ecab44dd094552`, with the matching prior plan hash. This is an accepted controller manifest defect (A-01 / B-Q4), not conflicting product designs, reviewer divergence or permission to change frozen content. The control bytes at that review checkpoint had SHA256 `9e02280f636681939a32eb8120d089ab08b9554bc570a3f09224de46ee44cf7e`; the active adjudication added since then yields the current control hash below. Do not confuse that intentional checkpoint change with the old line-ending manifest error.

Fresh controller-directed re-review must freeze the **fixed plan hash above** and these corrected canonical inputs, checking byte identity in both isolated checkouts before review. The old dispatch manifest is superseded; no re-review is initiated here.

| Canonical input | SHA256 in this fix checkout |
| --- | --- |
| `docs/gateflow/project-execution-control.md` | `8fc24d03ae8a7f238536e2281790a3dfefe21370a99621cc9a2bf1a4e58e4ca0` |
| `docs/12-technology-stack-and-system-architecture.md` | `772ccaee7b188f68fcc694f3e8c00983ad9de028547f8e0b0c8638a8d8cae435` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `3a506e578fdde6a89e684a64ddd31261b397e22f7bdb0ac589331bb6eebba761` |
| `docs/15-production-openapi-surface-spec.md` | `0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02` |
| `docs/17-production-security-design-spec.md` | `4e7ac923556c5ac1b8ec4103e7a85f02e79597e852ab4a4a51c84db54b6cd447` |
| `backend/src/main/resources/db/migration/V009__sharing.sql` | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| `api/openapi/onecall-v1.openapi.yaml` | `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30` |

All seven current hashes were verified against the dispatch. Design/V009/OpenAPI/control bytes are unchanged by this fix. Implementation entry must independently match its controller-frozen plan/input manifest; this fix's hash check is not environment-entry approval.

## 3. All adjudicated finding statuses

Statuses describe plan repairs only; all planned implementation assertions still require execution after authorized entry.

| Finding | Controller disposition / resulting status | Repair and exact verification destination |
| --- | --- | --- |
| A-01 | Accepted controller manifest defect; corrected for handoff | Artifact §2 and plan §1 identify canonical Linux inputs, historical versus current control, and fresh identical-byte re-review requirement. |
| B-Q4 | Accepted controller manifest defect; corrected for handoff | Same manifest correction as A-01; no design content changed or old manifest reused. |
| A-02 / B-F1 | Accepted; repaired | Plan §§2–6/8–9 remove listing advisory/foreign row locks and rank tests, cite docs/14:155/:2973, require exact current ACTIVE facts every query, and replace IT 10 with acknowledged commit/rollback and next-query denial tests. No lock-timeout-to-503 or invented production bound. |
| A-03 | Accepted issue under old design; removed with runtime migration | Plan §§4–5/7–9 contain no runtime migration connection or same-target validator to invent. Fixture provisioning targets its own container before query-only context startup. Production target/identity/provisioning remains Q17/Q23. |
| A-04 | Obsolete after runtime migration removal | No optional migration startup mode remains. Renamed IT 1 proves the sole query runtime against fixture-provisioned V001–V009, with no Flyway bean or migration credentials. No separate migration-disabled runtime claim/test is retained. |
| B-F2 | Accepted; repaired | Plan §§2/6/8–9 specify OffsetDateTime/timestamptz to Instant mapping and exact UTC anchors, frozen-fixture DB-derived total order, C/C fixture collation/database facts, UTC versus non-UTC JVM/session evidence and sub-millisecond ties. Production collation/index/performance remains owned in §11. |
| B-F3 | Accepted; repaired | Plan §§4–5/8–9 name test-only DataSource/Connection/PreparedStatement capture of the actual service's eleven-column SELECT and 2/3/5/6 parameter counts. No values retained; no product listener/test switch or copied service DSL. Product execute-listener SQL/bind logging stays off. |
| B-F4 | Accepted; repaired | Plan §§4/6/9 define one six-field diagnostic record per failure: constant operation, service error code, exception class, validated SQLSTATE, category, role-assertion boolean. Named unit 8 and IT 8/10 prove useful classification and secret-free canaries; no raw cause/record in public exception messages. |
| B-F5 | Accepted; repaired | Plan §§1–2/4–5/7–10 remove runtime Flyway and all migration property/environment/ordering/credential contracts. Flyway stays test-only, Surefire exclusions and product/JAR bans remain. Fixture applies/validates V001–V009 with admin identity before restricted query login is used. |
| B-F6 | Accepted; repaired | Plan §§3/8–9 require controller entry record before product edits or any tests: real artifact resolution, proposed effective POM/tree with convergence, Docker/PostgreSQL 16, fixed plan/input hashes. Disposable probe POM avoids premature product edits. Local unit/config/boundary versus capable PostgreSQL/package/CI evidence is explicit, with no waived criterion. |
| B-F7 | Deferred-with-owner; explicitly recorded | Plan §§4/9/11 retain service/contracts in sharing/internal now. Sharing/Backend owns a same-module adapter or named public module API promotion when synchronous customer API integration requires it, updating Modulith boundary tests then. No public API is added now. |

## 4. Exact changed plan sections

All twelve existing numbered sections were updated in place; no new behavioral slice or product feature was added.

| Section | Bounded change |
| --- | --- |
| §1 Authority, goal and success signal | Fix task/gate/checkpoint, manifest reference, query-only success signal and exact two-file fix write boundary. |
| §2 Goal alignment and first-principles judgment | Remove runtime Flyway/read locks from signal mapping; make listing advisory, current-state checked, exact typed and DB-order evidenced. |
| §3 Reconciled sources and direct evidence | Correct docs/14 lock-contract interpretation; label previous dependency/Docker/test probes historical, not current entry evidence, including uncached chosen libraries. |
| §4 Implementation ownership and call path | Exact paths for query-only properties/config, diagnostic record, test fixture/capture, guard/test/CI/docs ownership; unlocked call path and retained internal API. No service/repository signature expansion. |
| §5 Runtime, credentials and transaction decisions | Query Hikari/transaction/jOOQ only; three query keys/environment inputs; Flyway strictly test; no migration runner or common datasource credentials; fixed role/read-only transaction preserved; current ACTIVE checks without locks. |
| §6 Exact service, SQL and cursor contracts | Exact typed jOOQ fields/UTC microsecond anchors; keep eleven-field closed projection, creator/workspace SQL and keyset semantics; add six-field sanitized diagnostic allowlist and remove unproven timeout/migration response claims. |
| §7 Preserve architecture and evolve only PA0 guards | Retain runtime Flyway bans/Surefire exclusion, require query libraries in JAR, exclude Flyway and concrete forbidden stacks, preserve fourteen modules and historical schema/ACL/tests. |
| §8 Single verifiable behavioral implementation slice | Add controller environment-entry gate before edits/tests with disposable probe-POM evidence; fixture/admin provisioning only; named query-property injection; fixture collation/time-zone/DB order oracle; exact test-only actual-statement capture. |
| §9 Exact tests, method sets and validation commands | Rename config test 2 and IT 1/10; strengthen unit 8, boundary and IT 5/8; retain 8+2+1 new Surefire and ten new IT methods; retain 21 total Surefire/74 total Failsafe. Reject container parameterized/repeated/dynamic/disabled tests. Update expected scope/JAR evidence and local/capable-CI allocation. |
| §10 Documentation decision | Future implementation README is query-only/Flyway test-only, unlocked/advisory/no-snapshot; distinguish this fix's two-file scope from future controller-owned control update. |
| §11 Residual risks, owner and destination | Explicit API promotion, production collation/index/scan/timeout ownership, unlocked drift, query-only Q17/Q23, environment entry, deferred auth/wire/public/mutation/policy ownership. |
| §12 Completion report format and boundedness | Require entry/hash provenance, query-only scopes, actual statement/count and DB order/zone/collation evidence, diagnostic canaries and committed lifecycle denial, exact tests, timings and local/remote provenance. |

Interfaces remain exactly one service business method `listCustomerShares(TrustedShareCaller, SharePageRequest)` and two repository methods `requireActiveCreatorContext(TrustedShareCaller)` / `fetch(TrustedShareCaller, SharePageRequest, int)`. Diagnostic record is not a new service/API. New test methods remain 11 Surefire + 10 Failsafe; historical 10 Surefire (one guard rename) and 64 Failsafe are preserved. Implementation commands remain future validation only: effective POM/tree/validate, targeted tests, default verify, real container verify, packaged smoke/inventory and unchanged contract-regression commands, with updated expected scope and evidence rules.

## 5. Preserved goal and non-goals

The only behavior is read-only creator listing from trusted in-process account plus optional organization. Every grant branch retains `created_by_account_id = caller`, personal `owner_organization_id IS NULL` or exact selected organization equality. Current account/organization/membership ACTIVE checks do not widen visibility by role. Stable database-native `(valid_from DESC, share_id DESC)` keyset paging, limit default 20/range 1–100, last-returned-row cursor, eleven metadata fields and sanitized failures remain binding. Default bootstrap is non-web/database-free; no mutation lease or cross-page snapshot is promised.

No HTTP/controller/auth/session implementation, public module API, create/revoke/rotate/public-share GET, admission/ticket/media/playback, evidence collaborator, Redis/COS/KMS/KDF/OTP, Q12/Q22 policy value, new schema/migration/index/ACL, provisioning implementation, production identity/timeout/SLO, generic framework or production-readiness claim is introduced. Structured diagnostics are bounded logging, not a business event/table. Fixture-only setup/capture is not product capability. No staging, commit, push, PR/issue mutation, approval, external comment or entry to re-review occurs.

## 6. Residual owners and limitations

| Residual | Owner / destination | Limitation retained |
| --- | --- | --- |
| Production collation/zone, listing index/scan/sort capacity and timeout/SLO | Backend/Database/Platform Reliability; production database/capacity review | Fixture C collation and exact DB comparator are evidence, not a production configuration choice or latency guarantee. No index/migration added. |
| Q17/Q23 execution/provisioning identities, stage/order and privilege deployment | Platform/Database/Security; deployment/security design | Runtime has query credentials only; test/CI admin setup is not production provisioning. V009 sharing_role is not globally read-only. |
| Real authentication and synchronous customer API access | Identity/Backend/Security for authentication; Sharing/Backend for same-module adapter or named public API promotion at customer API integration | Internal trusted context only now; update Modulith tests when integration needs an API. |
| Q15 wire cursor and transport errors/rate limiting | Backend/API/Security; customer API/cursor integration | Typed internal anchor only, no codec/keys/HTTP mapping or wire-safety claim. |
| Public-share/mutation/admission/media implementation and Q12/Q22 policy | Sharing/Backend/Evidence/Product/Security; existing sharing/security design | Entirely deferred; no new slice or policy value. |
| Advisory state drift and non-snapshot pages | Backend/API; protected-request/customer API integration | A later committed lifecycle change is checked on the next query; a concurrent commit after checks may leave an in-flight projection stale. |
| Downstream diagnostic/projection egress | Sharing/Backend plus Security/Observability/API; R-A3-REDACTION-RUNTIME | Local canaries and fixed allowlists do not prove every external serializer/collector/edge. |
| Prepared environment and complete validation | Controller + Backend/CI; entry record and implementation report | Historical missing artifacts/DNS/Docker are not fixed by plan prose. Entry must be demonstrated; local versus capable-CI evidence and actual timings remain outstanding. |

## 7. Read-only fix validation and stop point

- Confirmed detached HEAD and prior plan SHA256, active control adjudication, both review artifacts and exact docs/14 listing exceptions.
- Verified all seven canonical input hashes above; unchanged frozen design/V009/OpenAPI/control bytes remain outside writes.
- `git diff --check` passes; stale positive runtime migration/read-lock contracts and old renamed test names are absent. Exactly the allowed plan and this new artifact are the intended changes; pre-existing untracked `.codex` is untouched.
- Document inventory is still one slice, one service business method, two repository methods, 11 new Surefire and ten new IT methods; planned totals 21/74. These are plan assertions, not test results.
- No product/POM/test/CI edits or test/build/Docker/network execution occurred in this fix. No environment readiness or runtime behavior is claimed as newly proven. Only read-only repository/document/hash/diff checks were run in addition to the two allowed document writes.

status: plan-fix-complete

Current gate remains **fix**. Await controller intake and fresh independent re-review; do not implement or self-advance.
