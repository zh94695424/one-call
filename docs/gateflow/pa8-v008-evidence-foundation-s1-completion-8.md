# PA8 V008 Evidence Foundation S1 — review-fix 9

- Task: `pa8-s1-gpt-fix9-20261030T162927Z`.
- Local date: 2026-10-31 (America/Los_Angeles).
- Requested model/reasoning: GPT `gpt-6.1-sol` / high. This session cannot switch its model; the requested model is not claimed as verified execution metadata. No delegation.
- Verdict: all five controller-accepted source/evidence corrections applied; runtime validation and gate disposition remain controller-owned. No gate advance or test-pass claim.

## Exact paths and accepted dispositions

1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
   - **A-1 corrected:** expanded the existing ACL helpers, without adding a test or a generic permission framework. Every `S1_ROLES + V004_ROLES` role is denied all seven table privileges and all four column privileges on all five evidence tables. `evidence_role` has exactly table SELECT/INSERT; every live column is checked for SELECT/INSERT/UPDATE/REFERENCES, with UPDATE allowed only on the six recording and three evidence_record columns declared in plan §3.9 / doc-14 §10.21 / current migration. Thus every immutable column and every append-only column denies UPDATE and all columns deny REFERENCES. Catalog column enumeration is ordered by `attnum` and excludes dropped/system columns.
   - Foreign schema checks prove USAGE without CREATE for platform, audit, subject_number, ivr and entitlement_metering. A sorted, local four-parent map pins the declared SELECT lists: business_subject(id), call_record(call_id,session_id,subject_id), inbound_session(id,call_id,subject_id), usage_reservation(id,call_id,subject_id). All seven table privileges are denied on each foreign parent; every live column is checked against the exact SELECT list and denies INSERT/UPDATE/REFERENCES. Existing platform/audit table denials, function fences, PUBLIC evidence, and all eight FOR SHARE/FOR UPDATE `42501` probes are retained.
   - **A-2 corrected:** FK inventory now includes child schema/table and ordered child columns, referenced schema/table and ordered parent columns, using `conrelid`, `confrelid`, ordered `conkey`/`confkey` expansion with ordinality and `pg_attribute`. Exact sorted rows pin all ten FKs, in addition to retained name sets and action/deferrability classification assertions: eight immediate RESTRICT and two initially deferred NO ACTION. Both columns of the composite source-event FK are ordered explicitly.
   - **A-5/B-1 corrected:** after the existing atomic cleanup commits, its fresh verification connection checks each artifactIds, recordingIds and mediaIds value for zero rows. An additional recordId-scoped artifact check proves no artifact remains for the input evidence record, including artifacts deleted by evidence_id rather than supplied artifact ID. Existing record/event/parent per-ID checks remain intact. The current S1 caller's three empty lists are unchanged; nonempty-list checks preserve the S2 helper contract without adding S2 coverage.
2. `backend/src/main/resources/db/migration/V008__evidence_media.sql`
   - **A-4/B-2 corrected:** moved the exact ten FK ALTER statements, in their existing order, before all five trigger functions and all seven triggers. After tables/indexes, RESET ROLE restores the migration identity for every FK; SET LOCAL ROLE evidence_owner restores function/trigger/grant ownership before function creation. The later RESET ROLE and every cross-module owner transition remain intact. This is plan §3.7/§4 and doc-14 §11.5 steps 7–8 ordering fidelity, not a change to FK clauses, function bodies, predicates, grants or commit-time assertions.
3. `backend/README.md`
   - **A-3/B-3 accepted portion corrected:** only three cumulative lines change: the introductory migration enumeration includes V008, the V008 migration-method description says eight migrations and includes V008, and cumulative creation-order/checksum/inventory wording includes V008. Historical PA7 wording and V001–V003-specific 15-function/11-trigger inventory remain unchanged; no global 40/40 substitution.
4. `docs/gateflow/pa8-v008-evidence-foundation-s1-completion-8.md`
   - New record only; no accepted plan/spec/reconciliation/review or historical implementation/completion artifact is edited.

## Source inspections and non-test checks actually run

- Read accepted plan and plan-fix-3, frozen V008 spec sections and reconciliation, implementation report, completion artifacts 1–7, and both submitted review artifacts. Inspected migration source and ownership transitions before moving SQL; inspected affected ACL, FK, deferred-probe and cleanup paths.
- Used bounded `git diff --stat`, `git diff --numstat`, unified diff, README word-diff, `rg`/`sed` source inspections and `git status --short`. The only pre-existing untracked entry is `.codex`, untouched.
- Python3 source-only audits compare the original snapshot to the candidate: all 54 ordered container method names are unchanged, unique, and equal to the unchanged CI expected set; exactly three names start with v008. No test is executed by these audits.
- Byte comparisons prove the Java prefix before the ACL helper, schema/function fence helper, foreign row-lock probes, fixtures, missing-parent/parent-delete/deferred probes, survival-before-cleanup evidence and the entire cleanup transaction through entry into its fresh verification block are unchanged. README bytes equal exactly the three intended cumulative substitutions.
- Migration source audits confirm 5 tables, 56 columns (11/15/17/5/8), 39 checks, 10 FKs, 5 functions and 7 triggers; eight RESTRICT clauses and two deferred FK declarations. All ten FK statement blocks are byte-identical and ordered as before, now preceding functions/triggers. The complete SQL line multiset is unchanged except the added RESET ROLE / SET LOCAL ROLE evidence_owner and separating blank lines. V001–V007 bytes match HEAD via read-only `git show` comparisons.
- One initial static column-count expression excluded digits in column names and reported 51; correcting the audit expression to include digits yields the frozen 56. This was an inspection-script issue, not a source or runtime failure. An initial `python` invocation was unavailable; subsequent audits used `python3`. An initial generated patch used unsupported numbered hunk headers and applied nothing; the corrected patch applied successfully.
- Original Java endings: 24,779 CRLF / 176 bare LF. Candidate: 24,810 CRLF / 176 bare LF. README remains 116 CRLF / zero bare LF; migration remains LF-only (543 → 547 lines). Unchanged line bytes are preserved; no entire-file line-ending normalization remains in the diff.
- `git -c core.whitespace=cr-at-eol diff --check` passes. The new artifact also receives a bounded no-index whitespace check. CR-aware checking respects the frozen mixed line-ending convention.
- No compilation, Maven, Docker, Flyway, PostgreSQL, focused tests or full tests are run. Prior controller/reviewer runtime results are context only, not validation of this candidate.

## Preserved invariants and adjudication limits

- No added/renamed tests; existing 54 Failsafe names, three focused V008 names and separate 10-Surefire validation target remain unchanged. CI and all other tests remain unwritten.
- Five-table/56-column/39-check/10-FK/5-function/7-trigger inventory, names, FK actions/deferrability, grants, function ownership/bodies, trigger predicates and error contracts remain intact.
- Cleanup deletion order and role boundaries are unchanged. Cleanup remains atomic; trigger disable/restore and enabled-state checks, SET CONSTRAINTS ALL IMMEDIATE drain, rollback handling and final commit are unchanged. No deferred FK or creation-cycle assertion is disabled or weakened; deferred-probe transactions and survival-before-cleanup observations are byte-identical.
- Frozen NULL-tolerant CHECK expressions and same-status revision-only behavior are not changed; design owner / later S2 adjudication retains those questions. CI Surefire-count and runtime XML provenance observations are outside accepted S1 defects and remain untouched. No new lifecycle/concurrency fixtures or tests.
- No accepted item requires a frozen-design or behavior change; none is blocked at the source-correction level. Runtime confirmation remains outstanding.
- No delegation, staging, commit, push, branch/remote mutation, PR operation or gate advancement.

## Candidate hashes

| Path | SHA-256 |
| --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `8ed77c01b06df9909205c3d8682b565b413af7c76109f430ee00d76e52dcfd59` |
| `backend/src/main/resources/db/migration/V008__evidence_media.sql` | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| `backend/README.md` | `98b76134036657d882928e356f3ae16c14ad11cc0f78f7d9fd78cfc03d08adcb` |

## Pending controller validation

Controller must independently compile and run the focused **three V008 methods first**, then the full **54 Failsafe + 10 Surefire** validation with exact report inventories and zero skips/failures/errors. Confirm live catalog query execution, exhaustive ACL expectations, migration-role/function ownership, unchanged deferred enforcement, trigger restoration, atomic cleanup and fresh-connection per-ID residue assertions. The current S1 caller uses empty artifact/recording/media lists; their nonempty S2 reuse contract is source-corrected, not runtime-proven here. Runtime evidence, any further bounded failure triage, and gate disposition belong to the controller. S2 is not started or authorized by this artifact.
