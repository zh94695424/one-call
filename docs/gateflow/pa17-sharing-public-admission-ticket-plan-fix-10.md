# PA17 Public Admission And Ticket — Bounded Plan Fix 10

## Identity and result

- Work unit: `PA17-sharing-public-admission-ticket`.
- Task label: `pa17-public-admission-ticket-plan-fix10-gpt-20261106`.
- Date: 2026-11-06; gate: bounded `fix` after implementation attempt 1 blocked.
- Status: **plan-fix-complete**, documentation only. This resolves the accepted replay ticket-policy contract gap; it does not accept the amended plan, authorize implementation, or enter re-review.
- Binding authority: Control's Active PA17 Goal Confirmation and implementation-blocker adjudication, especially Control lines 87–90; the blocked implementation report remains unchanged historical evidence.
- Exactly two task outputs: the amended plan and this new artifact. No source, test, migration, OpenAPI, CI, README, Control, review, historical fix, private file or Git state was modified by this task.

## Input and output identities

Counts and SHA-256 below are over actual file bytes, with LF-counted lines; no newline conversion was performed. Read-only `git show` independently matched the input plan to accepted checkpoint `8d1820d0837eafb9002ce9158726c42e634660cc`.

| Document | Lines | Bytes | SHA-256 |
|---|---:|---:|---|
| Accepted input plan | 396 | 96,741 | `9c9b1e4138d54c704546b062ca42a08de15001c9725036cc35f276a9f4ef1f6e` |
| Output `pa17-sharing-public-admission-ticket-plan.md` | 404 | 106,414 | `b8d0b99d639c5d99df06a5b4035d76e7e626960e5c1414e14d656ded21711025` |
| Binding `project-execution-control.md`, unchanged | 3,908 | 1,549,328 | `ec9a4e34d1239e0b291c779a2fe72377b5a08cee7f881f3beb986cc436b9ba1b` |
| Blocked implementation report, unchanged | 93 | 11,663 | `df593c7e5e52ca679fbf04e9edec007a4e56d2d25bd806fbaa653ea99be9dd21` |
| `docs/14-production-data-model-and-flyway-schema-spec.md`, unchanged | 4,908 | 902,580 | `207d22ab35ad8140708a5344127511cd39c2b4794d66168e82d06b9b3a7f46a9` |
| `api/openapi/onecall-v1.openapi.yaml`, unchanged | 7,260 | 266,916 | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` |

This artifact has no input counterpart. Its final line/byte/SHA-256 identity is reported in the completion response rather than embedding a self-referential digest.

## Design-source evidence and bounded proof

- docs/14 T-S3-18 (line 4755) requires original-admission replay after lost response/cache, exactly one count and `ADMISSION_GRANTED`, and no second session after expiry. T-S3-21 (line 4758) requires every issued ticket's current-time Q22 TTL minimum, positive Redis TTL only, and no admission/session extension when the grant is extended.
- docs/14 T-S3-24 (line 4761) requires admission/event/count commit before Redis, a durable ticket failure for Redis failure, and no recount on recovery. T-S3-25 (line 4762) preserves recipient authentication, lifecycle ranks 7/8/9, all current-state/generation/risk/member checks, transaction closure before external I/O, and deterministic original identities.
- docs/14 §9.31 (lines 3420–3426) defines `ticketExpiresAt = min(clock_timestamp() + approved Q22 ticket TTL, sessionExpiresAt, admissionExpiresAt, current grant expires_at)` for issue/reconstruction and bounds Redis TTL by the positive remaining difference. Its reconstruction rule requires the original witness and current grant/session/admission checks before closing PostgreSQL and reissuing.
- OpenAPI `createPublicShareAdmission` (line 3701; description at 3703–3724 and 503 response at 3797) explicitly permits post-commit unavailable ticket/session policy to fail closed with `SHARE_SESSION_POLICY_UNAVAILABLE`/`DEPENDENCY_UNAVAILABLE`, without recount or re-rolling, with same-key reissue. No OpenAPI edit is needed.
- Let original persisted expiries be `S` and `A`, current grant expiry be `G`, current issue time be `N`, and fresh approved ticket TTL be `D`. Reconstruction computes only ephemeral `E = min(N + D, S, A, G)`; hence `E <= S`, `E <= A`, and `E <= G` regardless of later policy changes. The service does not assign `S`, `A`, or a grant expiry on replay. A longer or shorter later approved `D` may change `E`, not a persisted window. The original admission-time policy remains relevant only through its already-persisted session/admission witness; it need not be recovered as an original ticket TTL.
- Preserve the original fresh-admission window formula and exact six-field non-member/eight-field member `ADMISSION_GRANTED` witness, seven-column immutable admission row, and ticket event payloads. Replay consumes the fresh current TTL only for ephemeral ticket issuance/reconstruction; no original TTL, policy reference, extra schema/event field, production value or new path is introduced.

## Exact changed sections

All line references in this table use the respective input/output plan identities above. Unlisted content is preserved.

| Section | Input lines | Output lines | Exact correction |
|---|---|---|---|
| §1 identity | 6–7 | 6–7 | Fix-10 task/gate after the blocked implementation attempt. |
| §2.2 policy/replay | 34 | 34 | Replace the overbroad policy prohibition with immutable persisted windows plus exactly one post-transaction current ticket-policy lookup and distinct failure behavior. |
| §2.3 ordering | 44–46 | 44–46 | Successful replay closes PostgreSQL/locks before policy; fresh policy failure precedes durable write; preliminary-miss race discards precomputed policy for reconstruction and performs one post-transaction lookup. Fresh true insertion needs no second lookup. |
| §2.4 ticket/failure/reconstruction | 52, 54, 56 | 52, 54, 56 | Policy approval precedes ticket operation; policy absence is not a ticket-failure reason; reconstruct using current approved TTL and unchanged original windows. Formula, seven reasons and event payloads remain unchanged. |
| §2.5 precedence | 61 | 61 | Current-state/witness replay denials precede policy; unavailable/malformed policy maps to exact 503 with no ticket operation or failed event. |
| §4 provider contract | 140 | 140 | Keep `ticketPolicy(SharePolicyRequest)` and its existing result types; freeze fresh/replay call timing, exactly-once replay lookup, malformed handling and no original-TTL persistence. |
| §5 acceptance map | 281 | 281 | Qualify no-rows assertion as fresh-admission behavior and point to replay-policy assertions. Race/method mapping is retained. |
| §6 existing-method assertions | insertion before 309 | 309–315 | Add one seven-line assertion subsection, using only the four existing methods specified below. |
| §8 ticket residual | 342 | 349 | Distinguish post-ticket-operation failure audit from pre-ticket replay policy failure without `TICKET_FAILED`. |
| §9A authority | 374, insertion before 377, 379 | 381, 384, 387 | Make fix-10 authoritative; state the full lookup/window/failure contract; exclude policy absence from ticket-failure events alongside existing terminal denial exclusion. |
| §10 completion | 388–389 | 396–397 | Update task/gate history and fix-10 artifact reference; retain implementation stop. |

The four existing methods receive these exact planned assertions, not executable changes in this gate:

- `ticketPolicyUnavailableBeforeAdmissionLeavesNoRows`: unavailable/malformed fresh policy is 503/no admission/count/event rows; valid replay policy failure is post-authoritative-transaction 503 with exactly one lookup, unchanged admission/witness/count/revision/windows/events and zero ticket I/O. Current-state/witness denials win before lookup; policy doubles assert transaction/lock closure.
- `lostResponseRetryReconstructsWithoutSecondAdmission`: successful reconstruction performs one fresh current-policy lookup before credential/ticket/Redis I/O; approved test TTL changes and grant extension alter only ephemeral expiry under the minimum. Witness/windows remain byte-identical, success remains 201, proof is not repeated, identity/count/event invariants hold, and a preliminary-miss race performs one new post-transaction lookup for replay.
- `redisUnavailableAfterCommitReturns503AndPersistsAdmission`: policy success precedes Redis failure with existing `TICKET_WRITE_UNAVAILABLE`; a subsequent replay-policy failure adds no event or ticket I/O; later approved-policy recovery looks up afresh and reconstructs without recounting.
- `ticketFailureEventIsDeterministicRedactedAndSingle`: neither fresh nor replay unavailable/malformed policy appends `TICKET_FAILED`, including no `INTERNAL_FAILURE` substitution. Once ticket operation starts, the seven reasons, exact event identities/payload/hash, single-event reuse/conflict, redaction, nonpositive-window 410 and separate success event remain frozen.

## Preservation validation

- Recounted exactly **37 distinct unconditional implementation paths**, `PUBLIC_ADMISSION_FILES` **8**, `ADMISSION_IMPLEMENTATION_FILES` **10**; both exact sets and all allowlist entries match the accepted input. No implementation path was created.
- Recounted exact named new-method inventories: **12 Surefire + 24 Failsafe + 8 Failsafe = 44**. The three inventory lists are byte-identical to the accepted input; no method was added, removed, renamed or moved to a different class.
- Parsed the existing CI Python inventories as data and compared every named method to Java source using CI's `@Test` method regex: **19/91 Surefire + 7/130 Failsafe = 221**. Arithmetic remains **91 + 12 = 103**, **130 + 24 + 8 = 162**, **20/103 + 9/162 = 265**. This is static source/CI validation, not execution or new XML evidence.
- Exact input/output block comparisons preserve §3/V012 privilege-only scope, §4's allowlist, both named sets, §5's named-method lists, the complete 18-row error table, and §7's migration hash table. The fixed-9 witness/terminal/current-state/LOCKED repairs, sole 201 success, lifecycle lock ranks, generation/risk/member checks, exact event identities/payloads/redaction, race map and one end-to-end slice are retained; only the policy distinction/associated assertions are clarified.
- Searched all plan policy/provider/recompute/renewal, pre-/post-commit absence, and `TICKET_FAILED` wording. No `does not recompute policy`, unqualified `Policy absence is pre-write`, or blanket `Any failure uses` assertion remains. Policy absence is distinct from the seven ticket failure reasons; session expiry, terminal denial and nonpositive ticket-window behavior retain their frozen mappings.
- Raw V001–V011 SHA-256 values all match accepted §7 (not merely normalized SQL); V009's three tables/lifecycle are unchanged. No V012 was created and its future privilege-only contract remains byte-identical.
- A before/after protected-file manifest covers **1,440 regular repository files**, excluding the two authorized outputs, `.git`, `.codex` and `.pa17-*` private inputs. Sort by relative path and hash concatenated `path + NUL + raw_file_sha256 + LF`: both manifests equal `05d43f4897996126f6d8817a764cfcce8daef09840239d8caef90021ccdac79c`. This proves no task mutation of Control, blocked report, design/OpenAPI, source/tests/migrations/CI/README, reviews or historical fixes. Excluded private/Git paths were not written; no claim of a private-state manifest is made.
- Plain Linux Git initially failed on the existing Windows gitdir pointer. Read-only Git with `GIT_OPTIONAL_LOCKS=0`, explicit `GIT_DIR` and `GIT_WORK_TREE` succeeded without pointer/config/index edits. The full tracked diff already includes Controller's binding Control update relative to HEAD; that is not a task edit and was not reverted. The protected manifest confirms its bytes stayed unchanged. Scoped `git diff --check` passes for the amended plan; artifact whitespace is checked separately before completion.

### Recomputed raw migration hashes

| Version | Raw bytes | Raw SHA-256, equal to accepted §7 |
|---|---:|---|
| V001 | 25,597 | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| V002 | 16,799 | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| V003 | 12,873 | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| V004 | 26,195 | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| V005 | 33,724 | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| V006 | 35,016 | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| V007 | 14,486 | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |
| V008 | 22,488 | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` |
| V009 | 19,320 | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` |
| V010 | 24,954 | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` |
| V011 | 14,132 | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` |

## Residual risks and stop

- Production Q22 policy values, provider/KDF/mobile-proof/Redis/media formats and deployment credentials remain absent and assigned to the existing Product/Security/Sharing Backend/Platform owners. Defaults remain unavailable; no production value is selected.
- The source-supported contract is now unambiguous at plan level. Executable transaction/lock timing, policy call counts, current-state races, ACLs, event recovery and TTL clipping still require the existing named tests at a separately authorized implementation gate. No Maven, PostgreSQL, Redis, container, media or network runtime command was run here, and no runtime correctness or green test result is claimed.
- Existing bounded post-issue exposure, event-store/delete failure, capacity/pool and Controller-owned provenance risks retain their owners/destinations; fresh policy availability does not promise recall of previously issued credentials or bypass any current-state check.
- Stop after these two documents. No implementation, staging, commit, push, PR mutation, merge, approval, ready action, reviewer request, external comment or re-review was performed or authorized by this artifact.
