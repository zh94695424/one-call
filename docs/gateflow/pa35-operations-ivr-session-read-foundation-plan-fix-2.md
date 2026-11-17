# PA35 bounded plan fix 2 — author evidence and Controller intake

Task label: `pa35-operations-ivr-session-read-plan-fix2-gpt-20261115-01`  
Work unit: `PA35-operations-ivr-session-read-foundation`  
Date: 2026-11-15 (Asia/Shanghai)  
Gate: bounded `plan fix2` only; not implementation or independent Planreview.

## 1. Scope and exact plan transition

Only these two authorized outputs change:
- `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md`
- `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan-fix-2.md`

The cumulative plan is complete, non-conditional and directly implementable
as one S1 slice, with no blocking open question. This is an author correction,
not a Planreview acceptance or permission to implement before the next gate.

| Cumulative plan measurement | Before fix2 | After fix2 |
|---|---|---|
| Raw SHA-256 | `16cffe417742d747a59d9b331ff80c67f3c6d12cbb5435dc18f04fecbd474c51` | `79561fd08d1fd4ee99d8340381ae52fc14479526eadd02a8a472ee482a1dfea3` |
| LF-normalized SHA-256 | `16cffe417742d747a59d9b331ff80c67f3c6d12cbb5435dc18f04fecbd474c51` | `79561fd08d1fd4ee99d8340381ae52fc14479526eadd02a8a472ee482a1dfea3` |
| Raw bytes | 52,946 | 61,496 |
| Lines | 737 | 853 |

Both plan versions use LF with a final newline; normalization replaces CRLF
with LF only. Delta: +8,550 bytes/+116 lines. Measurements use current on-disk
bytes, not historical report values. This evidence document's own hash is
reported separately to avoid a self-referential hash.

## 2. Exact accepted-finding dispositions

**PA35-PLAN-FIX-03 — author-corrected; false blocker removed.** Plan §2 and
§5.1 implement the confirmed public taskId filter with the exact V005 chain:
inbound_session.id=call_record.session_id, then
call_record.task_id=outbound_call_task.id, then public
outbound_call_task.task_id equals the bound text filter. The IVR owner emits
an EXISTS semijoin only for a present taskId, projects only the twelve safe
inbound-session summary columns and emits no call/task join or predicate when
taskId is absent. Sessions without calls/tasks therefore remain representable
without the filter. Literal root call_id/subject_id/status filters, SQL aliases,
bind order/types, keyset ordering and realistic frozen-index expectations are
fixed. No UUID/text comparison, dropped filter, invented column or migration.
The earlier fix1 conflict narrative is superseded; its historical artifact
remains byte-identical. No actual schema/contract mismatch was found.

**PA35-PLAN-FIX-04 — author-corrected; owner SQL and exact edge fixed.** Plan
§3/§7 move the same two new production adapters, without wrappers or additions:
- `backend/src/main/java/com/onecall/core/ivr/internal/operationsread/JooqOperationsIvrSessionReader.java`
- `backend/src/main/java/com/onecall/core/evidence/internal/operationsread/JooqOperationsIvrEvidenceReader.java`

Both implement distinct narrow ports under the existing
operations_iam::operations-subject-read named interface, with existing qualified
DSL/context resources and unique reader bean names. Operations service depends
on those ports only, never owner classes or foreign-schema SQL. Owner adapters
enter their own scope and restore Operations; service never double-enters an
owner scope. Detail's full IVR chain stays in ivr_role, then a separate
evidence_role read correlates only evidence_record.call_id/session_id and
artifact membership, never joining IVR/foreign tables or reading media.

Existing ivr/package-info.java gains exactly
operations_iam::operations-subject-read. Evidence already allows that interface;
its descriptor and Operations' descriptor remain unchanged. Existing
ModuleBoundaryTests EXPECTED_ALLOWED gains the exact IVR edge and its discovered
target multiset changes12→13 with Operations occurrences5→6 (seven other target
occurrences unchanged). This is not the total number of allowed descriptor
permissions. No reverse Operations→IVR/Evidence compile edge or new named
interface. The unrelated customer allowed-dependency size12 remains unchanged.

**PA35-PLAN-FIX-05 — author-corrected; existing tests included in lockstep.**
Plan §7 adds all three exact existing paths to S1:
- `backend/src/test/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadServiceTests.java`: retain `ownerHandoffsAssertAndRestoreLiteralRolesOnOneResource`; exact nested enum assertion gains IVR after ORDER, matching production's appended enum constant.
- `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadRoleLeastPrivilegeIT.java`: retain `normalAndExceptionalHandoffsRestoreOperationsRole`; replace Role.values() with the explicit PA33 fixture roles SUBJECT,ACTIVATION,TRIAL,EVIDENCE,ORDER, excluding IVR. Retain normal/exception restoration assertions; do not grant that fixture IVR. PA35 H separately proves IVR handoff on its own disposable login.
- `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`: update the exact map and edge assertions described above inside existing methods; preserve all nine test identities.

No new or renamed existing test identity results. Direct source scan confirms
these classes contain16,12,9 ordinary methods respectively:37 retained methods,
not additions. The future PA35 F7 identity now explicitly names the thirteen-edge
expectation; it is a planned new method, not a renamed existing source method.

**PA35-PLAN-FIX-01 and accepted parts of FIX-02 — retained.** No ACL blocker,
V018, new production grant, schema role or privilege delta. Disposable fixture
membership uses existing roles; external production credential provisioning
remains fail-closed, owner-routed deployment work. Reuse
operationsSubjectReadContext, its existing datasource/pool/DSL/transaction and
shared cursor ring; add only Role.IVR, not context mechanics. Endpoint-specific
purpose/AAD prevents cross-endpoint cursor use while retained shared keys verify
rotation. Exact DTO/literal-column/required-null/omission mappings, deterministic
ordering, fixed authority/error precedence, one snapshot, one slice and no media
remain. All FIX-02 manifests and inventory decisions are now completed with the
owner paths, descriptor and existing-test locksteps, not conditional dispatch.

## 3. Recomputed paths, test identities and inventory arithmetic

The exact future S1 allowlist is cumulative-plan §7, rows01–29:

| Category | New | Existing | Total |
|---|---:|---:|---:|
| Production (including existing IVR descriptor) | 9 | 4 | 13 |
| Test classes/helpers | 10 | 3 | 13 |
| CI/POM | 0 | 2 | 2 |
| Implementation report | 1 | 0 | 1 |
| Total | 20 | 9 | 29 |

Before25=20 new+5 existing; after29=20 new+9 existing. Moving the two adapter
paths changes location, not count. Four added existing paths are the IVR
descriptor and the three lockstep tests. The10 new test/helper paths remain
8 test classes+2 zero-test helpers. Existing paths were verified present; all20
new paths are absent in current source. CI, POM and implementation report remain
explicit manifest rows. These two current gate documents are separate from S1;
Control, prior artifacts and independent review outputs are not implementation
allowlist entries or writable here.

Current bytes were rescanned with CI's ordinary-method regex
`@Test\s+(?:public\s+)?void\s+(\w+)\s*\(`. CI dictionary construction was
evaluated statically (no XML/report checks executed); exact per-class method
sets equal current source for both runners. POM XML was parsed directly and
its31 Failsafe includes equal the CI/source IT class set. Current source has
76/704 Surefire +31/504 Failsafe =107/1208; token-lines1209 include the one
preserved PostgresContainerIT comment hit. No new runtime XML is claimed.

| Verification set | Surefire classes/methods | Failsafe classes/methods | Combined classes/methods |
|---|---|---|---|
| Current source=CI; POM31 IT includes | 76/704 | 31/504 | 107/1208 |
| New PA35 A–H only | 6/76 | 2/36 | 8/112 |
| Existing lockstep classes (already baseline) | 2/25 | 1/12 | 3/37 |
| Required focused future command | 8/101 | 3/48 | 11/149 |
| Full future source=CI=POM=XML | 82/780 | 33/540 | 115/1320 |

New exact ordinary-method counts are A12+B12+C18+D16+E10+F8=76 and
G24+H12=36; total112. Existing locksteps add zero classes, identities or token
lines. Token-lines1209+112=1321. POM31+2=33 explicit includes, exactly
OperationsIvrSessionReadsIT and OperationsIvrSessionReadRoleLeastPrivilegeIT;
no dependency/plugin/profile changes or Surefire include changes.

CI adds the exact §8 identities under pa35_unit_additions and
pa35_container_additions, preserves all existing named identities, and
subtracts only the new PA35 names from every pre-PA35 historical count.
Historical method totals remain290→320→360→400→445→480→523→543→555→613→677
→753→838→924→1014→1120→1208; historical token totals remain291→321→361→401
→446→481→524→544→556→614→678→754→839→925→1015→1121→1209.
New pa35_start_totals=[704,504], start token1209; final method_totals=[780,540],
total1320/token1321. Existing37 lockstep identities are not new additions or
historical subtractions. Update early Surefire aggregate704→780 as well as
the exact_inventory780/540 calls, class assertions82/33, POM33 and diagnostic
text. Do not weaken any historical subtraction, comment exception or regex.

Plan §11's future focused command includes all11 exact classes (the8 new plus
the3 existing lockstep classes); full verify covers115 classes. Require exact
XML method sets and zero failures/errors/skips for each, with the focused set
equal to the full run's subset for those11 classes. Existing JAR smokes,
dependency firewall and A1–A5 remain future implementation gates. No Maven,
build, container, dependency installation or runtime test ran at this gate.

## 4. Preserved inputs and static scope proof

Directly inspected active PA35 Control, cumulative plan/fix1, frozen OpenAPI
routes/DTOs, docs14/15/17, V005/V008 constraints/FKs/indexes/grants, current
PA33/PA34 source/tests/owner adapters/context/configuration/cursor patterns,
module descriptors, ModuleBoundaryTests, CI and POM. Historical implementation
reports are not substituted for current source inventories or hashes.

All21 cumulative-plan §10 raw/LF SHA-256 pairs were recomputed against current
bytes and match: one OpenAPI+three design docs+17 migrations. Exactly17 V001–V017
filenames remain; no V018. The unchanged table contains the exact measured
pairs, not assumed hashes. Additional preserved raw SHA-256 inputs:

| Input | Raw SHA-256 |
|---|---|
| `docs/gateflow/project-execution-control.md` | `e502d89967a54dd816c6abf2f54866a33df1ecfa74e3a3680a541aea25e284a1` |
| `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan-fix-1.md` | `c5be3390fd5c541c49ed6aad6362d140689c8d2d984fe173c48e40234c3f0016` |
| `.github/workflows/ci.yml` | `2c4addc3a09c2418869c6252e06be9257011d0fc34751289db6c15292887faf0` |
| `backend/pom.xml` | `6a0e01aa11959149a55cd731118809d9efc9231ee11fd645292d490a2d9ea19a` |

Git status cannot operate because this mounted checkout's existing .git pointer
targets `D:/codex-call/.git/worktrees/codex-call20`. It was not repaired or
modified. Filesystem scope evidence instead hashes every sorted file path,
then NUL, then its raw SHA-256 digest bytes, excluding exactly the two authorized
outputs above. Before fix2:2,114 preserved files, digest
`f75b7813a8d071ec600028aef9407bb5e281f3839ecb8962b4690523c8ffb4b3`.
Final scope verification must reproduce this exact count and digest. This
digest includes Control, fix1, source/tests, CI/POM and existing private files;
no private helper or other repository path is written. Only read-only inline
static checks and authorized apply_patch edits are used; no helper file is
created. Final scope/plan/artifact hash verification is reported at handoff.

## 5. Mandatory stop

Stop now for Controller intake and fresh independent A/B Planreview. The author
claims the three findings corrected in the plan, not independently accepted.
No Control/source/test/migration/OpenAPI/docs14/15/17/CI/POM/private-path edit,
build/container, stage/commit/push, PR mutation, merge, approval, mark-ready,
reviewer request or external comment is performed or authorized. No runtime,
hosted-CI, deployment, live/cloud/media or production-readiness claim is made.
External identity/credential/key provisioning and future verification remain
owner-routed residuals, not blocking plan branches. If a genuine frozen-schema
or contract mismatch/new privilege need appears later, stop for authorization
rather than fabricate a mapping or widen S1.
