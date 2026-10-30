# PA8 V008 Evidence Foundation S1 — bounded correction 6

- Task label: `pa8-s1-gpt-fix6-20261030`.
- Date: 2026-10-30.
- Requested GPT model/reasoning: `gpt-6.1-sol`, high. This session cannot select that model; the actual runtime model identifier and reasoning setting are not exposed. No delegation was used.

## Exact two-file write scope

1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
2. `docs/gateflow/pa8-v008-evidence-foundation-s1-completion-5.md` (this new artifact)

## Changes

- Reordered only the expected function inventory in `assertV008FunctionsAndTriggersExact` to match the existing query's `ORDER BY 1`: `guard_evidence_event_creation_cycle`, `guard_evidence_record_creation_cycle`, `guard_evidence_record`, `guard_recording`, `reject_row_mutation`. The query and exact inventory assertion remain intact.
- Completed both V008-local inbound-session insertions with `trusted_caller=false`, `access_number_masked`, `node_id`, `protocol_version`, and `prompt_version`. Metadata uses explicitly synthetic nonblank values within V005 length limits; nullable caller ciphertext, HMAC, and masked material remain absent.
- Gave the dedicated `fk_evidence_session` RESTRICT probe's `sessionOnly` fixture its own synthetic call ID, distinct from the helper-created call binding. The evidence record retains the valid helper-created call ID and references only the dedicated session through its session reference.
- Kept the shared helper's session and call-record inserts on the supplied connection in the same transaction, with its generated call ID unchanged. No SQLSTATE/constraint assertions, role switching, rollback/savepoints, trigger restoration, or cleanup behavior were changed.

## Validation actually performed

- Inspected relevant V005 inbound-session definitions/checks and V008 source, including both inbound-session insertions, all shared helper call sites, and their transaction/rollback/cleanup paths.
- Performed non-test source checks: reviewed the scoped diff and repository status; checked the bounded edits and preservation of existing mixed CRLF/LF line endings against the original source. Default `git diff --check` flags the preserved CRLF endings as trailing whitespace; the CRLF-aware `git -c core.whitespace=cr-at-eol diff --check` passes.
- No tests were added. No compilation, Maven, Docker, PostgreSQL, or tests were run. No migration, schema, spec, plan, CI, README, controller, or history files were changed; no commit, push, PR, or gate transition was performed.
- The supplied previous focused-run result (3 tests, 1 failure, 1 error, 0 skipped; ACL passed) is controller-reported context, not a result of this correction. Its external log was not inspected.
- Controller compilation and focused/full-suite reruns are pending. PostgreSQL success is not claimed.
