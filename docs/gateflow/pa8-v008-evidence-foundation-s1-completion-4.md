# S1 bounded correction5 completion record

- Task label: `pa8-s1-gpt-fix5-20261030`.
- Date: 2026-10-30.
- Provider/model/reasoning: GPT / `gpt-6.1-sol` / high (task-specified execution metadata).
- Workspace: `/tmp/pa8-s1-gpt-fix5-targeted-workspace-20261030`.

## Exact two-file write scope

1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
2. `docs/gateflow/pa8-v008-evidence-foundation-s1-completion-4.md` (new)

## Changes made

- In `assertV008FunctionsAndTriggersExact`, moved the expected
  `guard_evidence_record_creation_cycle` entry before `guard_evidence_record` to
  match the controller-reported deterministic SQL result order. The query, five
  function entries, metadata, and assertion strength remain unchanged.
- In `v008InsertCallAndSessionSameTx`, inserted the inbound session with
  `(id, call_id, subject_id)`, binding the already-generated `callId`. The
  corresponding call record still follows on the same connection in the same
  transaction. No constraint, SQLSTATE, deferred reciprocal FK behavior,
  transaction boundary, rollback, or cleanup code was changed.

## Validation actually performed

- Inspected relevant source, available constraint context, and all nine helper
  call sites: the shared missing-parent probe, seven RESTRICT parent-delete
  probes, and the committed deferred parent-delete fixture. Each continues to
  receive the generated call ID and retain the existing session-ID handling.
- Inspected the source diff and repository status; only the two allowed task
  files were changed or added. The pre-existing untracked `.codex` entry was
  left untouched. Preserved the edited Java lines' existing CRLF endings.
- Ran `git diff --check`: after preserving CRLF it reports the carriage returns
  on the three added Java lines as trailing whitespace. Ran
  `git -c core.whitespace=cr-at-eol diff --check` successfully with CRLF-aware
  whitespace checking.
- Source inspection also found a separate dedicated inbound-session insert in
  the `fk_evidence_session` parent-delete probe that still omits `call_id`.
  It is outside the two requested corrections and was not changed; the helper
  correction does not address that separate insert.
- No compilation, Maven, Docker, PostgreSQL, or tests were run. No tests were
  added, no migration/spec/plan/CI/README/controller/history files were edited,
  and no delegation, commit, push, PR creation, or gate transition occurred.

## Pending controller validation

Controller focused and full-suite reruns are pending. This record makes no
claim of PostgreSQL success or passing focused/full-suite tests.
