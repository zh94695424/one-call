# PA34 activation-history cumulative plan — fix 2

Task: `pa34-operations-activation-history-plan-fix2-gpt-20261115-01`.

Date: 2026-11-15 (Asia/Shanghai). Work unit: `PA34-operations-activation-history-read-foundation`. Gate: **bounded fix only**.

Managed worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa34-next-work-unit/codex-call`.

## 1. Authority and completion boundary

The binding PA34 Goal Confirmation and Controller decisions in `docs/gateflow/project-execution-control.md` remain unchanged. Inputs are the current Control PA34 section, cumulative plan, fix-1 evidence, and independent reviews `docs/reviews/plan-review-20261115-033925.md` and `docs/reviews/plan-review-20261115-035649.md`. The accepted finding is `PA34-PLAN-FIX-01` (Low), a stale fixture instruction contradicting accepted `PA34-PLAN-REV-03`.

This fix changes only the fixture cursor-configuration wording at plan line391 and adds this artifact. Only the frozen `GET /api/v1/operations/activations` read remains in scope: exact page/item projection, ACTIVE ADMIN/OPS employee authority, cursor paging, one read-only snapshot and no request writes. No endpoint, filter, field, table, migration, grant, role, dependency, test identity, slice or readiness claim is added or removed. No plan bookkeeping or unrelated wording is changed; the historical fix-1 measurements and P/F manifest entries remain historical evidence, not rewritten fix-2 metadata.

**Independent A/B re-review is pending. The fix does not self-pass.** Controller scheduling/acceptance and any implementation authorization remain separate; neither re-review nor implementation is started here.

## 2. Exact PA34-PLAN-FIX-01 correction

Location: `docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md:391`, section6, Fixture/helper boundaries. The exact replaced context is:

Before:

> Start web+persistence with existing read datasource/enabled properties and the new activation cursor properties, real HTTP random port and scoped synthetic keys; no new production grants/scripts.

After:

> Start web+persistence with existing read datasource/enabled properties and a real HTTP random port; the fixture supplies scoped synthetic cursor key values only to the existing shared `onecall.operations-subject-read.cursor.*` property names. No activation cursor prefix exists, and no separate property class, binder or operator secret is introduced; no new production grants/scripts.

Disposition: **wording fixed; independent re-review pending**, not a review-pass claim. Full-plan searches covered activation/cursor/property/prefix/binder/key-ring/secret wording and synonyms. No other affirmative separate-prefix/property-class/operator-secret instruction remains. Section4.2 specifies only the existing shared property names and unchanged binder; section10 assigns custody to the existing subject-read secret process. The retained method identity `activationKeysUseOnlyExactEndpointSpecificProperties` is explicitly described in section6 as testing shared configuration, rejecting activation-style keys, and introducing no separate binder or secret; section6 expressly preserves legacy identities. It is not permission to create an activation property prefix, and no test name is changed.

An inverse replacement reconstructs the exact pre-fix raw SHA-256, bytes and lines below, proving that every other plan byte, including all manifests, remains identical. This is one changed line, +190 bytes and no line-count change.

## 3. Plan identity and preservation baseline

Path: `docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md`.

| Measurement | Before fix 2 | After fix 2 |
| --- | --- | --- |
| Raw SHA-256 | `5f1e649c4234210f5a1a40e3597658b5ab1419dd4cbf029784ae2170533c5662` | `7d93c2773e30168e5e7bd7969211cd342b47e48badf2040c2ea48de276d0ce38` |
| Raw bytes | 83,335 | 83,525 |
| Lines (LF count) | 574 | 574 |

This fix-2 artifact's own final raw SHA-256, bytes and lines are reported **outside only**, not embedded here, avoiding a self-referential measurement.

Read-only input identities at fix-2 start, preserved at completion:

| Path | Raw SHA-256 |
| --- | --- |
| `docs/gateflow/project-execution-control.md` | `efc2cd19e2959a0f74d1de3086a0622fed37881faf0a5ca6317054f6855b3b6b` |
| `docs/gateflow/pa34-operations-activation-history-plan-fix-1.md` | `d3882c4b2eb72a1a5cae1998be2d37f5bf0a78fa906cb1a73b011e12291d628e` |
| `docs/reviews/plan-review-20261115-033925.md` | `cd1cadcf68eb86a36ee049c0a6fba9b944c4abd5074125a480d8203db289ef76` |
| `docs/reviews/plan-review-20261115-035649.md` | `00a59c3d094049175253959192b3efbc4c284407a5c3e9e872b1020253553d97` |

A whole-worktree raw-byte preservation check excludes only the two authorized output paths. It covers 2,067 existing files, including `.git` pointer, prompts, helpers, Control, fix1, reviews, implementation, tests, frozen documents and inputs. Its sorted aggregate SHA-256 before and after is `b64caf0189f73d82dd7a096082f2eb698a8cc27a48b1fe6a04e4075275346c51`: for each sorted relative POSIX path, hash the concatenation of UTF-8 path, NUL and its raw 32-byte SHA-256 digest. No excluded output existed newly except this artifact; no other file was added, removed or changed. External Git metadata is used read-only and is not included in this worktree-file aggregate.

## 4. REV-01..04 and exact arithmetic re-verification

All findings below remain corrected in the cumulative plan; these are static author checks, not independent review conclusions or runtime evidence.

| Finding | Re-verified requirement and plan location | Result |
| --- | --- | --- |
| `PA34-PLAN-REV-01` | Sections3/4.3/6 distinguish nine V007 DB-persistable shapes from five domain-legal shapes: INACTIVE→ACTIVE via IVR/IVR_CONFIRM, MINIAPP/ACTIVATION or WEB/ACTIVATION; ACTIVE→SUSPENDED and SUSPENDED→ACTIVE via OPERATIONS/OPERATIONS. Four DB-shape-only negatives are INACTIVE→ACTIVE via OPERATIONS/OPERATIONS and ACTIVE→SUSPENDED via IVR/IVR_CONFIRM, MINIAPP/ACTIVATION or WEB/ACTIVATION. Each invalid row, including lookahead, fails the entire page with sanitized503. Constraints remain unchanged. | PASS, retained |
| `PA34-PLAN-REV-02` | Sections3/4.1/6 retain port-local eleven-field `TransitionFacts`, all raw fields nullable and revisions boxed `Integer`, no constructor/owner semantic validation or unboxing. Owner maps SQL only; service exclusively validates all returned facts/lookahead before constructing the public eleven-field record with primitive `int` revisions. Malformed stub injection remains possible. | PASS, retained |
| `PA34-PLAN-REV-03` | Sections4.1/4.2/6 and corrected line391 reuse `OperationsSubjectReadProperties` and unchanged existing binder/key ring `onecall.operations-subject-read.cursor.*`; no activation prefix/property class/binder/operator secret. AAD retains exact endpoint purpose `onecall:operations-activation-list:cursor:v1\0`, fixed sort `occurredAt desc, transitionId desc\0`, and raw SHA-256 of UTF-8 `{}` without BOM/newline. Same-ring subject/activation tokens fail cross-purpose decoding in both directions; limit is not bound. Missing/bad shared material fails503. | PASS, retained and wording aligned |
| `PA34-PLAN-REV-04` | Section6 G retains captured application/server/pool/jOOQ logs inside existing `onlyTransitionFactsAreReadAndNoHiddenFieldsLeak`: success, malformed/tampered cursor400, dependency/integrity503 and empty200, with cursor, employee identity, SQL/JDBC and key-material canaries absent. Harmless marker proves capture; rendered/argument/MDC/throwable data inspected and fixture-local capture cleaned in finally. No nineteenth IT method or dependency. | PASS, retained |

The existing shared property carrier and actual `OperationsSubjectReadPersistenceConfiguration.operationsSubjectReadProperties` binder were checked directly. A preliminary filename lookup used the nonexistent shorter `OperationsSubjectReadConfiguration.java` (rg exit2); the actual persistence-configuration path was then located and inspected read-only. This lookup is not a validation pass or a source change.

Path verification resolves all section5 prefixes to exact paths and checks sequential unique IDs01–24, existing-file presence and future-new-file absence:

- **24 future S1 rows = 19 new / 5 existing**, including Controller-owned row23; 23 implementer-owned paths if the Controller applies its checkpoint.
- **Eight new production Java and ten new test/helper Java**; two existing filter Java, existing POM/CI/Control and one future new implementation report account for the rest. No activation property class or replacement file.
- All section6 A–H method identities are unique per class and remain byte-identical to fix-2 input: A12/B8/C8/D10/E14/F8/G18/H10. Six unit classes total60; two IT classes total28; **60+28=88**. Helpers have zero planned test annotations.
- Current source scan uses existing CI's ordinary `@Test` method regex and word-boundary token-line rule: **70/644 Surefire +29/476 Failsafe =99/1120**, token1121. Sole existing PostgresContainerIT annotation-token comment is retained. Current POM has29 unique Failsafe includes.
- Future additions remain +6/+60 and +2/+28: **76/704 +31/504 =107/1208**, token **1209**, POM includes **31**. No source/XML/runtime final inventory is claimed; these are unchanged planned totals reproduced from current static inputs.

## 5. All 21 frozen raw-byte hashes and migration set

The complete section8 manifest was parsed from the corrected plan, checked for21 unique paths and matched against raw file bytes. Every row below passed; none was normalized or regenerated from changed input:

```text
0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68  api/openapi/onecall-v1.openapi.yaml
0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31  docs/14-production-data-model-and-flyway-schema-spec.md
db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0  docs/15-production-openapi-surface-spec.md
7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e  docs/17-production-security-design-spec.md
96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6  backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql
4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505  backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql
4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7  backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql
8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030  backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql
6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065  backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql
7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d  backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql
bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81  backend/src/main/resources/db/migration/V007__activation_and_consent.sql
32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde  backend/src/main/resources/db/migration/V008__evidence_media.sql
3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1  backend/src/main/resources/db/migration/V009__sharing.sql
d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559  backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql
ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e  backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql
ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6  backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql
1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e  backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql
bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8  backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql
c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc  backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql
a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c  backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql
74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3  backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql
```

Exact migration filename-set equality passes: **17 SQL migrations, V001–V017 only; no V018**. No migration, schema, ACL or frozen-input edit occurred. Docs/12 and all other out-of-allowlist bytes also remain unchanged under section3's preservation check.

## 6. Hygiene, diff and command results

Static checks run through inline Python/shell only, with no helper, manifest, target/tmp evidence or Git metadata written:

| Check | Final result |
| --- | --- |
| Exact edit/inverse raw-hash proof; full-plan stale-context search | PASS, verification script exit0; one context replacement only. |
| REV-01..04, sequential resolved paths, eight method manifests, current source/POM counts and final arithmetic | PASS, verification script exit0; planned totals, not executed tests. |
| All21 raw frozen hashes and exact17 migration filename set/no V018 | PASS, verification script exit0; all entries printed as matched. |
| Both authorized artifacts: strict UTF-8, final LF, no CR, conflict markers, U+FFFD or trailing spaces/tabs; Markdown fences balanced | PASS, final hygiene verification exit0; zero findings. |
| Tracked `git diff --check` with existing newline configuration | PASS, exit0; empty stdout/stderr. |
| Each authorized untracked artifact: `git -c core.autocrlf=false diff --no-index --check /dev/null <path>` | Exit1 for added content, empty stdout/stderr and no whitespace diagnostics; not misreported as exit0. |
| Read-only baseline, changed-path scope and input preservation | PASS; section3's2,067-file aggregate identical, only two authorized output paths differ/exist newly. |

Linux Git cannot directly resolve the existing Windows `D:/...` gitdir pointer. Corrected read-only Git checks set `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call18`, `GIT_COMMON_DIR=/mnt/d/codex-call/.git`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0`; no pointer/config/index repair or refresh is performed. The first direct status probe failed on that pointer. A preliminary tracked check incorrectly used transient `core.autocrlf=false` and exited2 on existing CRLF files across the repository; a following no-index probe without the explicit Git environment exited128. Both were command-configuration failures, not artifact changes, and were corrected: tracked diff uses existing newline configuration; only no-index checks use the transient override, with explicit Git environment throughout. Final commands and their distinct exit semantics are recorded above. No repository newline/configuration changes were made to hide diagnostics.

## 7. Residuals and owners

| Residual | Owner | Boundary / destination |
| --- | --- | --- |
| Independent fresh peer-isolated A/B re-review and plan acceptance | Controller | Pending; this fix does not self-pass, dispatch reviewers, or authorize implementation. |
| Global occurredAt/id sort without matching global V007 index | Backend/DBA | Existing bounded performance residual; timeout/budget may fail503. Carry to authorized implementation report and existing deployment-capacity assessment; no index/migration/readiness claim. |
| Snapshot-relative employee revocation and fresh continuation snapshots | Backend/Security | Existing accepted semantics, not linearizable revocation or cross-page pinning; no new cursor field or expiry. |
| Connector rejection before boundary filters | Backend | Existing environment nuance; future authorized HTTP/filter probes, no relaxed admission. |
| Independent verifier, employee/read-login provisioning and credentials | Security/Operations IAM and DBA/Backend | Existing external prerequisites; absent dependencies fail503; no credential/role/grant work. |
| Shared-ring custody/rotation and AEAD nonce uniqueness | Security/Backend | Existing subject-read key process and future implementation report; no separate activation secret. |
| Toolchain/Docker/network and mandatory runtime validation | Controller/authorized implementer | Pending implementation validation, no waiver/skip or readiness inference. |
| Unexpected frozen/schema/privilege/API requirement | Controller/user | Stop for renewed authorization; no silent expansion or V018. |

## 8. Exact changed paths and stop

1. `docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md` — only line391 fixture cursor wording.
2. `docs/gateflow/pa34-operations-activation-history-plan-fix-2.md` — this new evidence artifact.

**Runtime NOT RUN.** No Maven/build/test, application, Docker/PostgreSQL, generated XML, JAR/firewall smoke, install, contract/client generation or acceptance validator is run or claimed. Static file inspection, regex inventory and raw-byte hashing do not substitute for implementation/runtime proof. No implementation or test is edited or added; no source, migration, OpenAPI, docs/12–19, POM, CI, Control, fix1, review, prompt, private helper or target/tmp output is edited.

No staging, commit, push, branch/PR mutation, approval, review request, external comment, re-review dispatch or implementation/later gate occurs. **Bounded fix writing and static verification complete; independent A/B re-review pending; the fix does not self-pass.** Stop for the Controller.
