# PA8 V008 S1 — bounded correction8 completion

- Task: `pa8-s1-gpt-fix8-20261030T153015Z`.
- Date: 2026-10-30.
- Verdict: bounded source correction complete; runtime acceptance pending controller validation. No gate advance.
- Scope: isolated snapshot only; no delegation, staging, commit, push, PR, remote mutation, migration change, or accepted-plan change.

## Exact changes

1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`: add exactly one executable line in `cleanupV008LocalRows`, immediately before the first immutable-trigger restoration:

   `statement.execute("SET CONSTRAINTS ALL IMMEDIATE");`

2. Add this completion artifact. No other repository files are changed by this correction.

The statement runs on the existing cleanup connection and transaction, still as `evidence_owner`, after artifacts, events, recordings, the record, and requested media have been deleted. Both sides of the committed reciprocal pair are therefore absent before queued constraints are validated. It drains pending deferred events before any evidence trigger-restoration DDL and leaves constraints immediate for the rest of this cleanup transaction. It does not commit, disable constraints, or change another connection or the negative-probe transaction.

## Migration evidence and remaining cleanup

- V008 lines 490–501 declare `fk_evidence_source_event` and `fk_evidence_event_record` as `DEFERRABLE INITIALLY DEFERRED`, with default NO ACTION. Their deletion checks must see the final two-sided deletion, not an intermediate half-deleted pair.
- V008 lines 403–415 declare the two deferred creation-cycle constraint triggers for INSERT / selected UPDATE events, not DELETE. V008 declares their functions SECURITY INVOKER with evidence-local queries. The drain runs before leaving `evidence_owner`, while both constraints and creation-cycle triggers remain enabled. This helper starts a fresh transaction and deletes already-committed fixtures; it does not insert/update a creation pair in that transaction.
- Inspection of all V001–V008 migration sources finds deferred declarations / constraint triggers only in V008. Its other eight FKs are immediate RESTRICT. V006 cross-module reservation/balance/grant/policy FKs are immediate RESTRICT; its grant immutability trigger is an ordinary BEFORE UPDATE OR DELETE trigger.
- Consequently, subsequent reservation/balance deletes precede grant trigger-disable DDL with no deferred constraint queue from those deletes under these migrations. Keeping ALL constraints immediate also prevents later cleanup operations from accumulating deferred events before grant trigger restoration. Existing FK-safe deletion order, owner switches, grant restoration assertion, and final commit are unchanged.
- No contradiction requiring a contract or schema change was identified by source inspection. Runtime behavior remains unverified by this correction.

## Preserved invariants

- All 54 ordered test method names and all existing negative SQLSTATE / constraint assertions are byte-identical.
- All preexisting source bytes, including historical tests/helpers and mixed line endings, are retained; the only source delta is the inserted line with CRLF.
- COMMIT-time negative probes and survival-before-cleanup assertions are unchanged. The immediate mode is confined to the independent cleanup transaction.
- Same-connection role isolation and all owner boundaries are unchanged; no extra privilege, role reset, or role switch is introduced.
- Cleanup remains atomic: immutable triggers are restored and enabled-state assertions run before the existing commit. SQL failures propagate through existing rollback handling; no new catch/ignore behavior is introduced.
- Per-ID zero-residue checks remain unchanged. No deferred FK / creation-cycle trigger is disabled, no session replication setting is used, and no commit occurs while immutable triggers are disabled.

## Actual static verification only

- Inspected `cleanupV008LocalRows`, its committed-pair caller and survival checks, role helpers, V008 FK / creation-cycle definitions, V006 remaining-cleanup constraints / grant trigger, and deferred declarations across V001–V008.
- Binary comparison against the in-memory pre-edit source proves that removing exactly the inserted line reproduces the entire original file. An ordered anchored test-method-name extraction finds the same 54 names before and after.
- Original source SHA-256: `be7d5265ad3615d3d1cbb40339971e3dddc4cbdb928f58214b43880170790f68`.
- Corrected source SHA-256: `9ca8c3a2e35df38c4a1f3e2c0e55302978222c84215678a8ac78aec196500a5f`.
- Original endings: 24,778 CRLF and 176 bare LF. Corrected endings: 24,779 CRLF and 176 bare LF.
- Bounded diff inspection shows one tracked source insertion; the new completion artifact is untracked, and the preexisting untracked `.codex` entry is untouched. `git diff --exit-code` for migrations and the accepted plan reports no changes.
- Default `git diff --check` flags the preserved CRLF on the added line as trailing whitespace; `git -c core.whitespace=cr-at-eol diff --check` passes. No line-ending normalization is applied.
- No compilation, Maven, Docker, PostgreSQL, focused test, or full-suite execution is performed here. No test-pass claim is made. Earlier correction7 results in the task description are controller-provided context, not validation of this patch.

## Residual risks and owners

- Controller: independently compile and exercise the drain under PostgreSQL 16.15, including the `evidence_owner` execution context, pending-event removal before ALTER TABLE, remaining grant DDL, successful atomic commit, restored immutable triggers, and per-ID zero residues.
- Controller: focused `v008*` rerun is pending; then full 54 container tests plus 10 unit tests and their exact report inventories are pending. The earlier focused result (3 tests, 0 failures, 1 error, 0 skips) does not establish acceptance.
- Controller: retain ownership of runtime evidence, any remaining failure triage, and gate disposition. This artifact authorizes no gate advance.
