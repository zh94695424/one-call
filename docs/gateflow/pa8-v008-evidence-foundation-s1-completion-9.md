# PA8 V008 Evidence Foundation S1 — review-fix 10

- Task: `pa8-s1-gpt-fix10-20261030T170956Z`.
- Local date: 2026-10-31 (America/Los_Angeles).
- Requested model/reasoning: GPT `gpt-6.1-sol` / high. This session cannot switch its model; the requested model is not claimed as verified execution metadata. No delegation.
- Verdict: the three controller-accepted bounded evidence corrections are applied. Static/source checks only; controller runtime validation and gate disposition remain pending. No gate advance or test-pass claim.

## Exact files and dispositions

1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
   - **A1 — complete platform/audit table denials:** `assertV008CrossModuleGrants` adds `platform.outbox_event`, `platform.idempotency_record`, and `audit.audit_fact` to the existing sorted parent map with empty allowed-SELECT lists. The existing table loop checks SELECT, INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, and TRIGGER for each; the existing live-column loop checks SELECT, INSERT, UPDATE, and REFERENCES, denying all four on each empty-list table. The four existing foreign-parent SELECT sets remain exactly unchanged.
   - **A1 — exact effective EXECUTE set:** `assertV008SchemaAndFunctionFences` now inventories effective `evidence_role` EXECUTE privileges against the same user-function catalog scope used by the existing global function inventory (`nspname <> information_schema`, excluding `pg*` built-ins), excluding extension members. It compares identity arguments, not names alone, to exactly `audit.append_fact` with its eleven declared arguments, `platform.outbox_append` with its eight declared arguments, and the five zero-argument `evidence` trigger functions. Existing positive and negative witnesses remain.
   - **A2 — trigger deferrability and enabled mode:** the exact seven-trigger catalog projection now includes `tgdeferrable`, `tginitdeferred`, and `tgenabled`. Both creation-cycle constraint triggers are pinned `true/true/O`; all five BEFORE triggers are pinned `false/false/O`. Existing names, operations, tables, functions, and ordering remain.
   - **Accepted plan §8.1 question:** both `v006EntitlementReservationUsageInventoryAndGuardShapesAreExact` and `v007ActivationConsentInventoryAndGuardShapesAreExact` invoke `assertV008StagedPrefixBooleansStillTrue()` in addition to the existing final-state helper. Staged captures and semantics are unchanged.
2. `docs/gateflow/pa8-v008-evidence-foundation-s1-completion-9.md`
   - This bounded completion record only. Plan, spec, reconciliation, reviews, migration, README, CI, prior reports, and historical artifacts remain untouched.

## Preserved invariants and adjudication

- Source still has exactly 54 unique `@Test` methods and exactly the same three V008 S1 method names; CI text still contains all 54 method names. No S2 tests, test framework/helper cleanup, or schema/product changes.
- Existing four foreign-parent SELECT allowlists are unchanged; exact app-function identity/signature inventory and global function inventory are not weakened.
- PostgreSQL source, schema grants, SQL, README, CI, historical migrations/artifacts, accepted plan/spec, and reconciliation are unchanged. The Java source retains its original mixed CRLF/LF distribution for unchanged lines; no whole-file normalization.
- The submitted reviews’ Windows-to-Linux copy mode finding remains rejected as an isolated transport artifact; formal Git records no mode change. No product filemode change was made.
- Other review residuals remain with their existing S2/design/service owners. No isolated cleanup beyond the accepted three source/evidence items.

## Checks actually run

- Read the accepted plan, plan-fix-3, spec reconciliation, current source and V008 migration signatures/trigger DDL, both submitted reviews, and completion8.
- Bounded static checks confirmed: 54 unique source test names; three V008 names; all source names represented in CI; both V006/V007 prefix-and-final-state call pairs; all seven trigger flag expectations; all three empty-allowlist table entries; no Git mode diff.
- `git -c core.whitespace=cr-at-eol diff --check` passed. Diff/status inspection is limited to the permitted source and completion9 artifact, plus the pre-existing untouched untracked `.codex` entry.
- No Maven, Docker, database, Flyway, compilation, or test execution was run, as required.

## Controller-owned pending validation

Controller must run the focused three V008 methods first, then full 54 Failsafe + 10 Surefire validation, confirming exact inventories and zero skips/failures/errors. Runtime evidence, any failure triage, and gate disposition remain controller-owned. No staging, commit, push, PR, remote mutation, delegation, or gate advance was performed.
