# PA12 customer share list API — bounded plan fix 1

- Work unit: `PA12-customer-share-list-api`.
- Task: `pa12-customer-share-list-api-plan-fix1-gpt-20261102-01`.
- Date: 2026-11-02. Current gate: `fix` (unchanged).
- status: plan-fix-1-complete
- Revised plan: `docs/gateflow/pa12-customer-share-list-api-plan.md`.
- This is a documentation-only closure of the Controller-adjudicated findings, not implementation authorization, reviewer acceptance, or entry to re-review.

## 1. Binding inputs and integrity

SHA-256 values below are raw file bytes observed before this author's first write, not hashes inferred from historical review snapshots:

| Input | SHA-256 |
| --- | --- |
| `docs/gateflow/project-execution-control.md` (PA12 goal/adjudication) | `2d01a82911ac887e062dc90739667237de9ea99fcab4651ca4e04a6f4b37b178` |
| `docs/gateflow/pa12-customer-share-list-api-plan.md` (input plan) | `d0d2587af4bd36efa649846ff6981f5e274693bdb75b095dd617b4d4f8714830` |
| `docs/reviews/plan-review-20261102-122743.md` (A) | `99620c5701cebecc5a32975e3b40da8db2a3aaac76ff60ca6c1ab002d44aa9d8` |
| `docs/reviews/plan-review-20261102-124556.md` (B) | `51c99c897f98c8703097df250f4de2a3cbccfb92d3b7740eefeabd116afc7b3e` |

External pre-write regular-file manifest: `/tmp/pa12-plan-fix-1-before.sha256`, 1163 files, SHA-256 `1feeb7790e628287e18ac70bd9d4b678312c6d030d85e23cc76e8cba6c08c728`. The recorded input plan is LF; the earlier review's raw CRLF hash is not this input hash. No Git-head/index provenance is claimed: `git status --short` fails because `.git` points to unavailable Windows-style `D:/codex-call/.git/worktrees/codex-call10`. No metadata repair was attempted.

Output hash protocol: after both documents are final, run `sha256sum docs/gateflow/pa12-customer-share-list-api-plan.md docs/gateflow/pa12-customer-share-list-api-plan-fix-1.md` and publish both values externally in the completion response and `/tmp/pa12-plan-fix-1-outputs.sha256`. Neither final output hash is embedded in either document: no self-reference or mutual-hash cycle.

## 2. Findings and dispositions

| Accepted finding / adjudication | Exact disposition in revised plan |
| --- | --- |
| A-02 / F-B-01: selection vs Accept precedence | §§2/4/5/6/7 explicitly carry selection through shared authentication without rejecting structure. Shares controller validates after unchanged filter Accept, before limit/cursor/query provider resolution. Valid bearer + invalid non-null selection + incompatible/malformed Accept is 500; acceptable Accept is 503. Combined assertions are folded into existing `negotiationAndBufferedFailuresPreservePa11Boundary`, not a new method. |
| F-B-02: unchanged `/me` | §§2/7/8 explicitly ignore selection structure on `/me`, including null workspaceType, PERSONAL with organizationId, ORGANIZATION without organizationId, null selection and inaccessible organization. Record/session/caller constructors must not throw for those structures. Existing response/order/account checks/dependency ownership are pinned in `meRemainsCompatibleWithLegacyAndSelectedSessions` and existing CustomerMeWebTests methods. |
| A-01 and A-06: named-interface evidence/export precision | §3 records Controller-adopted B's executed Modulith 2.1.2 V1/V8 evidence under fixture and SpringBootApplication roots; V2 bare sharing insufficient; V5 persistence/default sharing API inaccessible. Whole-package export includes diagnostic/public constructor; consumer restrictions are exact import/source assertions, not Java access control. No unverified spike or alternative production seam is introduced. |
| F-B-05: named-interface discovery failure | §3 requires exact non-unnamed interface name, all eight top-level members and the three declared nested enum members; absent/misspelled interface asserts IllegalArgumentException during ApplicationModules.of discovery, not assumed Violations from verify. Fold into existing architecture methods/fixtures. |
| A-03 / F-B-04: executable test seams | §§7/8 name a test-context-only sentinel throwing ShareSummary serializer/module; fixture-owned dual-pool name mapping and JDBC proxies; sharing/evidence full-row snapshots; denied-login, missing-graph and separate verifier contexts. `assertSharingReadOnlyWriteDenied()` uses an independent test-owned read-only sharing-login transaction, SET LOCAL ROLE sharing_role and one permitted sharing UPDATE for 25006. Independent autocommit probes prove 42501. No production hook or existing fixture edit. |
| A-05 / F-B-03: signature and measured JSON | §§2/4/7/8 use Principal parameter with CustomerRequestPrincipal type check, record Controller-adopted B's executed Jackson 3.1.7 null/ISO Instant evidence, start without redundant annotation, make wire test normative, and permit only a new-page record/property-local inclusion annotation if that actual test proves omission. No global/PA10 model change. |
| Cursor wording (user clarification; F-B-06 remains rejected-with-reason as a defect) | §4.1 removes the vacuous alternative-forms rejection and explains fixed-width scalars have no variable-width overlong representation. Canonical base64url, equality re-encode, alphabet/padding, pad bits, trailing bytes, scalar/date/nanos/count/wire bounds remain operative. No new test or decoder scope. |
| A-04: no supporting list index | §9 explicitly defers creator/workspace/sort capacity to Backend/Database/Platform. No migration/index work; fixture success is not capacity evidence. |
| F-B-07: composition exception already explicit | §3 retains the transport-composition exception, potential allowed-map cycle warning and Architecture ownership. §9 keeps it visible through implementation/PR gates, not presented as permission already in docs/12. No architecture document edit or scope reopening. |

## 3. Changed sections and unchanged boundary

- Metadata: distinguish historical original plan completion from current bounded fix/gate/status.
- §2: carrier/constructor semantics, shares-only `requireSelectedWorkspace(TrustedCustomerCaller)` after Accept, explicit legacy `/me` structural indifference.
- §3: adopted executed evidence, package-wide export, exact member/import assertions and correct discovery exception behavior; one declared/discovered edge remains.
- §4 / §4.1: measured JSON decision, corrected controller sequence and nonvacuous cursor checks.
- §5 / §6: canonical 500-before-503 combined case, shares-only step 4, provenance vs structure vs lifecycle ownership.
- §7: production file/function instructions aligned with carry-only authentication; executable test-only serializer/context/pool/snapshot/read-only hooks named in existing planned new test files.
- §8: same 26 new methods; replace second planned session method name with `workspaceStructureIsCarriedWithoutChangingAuthentication`; expand existing wire, `/me`, negotiation and ACL methods, not method counts.
- §9: deferred no-index capacity residual and explicit two-document fix/architecture-exception boundary.
- §10: fix-specific manifest/hash/validation rules and stop condition; historical plan-only facts remain historical. §1 goal/evidence/prerequisites and the implementation validation commands remain unchanged.

Exactly one end-to-end read-only S1 remains: only GET /api/v1/customer/shares over the existing opt-in customer boundary, PA10 query reuse and ACTIVE creator-context rechecks, immutable frozen SharePage projection, independent customer/sharing roles/pools, default non-web/database-free startup and production verifier fail-closed behavior. No extra endpoint, mutation, provider/session adapter/store, TTL/revocation policy, database/schema/grant/migration/index/status change, frontend, dependency/module expansion or generic cursor framework. No source/test/control/OpenAPI/design/security/CI/README implementation edits are authorized in this fix. Future S1 file instructions do not authorize writing those files now.

## 4. Plan-only validation

- Structure: all ten numbered plan sections remain in sequence; exactly one behavioral S1. All new executable hooks are test-owned in the already planned two new test/support files; no production test hook added.
- Baseline re-derived with the CI-style `@Test` regex from current Java sources: Surefire 49 methods/11 classes; Failsafe 84 methods/3 classes. Planned ordinary named lists re-counted as 2 session + 6 codec + 10 web = 18 Surefire additions, and eight Failsafe additions. No parameterized/dynamic/repeated/disabled substitute.
- Arithmetic verified: 49+18=67 / 11+2=13; 84+8=92 / 3+1=4; 64+10+10+8=92; 67+92=159. No mathematical correction required. Cursor maximum recomputed: 46+ceil(21*128/8)=382 bytes; ceil(382*8/6)=510 unpadded characters, within 512.
- Stale contradictory wording checked: no authenticator structure rejection, old `validate/carry attestation`, application-query write injection claim, conditional “if necessary” inclusion prose or vacuous “overlong alternative forms” requirement remains. Final check requires no trailing spaces/tabs in either output and unchanged review input hashes.
- Author writes: only the revised plan and this new artifact. Full-tree SHA-256 comparison is used because Git is unusable. **An unqualified clean-tree/no-other-change result cannot be claimed:** concurrent changes not authored by this fixer appeared after the pre-write manifest; see next paragraph. No unrelated file was reverted or edited to hide this discrepancy. No runtime tests/library probes were rerun; prior B probes are attributed evidence, not newly executed project tests. No Maven/Docker/HTTP/npm/GitHub validation was run.

Concurrent scope evidence: the control document was observed later at SHA-256 `874410bdbc2ed167c63f86ecc7834ac72b3c6636281739250954619d9fa991e9` (mtime 2026-11-02 13:03:46 +0800); its PA12 adjudication/gate was re-read and still specifies this same bounded fix. Seven unrelated regular files appeared: `tmp/env-config/agent-tools.zsh`, `tmp/env-config/codex-agent-run`, `tmp/env-config/qwen38max-config.toml`, `tmp/env-config/sub-agents-SKILL.md`, `tmp/install_qwen38max.sh`, `tmp/probe_qwen_max.sh`, `tmp/probe_qwen_models.sh`. This author issued no writes to those paths/control and does not attribute their producer. Final manifest comparison must enumerate these separately from the two authorized author outputs, identify any further drift, and retain the provenance limitation. This is not permission to change them or a claim that the original full-tree equality condition passed.

## 5. Residual owners and stop condition

| Residual | Owner / destination |
| --- | --- |
| Creator/workspace/sort query lacks supporting index; no production capacity guarantee | Backend/Database/Platform capacity; separate authorized assessment/schema work, never S1 migration/index additions. |
| Explicit customer-web → sharing-query composition exception; future permitted edges could close a cycle | Architecture + Gateflow controller; future authorized docs/12 reconciliation/composition extraction before cycle-closing implementation. Exception stays visible at later gates. |
| Provider-neutral trusted selection, session issuance/storage, TTL/revocation policy | Customer IAM/Identity + Security; separately authorized adapter integration, production fail-closed until then. |
| Unsigned cursor fingerprint is not integrity protection | Backend/API governance + Security; future separately authorized signing/encryption/key lifecycle policy, no S1 expansion. |
| Provisioning/logins/image pinning and runtime/container execution | Platform/DBA + environment owner; later functional pinned environment. Existing ACLs/transaction behavior only. |
| Unusable Git provenance and concurrent control/tmp drift | Workspace controller/environment owner; reconcile independent writer provenance externally before claiming globally clean scope. Do not repair metadata or undo unrelated work here. |

Stop with `status: plan-fix-1-complete` after two-document validation and externally published output hashes. No implementation, staging, commit, push, PR, reviewer dispatch, re-review entry or gate advancement. Controller owns the next authorized action and any reconciliation of concurrent workspace changes.
