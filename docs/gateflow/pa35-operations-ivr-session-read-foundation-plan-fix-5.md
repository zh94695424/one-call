# PA35 Plan Fix 5 — Merged PA36 Baseline and Portable Frozen Verification

Task label: `pa35-operations-ivr-session-read-plan-fix5-gpt-20261117-01`
Work unit: `PA35-operations-ivr-session-read-foundation`
Date: 2026-11-17 (America/Los_Angeles)
Gate: bounded `plan fix5`; author correction, not independent acceptance.

## 1. Binding Scope and Checkpoint

The user's 2026-11-17 confirmation remains the scope contract: only
listOperationsIvrSessions and getOperationsIvrSession, the two frozen Operations
GET routes, current ACTIVE server-derived ADMIN/OPS authority, existing opaque
employee-session audience, safe projections and one existing shared read context.
No public field/filter/endpoint, schema, migration, grant, role, pool, media path
or wider exposure is added. Existing Operations routes and error precedence stay
intact. Cumulative plan §§1–6 retain the exact filter/SQL/mapping/cursor/lifecycle
specification; this correction does not reopen prior accepted design decisions.

Verified branch `codex/pa35-operations-ivr-session-read-foundation` and HEAD
`9a4fbaddb0fb8f619559734cc4a5e048a44096c1`. The Windows `.git` pointer is
unchanged; existing metadata is reachable through
`/mnt/d/codex-call/.git/worktrees/codex-call23`. No branch/commit/index/ref
mutation is requested. Initial Git status already contains modified Control,
untracked `.codex`, the cumulative plan, fixes1–4 and six imported reviews.
Those pre-existing paths are not cleaned up or treated as this task's edits.

The only authorized output paths are the cumulative plan and this new artifact.
The design source, Control, six effective prior reviews, fixes1–4 and merged
source/CI/POM/test inputs were read directly. Earlier fixes/reviews remain history.
No Controller acceptance, implementation authorization or runtime success follows.

## 2. Accepted Finding Closure

### PA35-PLAN-REREVIEW3-01

PA36 supplies the formerly missing scalar association capability. V018 grants
evidence_role only owner-issued, non-grantable SELECT(session_id,call_id) on
evidence.evidence_record. It does not grant broad table SELECT. The cumulative
plan now correctly derives current Evidence authority from V014 plus V018,
not from original V008 broad grants. The owner-local adapter selects only
id,status,session_id,call_id; filters session_id=UUID OR call_id=text; checks
both associations before mapping only safe evidenceId/status/artifactRoles.
The existing artifact projection remains evidence_id/artifact_role. No alternate
source_ref_id, snapshot or recording association is introduced.

H3/H10 now require real-role success for the exact four-column association
query, and42501 denial for protected record columns/broad record SELECT. IVR's
broader raw reads are distinguished from Evidence's actual column ACL; static
SQL and wire/log canaries still prove narrower application exposure. Reusing
V018, rather than authoring another grant, closes the accepted severe blocker.

Current ACL/history/topology prerequisites are explicitly pinned:
- Successful SQL history18, V001–V018; V018 checksum1606293398, no V019.
- Customer53 + Evidence18 + others219 = original-seven290; expanded-nine410
  additionally monitors unchanged Operations14 + IVR106. V018's own target-only
  oracle is16→18, not a hard-coded410 cross-module migration predicate.
- PA25 topology7 logins/9 ACL-monitored roles/11 edges; four external inputs.
  LO has all seven existing shared-context SET targets, including IVR, with
  ADMIN FALSE/INHERIT FALSE/SET TRUE. No context-target ACL tuples enlarge410.
- PA25 SQL/current docs17/runbook already specify the shared Operations login;
  the old claim that its identity/IVR edge needs new documentation is removed.
  Actual credential/provisioning/production verification remains external.

Cumulative §9.1 enumerates all36 test/helper Java paths changed by merged PA36,
with their exact history18, migration-tail, docs17 historical-body, customer53,
original-seven290, evidence18, expanded410 and topology11 assumptions. They are
preserved, not rewritten. Historical V015/V016/V017 target fixtures, prefix
oracles/checksums and method identities remain intact. One newly necessary
future existing-path lockstep is EvidenceRoleIvrAssociationAclBoundaryTests:
only ciPomAndOrdinaryMethodInventoryMatchExactly's current final inventory
literals change; historical PA36-start704/504 and token1209 remain unchanged.

### PA35-PLAN-REREVIEW3-02

Frozen verification is now checkout-representation independent. F8/G1 each
independently hard-code the same26-path raw/LF pair map. They compare table/map
pairs and bare-line RAW members exactly, then accept recorded raw or canonical
LF bytes (or canonical-equivalent CRLF material) on disk, while ALWAYS requiring
canonical LF equality. A plan edit alone cannot redefine the independent map.
No trimming, BOM stripping, arbitrary re-encoding or frozen-source rewrite is
allowed. Source byte preservation is independent of checkout portability.

Both planned identities cover LF and CRLF temporary materializations, content
and BOM drift, changed table/bare hashes, missing/extra/duplicate paths, exact
eighteen migration names and unchanged independent-verifier boundary. G1 also
requires PostgreSQL16/UTF8,18 successful migrations and exact V018 checksum.
Two PA35-new identities are deliberately renamed from Seventeen to Eighteen;
no existing/historical test is renamed or counted as an addition.

## 3. Exact Implementation Manifest and Arithmetic

The future allowlist is30 paths =20 new +10 existing:
- Production13 =9 new +4 existing, including the unchanged-context-mechanics
  IVR enum addition and IVR's single named Operations dependency edge.
- Tests/helpers14 =8 new test classes +2 zero-test helpers +4 existing classes.
- CI/POM2 existing; implementation report1 new.

The extra row30 is the PA36 current-inventory assertion lockstep. Existing test
assertions change in four classes but add zero methods:16+9+8 Surefire and12
Failsafe =45 existing identities. Two owner adapters, two narrow qualified
ports and the existing shared resources remain the only architecture required.
The one implementation slice S1 is still a complete authority-to-safe-chain
read behavior; a verification-only baseline correction is not a second slice.

| Inventory | Merged current | PA35 additions | Expected after PA35 |
|---|---|---|---|
| Surefire classes/methods | 77/712 | 6/76 | 83/788 |
| Failsafe classes/methods | 32/520 | 2/36 | 34/556 |
| Combined | 109/1232 | 8/112 | 117/1344 |
| Annotation-line scan | 1233 | 112 | 1345 |
| POM container-smoke includes | 32 | 2 | 34 |
| All-POM include elements | 34 | 2 | 36 |

Focused future verification includes all eight new classes plus four existing
lockstep classes:9/109 Surefire +3/48 Failsafe =12/157. Full future XML must
prove exact83/788 +34/556 =117/1344 method identities and zero skipped/failed/
errored tests. These are expectations, not executed XML results.

CI preserves PA36's8/16 additions and all older named maps/subtractions. PA35
identities are subtracted before EVERY pre-PA35 history/token reconstruction,
including PA36-start. New pa35_start totals712/520, tokens1233; final788/556,
tokens1345; early Surefire report assertion712→788, maps83/34, scoped POM34.
Historical sums remain290/320/360/400/445/480/523/543/555/613/677/753/838/924/
1014/1120/1208, followed by new PA35-start1232. Corresponding historical token
totals291/321/361/401/446/481/524/544/556/614/678/754/839/925/1015/1121/1209
remain, followed by1233. PostgresContainerIT64 methods and one comment token stay.

## 4. Frozen Inputs and Static Evidence

Cumulative §10 remeasures all26 protected raw/LF pairs: unchanged OpenAPI,
docs12/14/15, current post-PA36 docs17, V001–V018, PA25 SQL, current runbook and
backend README. CI/POM have separate current-baseline pins because future S1
changes only their explicitly allowlisted inventory configuration. Every cited
pin was recomputed; no old docs17 hash is represented as the current whole file.

Important current pins (the complete table and26 bare RAW lines are in §10):
- V018 raw `ea2911ea92fa4b3d043f7fe63777d8cf35fb0733202784952886ac7978a4b37c`;
  LF `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817`.
- Current docs17 raw `b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205`;
  LF `4e7df589380f70ba7e9e1a5b7e9c1542ca6ed89313a775dacfc91e109852ceb6`.
- Pair-map digest `ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409`:
  path-sorted UTF-8 path,NUL,raw digest bytes,LF digest bytes; no row terminator.
- Ordered [two-digit-row,state,path] JSON digest
  `18b39e0219dabcd541afe15c940d70da8c8b4481dcd2cdfb013a7d7a7d94f7b6`.
- Full-class→ordered-method-list sorted-key compact JSON digest
  `1009762ecc58015cc34a5e99d3aa5de007705a20bb0342c6e45b6aba6578c681`.

Exact local commands used, from the workspace unless explicitly absolute:

```bash
git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call23 --work-tree="$PWD" branch --show-current
git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call23 --work-tree="$PWD" rev-parse HEAD
git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call23 --work-tree="$PWD" diff HEAD^ HEAD --name-only
python3 /tmp/pa35_fix5_verify.py snapshot
python3 /tmp/pa35_fix5_verify.py baseline
python3 /tmp/pa35_fix5_verify.py final
git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call23 --work-tree="$PWD" diff --check -- docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md docs/gateflow/pa35-operations-ivr-session-read-foundation-plan-fix-5.md
```

The temporary verifier and all snapshots/patches are under `/tmp`, not source.
It parses the existing CI heredoc AST, seeds the Postgres/query name sets,
executes only its source/history/map/POM assertions from backend cwd, skips XML
inventory invocations and suppresses the CI success-print statement. It also
statically checks the existing XML expected-map/totals arguments712/520. That
code path does NOT read runtime reports, run Java or attest test outcomes.

Static assertions/results:
- Source=CI exact named sets77/712 +32/520; scoped POM includes32 match exact IT
  class set; XML expected arguments agree. Annotation count1233, historical
  assertions and all global/scoped include counts pass.
- Thirty contiguous unique manifest paths;20 future paths absent,10 existing
  paths present. All eight class paths match the112 unique new method identities:
  A12+B12+C18+D16+E10+F8 and G24+H12. Future map merge117/1344/tokens1345
  and existing-class45/focused12/157 arithmetic pass.
- All26 table pairs equal disk raw/LF pins;26 unique bare entries equal table
  RAW map; baseline snapshot independently matches raw digests. Eighteen exact
  migration inputs; no V019. All36 merged test/helper paths equal §9.1's ledger.
-52 actual external temporary materializations (26 LF +26 CRLF) pass canonical
  verification.104 content/BOM negatives plus table/bare/map missing, duplicate
  and changed-hash negatives fail as required. Repository frozen files unchanged.
- Both outputs are UTF-8/no BOM/no U+FFFD/no conflict markers/no trailing
  whitespace. Allowed-path `git diff --check` passes; untracked outputs receive
  the separate byte/text checks rather than relying on tracked diff coverage.

The exploratory baseline check intentionally exposed stale fix4 docs17 pins;
the corrected plan remeasures them. One generated patch initially used numeric
unified-diff headers unsupported by apply_patch and applied nothing; the helper
was corrected to apply_patch's hunk syntax. Neither event is a project-test
failure. No Maven/Docker/full suite was needed or run at this document gate.

## 5. Scope Proof, Residuals and Stop

Before edits, every repository file except exactly the two authorized outputs
was snapshotted:2151 paths. SHA-256 over sorted UTF-8 path,NUL,raw digest bytes:
`f95826de2ff63f608e97a7916cf920c09ee57f8e9515e5a84034cb287d568646`.
Final validation requires identical path set, every file digest and aggregate;
initial/final Git status delta is only the new fix-5 artifact because the
cumulative plan was already untracked. Its before/after bytes separately prove
the intended update. Thus exactly two paths change/add from this task, and
pre-existing Control/private/imported evidence dirtiness is preserved unchanged.

No goal drift or overdesign occurs: PA36 supplies the prerequisite, PA35 consumes
it through existing owner boundaries, and the frozen API and single-slice scope
do not expand. There is no remaining schema/contract/ACL blocker on this baseline.
A genuine need for a new public field/filter/endpoint, migration/grant/role/pool
or wider exposure requires stopping for Controller authorization, not guessing.

Residuals retain named owners/destinations in cumulative §12: actual production
identity delivery → Platform/Database/Security and current PA25 runbook;
opaque-session issuer → Identity/Security acceptance; shared cursor-ring custody/
rotation → Security/Platform deployment runbook; required-call invariant and
artifact metadata omission → IVR/A2 plus Contract/Data clarification backlog;
TRACE/ERROR dispatch → existing B-F04 Operations boundary owner; shared-read
capacity/deadlines and large chains → Backend/Database/Operations capacity work;
PostgreSQL16/hosted CI → Test/Platform. None authorizes extra PA35 paths.

Completion report: identify the two outputs, findings corrected, current/future
inventory and manifest totals, pin/representation/static checks and preservation
proof; explicitly distinguish planned XML/runtime validation from execution.
The implementation report requirements remain in cumulative §12 for a later gate.

Stop after these documents and static validation. No stage/commit/push, branch
creation, PR/issue mutation, approval, mark-ready, reviewer request, external
comment or later-gate entry. Fresh independent A/B Planreview is the next
Controller gate, not requested or entered here. No implementation, runtime,
hosted-CI, live/media or production-readiness success is claimed.
