# PA17 Public Admission/Ticket — Bounded Plan Fix 9

## Identity And Status

- Work unit: `PA17-sharing-public-admission-ticket`; gate: `fix`, round 9 after independent re-review 7.
- Task label: `pa17-public-admission-ticket-plan-fix9-gpt-20261106`; date: 2026-11-06.
- Completion status: `plan-fix-complete`. This is a plan-only correction; implementation has not started and no re-review or later gate is entered.
- Binding scope: the active Goal Confirmation in `docs/gateflow/project-execution-control.md`; only the four user-authorized corrections are applied.
- Authorized writes performed: `docs/gateflow/pa17-sharing-public-admission-ticket-plan.md` and this newly created artifact. No historical review/fix artifact is edited.

## Frozen Input And Output

| Plan version | Lines | Bytes | SHA-256 |
|---|---:|---:|---|
| Verified fix-8 input | 395 | 94,136 | `4b207f6af6c3c4fa7eb73413aae1f1db83f106c45fab4cba26e59379f5d76cc4` |
| Fix-9 output | 396 | 96,741 | `9c9b1e4138d54c704546b062ca42a08de15001c9725036cc35f276a9f4ef1f6e` |

Counts are newline-terminated UTF-8 file lines and raw bytes. The input matches the supplied frozen identity exactly. This artifact did not exist before this task; its final line/byte/SHA-256 identity is reported externally after final validation, avoiding a self-hash claim.

## Exact Changed Sections

Line numbers below refer to the target plan, not this artifact. All unlisted input lines are preserved byte-for-byte by the bounded patch.

| Section | Input lines | Output lines | Correction |
|---|---|---|---|
| §1 identity | 6–7 | 6–7 | Advance only the fix-round/task identity to fix 9 after independent re-review 7. |
| §2.3 replay transaction, step 2 | 44 | 44 | Replace blanket expired-replay session mapping with the corresponding current-state outcome in exact §2.2 order; ended session remains 410 `SHARE_SESSION_EXPIRED`. |
| §2.4 replay/reconstruction | 56 | 56 | Explicit valid unexpired replay 201; session/admission/grant expiry follows frozen current-state outcomes and exact predicate order, without a second admission. |
| §6 baseline evidence | 211 | 211 | Identify numeric aggregate Surefire guard at CI line 162 and its message at line 163 separately. |
| §6 future CI inventory | 222 | 222 | Require both guard and message `91 -> 103`; preserve `130 -> 162` and combined `221 -> 265`. |
| §9A authoritative witness rule | 376 | 376 | Existing admission plus missing/malformed/ambiguous witness is replay/reconstruction-only 503 `DEPENDENCY_UNAVAILABLE`; fresh first-witness creation remains valid. |
| §9A authoritative same-key replay rule | insertion after 376 | 377 | Sole-201 success, expired/current-state outcomes, exact predicate order and no second admission; assertions use existing named tests only. |
| §9A post-commit terminal/window rule | 378 | 379 | Only `REVOKED`/`EXPIRED` are terminal; concurrent `LOCKED` alone cannot suppress ticket issuance for this slice's committed unexpired admission. |
| §9A CI reference | 382 | 383 | Remove erroneous 162-to-163 correction; identify both correct live lines and require both values to become 103. |
| §10 completion identity/artifact | 387–388 | 388–389 | Record plan-only completion and the fix-9 artifact without rewriting historical reports. |

## Four Accepted Corrections

- **Witness integrity:** The authoritative §9A mapping is explicitly scoped to replay/reconstruction after an existing admission is found. Missing, malformed or ambiguous `ADMISSION_GRANTED` fails closed as 503 `DEPENDENCY_UNAVAILABLE`; no missing pre-insert witness failure is introduced. Fresh member admission derives recipient IDs from the trusted current caller and writes its first witness. Only existing `wrongTenantAccountOrMembershipFailsClosed` and `lostResponseRetryReconstructsWithoutSecondAdmission` receive the specified integrity/fresh-success assertions.
- **Post-commit grant state:** `REVOKED`/`EXPIRED` deny ticket issuance with their frozen current-state outcome, no `TICKET_FAILED`, no rollback and no recount. `LOCKED` is non-terminal, blocks only a new admission before insert, and alone cannot block required ticket issuance after this slice's fresh admission commit. Other current predicates remain binding. Existing `revokedExpiredLockedAndStaleGenerationMapExactly`, `noneAdmissionCommitsOneRowEventCountAndRevision`, and `ticketFailureEventIsDeterministicRedactedAndSingle` pin these distinctions. Nonpositive ticket-window handling remains one deterministic `TICKET_FAILED` with `TICKET_WINDOW_EXPIRED` and 410 `SHARE_SESSION_EXPIRED`.
- **Same-key replay:** Valid unexpired replay returns 201 after all current predicates pass. Session/admission/grant expiry returns the corresponding frozen current-state outcome in exact §2.2 predicate/error order; ended session is 410 `SHARE_SESSION_EXPIRED`. Other predicate denials retain that order; replay never inserts/recounts or renews an expired session. Existing replay, expiry and terminal methods named in §9A carry the assertions. §§2.3/2.4 agree with this authoritative rule and the sole-201 contract.
- **CI precision:** Live `.github/workflows/ci.yml:162` contains the numeric `sum(...) != 91` guard; line 163 contains `expected exactly 91 Surefire unit/module tests`. The plan requires both to become 103, not just the message. Exact inventories at lines 401–403 retain the required future arithmetic. CI itself is not edited.

## Preserved Invariants

- Exactly 37 unique, unconditional implementation paths; exact `PUBLIC_ADMISSION_FILES` 8 and `ADMISSION_IMPLEMENTATION_FILES` 10, with their original members and ordering. The entire allowlist/named-set block is byte-identical to the frozen input.
- Exactly 12 `ShareAdmissionServiceTests`, 24 `PublicShareAdmissionIT` and 8 `SharingAdmissionRoleLeastPrivilegeIT` new methods: 44 unique existing planned names, no method/path addition or rename. The exact method inventory block is byte-identical. Future `20/103 + 9/162 = 265`; baseline `19/91 + 7/130 = 221`; differences `91 -> 103`, `130 -> 162`, combined `221 -> 265`.
- V001–V011 raw SHA-256 values match all eleven frozen §7 entries. V009's grant/admission/access-event tables, constraints, lifecycle and immutability are untouched; V012 remains privilege-only. Entire §§3, 7 and 8 are byte-identical to the input.
- One end-to-end slice, unchanged Goal Confirmation, acceptance boundary, public contract, provider/deployment ownership, no production Q22 or provider/deployment values.
- Existing event identities, payloads and hashes, exact seven `TICKET_FAILED` reasons, predicate/error order, replay/T/201/race semantics and no-I/O-under-lock rules are retained, subject only to the four expressly accepted clarifications. No prior accepted finding is reopened.

## Validation Performed

- Verified frozen input line/byte/hash identity before patching; compared the exact in-memory input/output patch, including byte equality of the preserved sections and inventory blocks. Recomputed final plan identity.
- Re-read the binding Goal Confirmation, docs/14 T-S3-04/T-S3-05 and grant-guard/reconstruction rules, and OpenAPI `createPublicShareAdmission`, 201 response and `SHARE_LOCKED`/`SHARE_SESSION_EXPIRED` definitions. They distinguish non-terminal LOCKED from revoked/expired and require ended-session 410, commit-before-ticket and current-state replay checks.
- Recounted all 37 paths, both exact 8/10 named sets and membership in the allowlist, and the exact 12/24/8 method lists, including 44 unique names. Checked every newly referenced assertion method against that unchanged inventory.
- Parsed the live CI Python inventory statically, including literal sets/maps, `.update(...)` additions and subscript assignments; confirmed complete baseline 19 classes/91 Surefire methods and 7 classes/130 Failsafe methods. Checked numeric/message evidence at lines 162/163 and exact-inventory evidence at lines 401–403. Initial incomplete-map checks failed because the validation parser omitted later updates/subscript assignments; the corrected complete static parser passes. No CI code or tests were executed.
- Rehashed all eleven on-disk V001–V011 migrations and matched their recorded raw hashes. Checked all seven allowed ticket-failure reasons without changing their list.
- Searched for superseded blanket same-key 410 wording, post-commit terminal LOCKED wording, and erroneous CI guard/message line references; none remains. Reviewed complete edited clauses for partial sentences, duplicate conflicting mappings and accidental scope expansion.
- Ran `git diff --check`-equivalent text checks: no trailing space/tab, conflict markers or missing final newline in either output. No Git command/state mutation is required or claimed.

## Read-Only Evidence Identities

| Evidence | Lines | Bytes | SHA-256 |
|---|---:|---:|---|
| `docs/gateflow/project-execution-control.md` | 3,896 | 1,540,035 | `7b9547ac943dbe1e68cc066749a724ebb7225055e78cd110e68ebf9e7937e0ee` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | 4,908 | 902,580 | `207d22ab35ad8140708a5344127511cd39c2b4794d66168e82d06b9b3a7f46a9` |
| `api/openapi/onecall-v1.openapi.yaml` | 7,260 | 266,916 | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` |
| `.github/workflows/ci.yml` | 932 | 56,494 | `bc92f0d6317609e5b30f303c00466402bc9710c4f912fd7fb71859c54f46cec4` |

## Residual Risks And Stop

- Static plan validation does not prove runtime witness corruption handling, post-commit terminal/LOCKED races, ticket-event durability, or real PostgreSQL/Redis behavior. Those remain requirements of the unchanged named tests at an authorized implementation gate, not evidence claimed here.
- Existing §8 policy/provider/deployment, ACL/catalog and concurrency risks and owners are unchanged. No new production values, migration behavior, deployment assumption or public outcome is invented.
- No source, tests, migrations, OpenAPI, POM, CI, README, Control, historical artifact, private harness or Git state was written. No Maven/PostgreSQL/Redis execution, staging/commit/push/PR/merge/approval/ready/reviewer request/external comment or re-review action occurred. Stop after these two authorized documents and their static validation.
