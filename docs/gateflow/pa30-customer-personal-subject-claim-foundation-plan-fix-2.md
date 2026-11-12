# PA30 plan fix 2 — accepted re-review finding closure

## 1. Authority and outcome

- Task: `pa30-plan-fix2-gpt-20261112-01`; date: 2026-11-12 (Asia/Shanghai).
- Supplied runtime/provider: Codex `gpt-6.1-sol`, reasoning `high`; dispatch metadata, not independent model introspection.
- Gate: **fix after independent plan re-review**. Both accepted re-reviews and current Control PA30 goal/re-review adjudication were read. No delegation.
- Supplied base: `a4c6abacc8afeec14d0308e854a7daff442a770f`; not locally Git-verified because Windows Git interop is unavailable.
- Authorized writes: modify the plan and add this fix-2 artifact, exactly two documents. Fix-1, Control and review inputs remain byte-identical.
- Result: REV-09, REV-10 and REV-11 closed at the plan level; all prior Controller-accepted REV-01 through REV-07 findings remain binding. REV-09 completes REV-05. No implementation or later gate occurred or is requested.

## 2. Exact inputs and output identity

All five raw SHA-256 identities matched the supplied dispatch before editing:

| Input | Raw SHA-256 |
| --- | --- |
| Plan before fix-2: `docs/gateflow/pa30-customer-personal-subject-claim-foundation-plan.md` | `9cf8d1328b896686b9a297f355f0504ead323ffaabecc0f1ba9b6893ab135f75` |
| Fix-1: `docs/gateflow/pa30-customer-personal-subject-claim-foundation-plan-fix-1.md` | `0d7c3f182d1df383b7a75751fc029cf2297021a2e6632029c8af5fb972500fd0` |
| Control: `docs/gateflow/project-execution-control.md` | `96f1b5af827fd5bea40ab15aa26091a1e9df95c22cc5217b721ef0356b267920` |
| Re-review A: `docs/reviews/plan-review-20261112-231948.md` | `26423a222472ad1c3021415f38091a92332d2931898a005a7f66f8adfb9e3351` |
| Re-review B: `docs/reviews/plan-review-20261112-232827.md` | `85ab998085b56b112f5e24b368aeb92ae0d239d1251476cd2cd3d36222bc2dcc` |

Control's hash here is dispatch/input provenance only, not an implementation freeze or A06 oracle. Control is mutable Phaseflow goal/gate authority. Fix-1 and both accepted re-reviews remain historical evidence; this plan/fix-2 supersedes only their manifest/freeze arithmetic and incomplete firewall-command claims, without editing them or dropping any accepted behavioral finding.

Changed plan final raw = LF SHA-256: `71188f38486578979cfcc7a9949900c3b06f90a14ae318002f3a0281409a0191` (99009 bytes; 744 LF lines). Fix-2's final raw/LF hash is emitted by validation and the completion response, not recursively embedded in itself.

## 3. Finding closures

### PA30-PLAN-REV-09 — High: four exact existing dependency assertions

Plan §4 adds existing manifest rows 38–41 and a contiguous fully expanded 41-path allowlist. Plan §9.3 binds the exact assertion-only edits below; §10.1's focused existing-test command includes all four classes. In every case the customer package map gains only `subject_number::customer-subject-claim-command`, retaining all ten old entries and every other assertion, method identity and method count.

| Existing file (under `backend/src/test/java/com/onecall/archtests/architecture/`) | Exact existing method | Required edit |
| --- | --- | --- |
| `CustomerBusinessNumberBoundaryTests.java` | `customerImportsOnlyPublicQuerySeamsAndNoForeignInternalImplementation` | Customer dependency `hasSize(10)`→`hasSize(11)` only |
| `CustomerEvidenceBoundaryTests.java` | `namedOwnerInterfacesAndTenCustomerDependenciesAreExact` | Exact ten-element customer dependency set/list gains the named edge, becoming eleven; retain `Ten` in method name |
| `CustomerSubjectDetailBoundaryTests.java` | `subjectEvidenceAndOrderExposeOnlyMinimalNamedQueryInterfaces` | Customer dependency `hasSize(10)`→`hasSize(11)` only |
| `SharingQueryBoundaryTests.java` | `sharingQueryApiAndProjectionAreExact` | Exact ten-element customer dependency set/list gains the named edge, becoming eleven |

Manifest: **41 unique =25 new +16 existing**. Existing16 =4 main-source +1 persistence YAML +1 POM +1 CI +1 README +8 existing test files. New25 =16 main-source +7 test classes +2 annotation-free helpers. These four extra existing files add no tests/classes. Existing ModuleBoundaryTests/CustomerApiBoundaryTests and default-context edits remain exactly as previously accepted. Static status verification confirms all new paths absent and all existing paths present.

### PA30-PLAN-REV-10 — Medium: firewall prerequisite extraction

Plan §10.2 now starts with `set -eu` and a backend-working-directory subshell, removes stale prerequisite outputs and executes these exact CI commands before any extracted Python body:

```bash
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
./mvnw -B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa11-runtime-dependencies.txt
```

Both files must be nonempty (`test -s`); the Python extractor independently asserts both are present/nonempty before dispatch. All three original bodies still execute with `cwd='backend'`, `check=True`, in inventory → packaged JAR smoke → firewall order. The expected evidence remains source=CI=XML equality, packaged route/default-profile probes, exact firewall **82 intermediate /116 final** and the JAR index/dependency inventory assertions.

Plan-only verification used an isolated `/tmp` harness with fake jar/Maven commands and intercepted `subprocess.run`: successful prerequisite creation; jar/Maven command failures; empty JAR index; absent dependency output; independent removal of either prerequisite before extraction; exactly three compilable CI bodies dispatched in backend. No real Maven/JAR/HTTP/firewall body was run. The harness validates command ordering/fail-closed mechanics only, not actual packaged/runtime evidence.

### PA30-PLAN-REV-11 — Medium: mutable Control excluded from freezing

Plan §2, A06 in §9.1, §10.3's Git/hash commands and §11–§12's report/completion contract now use exactly **20 unique implementation-frozen paths =three non-migration files +17 migrations**: OpenAPI, docs/14, docs/15 and V001–V017. V003 is counted once. Control is excluded from both raw/LF freezing and A06 while remaining authoritative for current goal/gate state. No stale implementation-freeze count or Control hash oracle remains in the revised plan/fix-2; the input-provenance table above is not a freeze.

All 20 raw/LF expected pairs are recorded in §6 and independently match the unchanged tree and historical contract/migration identities. docs/15 protection is retained, not traded away. Historical fix-1/re-review text is superseded rather than modified, as required by the write boundary.

## 4. Preserved design and test arithmetic

The revised plan's entire design sections §5–§8 are byte-identical to the input plan. One behaviorally coherent slice, one authoritative transaction per whole attempt (three attempts maximum), local fail-closed SPIs only; no preparation state machine, staged payload map, `PreparationChanged`, network/KMS/provider I/O under locks or generic retry framework. Risk → immutable row identity → candidate stored open, with zero foreign opens, remains binding. Public statuses stay exactly 200/401/403/409/422/429/503/500 with three domain 409s. V001–V017 ACL reuse/no V018, precise fixture wiring, audit mapping and sensitive constant-redaction obligations are unchanged.

| Inventory | Current source/CI | Additions | Final implementation requirement |
| --- | --- | --- | --- |
| Surefire classes/methods | 49/417 | 5/51 | 54/468 |
| Failsafe classes/methods | 21/336 | 2/34 | 23/370 |
| Combined classes/methods | 70/753 | 7/85 | 77/838 |
| Annotation lines | 754 | 85 | 839 |
| POM explicit Failsafe includes | 21 | 2 | 23 |
| Customer internal source files | 39 | 4 | 43 |
| Flyway migrations | 17 | 0 | 17 |
| Production modules | 14 | 0 | 14 |

Exactly **85 unique proposed method identities**, unchanged from the input plan: W12 +U17 +R8 +C8 +A6 =51 Surefire; I24 +L10 =34 Failsafe. U06 remains absent; all IDs/names remain unique. Current Java method sets equal the CI's literal map-building inventory, with exact POM class/include equality; the sole historical commented annotation stays preserved. Final source=CI=XML equality is still a future implementation obligation, not runtime evidence from this fix.

## 5. Validation actually performed and scope

| Plan-only check | Actual outcome |
| --- | --- |
| Five exact dispatch input hashes; unchanged fix-1/Control/re-review artifacts | PASS |
| Plan/fix-2 UTF-8 without BOM, LF-only, final newline, no trailing whitespace; raw/LF hashes | PASS; final independent validator emits both output hashes |
| Manifest row ordinals, unique expanded path membership, contiguous allowlist and on-disk status | PASS; 41 =25 absent/new +16 present/existing |
| Proposed test identities unchanged, unique IDs/names, exact split | PASS; 85 =51+34 across seven proposed classes |
| Current source=CI method maps, annotation scan and POM include equality; final arithmetic | PASS statically; 54/468 +23/370 =77/838, annotation839/POM23/customer43/migrations17/modules14 |
| Design preservation | PASS; plan §5–§8 byte-identical to input; no existing implementation/test file changed |
| Frozen raw/LF pairs, docs/15, migration set and V003 counted once; no stale freeze/Control oracle | PASS; 20 =3+17, exact pairs in §6 |
| Exact §10.2 preamble and three CI-body extraction/dispatch with missing-file failures | PASS in stub-only harness; real 82/116/JAR evidence NOT RUN |
| `git.exe diff --check`, `git.exe status --short`, `git.exe diff --name-status` | ATTEMPTED; each exit1, WSL `UtilBindVsockAnyPort: socket failed 1`; not passing Git evidence |
| Before/after whole-workspace SHA-256 snapshot | PASS; 1867 initial files →1868 final: one authorized plan modified, one fix-2 added, all other 1866 files unchanged, no deletions |
| Real Maven/Docker/PostgreSQL/XML/JAR/firewall/OpenAPI toolchain/endpoint validation | NOT RUN at plan fix; deferred to implementation gate |

Ancestor `.codex/AGENTS.md` is empty; no additional scoped instructions were found. Private scripts, baseline snapshots, textual diff and stub outputs are under `/tmp`, not repository deliverables. Windows Git tracked status/diff/base cannot be confirmed in this environment; no Linux-Git substitution or unverified tracked-status claim is made. Filesystem evidence independently confirms exactly the two authorized task writes, with pre-existing Controller/input artifacts unchanged. Controller must repeat Windows Git checks in a functioning Windows environment; this limitation does not authorize a gate transition.

An initial private snapshot comparison mixed absolute and relative path keys and failed before providing scope evidence. The validator was corrected to workspace-relative keys and rerun successfully; no repository content was changed by that diagnostic failure.

## 6. Exact implementation-frozen raw/LF pairs

**20 unique paths =OpenAPI +docs/14 +docs/15 +17 migrations; no Control.** All pairs match both the historical contract/schema evidence and current filesystem bytes; none was modified during fix-2.

| Frozen path | Raw SHA-256 | LF-canonical SHA-256 |
| --- | --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` | `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31` | `bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58` |
| `docs/15-production-openapi-surface-spec.md` | `db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0` | `0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02` |
| `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| `backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql` | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| `backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql` | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| `backend/src/main/resources/db/migration/V007__activation_and_consent.sql` | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |
| `backend/src/main/resources/db/migration/V008__evidence_media.sql` | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| `backend/src/main/resources/db/migration/V009__sharing.sql` | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| `backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql` | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` | `fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343` |
| `backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql` | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` | `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` |
| `backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql` | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` | `13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828` |
| `backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql` | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` | `691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6` |
| `backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql` | `bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8` | `e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a` |
| `backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql` | `c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc` | `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3` |
| `backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql` | `a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c` | `988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f` |
| `backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql` | `74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3` | `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad` |

## 7. Gate boundary and remaining verification

No design blocker remains after the three accepted plan corrections. Residual operational verification is unchanged: real fixture boot, dedicated login/privilege/PID/lock/retry cleanup, concurrency, audit durability/redaction, complete reports and packaged JAR/firewall evidence require the later implementation gate; a true privilege gap still stops `BLOCKED_NEEDS_GOAL_RECONFIRMATION`. Control stays mutable authoritative gate state, never a byte-frozen implementation input.

No source/test/config/migration/OpenAPI/CI/POM/README/existing other document/Control edit, delegation, commit, push, PR, reviewer request, approval, ready action, external comment/issue mutation or later gate occurred. No implementation, fresh review request or acceptance/production-readiness claim is made. Stop at fix-2 completion; only the Controller can authorize any next gate.
