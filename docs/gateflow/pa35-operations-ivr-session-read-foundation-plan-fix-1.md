# PA35 bounded plan fix 1 — author evidence and Controller intake

Task label: `pa35-operations-ivr-session-read-plan-fix1-gpt-20261115-01`  
Work unit: `PA35-operations-ivr-session-read-foundation`  
Date: 2026-11-15 (Asia/Shanghai)  
Gate: bounded `plan fix` only; not independent Planreview or implementation.

## Authorized outputs and baseline

Only the existing cumulative plan and this new fix-1 evidence document change:
- `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md`
- `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan-fix-1.md`

Input plan raw SHA-256:
`2b3f34bb0725175f2a18e257baf6cfc26e154f3623163293c3465061fc5ba458`.
Directly read active PA35 Control, the original plan, OpenAPI routes/DTOs,
docs14/15/17, V005/V008 and current PA33/PA34 Operations code/tests, CI and POM.
No historical report is substituted for current source counts or frozen hashes.

## Accepted findings

**PA35-PLAN-FIX-01 — author-corrected.** Removed the erroneous ACL blocker,
V018/new-public-grant proposal and conditional ACL implementation design.
PA34 fixture already grants Operations/Evidence memberships. PA35's own
disposable login grants existing Operations/IVR/Evidence roles using ADMIN
FALSE, INHERIT FALSE, SET TRUE. Only the existing context enum gains IVR;
EVIDENCE remains. Production credential/membership delivery is an external
owner-routed fail-closed prerequisite, not an implementation-plan blocker.
No schema role/grant migration or new production privilege is needed.

**PA35-PLAN-FIX-02 — concrete decisions/inventories supplied; readiness not
claimed because direct inspection proved a separate incompatible instruction.**
The cumulative plan now fixes DTO fields/literal columns, omissions, required
nullable fields, nested ordering including NULL sequence, evidence selection,
authority/error precedence, exact existing qualified infrastructure, one shared
ring and one AES-GCM envelope with authenticated digest mismatch distinction.
It supplies25 intended S1 paths (20 new/5 existing), all112 ordinary test
identities across8 classes (6 unit/2 IT), exact CI dictionary additions and
POM include delta, source/token/history/XML arithmetic,21 current raw/LF frozen
hash pairs, exact future focused/full commands and JAR/firewall/A1–A5 gates.
One behaviorally coherent two-endpoint slice remains; no runtime execution is
claimed. Query adapters live in Operations internals with narrow per-owner
SQL, retaining the existing named interface/module graph and avoiding any
unlisted module-descriptor or old boundary-test change.

## Demonstrated incompatibility requiring Controller clarification

The instruction requires taskId filtering AND list SQL reading only
ivr.inbound_session. V005 lines92–145 / docs14 §4.24 prove that table has no
task column. The existing link is call_record.task_id (UUID at V005 line157)
→outbound_call_task.id (FK at670–673), while public taskId is the separate
outbound_call_task.task_id text1–64 (line16). It cannot be compared to a root
column that does not exist. Following the real association requires reading
two other tables, contrary to the explicit inbound-only restriction.

This evidence does not revive the rejected ACL finding. It invokes the user's
stop-rather-than-infer rule on a proven missing column/association constraint
now, rather than waiting for code generation to rediscover it. No new SQL
scope, migration, field, filter removal, guessed association, ignored predicate,
UUID interpretation or false empty result is adopted. Plan §2 routes the
clarification to Controller and IVR/Backend contract ownership. The plan's
three-filter literal query is explicitly a representable subset, NOT an
implementation of the confirmed endpoint or permission to dispatch it.
Consequently a fully code-generation-ready plan cannot honestly be certified
under all supplied constraints. Fresh independent A/B Planreview and the
Controller's clarification remain required; this author claims neither.

## Static validation and limits

Direct baseline source scan:76 unit classes/704 methods +31 IT classes/504
methods =107/1208; token-line1209 including one preserved comment. Current
CI expects the same source/POM/XML inventory; POM31 explicit IT includes.
Static extraction of CI's named maps confirmed exact source identity equality
for both runners and equality of the31 POM includes with the CI IT class set.
Planned delta6/76 +2/36 =8/112; expected final82/780 +33/540 =115/1320;
token-line1321; POM33 includes. Exact identities are cumulative-plan §8,
CI/history arithmetic §9, measured hashes §10 and future commands §11.
No local XML execution is claimed; PA34's report is historical evidence only.

Git status cannot operate in this mounted checkout: `.git` points at
`D:/codex-call/.git/worktrees/codex-call20`, which is not resolvable here.
Do not edit that pointer or private helpers. Filesystem digest verification
is used instead: SHA-256 over sorted file paths, NUL, and each raw-file SHA-256,
excluding exactly the two authorized outputs. Before-fix digest:
`747f665913933c63c5d1912f97d25896187c5adbe3ca459b71ce2e15fb14fc19`.
Final verification must match that value. Output-file hashes are reported
outside these documents to avoid self-reference.

No Maven/build, container, runtime test, dependency installation, stage,
commit, push, PR mutation, merge/approval/mark-ready/reviewer request or
external comment occurs. Stop for Controller intake and fresh independent
A/B Planreview; production readiness remains unclaimed.
