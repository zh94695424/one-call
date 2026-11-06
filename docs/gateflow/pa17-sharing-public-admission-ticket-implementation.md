# PA17 Public Admission And Ticket — Implementation Report

## Identity and status

- Work unit: `PA17-sharing-public-admission-ticket`.
- Gate: `implementation`, approved single slice S1; no later gate entered.
- Task label: `pa17-public-admission-ticket-implementation-gpt-20261106`.
- Status: **blocked** before production implementation.
- Binding inputs read directly: Active PA17 Goal Confirmation in `docs/gateflow/project-execution-control.md` and accepted `docs/gateflow/pa17-sharing-public-admission-ticket-plan.md`.
- Accepted plan checkpoint: `8d1820d0837eafb9002ce9158726c42e634660cc`.
- Control checkpoint / observed HEAD: `c8163d1b48e5cb446da9304a35c610ac49b42643`.
- Accepted plan identity verified: 396 lines, 96,741 bytes, SHA-256 `9c9b1e4138d54c704546b062ca42a08de15001c9725036cc35f276a9f4ef1f6e`. The checkpoint's plan blob has the same SHA-256.

## Blocking contract conflict: reconstruction ticket policy

No source change is proposed to resolve this without a Controller decision. The following frozen requirements cannot be implemented together as an unambiguous witness-only reconstruction contract:

1. Plan §2.2, line 34: existing-key replay uses the recorded witness and **does not recompute policy**. Lines 32 and 45 obtain approved policy on a preliminary miss, outside transactions; the replay branch at line 44 does not obtain policy.
2. Plan §2.3, line 47: `ADMISSION_GRANTED` has exactly six fields for non-member modes: `verificationMode`, `revocationGeneration`, `codeGeneration`, `grantExpiresAt`, `admissionExpiresAt`, `sessionExpiresAt`. Member mode adds exactly the two recipient IDs. Neither branch records approved ticket TTL or a ticket-policy reference.
3. Plan §4, line 131: `AdmissionRow` has exactly the seven immutable V009 columns. Its `AdmissionWitness` carries the frozen event, mode, generations, expiries and optional recipients, not ticket TTL. V009 supplies no separate ticket-policy persistence column.
4. Plan §2.4, line 53: reconstruction/reissue requires `ticketExpiresAt = min(clock_timestamp() + approved Q22 ticket TTL, sessionExpiresAt, admissionExpiresAt, current grant expires_at)` and a positive Redis TTL derived from that expiry.
5. Plan §2.4 requires same-key reconstruction after post-commit provider/cache failure, including failure before a credential or Redis ticket exists. The failure payload is also exact and contains no policy. Policy values must not be invented; V009/schema changes and additional persistence paths are forbidden.

The recorded session window can establish its own end, but it does not identify an independent approved ticket TTL. Consider two approved policies with the same session duration and different ticket durations, neither clipped by session/admission/grant expiry. Their committed admission row, witness, witness hash, grant count/revision and pre-credential-failure event can be identical. Their required reconstructed ticket expiries differ at the same replay time. Therefore the frozen durable projection alone cannot choose the required ticket expiry. A process-local policy retained from the original call does not provide durable reconstruction after that process is lost.

There is an interpretation under which “does not recompute policy” means only “does not renew/recompute the session,” while post-commit replay may independently retrieve approved ticket policy. That interpretation could supply the missing value, but it is not stated by the frozen replay branch and cannot safely be substituted for the broader prohibition under this dispatch's exact-contract/no-redesign rule.

### Controller decision needed

Owner: Controller, with Sharing Backend and Product/Security policy owners.

- Explicitly authorize a post-transaction ticket-policy lookup on replay, if that is the intended interpretation. Define whether the original or current approved ticket TTL applies, its unavailable/malformed outcome, and preserve the original session/admission expiry, no fresh proof, no recount and no transaction/provider overlap.
- Alternatively, authorize a durable original-policy witness and its exact persistence/payload/API implications. This would change frozen contracts; this implementation has not made that change.

These are decision alternatives, not implemented redesigns. No new production policy value is suggested. Merely returning 503 for every otherwise-valid reconstructed admission would not implement the required replay success path. Changing only test adapters would conceal rather than resolve the contract gap.

## Exact changed-file inventory and diff scope

Exactly one repository file is added by this task:

`docs/gateflow/pa17-sharing-public-admission-ticket-implementation.md`

None of the 37 production/test/configuration/documentation allowlist paths is changed. No V012 is added. No source, test, OpenAPI, POM, CI, README, Control, accepted plan, review or historical fix artifact is changed. The pre-existing untracked `.codex` and `.pa17-plan/` entries are preserved and were not used as implementation output locations.

The worktree's `.git` contains a Windows absolute gitdir pointer, which plain Linux `git` cannot resolve. Read-only Git inspection succeeds using `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call17` and `GIT_WORK_TREE` equal to the dispatched workspace. The pointer and Git configuration were not edited.

## Implemented contracts and state transitions

None. The POST endpoint, admission transactions, providers, replay state machine, dedicated datasource, V012 and 44 planned test methods remain unimplemented. Existing PA15/PA16 and creator source is unchanged. No claim of admission, ticket, session, ACL or concurrency runtime correctness is made.

## Actual commands, results and limitations

Commands ran from the dispatched workspace. Evidence is outside the repository under `/tmp/pa17-evidence/`; it is local scratch evidence, not a committed test artifact.

| Command / check | Actual outcome |
|---|---|
| Initial plain `git status --short` | Exit 128: unresolved Windows gitdir pointer. No repair attempted. |
| `GIT_DIR=... GIT_WORK_TREE=... git rev-parse HEAD` | Exit 0; HEAD equals the supplied Control checkpoint. |
| Git status/diff baseline with those variables | Exit 0; no tracked diff; only the two pre-existing private untracked entries. Saved as `status-before.txt` and `tracked-diff-before.txt`. |
| `wc -lc` and `sha256sum` for accepted plan | Exit 0; exact binding identity shown above. |
| `git show 8d1820d0837eafb9002ce9158726c42e634660cc:docs/gateflow/pa17-sharing-public-admission-ticket-plan.md \| sha256sum` | Exit 0; same accepted SHA-256. |
| `git ls-files -z \| xargs -0 sha256sum` | Exit 0; baseline tracked-file byte manifest saved as `tracked-before.sha256`. |
| `sha256sum backend/src/main/resources/db/migration/V00{1,2,3,4,5,6,7,8,9}* backend/src/main/resources/db/migration/V01{0,1}*` | Exit 0; raw pre-edit manifest saved as `migrations-before.sha256`; all eleven hashes match accepted §7. |
| `JAVA_HOME=/opt/jdk21`, supplied PATH; `java -version`; `/opt/maven-3.9.9/bin/mvn -version` | Exit 0; Temurin 21.0.13 and Maven 3.9.9 available. No Maven build was invoked. |
| `curl --connect-timeout 5 --max-time 10 --silent --show-error --fail http://172.20.211.232:2375/_ping` | Exit 0; `OK`. Docker daemon reachability only, not PostgreSQL/container validation. |
| `/usr/bin/time ... python3 -` synthetic contract counterexample | Exit 0; elapsed 0.02 seconds. `reconstruction-counterexample.txt` and `.time` record equal frozen durable projections, distinct required ticket expiries, and both sessions unexpired. This is an information-gap demonstration, not execution of a production service, PostgreSQL, or a new JUnit test. All durations and IDs used there are synthetic test-only inputs; none is a production policy value. |
| `/usr/bin/time ... sha256sum -c /tmp/pa17-evidence/migrations-before.sha256` | Exit 0; elapsed 0.02 seconds; all eleven migrations `OK`. Results saved as `migration-byte-check.txt` and `.time`. |

The exact environment requested by the dispatch was used for the environment probe: `JAVA_HOME=/opt/jdk21`, PATH prefixed with `/opt/jdk21/bin:/opt/maven-3.9.9/bin`, `DOCKER_HOST=tcp://172.20.211.232:2375`, `TESTCONTAINERS_HOST_OVERRIDE=172.20.208.1`. No wrapper was invoked. Environment availability is not the blocker.

Final scope audit: `sha256sum -c /tmp/pa17-evidence/tracked-before.sha256` verified all **1,440 tracked files** unchanged, exit 0, elapsed 2.08 seconds (`tracked-byte-check.txt` and `.time`). The final eleven-migration byte check also exited 0 (`migrations-final.txt`). `git diff --check` exited 0; tracked diff remained empty and status showed only this new report plus the two pre-existing private entries. `git diff --no-index --check /dev/null` against the new report exited 1 because this is a new-file comparison; it emitted no whitespace diagnostic, only the repository's LF-to-CRLF warning. An independent `python3 -` byte check for report trailing whitespace, final newline and NUL exited 0 (`report-whitespace-check.txt`). Existing plan/fix/review LF-to-CRLF warnings during Git inspection were not acted on; no normalization or configuration change was made.

### Required acceptance work not run

- Focused new unit/container tests and fresh clean full suite: not run; new source and tests are not written. There is no claim of `20/103 + 9/162 = 265`, no new Surefire/Failsafe XML identity, and no source=CI=XML validation result.
- PostgreSQL 16, Flyway clean/migrate/repeat/rollback, V012 ACL/grant/denial checks: not run. No SQLSTATE witness is claimed.
- Two-connection concurrency, commit-before-provider, rollback injection and replay recovery: not run. No backend PIDs, winners/losers, or event/row/count results exist for this task.
- Existing full-suite success or regression freedom: not claimed. Byte preservation is not a substitute for runtime tests.

## Redaction and preserved invariants

No capability token, extraction code, OTP, designated-mobile number/proof, media credential, provider credential or real request ID was obtained, generated or printed by this task. The analytical counterexample prints only fixed boolean conclusions, not its synthetic identities, payloads or durations. No production provider was called and no credential-bearing Docker operation was issued.

The original V001–V011 manifest is rechecked after this report is written. V009 is unchanged byte-for-byte: no table, column, constraint, function, trigger, lifecycle, immutability guard or existing ACL edit. The PA16 role and all pool/source definitions are unchanged. There are no application changes whose redacted `toString()` or runtime no-leak properties could be validated or claimed.

## Residuals and completion boundary

- Blocking replay ticket-policy interpretation: Controller / Sharing Backend / Product-Security; resolution required before exact implementation can proceed.
- All unimplemented S1 behavior and acceptance evidence: Sharing Backend / Database test owner, after the Controller supplies a coherent binding contract.
- Production Q22/KDF/SMS/media/Redis/deployment values and providers: existing Product/Security/Platform owners; not supplied or inferred here.
- Original media/playback non-goals and all later gates remain out of scope.

No staging, commit, branch creation, push, PR operation, approval, readiness action, reviewer request, external comment, code-review dispatch, merge or later-gate transition was performed. No Control/plan/review/prior-fix mutation was performed. The task stops **blocked**, not implementation-complete.
