# PA17 Public Admission Ticket Plan Fix 1

## Gate record

- Work unit: `PA17-sharing-public-admission-ticket`.
- Gate: `fix` after `plan review`.
- Task label: `pa17-public-admission-ticket-plan-fix1-gpt-20261106`.
- Status: fixed plan ready for plan re-review; implementation has not started.
- Changed paths: `docs/gateflow/pa17-sharing-public-admission-ticket-plan.md` and this single fix artifact.
- Binding scope and one-slice rationale remain aligned with the active PA17 checkpoint in `docs/gateflow/project-execution-control.md`.

## Accepted findings repaired

1. **Inventory arithmetic — fixed in plan §§4–6, 9–10.** Baseline remains 19/91 Surefire plus 7/130 Failsafe = 221. Additions are 12 Surefire + 24 + 8 Failsafe = 44. Future is 20/103 Surefire + 9/162 Failsafe = 265. The plan explicitly changes aggregate Surefire 91 at CI line 162, exact CI inventory totals at lines 401–403, and current README PA16 arithmetic at lines 40, 292, 301, and 323. All future totals are reconciled.
2. **Closed future allowlist — fixed in plan §4.** The implementation allowlist is exactly 35 unconditional paths. Optional/preferred `PublicSharesIT` and `PublicSharePersistenceConfigurationTests` entries are removed; `PublicShareWebTests` is included for the required bounded controller contract assertions.
3. **V012 migration witnesses — fixed in plan §§4 and 6.** `PostgresContainerIT`, `CustomerMeFixture`, `SharingQueryFixture`, `SharingQueryServiceIT`, and `SharingRoleLeastPrivilegeIT` have bounded assertion-only updates. Current history is 12; historical V010/V011 probes retain their own target versions and one-migration assertions. Existing test names/counts are retained.
4. **Modulith boundary — fixed in plan §§2.2 and 4.** Sharing exports named interface `public-admission`; customer IAM allows `sharing::public-admission`; sharing defines `TrustedShareAdmissionCaller` and does not import `customer_iam.TrustedCustomerCaller`. Exact edits to `CustomerApiBoundaryTests`, `ModuleBoundaryTests`, `SharingQueryBoundaryTests`, and `PublicShareWebTests` are allowlisted.
5. **V012 least privilege — fixed in plan §3.** Exact 20-column grant SELECT supports immutable SECURITY INVOKER `SELECT *`; grant UPDATE is restricted to four columns. Admission/event column SELECT and INSERT sets are enumerated without table-level grants or mutation rights. Cross-schema column sets, no foreign writes/locks, no trigger-function EXECUTE, zero function/sequence/DDL privileges, and exact expiry/count updates are frozen.
6. **Replay-before-proof — fixed in plan §§2.2–2.3.** Resolve digest/grant identity without a lock, derive exact 74-character key, then resolve durable admission and `ADMISSION_GRANTED` witness before new proof. Same-key replay ignores changed/unavailable proof, checks current policy facts, permits `LOCKED` replay while valid, and denies new keys. Raw request ID is not persisted.
7. **Success/error contract — fixed in plan §§2.4–2.5.** First success and replay both return the sole OpenAPI success `201`. Optional bearer precedence matches public GET. Policy absence returns pre-write 503/no rows; post-commit media/ticket failure returns 503 while preserving admission and enabling same-key reconstruction.
8. **Verifier/provider seams — fixed in plan §§2.2, 2.4, 4, and 6.** Admission-only code and designated-mobile verifier interfaces have sealed verified/denied/unavailable/malformed results, redacted diagnostics, fail-closed production defaults, and deterministic test adapters only. Ticket composition order and expiry bounds are explicit; no external I/O occurs under transaction/lock.
9. **Locks and event identities — fixed in plan §§2.2–2.3.** Ranks 7/8/9 are lifecycle advisory locks followed by non-locking reads only. `VERIFICATION_FAILED` uses docs/14 `share-observation:` identity/fingerprint including request-key digest and mode/result/reason. Expiry uses docs/14 `share-transition:` identity/fingerprint for `{kind: EXPIRY, expiresAt}`.
10. **Persistence gating/injection — fixed in plan §§3–4 and 6.** Dedicated datasource requires `@Profile("persistence")` and explicit enable property, uses fixed qualified beans/properties and exact `sharing_admission_role` username, redacted settings, and qualified lazy `ObjectProvider` injection. Default/web-only contexts remain database-free; no credential fallback or `SET ROLE`.
11. **Race acceptance — fixed in plan §§5–6.** The exact 24 IT methods map to revoke, rotate, expiry, lock, risk-open, member-revocation, duplicate-key, final-slot, and post-commit failure/reconstruction races. No unnamed, parameterized, or additional methods are authorized.
12. **Test class counts — fixed in plan §§4–6.** New classes remain exactly 12 Surefire, 24 Failsafe, and 8 Failsafe methods. Existing classes retain method names/counts; fixture and witness changes are assertion/setup-only.

## Validation and limitations

- Static source/CI arithmetic and current README sites were inspected; planned arithmetic recomputes to 103 + 162 = 265.
- SHA-256 for V001–V011 matches the plan manifest. V009 grant definition/trigger confirms 20 columns and SECURITY INVOKER `SELECT *`; V012 is not yet present and is constrained to privilege-only.
- Modulith annotations, exact architecture-test files, admission OpenAPI success/error surface, and existing public GET bearer precedence were inspected.
- No Maven, unit/integration test, PostgreSQL, Redis, Flyway, build, or CI runtime validation was run. PostgreSQL 16 trigger-privilege probe evidence is accepted from the supplied finding and was not rerun in this static fix gate.
- No Control, source, test, migration, OpenAPI, POM, CI, README, review, private path, Git index/ref/remote, or external system was modified. No later gate was entered.
