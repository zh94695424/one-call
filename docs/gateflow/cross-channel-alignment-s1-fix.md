# Cross-channel alignment S1 fix

## Gate / scope / status

- Work unit: `cross-channel-alignment`.
- Gate: `fix`, slice `S1 — A single executable demo contract` code review loop.
- Date: 2026-10-20.
- Review artifacts: `docs/reviews/code-review-20261020-125605.md` and `docs/reviews/code-review-20261020-133458.md`.
- Fix artifact: `docs/gateflow/cross-channel-alignment-s1-fix.md`.
- Round 1 scope: accepted findings CR-001 through CR-004 only, plus their tests, contract documentation and generated marker/preview refresh.
- Round 2 scope: accepted findings CR-005 through CR-007 only, plus necessary tests, documentation and generated marker/preview refresh.
- Status: **complete**. CR-001 through CR-007 are all `已修复`; required validation passes.

This fix did not enter S2/S3, change employee/admin or customer UI semantics, update screenshot baselines, modify the controller, commit, review, push or create a PR.

## Changed files

- `shared/onecall-business.js`
- `shared/qa/business.test.js`
- `shared/README.md`
- `docs/10-cross-channel-rules.md`
- `docs/gateflow/cross-channel-alignment-s1-fix.md`
- Generated `ONECALL_BUSINESS` marker sections and regenerated previews in the operations, miniapp and Web prototypes

The three QA runners rewrote five tracked Web screenshots during verification. Those generated PNG changes were restored to HEAD because this fix has no visual behavior or screenshot-baseline change.

## Finding status

### CR-001 — 已修复

`processedEvents` now has explicit action domains: `ACTIVATION`, `CLAIM`, `IVR_CONFIRM`, `EVIDENCE`, `ORDER`, `PAYMENT` and `ENTITLEMENT`. Records use a `$`-prefixed encoded key, bind the event to a canonical input fingerprint and store a deep-cloned first-result snapshot. A cross-domain reuse cannot overwrite another action; same-domain same-key/different-input calls reject atomically; exact replay returns the first result even after mutable order/grant state advances. Special event IDs `__proto__`, `constructor` and `prototype` remain own safe keys without prototype mutation.

Tests cover cross-domain reuse, changed-input rejection, immutable IVR grant/evidence/payment replay and all three special identifiers.

### CR-002 — 已修复

ID allocation maps each prefix to its owning collection and advances the imported `sequence` hint until the next candidate is unused. Every successful mutation runs full `validateState` before replacing the caller state, so no action can report success while committing a duplicate-ID or otherwise invalid snapshot.

Tests import a valid state with the immediate next candidate occupied for `ORDER`, `TRIAL`, `SUBJ`, `ACT`, `EVID`, `CLAIM`, `ENT` and `IVR`; every action allocates a different ID, retains collection uniqueness and produces a valid exportable snapshot.

### CR-003 — 已修复

The public `offerVersionToken` helper canonicalizes validated catalog data with sorted object keys. `visibleOffers` returns the same token on every visible offer. `createOrder` requires a non-empty exact `versionToken`; missing, empty and stale values return `STALE_VERSION` without mutation.

Tests cover missing/empty/stale/current tokens, equal tokens after recursive property reordering and different tokens after content change.

### CR-004 — 已修复

Catalog validation now requires every enterprise version to be `ENTERPRISE + QUOTE + billingCycle=QUOTE + priceCents=null`. Personal versions cannot be quote/billing-cycle quote and cannot contain seats or fixed lines. `createOrder`, imported order validation and `issueEntitlements` all require a personal transactional snapshot valid for the order channel.

Tests reject an enterprise non-QUOTE catalog version, a personal QUOTE version, imported paid enterprise/QUOTE orders and issuance from both enterprise and personal QUOTE snapshots.

## Validation

| Command | Result |
| --- | --- |
| `node --check shared/onecall-business.js` | PASS |
| `node --check shared/build-prototypes.js` | PASS |
| `node --check web-prototype/qa/qa.js` | PASS |
| `node --test shared/qa/business.test.js` | PASS, 16/16 after preview regeneration |
| `node --test shared/qa/runner-exit.test.js` | PASS, 1/1 |
| `node shared/build-prototypes.js --check` | PASS |
| `powershell -NoProfile -ExecutionPolicy Bypass -File operations-prototype/qa/run.ps1` | PASS, 13 routes and existing IVR/catalog/role/responsive assertions |
| `powershell -NoProfile -ExecutionPolicy Bypass -File prototype/qa/run.ps1` | PASS, 57 pages plus navigation, caller flows, checkout and 320px layout |
| `powershell -NoProfile -ExecutionPolicy Bypass -File web-prototype/qa/run.ps1` | PASS, login/session, interaction, authorization, content and responsive checks |
| `git diff --check` | PASS; line-ending conversion warnings only |

## Documentation decision

`shared/README.md` and `docs/10-cross-channel-rules.md` now define the event domains and immutable replay rule, imported sequence as a hint rather than uniqueness authority, public version-token ownership, and the enforced enterprise quote/personal transaction boundary. No S2/S3 UI documentation was advanced.

## Residual risks

| Classification | Item | Owner / destination |
| --- | --- | --- |
| covered by later approved slice | The three current UIs have not yet migrated to consume these shared helpers and states | S2 admin and S3 customer/IVR slices |
| assigned to later work unit | Durable idempotency storage, concurrent ID/quota allocation, trusted caller, payment verification and fulfillment | Production backend, security, payment and IVR work units |
| requiring explicit user decision | Production prices/rights, renewal rules and real support contact details | Product, finance and operations |
| existing external-state blocker | No Git remote is configured | Repository maintainer at later push/PR gate |

No unclassified S1 fix risk remains.

## Completion / next entry (round 1 record)

The S1 fix completion signal is met: all four accepted findings are `已修复`, regression coverage is present, generated sources/previews match the canonical module, all required validation passes and screenshot baselines are unchanged. The next Gateflow entry is S1 `re-review`, owned by the controller. This artifact does not perform that review or enter a later gate.

## Fix round 2

### Gate / scope / status

- Gate: `fix`, S1 code review loop round 2.
- Review input: `docs/reviews/code-review-20261020-133458.md`.
- Accepted scope: CR-005, CR-006 and CR-007 only.
- Final status: all three findings are `已修复`.
- Non-goals preserved: no S2/S3 UI semantics, controller edit, baseline expansion, screenshot update, staging, commit, review, push, PR or later gate.

### CR-005 — 已修复

`createOrder` now requires the selected catalog version to be `ACTIVE` in addition to satisfying the personal transactional snapshot rules. DRAFT and OFFSALE versions reject atomically before token comparison or state mutation. This current-saleability rule remains local to new checkout: historical order and entitlement validation continues to use the frozen personal transaction snapshot and channel, without consulting the current catalog status.

Regression tests cover DRAFT/OFFSALE with both their current token and the former ACTIVE token, complete-state non-mutation, a V1 order followed by V1 retirement and V2 publication, snapshot import, PAID transition and entitlement issue while preserving the V1 price/rights snapshot.

### CR-006 — 已修复

`validEventRecord` is now the single shared owner used by both imported-state validation and replay defense. Each event domain validates its exact input/result structure, canonical input fingerprint, stable subject/session/order identity, immutable business fields and references to one compatible current object. Historical mutable facts are explicitly separated: order `payment`/`entitlement`, trial `usedSeconds` and session guidance may advance while the original result remains valid; frozen product, grant, evidence, consent and ownership facts may not drift.

Tests create and import legitimate records in all seven domains. They verify an old evidence result with `usedSeconds=60` after current usage reaches 100 and an old payment `UNKNOWN` after the order reaches PAID/ISSUED. Separate payloads containing a null result, missing field, ghost order, cross-subject claim, entitlement result in the payment domain or fingerprint subject mismatch all reject atomically.

### CR-007 — 已修复

`consumeTrial` now proves exactly one CONFIRMED session, non-null equal `subjectId`/`confirmedSubjectId`, exactly one matching subject and valid existing evidence references before reading `processedEvents`. Only after that structural context passes may an exact event replay return. Suspension, grant lookup, expiry, exhaustion and remaining-quota checks remain after replay, so a legitimate first result survives later exhaustion or elapsed expiry without another deduction.

The five existing tamper fixtures now exercise both the previously processed key and a fresh key. Cross-subject, unresolved, removed, replaced and duplicated subject contexts all return `INVALID_TRANSITION` with the complete state byte-for-byte unchanged. A separate regression exhausts the grant after the first recording, advances time beyond expiry and confirms the original key still returns the first snapshot without mutation.

### Changed files

- `shared/onecall-business.js`
- `shared/qa/business.test.js`
- `shared/README.md`
- `docs/10-cross-channel-rules.md`
- `docs/gateflow/cross-channel-alignment-s1-fix.md`
- Generated `ONECALL_BUSINESS` sections and previews in all three prototypes

No hand-maintained page semantics or screenshot baselines changed. Before browser QA there were no screenshot diffs. The runners rewrote one miniapp and eight Web PNGs; exactly those nine generated files were restored to HEAD afterward.

### Validation

| Command | Result |
| --- | --- |
| `node --check shared/onecall-business.js` | PASS |
| `node --check shared/qa/business.test.js` | PASS |
| `node --check shared/build-prototypes.js` | PASS |
| `node --check web-prototype/qa/qa.js` | PASS |
| `node --test shared/qa/business.test.js` | PASS, 19/19 after preview regeneration |
| `node --test shared/qa/runner-exit.test.js` | PASS, 1/1 |
| `node shared/build-prototypes.js --check` | PASS |
| `powershell -NoProfile -ExecutionPolicy Bypass -File operations-prototype/qa/run.ps1` | PASS, existing 13-route/IVR/catalog/role/responsive suite |
| `powershell -NoProfile -ExecutionPolicy Bypass -File prototype/qa/run.ps1` | PASS, existing 57-page/navigation/caller/checkout/320px suite |
| `powershell -NoProfile -ExecutionPolicy Bypass -File web-prototype/qa/run.ps1` | PASS, existing login/session/interaction/authorization/content/responsive suite |
| `git diff --check` | PASS; line-ending conversion warnings only |

`runner-exit.test.js` and the business hash test both touch/read Web preview generation, so their final recorded executions were run sequentially rather than concurrently.

### Documentation decision

`shared/README.md` and `docs/10-cross-channel-rules.md` now distinguish current ACTIVE saleability from historical transaction validity, define the domain-owned event result schema/reference boundary, and record the structural checks that precede trial replay. No S2/S3 documentation status was advanced.

### Residual risks

| Classification | Item | Owner / destination |
| --- | --- | --- |
| covered by later approved slice | Existing pages still do not consume all S1 helpers and state | S2 admin and S3 miniapp/Web/IVR slices |
| assigned to later work unit | Durable idempotency storage, concurrent ID/quota allocation, trusted caller, payment verification and fulfillment | Production backend, security, payment and IVR owners |
| requiring explicit user decision | Production prices/rights, renewal/stacking rules and real support contact/SOP | Product, finance and operations |
| recoverable build risk | Generator writes sources sequentially; `--check` and rerun detect/recover partial updates | Shared generator owner; not expanded in this correctness fix |
| existing external-state blocker | No Git remote is configured | Repository maintainer at the later push/PR gate |

No unclassified S1 round-2 fix risk remains. The next entry is independent S1 `re-review`, owned by the controller.

## Fix round 3

### Gate / scope / status

- Gate: `fix`, S1 code review loop round 3.
- Review input: `docs/reviews/code-review-20261020-140501.md`.
- Accepted scope: the remaining CR-006 reverse-uniqueness gap only.
- Final status: CR-006 is `已修复`; creating-event identities now map one-to-one to side-effect objects within their action domain.
- Non-goals preserved: no controller edit, S2/S3 work, hand-maintained UI semantics, baseline expansion, staging, commit, review, push or PR.

### CR-006 — 已修复

State validation now requires `claims[].requestId` and `orders[].idempotencyKey` to be unique. Activation `eventId` uniqueness is scoped by action domain: non-IVR activations belong to `ACTIVATION`, IVR activations belong to `IVR_CONFIRM`, so the same string remains valid once in each domain but cannot create two objects inside either domain.

Imported processed-event records also reject duplicate created-result identities within `ACTIVATION`, `IVR_CONFIRM`, `EVIDENCE`, `CLAIM`, `ORDER` and `ENTITLEMENT`. Direct event validation resolves activation, claim and order event identities to exactly one current object and requires that object's ID to match the frozen result ID. `PAYMENT` is intentionally exempt from result-identity uniqueness because distinct callback events may legitimately advance one order from `UNKNOWN` to `PAID`.

Malformed result handling now validates object structure before reading IDs, so null or non-object results reject as `INVALID_TRANSITION` rather than escaping validation with a property-access exception. Historical mutable replay remains unchanged: old payment/grant values may differ from current state, and historical personal orders remain importable, payable and issuable after catalog retirement.

Regression coverage now proves:

- Two orders with the same `idempotencyKey` and different IDs fail `snapshot`, fail `loadReviewSnapshot` atomically and cannot be separately paid or issued.
- Two client activations with one `eventId` fail atomically; two IVR activations with one `eventId` fail atomically.
- The same string is valid in `ACTIVATION` and `IVR_CONFIRM` because those are separate action domains.
- All seven domains import and replay their immutable first results, including both `UNKNOWN` and later `PAID` payment events targeting one order.
- Existing cross-domain event reuse, `UNKNOWN -> PAID`, and V1 historical fulfillment after V1 retirement/V2 publication continue to pass.

### Changed files

- `shared/onecall-business.js`
- `shared/qa/business.test.js`
- `shared/README.md`
- `docs/10-cross-channel-rules.md`
- `docs/gateflow/cross-channel-alignment-s1-fix.md`
- Generated `ONECALL_BUSINESS` sections and previews in all three prototypes

No hand-maintained page behavior or screenshot baseline changed. Before browser QA there were no PNG differences. The miniapp runner rewrote one tracked PNG and the Web runner rewrote seven; exactly those eight generated differences were restored to HEAD afterward.

### Validation

| Command | Result |
| --- | --- |
| `node --check shared/onecall-business.js` | PASS |
| `node --check shared/qa/business.test.js` | PASS |
| `node --check shared/build-prototypes.js` | PASS |
| `node --check web-prototype/qa/qa.js` | PASS |
| `node --test shared/qa/business.test.js` | PASS, 20/20 after preview regeneration |
| `node --test shared/qa/runner-exit.test.js` | PASS, 1/1 |
| `node shared/build-prototypes.js --check` | PASS |
| `powershell -NoProfile -ExecutionPolicy Bypass -File operations-prototype/qa/run.ps1` | PASS, existing 13-route/IVR/catalog/role/responsive suite |
| `powershell -NoProfile -ExecutionPolicy Bypass -File prototype/qa/run.ps1` | PASS, existing 57-page/navigation/caller/checkout/320px suite |
| `powershell -NoProfile -ExecutionPolicy Bypass -File web-prototype/qa/run.ps1` | PASS, existing login/session/interaction/authorization/content/responsive suite |
| `git diff --check` | PASS; line-ending conversion warnings only |

`runner-exit.test.js` and the final business/hash test were run sequentially around browser preview generation. Final generated-section validation and the 20-test business suite both ran after the three browser runners.

### Documentation decision

`shared/README.md` and `docs/10-cross-channel-rules.md` now define creating-event identity as domain-owned one-to-one mapping, state the separate activation scopes and explicitly preserve the multi-event `PAYMENT` exception. No S2/S3 documentation status was advanced.

### Residual risks

| Classification | Item | Owner / destination |
| --- | --- | --- |
| fixed in current slice | CR-006 reverse uniqueness and its missing regression coverage | S1 round-3 fix; complete |
| covered by later approved slice | Existing pages still do not consume all S1 helpers and state | S2 admin and S3 miniapp/Web/IVR slices |
| assigned to later work unit | Durable idempotency storage, concurrent allocation, trusted caller, payment verification and fulfillment | Production backend, security, payment and IVR owners |
| requiring explicit user decision | Production prices/rights, renewal/stacking rules and real support contact/SOP | Product, finance and operations |
| recoverable build risk | Generator writes sources sequentially; `--check` and rerun detect/recover partial updates | Shared generator owner; unchanged and outside this correctness fix |
| existing external-state blocker | No Git remote is configured | Repository maintainer at the later push/PR gate |

No unclassified S1 round-3 fix risk remains. The next entry is independent S1 `re-review`, owned by the controller.

## Fix round 4

### Gate / scope / status

- Gate: `fix`, S1 code review loop round 4.
- Review input: `docs/reviews/code-review-20261020-142931.md`.
- Accepted scope: CR-006 domain-level created-result uniqueness bypass during replay only.
- Fix status: `已修复`, pending independent S1 re-review; this artifact does not make that review decision.
- Non-goals preserved: no controller edit, hand-maintained UI change, S2/S3 work, screenshot baseline update, staging, commit, review, push, PR or delegation.

### CR-006 — 已修复

`validEventDomain` is now the shared owner of a domain's record validation and aggregate created-result identity uniqueness. `validProcessedEvents` uses it for every domain during snapshot/export/import validation. `replayEvent` uses the same helper for its owning domain before looking up either an existing or a fresh key. A valid single record can no longer bypass a conflicting or malformed same-domain history. The canonical input fingerprint is then compared only after domain validation succeeds.

The call graph is acyclic: `validateState -> validProcessedEvents -> validEventDomain -> validEventRecord -> leaf structural/reference helpers`, and `replayEvent -> validEventDomain`. Neither the domain nor record validator calls back into `validateState`, `validProcessedEvents` or `replayEvent`. All seven domains use this path; no EVIDENCE/ENTITLEMENT-only replay exception was added.

Created-result identity uniqueness still applies to ACTIVATION, IVR_CONFIRM, EVIDENCE, CLAIM, ORDER and ENTITLEMENT. PAYMENT continues to validate its individual records while allowing distinct events to reference the same order. Cross-domain reuse of the same string remains valid. Historical mutable first results, current ACTIVE checkout requirements and historical off-sale order fulfillment retain their existing semantics.

### Regression evidence

- A valid session creates two distinct evidence objects under two different event IDs; snapshot import and both original-key replays succeed without mutation.
- Replacing the second event's result with the first evidence result leaves individually compatible references but creates duplicate domain ownership. Snapshot and load reject atomically; both original keys and a fresh key return `INVALID_TRANSITION`, with the complete state unchanged.
- A valid paid order issues one entitlement. Issuing again with a new key returns the existing `DUPLICATE` result and leaves the full state, including its single ENTITLEMENT record, unchanged.
- Adding a second ENTITLEMENT record that declares the same entitlement identity causes snapshot/load, both original keys and a fresh-key call to reject atomically.
- The existing seven-domain import/replay test now also adds a malformed peer record separately to each owning domain. Import and old-key replay reject consistently for all seven domains, including both payment events.
- Existing positive tests continue to cover UNKNOWN-to-PAID multi-event payment, cross-domain same-string reuse, immutable historical grant/payment results, and V1 historical import/payment/issue after V1 retirement and V2 publication.

### Changed files

- `shared/onecall-business.js`
- `shared/qa/business.test.js`
- `shared/README.md`
- `docs/10-cross-channel-rules.md`
- `docs/gateflow/cross-channel-alignment-s1-fix.md`
- Generated `ONECALL_BUSINESS` marker sections and corresponding previews in operations, miniapp and Web prototypes

Prior round records were preserved. No screenshot differences existed before QA. The runners rewrote one miniapp PNG and ten Web PNGs; exactly those eleven newly generated differences were restored to HEAD. No screenshot difference remains.

### Validation

| Command | Result |
| --- | --- |
| `node --check shared/onecall-business.js` | PASS |
| `node --check shared/qa/business.test.js` | PASS |
| `node --check shared/build-prototypes.js` | PASS |
| `node --check shared/qa/runner-exit.test.js` | PASS |
| `node --check web-prototype/qa/qa.js` | PASS |
| `node --test shared/qa/business.test.js` | PASS, 21/21 after all preview regeneration |
| `node --test shared/qa/runner-exit.test.js` | PASS, 1/1 controlled failure propagated |
| `node shared/build-prototypes.js --check` | PASS |
| `powershell -NoProfile -ExecutionPolicy Bypass -File operations-prototype/qa/run.ps1` | PASS, existing 13-route/IVR/catalog/role/responsive suite |
| `powershell -NoProfile -ExecutionPolicy Bypass -File prototype/qa/run.ps1` | PASS, existing 57-page/navigation/caller/checkout/320px suite |
| `powershell -NoProfile -ExecutionPolicy Bypass -File web-prototype/qa/run.ps1` | PASS, existing login/session/interaction/authorization/content/responsive suite |
| `git diff --check` | PASS; line-ending conversion warnings only |
| Final tracked PNG diff | Empty |

Runner-exit verification, the three browser runners and final business tests ran sequentially so preview generation could not race the module-hash check. The generator check and complete business suite passed after all three browser runners.

### Documentation decision

`shared/README.md` and `docs/10-cross-channel-rules.md` now state that snapshot import and event lookups validate the complete owning domain, including peer records and created-result uniqueness, before returning a historical result. They also distinguish legitimate distinct evidence creation and new-key repeated issuance from a malformed duplicate-result history. No later slice's UI or acceptance status was advanced.

### Residual risks / uncovered areas

| Classification | Item | Owner / destination |
| --- | --- | --- |
| fixed in current slice | CR-006 aggregate domain validation omitted from replay, including its missing EVIDENCE/ENTITLEMENT old-key regressions | S1 round-4 fix; implementation/validation complete, independent re-review next |
| covered by later approved slice | Admin/client consumption of shared state and cross-surface prototype integration | S2/S3 owners |
| assigned to later work unit | Durable idempotency, concurrent quota/ID allocation, trusted caller, real payment verification and fulfillment | Production backend/security/payment/IVR owners |
| assigned to later work unit | Sequential generator partial-write recovery remains detected by `--check` and rerun | Shared generator owner; unchanged and outside this fix |
| requiring explicit user decision | Production prices/rights, renewal/stacking rules and real support contact/SOP | Product/finance/operations before release |
| assigned to later work unit | Repository has no Git remote, so later push/PR gates need a real configured destination | Repository maintainer at the push/PR gate |

No unclassified S1 round-4 fix risk remains. Next entry: independent S1 `re-review`, owned by the controller. No accepted slice commit or later gate was performed.
