# PA19 sharing media gateway playback foundation — plan fix 1

## 1. Task, authority and disposition

- Task: `pa19-sharing-media-gateway-playback-foundation-plan-fix1-gpt-20261108`; date: 2026-11-08; gate: **fix**.
- Requested runtime: GPT `gpt-6.1-sol`, high; execution context: Codex CLI. The requested model label is dispatch metadata, not an independent in-session runtime attestation.
- Authority: Control's “Active resume checkpoint - 2026-11-08 / PA19 sharing media gateway playback foundation” accepted findings and precision, plus the exact two-path user write boundary.
- Changed only `docs/gateflow/pa19-sharing-media-gateway-playback-foundation-plan.md` and this new artifact, using `apply_patch`. Control, imported reviews, source, tests, migrations, POM, CI, OpenAPI and other existing artifacts remain unchanged from fix intake.
- Disposition: **author-reported documentation corrections complete**, subject to Controller intake and any subsequently authorized independent review. This is not plan acceptance, readiness, implementation, A/B re-review, or authorization to enter another gate. No stage, commit, push, PR mutation, merge, approval, reviewer request or external comment occurred.

## 2. Inspected inputs and identities

The complete original plan (400 lines / 63,959 bytes), Control resume/fix section and both complete imported review artifacts were read. Direct evidence inspection covered the merged PA19A API/resolver/configuration/closeout, PA18 admission/media witness and time predicates and relevant tests (including the existing post-admission extension), V009/V012/V013, docs/14 extension semantics, module boundary tests/maps and docs/12 adapter rules. Hashes below are SHA256 of raw working bytes, not normalized repository blobs; these read-only inputs were preserved. The initial `.git` indirection is Windows-specific; read-only Git commands used `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call21` and `GIT_WORK_TREE` set to this checkout, without editing metadata.

| Inspected input | SHA256 at intake |
| --- | --- |
| Original `docs/gateflow/pa19-sharing-media-gateway-playback-foundation-plan.md` | `6f28070623a768380a7e14f3b4edf39fa0b100ac8716902a1b468663372df37f` |
| `docs/gateflow/project-execution-control.md` | `007d9bb03f07287a831300151226f807aea6b18754a2a96ab197952c08a76f62` |
| `docs/reviews/plan-review-20261108-131036.md` (A) | `cf7c773cccb4540bfe00d92d0320e17fec78e4bb5898577f6080d25a719866d9` |
| `docs/reviews/plan-review-20261108-132655.md` (B) | `e2ab91a48bcd5dcd9403b01e555d120c3147b109c5204eacdf713dcde3bb6766` |
| `docs/gateflow/pa19a-sharing-evidence-media-read-acl-final-closeout.md` | `ba56d64f755729d43dce3ca258b61b2673e8dea565e0e36e5489489d19053902` |
| `backend/src/main/java/com/onecall/core/evidence/mediaread/EvidenceMediaObjectReader.java` | `5a67cfbe41efbde43c6ddd55c0bb3d96a633f5951fff6870e6f63c253713dc0c` |
| `backend/src/main/java/com/onecall/core/evidence/mediaread/ResolvedMediaObject.java` | `0dd53046467da8238bf26b4cbb04039b153a3cf0ed71591ce12f80bd1017c371` |
| `backend/src/main/java/com/onecall/core/evidence/mediaread/package-info.java` | `71b0a458238c10796938fd0dd0b7ede7ac7a3a9e4c7989ab38d4a99460bebde8` |
| `backend/src/main/java/com/onecall/core/evidence/internal/mediaread/JooqEvidenceMediaObjectReader.java` | `faf873f15b0d52cf5bc04d00d171802533660eec5996f65d84e2bebb04a37a49` |
| `backend/src/main/java/com/onecall/core/evidence/internal/mediaread/EvidenceMediaReadPersistenceConfiguration.java` | `9728540b756df1dfb48a8d52dab9ac7b7eb18e11543263ed6af1810f067d5135` |
| `backend/src/main/java/com/onecall/core/sharing/internal/admission/ShareMediaCredentialProvider.java` | `bacf3086c9bf48b85a1456c1554fd47958909364f9b13fe0ad2f2c8c7465421c` |
| `backend/src/main/java/com/onecall/core/sharing/internal/admission/DefaultShareMediaAuthorizationService.java` | `08c6251ad563dc0846f6ac6713624465c1dcbad1b2e2bdc4dc5346ea9c16ba24` |
| `backend/src/main/java/com/onecall/core/sharing/internal/admission/DefaultShareAdmissionService.java` | `26fe8b793ab124cceee747f0bb9333efa2ab80837d0701627721a39f16a305c9` |
| `backend/src/main/java/com/onecall/core/sharing/internal/admission/ShareAdmissionRepository.java` | `6405a7bd1a22a330ae4eef9667ad8bf506e7fc7333b2e99ce494826969eec072` |
| `backend/src/main/java/com/onecall/core/sharing/internal/admission/JooqShareAdmissionRepository.java` | `6ca0d0deb9df83c1a08a200e6d1887691771e6a314bd72749141fd6b3ae64c18` |
| `backend/src/main/java/com/onecall/core/sharing/internal/persistence/ShareAdmissionPersistenceConfiguration.java` | `fcf3dc38bd63e7bd4f6dc2ea0867c18b624eaa17e6901022bbc7ba4ace163e63` |
| `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java` | `276df40b2e06bd8ce54f8ee169dd7fa5058310a80ad966ad5cd3a0653aaadf8b` |
| `backend/src/test/java/com/onecall/archtests/sharing/ShareAdmissionServiceTests.java` | `44f5cb96dc4088a7910c9fc469013b0ba0a478dfec38fd4d84c700d0cbbc5d5a` |
| `backend/src/main/resources/db/migration/V009__sharing.sql` | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` |
| `backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql` | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` |
| `backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql` | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `207d22ab35ad8140708a5344127511cd39c2b4794d66168e82d06b9b3a7f46a9` |
| `docs/12-technology-stack-and-system-architecture.md` | `9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db` |
| `docs/gateflow/pa0-production-runtime-bootstrap-plan.md` | `9edaff561cb51611d7097fa5fc5e3d3cfbe867211f29156d451b4f10063605a7` |
| `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` | `7ad55b7bb1d054938e3841b40994ea22a117cf856176f4b700d798b1af28bcbf` |
| `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` | `37b060b7d4f04ff7e90bfb48c060f52d659add5e314bdeb39c2a06b6016be31e` |

Control supplies the PA19A merge `3afc3e9a68900ea718b534650ca661861f036d4e`; its older final-closeout document records the pre-merge checkpoint, not current merge authority. No remote fetch or external state verification was performed or needed for this bounded local fix.

## 3. Accepted findings and author-reported status

| Finding / precision | Author-reported status and exact disposition |
| --- | --- |
| A-01 medium — grant extension wrongly rejected | Addressed in plan §5 step 5 and §7 state-reader method 3 / IT method 3. Require `witness.grantExpiresAt <= current grant.expiresAt`; retain the minimum of current grant and frozen witness grant/admission/session expiries. Extension never rewrites the witness or lengthens admission/session/read authority. Existing methods must exercise an ACTIVE/unexpired post-admission extension with revision+1, Allowed/open only within frozen bounds, beyond-frozen claim Denied(SCOPE_MISMATCH), exclusive expiry denial and no added admission/event/count; only the fixture extension changes revision. No test method was added or implemented. |
| A-02 low — outcome identifier contradiction | Addressed in §4 redaction rules and §7 service method 20. The internal success handle intentionally retains exact object UUID/hash/size/MIME/duration and purpose/window/expiry; serialization/toString expose constant redacted labels, logs/exceptions expose no sensitive facts. Existing scope equality is preserved, not replaced with identifier removal. |
| A-03 low — inconsistent Allowed not pinned | Addressed in §4 step 6 / outcome matrix and §7 service method 10. Null checkedAt or non-null checkedAt before issuedAt/notBefore or at/after expiresAt is Unavailable, evidence/open=0. Only explicit state-reader Denied carries time-denial reasons at that boundary; the two local clock fences still emit their documented time denials. |
| B-01 low — provider module-boundary precondition | Addressed in both §9 production-provider residual rows. Any future cross-module verifier/reader requires an approved PA0-R-09 allowedDependencies/named-interface revision and exact architecture-map review, plus docs/12 adapter-placement resolution. An evidence→sharing bean would be a cycle and is not pre-authorized. No provider boundary is opened here; the original exact evidence::media-read refinement remains unchanged. |
| Trusted expectedPurpose / RENEW | Addressed in §4 step 4 and §7 service method 2: trusted internal invocation intent, never a client wire value; exact claim match, no broader RENEW read or extension of authority. |
| Dual local/database clocks | Addressed in §9 Q22 residual: local Clock versus repository.now()/clock_timestamp(), including provider-claim/admission-time comparisons; no numeric skew/tolerance invented. |
| Independent object-scope provenance | Addressed in §9 credential-provider residual: production mediaObjectId/duration/window comes from the authenticated issuance-time claim independently of consumption-time evidence lookup, making SCOPE_MISMATCH non-self-referential. Wire format remains undesigned. |
| Missing/inconsistent grant bounds | Addressed in §5 step 2 and §7 state-reader method 3: null validFrom/expiresAt and validFrom >= expiresAt, including null bounds on ACTIVE/LOCKED, are Unavailable before dereferencing. Explicit REVOKED/EXPIRED status precedence is retained. |
| Deterministic explicit-provider replacement | Addressed in §3 Wiring and §7 configuration methods 1/3. Choose service-local Unavailable fallbacks, not conditionally registered fallback beans. All four typed ObjectProviders enforce zero→local fallback, one→explicit bean, multiple→fixed-safe configuration failure even with @Primary. A private bounded stream().limit(2) candidate check lives in the existing configuration allowlist path; test both registration orders with overriding disabled and duplicate candidates inside existing method 3. No new resource/configuration file or allowlist widening. |

All statuses above are the author's report about plan text only. Imported A/B verdicts are historical and were neither changed nor treated as a fresh review of this revision.

## 4. Exact plan sections changed and preserved contracts

| Plan section | Change in this fix |
| --- | --- |
| §1 gate metadata | Append fix task/date/requested runtime and no-acceptance/readiness/re-review qualification; original goal/success/non-goals unchanged. |
| §3 Wiring | Replace scan-order-sensitive @ConditionalOnMissingBean verifier/reader defaults with deterministic service-local fallback/type-uniqueness contract; adjust only configuration factory signatures/helpers, all inside the same authorized file. |
| §4 steps 4/6, outcome matrix, exception/redaction rules | Trusted intent/RENEW, inconsistent Allowed→Unavailable branch, exact internal scope retention versus redacted exposure. |
| §5 steps 2/5 | Enumerate grant-bound failures; monotonic extension and frozen claim/read cap, no witness/session/admission extension. |
| §6 post-table gate sentence | Distinguish original plan-only authority from current exact two-document fix authority; all 21 implementation rows unchanged. |
| §7 existing method descriptions only | Service 2/10/20, state reader 3, configuration 1/3, MediaPlaybackIT 3; no method renamed, added, removed or recounted. |
| §9 residual rows only | Credential-provider provenance/boundary preconditions, Q22 local-vs-DB clock skew, controlled-reader provider boundary/adapter preconditions. |
| §10 appended fix validation paragraph | Separate historical plan validation from the fix baseline, preserved pre-existing Control/reviews/.codex, exact two-path audit and external final identities. |

Preserved: one integrated slice; byte-for-byte identical 21 implementation allowlist rows (`8 new main + 3 existing main + 4 new tests + 2 existing tests + POM + CI + report + Control`); all 40 planned new method names and their order; 20/8/4/2 unit-method allocation and six IT methods. Planned source=CI=XML remains `128+34=162`, `192+6=198`, `320+34+6=360`, classes `25/162 + 12/198 = 37/360`, and comment-inclusive line scan `321→361`. Entire §8 inventory/commands/migration-hash contract and §2 evidence table are unchanged. V001–V013 remain byte-identical and no V014 exists. No public API, schema, ACL, migration, production provider/policy, transport, PA18 issuance/event/count behavior or PA19A seam changed.

## 5. Validation performed and final identities

- Captured a 1,555-file raw-byte workspace inventory before edits, excluding `.git` metadata and the untouched pre-existing `.codex` marker; scratch baseline JSON SHA256 `826aca559cecd1bbe931636305932f1b71e740152071c3085971b6fb421f3950`. Compared the final path/hash inventory: exactly the plan modified and this artifact added, no deletion or other byte change. Thus all source/test/allowlist implementation paths, migrations, POM/CI/OpenAPI, Control and imported reviews retain their intake bytes. Scratch validation files live only under `/tmp`, not in the repository.
- Compared original/fixed plan structurally: all 21 exact allowlist rows equal; all 40 planned method names/order equal; §2/§8 and motivation/success/non-goals equal. Read-only source inventory reproduced `22/128 + 11/192 = 33/320`, line scan 321 and exactly 13 unchanged migrations. The first scratch line-scan assertion mistakenly put a word boundary before `@`; correcting that validation regex to `@Test\b` reproduced 321. No repository source/test was changed to satisfy the audit.
- Whitespace validation is restricted to these two paths: `git diff --check -- <plan-path> <fix-path>` and, for each untracked artifact, `git diff --no-index --check /dev/null <exact-path>`. No whitespace error; Git's existing LF→CRLF advisory is not a whitespace failure and no file normalization was performed. UTF-8/no-BOM/no-CR/no-trailing-whitespace/single-terminal-LF checks cover only these two documents.
- No Maven/build/runtime/container test, fresh XML comparison, implementation or independent re-review was run. The source count is a read-only baseline measurement; 162/198/360 remains an implementation target, not fresh test evidence.
- Final plan identity: **404 physical lines / 71,656 bytes / SHA256 `da9f33775d0641fa92236e500bcdb27fa1944f1629d783697e38e478f0fe6b2a`**. Final `wc -l -c` / `sha256sum` identities for both documents are recorded in the author completion report/tool output, outside this artifact to avoid a self-hash.

## 6. Remaining residual owners and destinations

| Residual | Owner / destination retained |
| --- | --- |
| Production credential verification, signatures/keys, authenticated issuance-time independent object scope | Security + Platform + Sharing; C10 future credential-provider plan. First resolve PA0-R-09 module/named-interface approvals and docs/12 adapter placement; default remains Unavailable, no wire design here. |
| Production controlled reader, private location resolution, COS/KMS/Secret Manager | Platform + Security + Evidence; future controlled-reader provider plan. Same approved boundary/adapter preconditions, no evidence→sharing cycle authorization; default remains Unavailable. |
| Q22 numeric TTL/skew/revocation propagation, local-vs-DB clocks and snapshot-to-open race | Security + Sharing + Platform; Q22 policy decision. No numeric tolerance, continuous recall or zero-race promise. |
| Transport/player/origin/CDN/range/streaming/byte delivery | Infrastructure + Security; R-A3-MEDIA-GATEWAY-TRANSPORT. No actual transport work in this slice. |
| End-to-end collector/edge/analytics redaction | Platform + Security + Observability; R-A3-REDACTION-RUNTIME. Local redaction tests are not a deployed-system claim. |
| Evidence integrity/optional duration/ambiguous facts and existing reader deployment credentials/ACL operational constraints | Evidence + DB/Platform/Security; plan §9 and unchanged PA19A closeout residual owners. Fail closed; no new ACL, migration or guessed metadata. |
| Implementation prerequisites, inventory or architecture contradiction | Backend implementer + Controller; exact future implementation report/Control checkpoint. Stop for scope correction rather than expand the 21 paths or weaken tests. |

This author fix stops here. Controller intake and any later gate decisions remain separate; no acceptance or readiness conclusion is made.
