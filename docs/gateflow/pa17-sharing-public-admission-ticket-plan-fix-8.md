# PA17 Public Admission/Ticket — Bounded Plan Fix 8

## Contract And Status

- Work unit: `PA17-sharing-public-admission-ticket`; Gateflow gate `fix`, round 8 after re-review 6.
- Task label: `pa17-public-admission-ticket-plan-fix8-gpt-20261106`; date: 2026-11-06.
- Status: accepted bounded corrections repaired at plan level only. No later gate is entered; implementation is not started.
- Authorized writes: only `docs/gateflow/pa17-sharing-public-admission-ticket-plan.md` and this new artifact.

## Plan Identity

| Version | Lines | Bytes | SHA-256 |
|---|---:|---:|---|
| Verified fix-7 input | 395 | 93,475 | `36169DD2C3B027326A4CE6EBB377E75C48EEBA893C5B92973BEF0DBC4BC89806` |
| Fix-8 output | 395 | 94,136 | `4b207f6af6c3c4fa7eb73413aae1f1db83f106c45fab4cba26e59379f5d76cc4` |

The output identity is stated here rather than self-hashed into this artifact.

## Repairs

- **Fresh member admission versus replay:** Fresh `ORGANIZATION_MEMBER` admission takes recipient organization/account from the trusted current caller and writes the first `ADMISSION_GRANTED` witness without requiring a pre-insert witness. Account/recipient, membership-pair/witness, and grant-owner/witness equality checks apply only after an existing witness is found for replay/reconstruction. Existing `wrongTenantAccountOrMembershipFailsClosed` and `lostResponseRetryReconstructsWithoutSecondAdmission` now distinguish fresh success from replay/reconstruction binding.
- **Post-commit grant-terminal response:** If the grant becomes `REVOKED`, `EXPIRED`, or `LOCKED` as applicable after admission commit and before ticket issue, map that current-state terminal outcome and issue no ticket. Do not append `TICKET_FAILED` for the terminal outcome; reserve it for credential/ticket/Redis/event failure and nonpositive ticket-window. Preserve admission and do not recount.
- **V012 expiry wording:** Expiry sets exactly `status`, `revocation_generation = OLD+1`, and `revision = OLD+1`; code generation and admission count remain unchanged.
- **Completion artifact lineage:** The plan names `docs/gateflow/pa17-sharing-public-admission-ticket-plan-fix-7.md` for the frozen fix-7 artifact; this report is the fix-8 artifact. Unrelated stale fix-6 references were removed.

## Preservation And Validation

- Preserved the goal/non-goals and one-slice scope; 37 paths, 8 API / 10 implementation paths, 12/24/8 method counts, `20/103 + 9/162 = 265`, V001–V011, V012 privilege boundary, and all prior exact contracts.
- Verified the input plan matched the supplied 395-line, 93,475-byte SHA-256 before edits. Recounted output lines/bytes and recomputed the output SHA-256; reviewed the four repaired clauses and stale-reference search. The Git metadata was unavailable in this shell, so no Git-state claim is made.
- Static/document checks only. No runtime, build, CI, or test claims are made.
- Only the target plan and this fix-8 report were written. No other path or external state was mutated.
