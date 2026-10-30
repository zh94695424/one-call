# PA7 V007 S1 Validation / Fix Supplement

Bounded documentation-only replacement. Supersedes the prior conflicting
prompt-mapping narrative in this path. The historical blocked S1 implementation
report remains untouched:
`docs/gateflow/pa7-v007-activation-consent-foundation-s1-implementation.md`.

## 1. Identity

| Field | Value |
| --- | --- |
| Gateflow work unit | `PA7-v007-activation-consent-foundation` |
| Gate | S1 implementation validation / bounded fix supplement |
| Task label | `pa7-resume-fix-mimo26flash-20261030-113037` |
| Provider / runtime | ClaudeCode / mimo26flash / `mimo-v2.6-flash` |
| Accepted plan checkpoint | `64b576abd4efce956d51d4e57a6b8afeeb13cd45` |
| Plan SHA256 | `CB2FC46F8A6DBA94FFC69D935BBA6E6B1A4B355C05142EF5DB395583DAB0682A` |
| Java source SHA256 | `797170378328236157E32210F2C1BD97EE98F2089B35BDF241639D05B16378AA` |
| V007 migration SHA256 | `BD919CF367F6713D26C7E6F1F7148E81D275C838E5E307CDE13EBD014E17D659` (unchanged) |

## 2. Authoritative scope

Exactly V007 persistence only: 3 tables, 43 columns, 31 checks, 3 PK, 5
unique, 12 total indexes, 11 FK, 6 defaults, 2 trigger functions, 3 triggers,
and no SQL service function. No S2/API/runtimeDataSource/frontend/cloud
scope. All six V001–V006 Git blobs match the accepted plan. 48 unique test
methods. No migration changes occurred during the fix series.

## 3. Accurate change summary (cumulative S1 fix series)

- Syntax closures for three `map.put` outer-paren omissions.
- Exact sorted index comparison; corrected standalone index predicate
  `pg_constraint.conindid = c.oid` (intermediate `pg_index` attempt rejected
  by runtime).
- Exact consent source/reason CHECK literals (no comparison weakening).
- Schema-qualified privilege probes (`hasTable`/`hasColumn`/`hasFunction`).
- Table-level UPDATE asserted false while only the three granted column
  UPDATEs on `service_activation` are true.
- Exhaustive V007 owned/cross-reader/schema/column/function matrices and
  four exact foreign read sets (plan §4.7/§7.2).
- Default-inclusive OID-0 PUBLIC ACL expansion for exhaustive
  schema/table/column/function zero-PUBLIC coverage, including defaults.
- Staged V004 orphan order insertion/rollback captured before V005; staged
  V006 absence retained.
- Cumulative final V007 table/function/trigger/grant expectations updated
  for the post-V007 catalog state.
- Ten isolated local FK 23503 probes with exact named constraints and valid
  positive controls; savepoint-isolated positive transition and one head
  per subject; representative account DELETE rejected by
  `fk_consent_account`; complete rollback with no-fixture evidence.
- Owner excluded only from the `reject_row_mutation` negative EXECUTE loop;
  runtime/foreign denials retained.

## 4. Factual corrections

1. **PUBLIC string semantics.** PostgreSQL 16 privilege functions CAN
   recognize the PUBLIC pseudo-role. Do not describe string `PUBLIC` as
   unsupported. OID-0 ACL expansion is retained solely as the stronger
   exhaustive zero-PUBLIC coverage (schema/table/column/function, including
   defaults).
2. **Parent RESTRICT name.** The representative parent DELETE RESTRICT
   constraint is `fk_consent_account`, not
   `fk_activation_transition_actor_account`. The actor-account
   missing-parent probe remains covered separately.
3. **Historical runs.** Failed historical runs are history, not final pass
   evidence.
4. **This document session** has not run runtime checks and does not claim
   controller commands.

## 5. Run evidence (author/controller fix series)

All runs: ClaudeCode / mimo26flash / `mimo-v2.6-flash`. Each directory retains
`result.json`, `stderr`, and exit status. Outer exit 0 with valid JSON
`success` / `is_error=false` / `end_turn` / nonempty result and empty stderr
does NOT mean runtime checks executed; original author runtime commands were
denied and the controller handled execution. Avoid claiming individual denied
git checks passed.

| # | Focus | Directory | Permission denials |
| --- | --- | --- | --- |
| 0 | Original S1 (historical blocked runtime report; author task `pa7-v007-s1impl-mimo26flash-20261029-201433`) | `/tmp/sub-agents.pa7-s1impl.GqchQ7` | 215 |
| 1 | Syntax closures | `/tmp/sub-agents.pa7-s1fix.9GvgxY` | 19 |
| 2 | Index ordering | `/tmp/sub-agents.pa7-s1fix2.o39UOq` | 37 |
| 3 | Rejected intermediate index predicate | `/tmp/sub-agents.pa7-s1fix3.dRSx3v` | 22 |
| 4 | Correct index predicate | `/tmp/sub-agents.pa7-s1fix4.MfMjSk` | 14 |
| 5 | Consent CHECK literal | `/tmp/sub-agents.pa7-s1fix5.laYsmo` | 39 |
| 6 | Reason CHECK literal | `/tmp/sub-agents.pa7-resume-fix.daY0oV` | 1 |
| 7 | Schema qualification | `/tmp/sub-agents.pa7-resume-fix.mKXJGy` | 4 |
| 8 | ACL semantics | `/tmp/sub-agents.pa7-resume-fix.S7Xy5D` | 2 |
| 9 | Completion / cumulative / FK / ACL + isolation + owner-loop | `/tmp/sub-agents.pa7-resume-fix.NLEaYc` | 11 |
| 10 | Final representative RESTRICT marker | `/tmp/sub-agents.pa7-resume-fix.sNlsRC` | 1 |

All 11 completed with outer exit 0 and the envelope properties above.

## 6. Controller VERIFIED final evidence (2026-10-30)

1. WSL: JDK 21.0.13, Maven 3.9.9, healthy Docker 29.1.3, PostgreSQL 16.15.
   Staged migrations 4→1→1→1 through V007. Windows initial test environment
   had malformed PATH and unavailable Docker Desktop pipe; the controller
   performed no global PATH edit or destructive reset and used the WSL
   environment instead.
2. `/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke verify` finished
   `2026-10-30T11:28:45+08:00`, exit 0: default Surefire 10/0/0/0 and
   Failsafe 48/0/0/0. Full log: `backend/target/pa7-resume-full-wsl.log`;
   final Failsafe XML under `backend/target/failsafe-reports`. Focused FK log
   `backend/target/pa7-resume-fk-wsl.log` passed 1/0/0/0 immediately before
   the full run. The previous seven-method 6-pass/1-fail run is historical,
   not final evidence.
3. Independent controller static parser: source = CI = XML = 48 unique exact
   names; counts 48/0/0/0; exactly V001–V007 filenames; all six old Git
   blobs and the V007 SHA unchanged; plan §9.3-equivalent checks passed.
   `git diff --check` passed.
4. Controller bounded `java -jar` startup passed Boot 4.1.2
   (`backend/target/pa7-resume-boot.log`). Jar-index forbidden-runtime-
   dependency firewall passed (`backend/target/pa7-resume-jar-index.txt`):
   no Flyway/PostgreSQL/Testcontainers/Security/jOOQ/Hikari runtime
   libraries. Production sources and migration unchanged since these
   checks.

## 7. Finish status and residuals

**Status:** S1 implementation/runtime intake is ready for independent A/B
code review. This is NOT an accepted checkpoint, NOT S2 completion, and NOT
PR or production acceptance.

Residuals with owners:

| Residual | Owner |
| --- | --- |
| Activation/consent service orchestration, locking, semantic currentness | Backend / Activation-IVR-Catalog (future service work) |
| S2 lifecycle, checks, rollback, concurrency | PA7 S2 BackendQA |
| Real provisioning/security Q17/Q23 and cloud/media/provider integration | PlatformSecurity / Telephony (future deployment units) |

No new product policy was chosen in this supplement.
