# PA35 bounded plan fix 4 — author evidence and mandatory stop

Task label: `pa35-operations-ivr-session-read-plan-fix4-gpt-20261116-01`  
Work unit: `PA35-operations-ivr-session-read-foundation`  
Date: 2026-11-16 (Asia/Shanghai)  
Gate: bounded `plan fix4` only; author correction, not independent acceptance.

## 1. Scope and exact before/after identity

Only these two authorized outputs change:
- `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md`
- `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan-fix-4.md` (new)

The goal remains exactly the two confirmed list/detail GETs, one S1 slice,
opaque employee-session authority, exact safe frozen DTOs and one read-only
REPEATABLE_READ snapshot. No endpoint, field, source/test/CI/POM implementation,
migration, grant, pool, schema role, production login or credential is added.
Existing role capabilities and their external provisioning obligations are
described, not expanded or provisioned. OpenAPI/docs14/15/17 and V001–V017
remain raw-byte frozen; no V018. All prior fix artifacts remain byte-identical.

| Cumulative plan measurement | Before fix4, actual checkout | After fix4 |
|---|---|---|
| Raw SHA-256 | `85c5a5332511c88a58b02c7697328f7ae782c714305cb3cdb62cd9f0bb087cf9` | `54592719d996bd866feaa131ca3d9cdd8c3da29cbc1782a163ae331e41f8ceca` |
| LF SHA-256 | `431c56117832b121dd3f9706231587640c5765c09475ee36d1ec6d5288301b68` | `986e4c5cf6fcb730305596357bd7701f4879789c705044192dd1d074632bd8cd` |
| Raw bytes / newline count | 70,017 /944 | 79,632 /1047 |
| CRLF count / LF-projected bytes | 944 /69,073 | 907 /78,725 |

Raw delta is +9,615 bytes/+103 lines; LF-projected delta is +9,652 bytes.
LF projection means CRLF→LF only. This mounted input plan is CRLF, unlike
the LF author snapshot recorded by fix3/reviews; its LF hash is exactly the
reviewed post-fix3 plan hash. The patch preserves unchanged CRLF lines and
uses LF for inserted/replaced lines. No frozen input or historical artifact
is normalized. This artifact's own final raw/LF hash, byte and line counts
are reported at handoff rather than embedded self-referentially.

## 2. Authority, inspected evidence and preserved identities

Read the active PA35 Control checkpoint, cumulative plan and fix1/fix2/fix3,
both fresh round-2 reviews and their findings against the current source.
Direct checks covered Operations/Customer route filters and shared error
controller, PA34 service/controller and transport tests, shared Problem writer,
PA33 subject cursor/properties/context/persistence configuration, inherited
frozen-input boundary/IT tests, PA33 role fixture, module descriptors/boundary
assertions, OpenAPI routes/DTOs, docs14/15/17 and V002/V005/V008 constraints and
grants, PA25 identity runbook, current CI maps/history/report bodies and POM.
No online, runtime, container or production evidence is substituted for these
static local checks.

| Preserved input | Actual raw SHA-256 | LF SHA-256 |
|---|---|---|
| fix1 | `6b9c16d46f32a2a5f9e83d66b3e5233cd8c3ab57d9f2de7765262ff759cb31fd` | `c5be3390fd5c541c49ed6aad6362d140689c8d2d984fe173c48e40234c3f0016` |
| fix2 | `ab530a444a83745e856825bf3a28ecda9e342b31d12a5b3d8216191e29ba31a2` | `979e4fc9973e3b741a1d72639f8eea430ad08fe35fd8e7ab163f02267eb3e22a` |
| fix3 | `5d74c4941360cda8a215a2b6ab016809c824937a478e54d49b401c93d6d70b0e` | `f2181d6062631ef9feb9f4d759d6d7de1d2d3a4e1676c2af7da3a6c6bb11dbd5` |
| round-2 A, plan-review-20261116-024007.md | `1ff66c0e83b1dc78c4d28990c26d3bb9b41756159e9ffd1227010d655dabc0b9` | `0157bf0e294ec7de47317a1181bcd895185f5849b5142bf5b1e0ee1005653eeb` |
| round-2 B, plan-review-20261116-013622.md | `94daa658ea6c0f0b772162b4098b2d1cf8afed5ea1c2004c9c58896d5ac8599b` | `da1e54c4229f146ce3bd6977169bfa2fc9d8807d49e243dbcb85336268fbee9d` |
| Control | `e7312a2a8054362fbc589d582146bb7eb659b5f5ad7e755190cc6a3506c8636c` | `aabdb8a50484ba407d206a98c1dc96bded01a4d5fe1d4986019a6d918a77424a` |
| .github/workflows/ci.yml | `2c4addc3a09c2418869c6252e06be9257011d0fc34751289db6c15292887faf0` | `f87c0b5efb0e531ee77da0e3a334d578da33511c12b4d97cd126e86a6a947139` |
| backend/pom.xml | `6a0e01aa11959149a55cd731118809d9efc9231ee11fd645292d490a2d9ea19a` | `82b15839179a241bab9a0e10badc34fdbfa11d843402eb7db013cbb469f6af58` |

Each row's after hash equals its before hash. Prior artifact/review LF values
match the reviewed identities; differing mounted raw values are checkout line
endings, not permission to rewrite history. Control also contains post-fix3
Controller intake/dispatch bookkeeping: its current 2,474,672 bytes/5854 lines
are not frozen plan inputs. Neither that bookkeeping drift nor checkout
representation changes the21 frozen raw/LF pairs. Control is not repaired.

## 3. Finding-by-finding dispositions

All seven Controller-accepted findings are **author-corrected**, subject to
future independent review, with no new method identity or implementation path.

| Finding | Changed plan sections | Exact correction and verification identity |
|---|---|---|
| PA35-PLAN-REREVIEW2-01 | §4, §8, §12 | REQUEST wrong methods/noncanonical routes concealed404. Real-HTTP TRACE on both new routes sanitized500 INTERNAL through existing shared ERROR dispatch. Operations ERROR branch remains verbatim, including activations-only TRACE404. G5 retains its name/count: six REQUEST wrong methods404 and TRACE500 on both routes; D3 retains REQUEST TRACE404; D15/PA34 expectations unchanged. B-F04 deferred ownership is not crossed. |
| PA35-PLAN-REREVIEW2-02 | §10 | Choose inherited plan parsing, not a new table parser:21 bare raw-hash/two-space/path lines, regex `(?m)^([0-9a-f]{64})  (.+)$`, independent hard-coded21-pair map in both F8/G1, exact unique path set, parsed RAW equality, on-disk RAW equality plus separately computed LF equality. No LF-for-raw substitution. Keep independent-verifier assertion and G1 PostgreSQL16/UTF8/Flyway17 checks; V001–V017/no V018. Existing identities unchanged. |
| PA35-PLAN-REREVIEW2-03 | §4, §8 | BOTH new service and controller preserve all nine known Failure/Code values and statuses; Failure catch precedes broad catches; controller forwards failure.code() directly to shared writer. No PA34 subset switch: NOT_FOUND404 and CURSOR_FILTER_MISMATCH400 survive. Only actual dependency/context/data/timeout/cleanup failures map503, unknown programming/runtime failures500; actual cleanup failure may override. C14/D11/G20, B8/C9/D10/G16 and C17/D11/D16/G23 retain identities and cover pass-through/error separation. |
| PA35-PLAN-REREVIEW2-04 | §6 | Remove undefined anchor range. Pin baseline 0<=nanos<1,000,000,000 and nanos%1000==0 followed by Instant.ofEpochSecond; construction failure CURSOR_INVALID. No new epoch-seconds window or wire-format change. Existing B12/G16 remain the validation identities. |
| PA35-PLAN-REREVIEW2-05 | §12 | Replace nonexistent existing runbook with Security/Platform-owned Operations subject-read secret-custody process and required external new/extended deployment secret runbook. Name indexed id/material, active-key-id,1–3 keys, unique active member, AES material, retained-key/replica rotation and per-key nonce discipline shared across three reads. Explicitly state no in-repo runbook covers this ring; PA25 customer HMAC instructions do not. Missing/invalid keys503, no new ring or PA35 path. |
| PA35-PLAN-REREVIEW2-06 | §7 | Remove stale current instruction to add fix-2. Generic Controller dispatch controls gate-document allowlist; prior fixes immutable history, Control/reviews Controller-owned; gate documents are not S1 manifest rows. Current fix4 metadata/stop updated in heading/§1/§12. |
| PA35-PLAN-REREVIEW2-07 | §5.2, §8 | Sort all evidence UUIDs in native ascending order and partition consecutive fixed1000-ID IN chunks (final chunk may be smaller), sequential same-scope/connection/snapshot queries, positional binds only. Query count ceil(n/1000), zero for n=0; never per-row N+1, identifier interpolation, truncation or partial200. Merge all IDs deterministically, reject out-of-chunk IDs, deduplicate and order ORIGINAL/DERIVED. Existing C13/G19/H3/F4 cover0/1/1000/1001/multiple full chunks plus fail-whole-response faults. |

## 4. Accepted clarifications and unchanged scope

- §5.2/§12: session-without-required-call is explicitly a corruption assumption,
  detail fails closed503; no claim of reachable-valid domain behavior. IVR/A2
  plus Contract/Data own invariant documentation clarification at docs14
  §9.13/§7.3 and A2 protocol documentation. No frozen document is edited.
- §3/§4/§8: null caller/employeeId and null/blank trusted audience503; only
  nonblank wrong audience401, separate from missing employee401. Existing
  C2/C3/D6/G4 variants cover this without renaming/adding methods.
- §3/§12: Platform/Database/Security's PA25 identity-runbook deliverable must
  reconcile all seven existing shared-read roles, not just IVR: Operations,
  Subject, Activation, Entitlement, Evidence, Catalog/Order and IVR, with literal
  database role names in §3. PA35's disposable fixture remains three roles;
  PA33/PA34 fixtures, broad owner grants and production resources do not change.
- §9: POM33 means existing container-smoke Failsafe include scope only,31→33;
  two unrelated global include elements remain, all-POM33→35.
- §1: post-fix3 Control drift is Controller bookkeeping, not frozen-input drift.

Changed cumulative-plan sections are metadata and §1/§3/§4/§5.2/§6/§7/§8/
§9/§10/§12. §2 and §11 remain unchanged. §7's29 actual rows and §8's112
class/method identities are unchanged; new prose only pins their assertions.

## 5. Recomputed static validation evidence

Read-only Python3 regex/hash/AST/XML checks against current files passed:
- Manifest:29 contiguous numbered rows,20 absent/new and9 existing/present;
  existence pattern `EEENNNNNNNNNNNNNNNNNNNEENEEEE`. Production13=9 new+4
  existing; tests/helpers13=10 new+3 existing; CI/POM2 existing; report1 new.
  Thus13+13+2+1=29,9+10+0+1=20 new,4+3+2+0=9 existing.
- Identity extraction: A12+B12+C18+D16+E10+F8=76 Surefire; G24+H12=36
  Failsafe;112 distinct class/method pairs and112 distinct method names.
  JSON identity digest `10ad5b9a6be0b121632679d3e6f2f87d23eab2059865ce60c41243273f453483`
  uses full-class→ordered-method-list map, sorted keys and compact separators.
  Row tuple JSON digest `e433bfb907faee1ddeb6f43377823a17ced4ea49c40f9e4ead1def9c15cf49b8`
  uses ordered [two-digit-number,state,path] tuples and compact separators.
- Current source uses CI's exact ordinary-method regex
  `@Test\s+(?:public\s+)?void\s+(\w+)\s*\(`:76/704+31/504=107/1208.
  Evaluate only CI's in-memory named-map construction and compare per-class
  method sets: exact source=CI equality. Token-line regex `@Test\b` gives1209,
  including exactly the preserved PostgresContainerIT comment;64 ordinary
  PostgresContainerIT methods remain. XML execution is not claimed.
- Parse backend/pom.xml with Maven XML namespace: container-smoke Failsafe31
  include classes equal current CI IT class set; global include count33.
  Future exact two additions yield scoped33/global35, not a global POM33 gate.
- Future full82/780+33/540=115/1320; delta8/112, token1209+112=1321.
  Existing locksteps16+9+12=37 add zero; focused8/101+3/48=11/149.
  Executing only the existing CI read-only source/history assertion block
  passed every historical method/token assertion (no XML/runtime body).
  All17 historical method sums and corresponding token totals remain unchanged:
  methods290/320/360/400/445/480/523/543/555/613/677/753/838/924/1014/1120/1208;
  tokens291/321/361/401/446/481/524/544/556/614/678/754/839/925/1015/1121/1209.
- Frozen manifest:21 table rows/42 digests all match disk. The inherited bare
  evidence regex now returns21 unique path/hash pairs exactly equal to table
  raw members. Migration directory has exactly V001–V017; no V018 path exists.
  Pair-set SHA-256 remains `1c0e5701d8c6a3cb5b04ae93946adc143e6ad98ef8bcaa4002911d42c013f285`,
  over path-sorted UTF-8 path,NUL,raw digest bytes,LF digest bytes per row.
- Cursor framing recomputed for every k=1..32: plaintext28+32=60,
  encrypted60+16=76, frame2+k+12+76=90+k=91–122 bytes and unpadded
  base64url122–163 characters; no changed envelope/digest/AAD design.

Exploratory static tooling was corrected locally: seed the CI map evaluation
with its Postgres expected-name set; use CI's word-boundary token regex rather
than counting @TestConfiguration substrings. An initial patch context mismatch
made no change and was retried. These were tooling corrections, not failed
project tests. No Maven/build, dependency install, A1–A5, runtime, container,
hosted-CI, production connection or live/cloud/media verification ran.

## 6. Scope proof, residuals and mandatory stop

The existing .git pointer targets unavailable Windows worktree metadata, so
git status/checkpoint verification cannot run; it is not modified. Before
editing, snapshot every file except exactly the two authorized outputs:
2118 paths. Aggregate SHA-256 over sorted UTF-8 path,NUL,raw-file digest bytes:
`6a2ef9d8ff49714146f82550d30fd1c44dc79ae6d4035615329154afa944b190`.
Final validation reproduced the path set, every individual raw digest, count
and aggregate exactly. This covers prior fixes/reviews, Control, frozen inputs,
source/tests, CI/POM and all private helpers; no other repository write occurs.

Residuals remain owner-routed, not unpinned implementation branches: external
complete-login/identity delivery → Platform/Database/Security and PA25 runbook;
opaque-session issuer → Identity/Security production acceptance; shared AES
secret custody/rotation/nonce discipline → Security/Platform and required
deployment secret runbook; missing-call invariant → IVR/A2 plus Contract/Data
documentation clarification; artifact provenance omission → Contract/Data
metadata backlog; TRACE concealment → existing B-F04 later Operations
route-boundary work; OR/global-order scans and bounded pool/deadlines →
Backend/Database/Capacity, docs18/EXPLAIN; unbounded child chains → Backend/
Operations timeout503, no partial result; PostgreSQL16/hosted CI → Test/Platform.
No new grant/index/migration/pool or public-contract edit is a workaround.

Mandatory stop after these two documents and concise handoff. Controller
intake and fresh independent A/B review are required future actions, not
entered or requested by this author. No implementation or independent
acceptance is authorized; no source/test/Control/frozen-doc/migration/CI/POM
or historical-artifact edit, stage/commit/push, branch creation, PR creation/
update, merge, approval, mark-ready, reviewer request, external comment or
issue mutation occurs. No production-readiness claim. A genuine future need
for new privilege or frozen-contract/schema change requires new authorization.
