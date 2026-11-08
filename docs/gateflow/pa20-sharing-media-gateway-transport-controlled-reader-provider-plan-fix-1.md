# PA20 media-gateway transport and controlled byte-source — plan fix 1

## 1. Task, authority and gate boundary

- Task: `pa20-sharing-media-gateway-transport-plan-fix1-gpt-20261108`; date: 2026-11-08; gate: **fix**; work unit: `PA20-sharing-media-gateway-transport-controlled-reader-provider`.
- Requested runtime: Codex/GPT `gpt-6.1-sol`, reasoning `high`; execution interface: Codex CLI. Requested model/reasoning are dispatch metadata, not an independently attested in-session model identity.
- Binding authority: Control's PA20 Goal Confirmation and accepted fix entry (`docs/gateflow/project-execution-control.md`, lines 15–42), plus the user's exact two-path write scope. Local `planreview` authority was read for evidence-based, goal-bound minimality, ownership, execution/testability and residual-risk checks; this is a fix, not a replacement independent review, and creates no review artifact.
- Changed only the current plan and this new fix artifact, using `apply_patch`. Source, tests, migrations, public-control OpenAPI, POM, CI, Control, both frozen reviews and every other repository file remain as at fix intake. The pre-existing Control modification, untracked plan/reviews and `.codex` are not this task's changes.
- Status: **accepted findings corrected in documentation; author static validation complete; independent A/B re-review mandatory and pending**. No plan acceptance, implementation dispatch, runtime/hosted-CI success or production readiness is claimed. No stage, commit, push, branch/PR mutation, merge, approval, ready action, reviewer request or external comment occurred.

## 2. Inputs and identities

Read the complete 429-line input plan, PA20 Goal Confirmation/fix entry, both frozen plan reviews and local `planreview` authority. Checked current PA19 service/claim/metadata reader/configuration/state/evidence transaction code; `MediaPlaybackIT` credential, fixture construction, verifier, counters and immutable media facts; `CustomerWebConfigurationTests` embedded web pattern and customer filter registration/closed routing/buffering; architecture named-interface/classification assertions; POM/CI exact inventories; and cited docs/12 §9.5/§18, docs/17 §16/logical deployment placement and PA19 closeout evidence. Hashes are raw working-file SHA256, not normalized Git blobs.

| Input | SHA256 at fix intake |
| --- | --- |
| `docs/gateflow/pa20-sharing-media-gateway-transport-controlled-reader-provider-plan.md` (429 lines / 72,539 bytes) | `7e776148731cc913f00a6a20883612805e2a4f11dc8b39e777835ff776f1cf44` |
| `docs/gateflow/project-execution-control.md` (4,277 lines / 1,759,067 bytes) | `1c4228269b480b8d51987419883d4d1f6e3cfdfb579b28a7da93bdfdec512e6b` |
| Reviewer A: `docs/reviews/plan-review-20261108-212516.md` (114 lines / 19,624 bytes) | `13d205805c7da923a799eff51bc44abb0305f08513950875285e813bd64e3c41` |
| Reviewer B: `docs/reviews/plan-review-20261108-214255.md` (87 lines / 15,872 bytes) | `2e702b0c9abd7fce3e5d0b0a56078a7bf0a8b2d4040382c853646b0f02b5a902` |
| Local `/root/.claude/skills/planreview/SKILL.md` | `4541ede651cfe85e6455fb4692eac6c961a1ae2a2df04234790492968d0d8cba` |

Read-only Git observed intake HEAD `7079da74dd713cced53a3c24263c33a0028bbcef`, descending from merge `9da37930651d2d0f4ba76a1e8c8d1c15a6ba435e`; tracked source/test/POM/CI/OpenAPI paths have no difference from that merge. The Windows `.git` indirection is invalid for ordinary Linux Git discovery; checks use `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD"` without changing metadata/configuration. The frozen-review manifest and diff hashes remain those recorded by Control/reviews; this fix does not replace or mutate their evidence.

## 3. Accepted finding dispositions

| Finding / severity | Final author status | Exact plan sections changed and correction |
| --- | --- | --- |
| `PA20-REV-A-01` / medium | Corrected; pending independent verification | §8.1 full/range descriptions; §8.6 methods 1/2/6 and fixture-reuse paragraph: pin `POSITIVE_HTTP_BEARER = "pa20-positive-playback-token"`, exact-byte acceptance by the fresh verifier over real state/evidence, and real200/206 assertions. Existing private punctuation-bearing CANARY is unchanged, reserved for DB/negative/redaction probes; HTTP presentation yields401 before verifier. Both credential probes are excluded from headers/problems/logs. No new fixture visibility edit beyond the existing allowlist. |
| `PA20-REV-A-02` / low | Corrected; pending independent verification | §3.1 and §8.1 route method: exact context-removal rule followed by raw case-sensitive `startsWith("/internal/media-gateway")`; canonical path-field checks; explicit terminal/pass-through table for double slashes, case, `media-gatewayx`, trailing/additional segments, encoded/double-encoded spellings, semicolons and context inconsistencies. Isolated chain-spy assertions distinguish terminal404 from untouched chain-once; customer catch-all404 cannot mask behavior. No normalization/extra decode. |
| `PA20-REV-A-03` / low | Corrected; pending independent verification | §4, §5.1, §8.3 method1, §8.5 method1: exactly one `ControlledMediaByteSource.ByteInterval` owned by evidence; explicit nested-type import in `SingleByteRange`; identical selected/request interval types, no parser duplicate or conversion, no evidence→sharing dependency. Architecture assertions pin sole ownership and unchanged named edge. |
| `PA20-REV-A-04` / low | Corrected; pending independent verification | §8 introduction and §8.6 methods5/6: direct invocation of real filter + real PG-backed service with deterministic throwing servlet response/output stream, captured prefix/explicit commitment, precommit reset and postcommit no-reset/no-problem/cause-free failure, exactly-once cleanup and DB/durable assertions. Separate embedded-Tomcat/Java-HttpClient source-fault truncation proof crosses the response buffer before failure. Neither mock-only service proof nor assumed deterministic TCP-disconnect write timing may substitute; no wrapper ordering before HIGHEST_PRECEDENCE. |

## 4. Precision and open-question dispositions

- **Frozen base — resolved (§1, §10.2, §12):** implementation compares against the Controller's future accepted-plan commit, supplied at S1 dispatch as exact `PA20_IMPLEMENTATION_BASE`, whose source tree descends from `9da37930651d2d0f4ba76a1e8c8d1c15a6ba435e`. Earlier authoring `f3179d7` and intake `7079da74` are correct chronological bookkeeping, not plan drift or alternate implementation baselines. Added explicit commit/ancestry validation; no future SHA invented.
- **Real web context — resolved (§8 introduction, §8.2 method2):** existing `CustomerWebConfigurationTests` `SpringApplicationBuilder(OneCallApplication.class)` + `web`, ephemeral embedded Tomcat, actual servlet context port and Java HttpClient; inspect real filter registrations and prove401/503 plus old routes/no DB effects over HTTP. Mock chain/ApplicationContextRunner alone is not sufficient.
- **Precedence — resolved (§3.1, §3.2 table, §8.1 partial-window method, §8.6 method2):** malformed query/body/Range→400 and malformed bearer→401 before PA19, with combined faults retaining existing step order. Syntactically valid satisfiable or unsatisfiable Range plus otherwise-valid partial-window SEEK claim completes successful PA19 metadata open, then single non-disclosing403 before byte-range resolution/416 and physical open. Tests pin both branches/counters.
- **Envelope ownership — confirmed (§3.1):** HEAD405/empty body and conditional headers400 are accepted plan-gate decisions, not reopened protocol questions. The existing audio/full-window fail-closed foundation restriction remains intentional; arbitrary media/time-to-byte mapping remains deferred.
- **Residual questions:** no blocking foundation question remains. The accepted-plan SHA is a later Controller handoff identity, not an unresolved design decision. Production verifier/storage/Q22/public-origin/deployment/runtime redaction and container pre-servlet/postcommit limitations retain the existing §11 owners and boundaries; no new production policy or dependency is selected here.

## 5. Exact changed sections and preservation

Changed plan sections are **§1; §3.1; §3.2; §4; §5.1; §8 introduction; §8.1; §8.2; §8.3; §8.5; §8.6; §9 introductory gate-output sentence; §10.2 base setup/comparison wording; §12**. All other sections remain unchanged. §9's complete 22-row implementation table, including paths, order and permitted-change descriptions, is byte-identical to intake; this fix artifact is not a 23rd implementation path.

- One composed S1 remains; **22 paths = 14 new + 8 modified**. No source/test/docs dependency or third fix path is introduced; helper mechanisms fit the existing IT/support paths. PA19 production files and all original six `MediaPlaybackIT` methods stay frozen under the original fixture-overload/visibility plan.
- Current **25/162 + 12/198 = 37/360**, planned **29/196 + 13/204 = 42/400**; exact new method identities unchanged, groups **[18,4,6,4,2,6] = 40**, POM includes **12→13**, one support file with zero tests. No class/method count from review prose supersedes these measured values.
- V001–V013/no V014, public-control OpenAPI/five-surface inventory, unchanged PA15–PA19 production semantics, no delivery business-state writes/post-open SQL, fail-closed production verifier/reader/byte provider, all non-goals and §11 residual owners remain intact.

## 6. Static validation and final measurements

- **PASS:** Python compared the entire 22-row allowlist table byte-for-byte and extracted all six plan test-name groups against the input copy: exact identities/order retained, lengths `[18,4,6,4,2,6]`; current/planned arithmetic remains `37/360→42/400`.
- **PASS:** evaluated only CI inventory assignments/updates from the workflow's embedded Python and compared every literal method name with source: Surefire25/162, Failsafe12/198, exact class coverage; parsed POM includes equal the 12 IT basenames. No `TEST-*.xml` exists; no runtime evidence was inferred.
- **PASS:** independently checked all 26 literal raw paths in the normative routing table against the pinned terminal predicate/canonical route; context/path-field rows were checked against the stated exact removal/equality rules. Positive token matches bearer grammar; existing private CANARY does not.
- **PASS:** thirteen migration files, no V014; public-control SHA256 remains `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68`. Intake file-manifest comparison preserves all other repository bytes, including Control/reviews/source/tests/POM/CI/migrations/OpenAPI; final changed-path set is exactly the two authorized documents.
- **PASS:** explicit-Git-dir `diff --no-index --check` against the input plan and whitespace scans of both outputs; no whitespace errors. Ordinary Git discovery initially failed at the known Windows pointer; the explicit read-only invocation succeeded (line-ending warnings only). The first read-only CI extraction helper lacked historical `expected` assignments; including those assignments resolved the helper error and the complete inventory check passed. No repository/toolchain fix was made.
- **Not run:** Maven, Docker/PG runtime suites, new servlet tests, OpenAPI lint, packaged web smoke, hosted CI or independent re-review; this is a two-document fix gate, not implementation validation.
- **Final plan:** `docs/gateflow/pa20-sharing-media-gateway-transport-controlled-reader-provider-plan.md` — **454 lines / 84,742 bytes**, SHA256 **`ea603bf87358079b700a0d975b5b2b5fc24b4b905b05720b539d2459a41b8fdf`**.
- This artifact's final SHA256/line/byte counts are computed after its bytes are finalized and reported in the task handoff, rather than embedding a self-referential digest. Reproduction: `sha256sum docs/gateflow/pa20-sharing-media-gateway-transport-controlled-reader-provider-plan*.md` and `wc -lc docs/gateflow/pa20-sharing-media-gateway-transport-controlled-reader-provider-plan*.md`.

Stop after these two documents for Controller intake and mandatory independent A/B re-review. No subsequent gate or remote action is authorized.
