# PA35 Plan Fix 8 — Closure Artifact

Task label: `pa35-operations-ivr-session-read-plan-fix8-gpt-20261117-01`
Provider/model/reasoning: Codex runner / GPT / `gpt-6.1-sol` / high (dispatch metadata)
Work unit: `PA35-operations-ivr-session-read-foundation`
Gate: bounded `plan fix8`; documentation-only author correction
Date: 2026-11-17 (Asia/Shanghai)

## Frozen inputs and authority

Close only Controller-accepted Low PA35-PR-F7-01, deduplicating review A's
PA35-PR-F7-A-01 and review B's PA35-PR-F7-B-01. Both independent fix7 reviews
conclude pass-with-risks with this same Low finding. Controller adjudication
commit supplied at intake is `84de8cb0fee2551c6a7c6e8bfae70225c7459e22`;
`f6bb522` is its bookkeeping child. Neither is a self-claimed fix8 Git HEAD.
Control remains read-only; this correction does not confer independent acceptance.

| Frozen input | SHA-256 |
|---|---|
| Cumulative plan before fix8 (102,442 bytes / 1,325 lines) | `733caef543df770728f9aa6ed23d7790f56330818263827571e624ff54eb14bf` |
| `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan-fix-7.md` | `0719691ca5550542e70cfe9813bfe494292c1f62cf1910f2271fe3e7d2af6d67` |
| `docs/reviews/plan-review-20261117-093602.md` (A) | `67e0e46436c1ac17ba935ab2acf93b6f35e22879991acfb29090d9a3f409976a` |
| `docs/reviews/plan-review-20261117-094553.md` (B) | `01d69022c096162f3e1c89243f87129f3bc0d8d4b7765cfc78cc20ca4eb30a2a` |

These four intake hashes match measured bytes. Fix7 and both review artifacts
remain immutable history; fix8 supersedes their unit-count mirror non-vacuity
statement only. All prior closures, confirmed goal/non-goals and one-slice
boundary remain binding. No implementation is authorized.

## Exact changed paths

1. `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md` — fix8 header, sole accepted finding, qualified row30 mirror and stop label.
2. `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan-fix-8.md` — this new closure artifact.

This is the entire write set, not the future 30-path implementation allowlist.
Control, source, tests, OpenAPI, CI, POM, migrations, prior artifacts, `.codex`
and private helpers remain untouched. Existing untracked `.codex` is intake
state, not a fix8 change; no whole-worktree cleanliness is claimed.

## Finding status and exact evidence

| Accepted finding | Final status | Correction |
|---|---|---|
| `PA35-PR-F7-01` (A-01/B-01 deduplicated, Low) | Closed | §7 requires `contains("len(unit_expected) == 83")` as row30's test-side mirror; workflow `assert len(unit_expected) == 83` remains load-bearing. Closure is author correction, not independent acceptance or executed implementation. |

Evidence: A's finding section PA35-PR-F7-A-01 and B's finding section
PA35-PR-F7-B-01 identify the same false-positive substring. The read-only
`.github/workflows/ci.yml:1942` contains
`assert pa31_start_totals == [468, 370] and sum(pa31_start_totals) == 838`;
line 1943 contains `assert pa31_start_line_scan_total == 839`. Each satisfies
the old unqualified unit-count prefix even without the final unit assertion.
These historical literals remain untouched. The complete qualified mirror
cannot be satisfied by either historical literal. The existing test at
`backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java:140`
still has the current mirrors; changing it and CI is later authorized
implementation work, not a write performed at this gate.

## Static validation and preserved invariants

- Read-only Git baseline plan hash matches frozen intake. Exact edit comparison
  confines the cumulative delta to the header/provenance, one qualified mirror,
  its explanatory paragraph and fix8 stop label. All other plan bytes survive.
- Exactly two authorized paths are changed/added relative to intake. Read-only
  Git uses explicit Linux gitdir/worktree overrides because the checkout's
  Windows-style `.git` pointer is not directly usable in WSL. No pointer or
  Git configuration is repaired; `GIT_OPTIONAL_LOCKS=0` avoids index refresh.
- Both documents contain `contains("len(unit_expected) == 83")`; neither
  contains the bare unit-count mirror. A static historical-literal negative
  confirms that the old prefix matches both historical lines but the qualified
  string matches neither. No workflow/test execution or runtime proof is implied.
- The unchanged §7 table has 30 paths = 20 new + 10 existing; production13 =
  9 new + 4 content-modified existing. The unchanged §8 manifest has 8 classes /
  112 unique class-method identities. No path, class or identity is added.
- Current 77/712 + 32/520 = 109/1232, scan1233, scoped POM32/global34;
  final 83/788 + 34/556 = 117/1344, scan1345, scoped POM34/global36 remain.
  Current guarded282 and its aggregate remain; final291 = 282 + 9 and its
  future recomputation stay deferred until authorized production bytes exist.
- All 26 raw/LF pins and the binary pair-map
  `ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409`
  remain exact; static recomputation verifies the existing inputs and current
  282-file aggregate. V001–V018 only/no V019; frozen OpenAPI/docs12/14/15/17,
  PA25 inputs and backend README remain unchanged.
- Final `[788, 556]`, scan1345, qualified container34/includes34, PA36
  `[704, 504]`/1209, historical PA35 `[712, 520]`/1233, global POM36 and
  immutable `baseline_jars == 34` retain their distinct meanings. Typed
  `ObjectProvider`/`one(ObjectProvider)`, no bean-name runtime contract and
  E8's test-managed context remain unchanged.
- Both outputs are strict UTF-8 without BOM, canonical LF with final newline,
  no U+FFFD/conflict markers/trailing whitespace. Exact-path `git diff --check`
  plus a separate new-artifact whitespace check are required static checks.
  Final SHA-256, byte and line counts are emitted in the completion report;
  no self-referential artifact digest or invented Git HEAD is embedded here.

Limitations: static checks only; no Maven, PostgreSQL, containers, Surefire/
Failsafe XML, JAR/firewall, A1–A5, hosted CI, live/cloud/media or production
readiness validation. Future workflow and test literals are requirements, not
claims that implementation already exists. Independent re-review remains pending.

## Residual owners and completion

The cumulative plan's §12 owner/destination table is unchanged: Platform/Database/
Security for production identity/SET proof; Identity/Security for issuer/IdP/MFA;
Security/Platform for key lifecycle/secret delivery; Contract/Data and IVR/A2 for
frozen metadata/invariant clarification; existing B-F04 Operations route-boundary
owner for TRACE; Backend/Database/Operations/Capacity for cleanup, deadlines and
capacity; Test/Platform for PostgreSQL16/hosted CI; Operations/Infrastructure for
live/cloud/media acceptance. Implementation owns the future291 aggregate and
focused/full zero-skip XML/JAR/firewall/A1–A5 evidence. Controller owns intake and
fresh independent A/B Planreview. No residual is silently closed or reassigned.

Completion: the sole accepted PA35-PR-F7-01 is Closed by this bounded author
correction. Stop after the two authorized document writes and read-only static
validation. No stage, commit, push, PR/issue mutation, merge, approval, mark-ready,
reviewer request, external comment, implementation or later gate is performed.
Artifact path: `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan-fix-8.md`.
