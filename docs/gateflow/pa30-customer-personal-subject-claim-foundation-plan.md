# PA30 — customer personal subject-claim foundation implementation plan

## 1. Authority, gate and outcome

- Task: `pa30-subject-claim-plan-gpt-20261112-01`; supplied runtime/provider: Codex `gpt-6.1-sol`, reasoning `high`, via `codex-agent-run`. This is supplied run metadata, not an independently introspected model identity.
- Date: 2026-11-12. Controller: Phaseflow. Current gate: **plan fix 2**, task `pa30-plan-fix2-gpt-20261112-01`; supplied runtime/provider Codex `gpt-6.1-sol`, reasoning `high` (not independently introspected). No delegation. Only this plan and its new sibling `pa30-customer-personal-subject-claim-foundation-plan-fix-2.md` may be written. Fix-1 and both accepted re-reviews remain unchanged historical inputs; fix-2 supersedes their manifest/freeze arithmetic and firewall-command omissions only. No implementation or later gate is authorized.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa30-next-work-unit/codex-call`; supplied branch `codex/pa30-next-work-unit`; supplied base `b48cbb88cec5302b00e3af3be5f621f69838c20f`; supplied goal checkpoint `a4c6abacc8afeec14d0308e854a7daff442a770f`.
- Implement later, in **exactly one behaviorally coherent slice**, only `POST /api/v1/customer/subject-claims`, operationId `createCustomerSubjectClaim`. Authentication, local proof/reference validation, command persistence, replay/risk behavior and its tests together form the one verifiable increment. File/module layers are not separate slices.
- Motivation: connect an authenticated personal customer to the existing append-only verified claim model, while preserving current-risk preemption and set-once subject ownership.
- Success signal: controlled verified-session/proof fakes exercise the real HTTP endpoint and PostgreSQL 16 under a separate subject-number command login; one accepted claim and at most one null-to-account owner transition commit atomically; replay, rejection, conflict, least privilege and concurrency assertions all pass. Production default remains unavailable until real proof/reference collaborators exist.
- Not delivered: login/session issuance, subject creation, subject real-name mutation, organization claims, owner transfer/repair, activation/consent/trial/order/evidence/share writes, real SMS/WeChat/caller challenge/manual-review processing, production verification-attempt storage, KMS/key management, credential provisioning, new database grants/functions/migrations, generic lock/provider/idempotency frameworks. OpenAPI and V001–V017 stay byte-identical.

## 2. Frozen evidence and first-principles judgment

The reviewed plan raw hash was `20ec9f9245ac1407fa63913db053bb2bbb91a34e7b293b49d1b9f82ca1877455`. Both accepted reviews were read and their exact raw hashes verified: Reviewer A `docs/reviews/plan-review-20261112-225639.md` = `f853d2f3bcbafe48ecb3ee11787e5949f8522be69830752695b29ca7622f2deb`; Reviewer B `docs/reviews/plan-review-20261112-213245.md` = `90661f87f37e54eee98474755d8f62cf2273de94088fab30c7a2bfa1b9e80e7e`. Failed A drafts are not evidence and were not accessed. Control's current PA30 goal confirmation and review adjudication bind this revision.

Current implementation-frozen identities below were independently recomputed. Control is mutable Phaseflow gate state and remains the authoritative goal/gate source, not an implementation byte-frozen artifact or an A06 hash oracle. No Control edit is made here. Both accepted re-reviews were read: A `docs/reviews/plan-review-20261112-231948.md` raw `26423a222472ad1c3021415f38091a92332d2931898a005a7f66f8adfb9e3351`; B `docs/reviews/plan-review-20261112-232827.md` raw `85ab998085b56b112f5e24b368aeb92ae0d239d1251476cd2cd3d36222bc2dcc`. Controller accepts REV-09 through REV-11 and preserves all prior accepted REV-01 through REV-07 findings; REV-09 completes REV-05. Exact input identities and the superseding closure record are in fix-2. Ancestor `.codex/AGENTS.md` is empty; no additional scoped instructions were found. Windows `git.exe` remains blocked by WSL `UtilBindVsockAnyPort: socket failed 1`; Linux Git cannot resolve this Windows worktree metadata. Base `a4c6abacc8afeec14d0308e854a7daff442a770f` remains supplied, not locally Git-verified. Repeat Windows Git checks before implementation.

**20 unique implementation-frozen paths = three non-migration contract/design files (OpenAPI, docs/14, docs/15) + 17 migrations.** V003 is displayed for lock/grant evidence but is already one of the 17 and is counted once. All 20 raw/LF pairs are recorded in fix-2; docs/15 protection is unchanged. Historical fix-1/re-review freeze arithmetic is superseded, not an implementation oracle.

| Frozen path | Raw SHA-256 | LF-canonical SHA-256 |
| --- | --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` | `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31` | `bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58` |
| `docs/15-production-openapi-surface-spec.md` | `db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0` | `0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02` |
| `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |

Direct evidence read; paths below are relative to the repository unless stated otherwise:

| Evidence | Implementation consequence |
| --- | --- |
| OpenAPI lines 455–527 and 4534–4572 | Exact operation, closed four-field input, three public domain 409s, 200 stored result, optional accepted-only `verifiedAt`. No contract editing needed. |
| `docs/15-production-openapi-surface-spec.md:238` | Internal claim reason codes are translated, not exposed. Invalid proof is 422. |
| `docs/14-production-data-model-and-flyway-schema-spec.md:973`, `:1013`, `:3053`, `:3175` | Append-only history, decrypted logical replay, exact advisory namespaces/order, whole-transaction retry without savepoints, risk before replay. |
| `V003__subject_number_and_claim.sql:57`, `:104`, `:187` | Global request uniqueness, partial accepted uniqueness, immutable owner/claim triggers, sufficient owner-local read/insert/update grants. |
| `V002__identity_organization_and_employee.sql:241`, `:303` | Subject-number can read account `id,status`; no foreign account row locking or new account privilege is needed. Organization projections exist but this personal operation does not use them. |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql:439`, `:532` | Existing `audit.append_fact` is executable by `subject_number_role`; audit is possible without direct audit DML or grants. |
| `subject_number/package-info.java:1`, existing `customerquery` / `customersubjectquery` | Subject-number currently has no module dependencies. Publish a narrow named command interface; keep SQL in its owner. |
| `customer_iam/package-info.java:1`; `CustomerSubjectsController.java:26`; `CustomerWebConfiguration.java:24` | Customer web controllers, qualified lazy services, trusted principal, customer-owned composition and closed problem writer are the established seams. |
| `CustomerBearerAuthenticator.java:25`; `CustomerContextService.java:30` | Reuse session authentication, not `getCustomerMe`: the latter maps disabled accounts to UNAUTHENTICATED, contrary to this command's required FORBIDDEN mapping. Read current account under the owner command's lifecycle lock instead. |
| `CustomerShareCommandJsonBinder.java:15`; `CustomerBoundaryFilter.java:45` | Reuse duplicate-key/trailing-token rejection and exact-route filter admission; do not rely on lenient Jackson DTO binding or bypass filter admission. |
| `SharingCommandPersistenceConfiguration.java:36`, `SharingCommandPersistenceProperties.java:5`, `JooqSharingCommandRepository.java:38` | Copy the small explicit dedicated-pool / qualified DSL / READ COMMITTED / REQUIRES_NEW / role assertion pattern; no new transaction framework. |
| `application-persistence.yml:1`; `CustomerBusinessNumbersFixture.java:27`; `CustomerShareCommandsIT.java:66` | Existing environment-property naming, PostgreSQL 16 / 17-migration fixtures, synthetic NOINHERIT logins, transaction-local role probes. |
| `.github/workflows/ci.yml:1238`, `backend/pom.xml:183`, architecture suites | Actual current source/CI baseline is 49/417 + 21/336, not older README history. POM has 21 Failsafe includes. Source-only CI/POM assertions passed independently. |

First principles: the authoritative facts already reside in one database. Subject-number owns the write and its invariants; customer IAM contributes authenticated context only. Advisory locks serialize logical lifecycle writers, row locks protect owner-local state, uniqueness handles the remaining cross-subject request-key race. The missing proof and encryption implementations cannot justify fake production approval, plaintext envelopes or broader persistence. Two fail-closed SPIs and test-only implementations suffice for this foundation. No additional user-visible policy decision is needed: an approved proof is a repeatably resolvable, immutable reference; no proof-consumption/expiry policy is invented here.

## 3. Goal-alignment ledger

Every decision, implementation group and validation below is assigned a goal guard. All belong to slice S1; none creates an independent delivery scope.

| Guard | Decision / implementation group | Binding goal protected | Validation |
| --- | --- | --- | --- |
| G1 | Strict route, JSON, header and closed response / problem mappings | Only frozen operation; no new public code/field | W01–W12; A02; contract/hash checks |
| G2 | Existing session verifier; personal-only customer composer; owner caller has no organization/role field | No client-derived account/workspace/role authority; inactive =>403 | W02–W04; U01–U02; I04; A01/A03 |
| G3 | Owner named interface and owner-only SQL | Preserve module ownership, no foreign-owner-internal SQL | A01–A06; existing module/customer/evidence boundaries |
| G4 | Dedicated explicit command credentials, local role assertion, same-connection transaction | No query-credential escalation/fallback; no new grant | C01–C08; L01–L10; I01/I02/I24 |
| G5 | Exact lock order, current rereads, risk-before-replay | Current lifecycle and risk control every result | U05/U07–U09; I06–I10/I18–I22 |
| G6 | Decrypted immutable v1 reference equality and durable outcome decisions | Exact request replay and context-conflict history | R01–R08; U03/U04/U10–U13; I05/I11–I15 |
| G7 | Conditional null-owner update plus one immutable insert; no repair | No owner transfer; append-only history | U14–U16; I03/I16/I17/I23; L03–L07 |
| G8 | Three complete single-transaction attempts; local fail-closed SPIs; no savepoints | No aborted-transaction reads, tentative owner survival or network-under-lock | U05/U17/U18; I18–I24; A04/A05 |
| G9 | Minimum verifier/cipher SPIs; default unavailable; redacted values/exceptions/audit | No provider/KMS subsystem, secret leakage or fake production success | R01–R08; U03/U04/U18; W11; I23; A06 |
| G10 | One slice; exact inventory/README current truth; frozen artifacts untouched | No drift, no migration, no unrelated behavior | C01; all suites; source=CI=XML/POM/hash/diff/JAR checks |

## 4. Exact implementation file manifest

This is the later implementation candidate set, **41 files: 25 new + 16 existing**. Plans, fix artifacts, reviews and Control are not included in those 41. This fix gate changes only those two documents. No other candidate may be silently added; if a genuinely necessary fixture adjustment is discovered, report and revise the plan at the appropriate Controller gate before expanding the set.

Prefixes: `M = backend/src/main/java/com/onecall/core/`; `T = backend/src/test/java/com/onecall/`. Every expanded row is one file.

| # | Status | Path (expand M/T) | Ownership / purpose |
| --- | --- | --- | --- |
| 01 | new | `M/subject_number/customerclaim/package-info.java` | Subject-number named interface `customer-subject-claim-command` |
| 02 | new | `M/subject_number/customerclaim/CustomerSubjectClaimService.java` | Owner command contract and nested public value types |
| 03 | new | `M/subject_number/customerclaim/SubjectClaimException.java` | Sanitized public-code exception |
| 04 | new | `M/subject_number/internal/claim/SubjectClaimProofVerifier.java` | Narrow proof SPI / request / approved-proof types |
| 05 | new | `M/subject_number/internal/claim/SubjectClaimReferenceCipher.java` | Narrow seal/open SPI / redacted envelope type |
| 06 | new | `M/subject_number/internal/claim/SubjectClaimVerificationReference.java` | Fixed v1 payload and strict canonical codec |
| 07 | new | `M/subject_number/internal/claim/SubjectClaimProviderResult.java` | Allowed / Invalid / Unavailable result union |
| 08 | new | `M/subject_number/internal/claim/DefaultCustomerSubjectClaimService.java` | Owner orchestration, one transaction per attempt, bounded retry loop |
| 09 | new | `M/subject_number/internal/claim/SubjectClaimRepository.java` | Owner SQL port and nested row/decision types; no prepared-state map |
| 10 | new | `M/subject_number/internal/claim/JooqSubjectClaimRepository.java` | Exact qualified SQL, locks, role assertion, audit function |
| 11 | new | `M/subject_number/internal/claim/SubjectClaimPersistenceConfiguration.java` | Dedicated pool and unavailable-by-default SPIs |
| 12 | new | `M/subject_number/internal/claim/SubjectClaimPersistenceProperties.java` | Explicit command credentials only |
| 13 | new | `M/customer_iam/internal/application/CustomerSubjectClaimCommandService.java` | Trusted personal-workspace composer; no SQL |
| 14 | new | `M/customer_iam/internal/web/CustomerSubjectClaimsController.java` | Frozen POST, DTO projection, local exception handlers |
| 15 | new | `M/customer_iam/internal/web/CustomerSubjectClaimJsonBinder.java` | Closed strict four-field parsing |
| 16 | new | `M/customer_iam/internal/web/CustomerSubjectClaimDtos.java` | Request / exact response DTOs |
| 17 | new | `T/archtests/CustomerSubjectClaimsWebTests.java` | W01–W12, 12 Surefire methods |
| 18 | new | `T/archtests/subjectnumber/CustomerSubjectClaimServiceTests.java` | U01–U05/U07–U18, 17 Surefire methods; obsolete U06 removed |
| 19 | new | `T/archtests/subjectnumber/SubjectClaimVerificationReferenceTests.java` | R01–R08, 8 Surefire methods |
| 20 | new | `T/archtests/SubjectClaimPersistenceConfigurationTests.java` | C01–C08, 8 Surefire methods |
| 21 | new | `T/archtests/architecture/CustomerSubjectClaimBoundaryTests.java` | A01–A06, 6 Surefire methods |
| 22 | new | `T/archtests/container/CustomerSubjectClaimsIT.java` | I01–I24, 24 PostgreSQL/Failsafe methods |
| 23 | new | `T/archtests/container/SubjectClaimRoleLeastPrivilegeIT.java` | L01–L10, 10 PostgreSQL/Failsafe methods |
| 24 | new | `T/archtests/container/CustomerSubjectClaimFixture.java` | Compose existing PostgreSQL fixture; no test annotations |
| 25 | new | `T/archtests/subjectnumber/ControlledSubjectClaimProviders.java` | Shared test-only proof map / randomized authenticated cipher; no annotations |
| 26 | existing | `M/customer_iam/package-info.java` | Add exactly `subject_number::customer-subject-claim-command` |
| 27 | existing | `M/customer_iam/internal/web/CustomerBoundaryFilter.java` | Admit exact POST; validate one key and JSON content type |
| 28 | existing | `M/customer_iam/internal/web/CustomerProblemWriter.java` | Claim exception overload, exactly three claim 409 mappings |
| 29 | existing | `M/customer_iam/internal/web/CustomerWebConfiguration.java` | Customer-owned qualified lazy composer bean |
| 30 | existing | `backend/src/main/resources/application-persistence.yml` | Explicit enabled flag and dedicated environment credentials |
| 31 | existing | `backend/pom.xml` | Add exactly two Failsafe includes; no dependency change |
| 32 | existing | `.github/workflows/ci.yml` | Exact new method/class inventories and packaged-route probes |
| 33 | existing | `backend/README.md` | Current one-operation truth, defaults/properties/inventory/evidence limits |
| 34 | existing | `T/archtests/architecture/ModuleBoundaryTests.java` | EXPECTED_ALLOWED customer set 10→11; both hasSize(10)→11 and both exact edge lists gain subject_number::customer-subject-claim-command; preserve methods |
| 35 | existing | `T/archtests/architecture/CustomerApiBoundaryTests.java` | Internal count 39→43; fixed profile/controller list gains web/CustomerSubjectClaimsController.java; command imports; preserve methods |
| 36 | existing | `T/archtests/CustomerWebConfigurationTests.java` | Existing absence/qualified/profile assertions include new composer/controller |
| 37 | existing | `T/archtests/OneCallApplicationTests.java` | Existing default-context assertion proves no command pool/provider service |
| 38 | existing | `T/archtests/architecture/CustomerBusinessNumberBoundaryTests.java` | Exact customer dependencies hasSize(10)→hasSize(11) in customerImportsOnlyPublicQuerySeamsAndNoForeignInternalImplementation; preserve every other assertion/method |
| 39 | existing | `T/archtests/architecture/CustomerEvidenceBoundaryTests.java` | Exact ten-element customer set→eleven in namedOwnerInterfacesAndTenCustomerDependenciesAreExact; preserve method name and all other assertions |
| 40 | existing | `T/archtests/architecture/CustomerSubjectDetailBoundaryTests.java` | Exact customer dependencies hasSize(10)→hasSize(11) in subjectEvidenceAndOrderExposeOnlyMinimalNamedQueryInterfaces; preserve every other assertion/method |
| 41 | existing | `T/archtests/architecture/SharingQueryBoundaryTests.java` | Exact ten-element customer set→eleven in sharingQueryApiAndProjectionAreExact; preserve every other assertion/method |

The contiguous 41-path implementation allowlist is exactly the expanded manifest above (25 new +16 existing):

```text
backend/src/main/java/com/onecall/core/subject_number/customerclaim/package-info.java
backend/src/main/java/com/onecall/core/subject_number/customerclaim/CustomerSubjectClaimService.java
backend/src/main/java/com/onecall/core/subject_number/customerclaim/SubjectClaimException.java
backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimProofVerifier.java
backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimReferenceCipher.java
backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimVerificationReference.java
backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimProviderResult.java
backend/src/main/java/com/onecall/core/subject_number/internal/claim/DefaultCustomerSubjectClaimService.java
backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimRepository.java
backend/src/main/java/com/onecall/core/subject_number/internal/claim/JooqSubjectClaimRepository.java
backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimPersistenceConfiguration.java
backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimPersistenceProperties.java
backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerSubjectClaimCommandService.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectClaimsController.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectClaimJsonBinder.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectClaimDtos.java
backend/src/test/java/com/onecall/archtests/CustomerSubjectClaimsWebTests.java
backend/src/test/java/com/onecall/archtests/subjectnumber/CustomerSubjectClaimServiceTests.java
backend/src/test/java/com/onecall/archtests/subjectnumber/SubjectClaimVerificationReferenceTests.java
backend/src/test/java/com/onecall/archtests/SubjectClaimPersistenceConfigurationTests.java
backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectClaimBoundaryTests.java
backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimsIT.java
backend/src/test/java/com/onecall/archtests/container/SubjectClaimRoleLeastPrivilegeIT.java
backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimFixture.java
backend/src/test/java/com/onecall/archtests/subjectnumber/ControlledSubjectClaimProviders.java
backend/src/main/java/com/onecall/core/customer_iam/package-info.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerProblemWriter.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java
backend/src/main/resources/application-persistence.yml
backend/pom.xml
.github/workflows/ci.yml
backend/README.md
backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java
backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java
backend/src/test/java/com/onecall/archtests/OneCallApplicationTests.java
backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectDetailBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java
```

No changes to `subject_number/package-info.java` (its allowed-dependencies set stays empty), owner query implementations, existing provisioning scripts, migration fixtures' ACL tuple oracles, V001–V017, OpenAPI, Control or old checkpoint artifacts. Existing customer evidence/detail/read-only fixtures remain unmodified because the flag defaults false and the new pool is opt-in. Existing V003 catalog/append-only tests remain intact. No production dependency, module, security stack or crypto library is added.

## 5. Exact public boundary and precedence

### 5.1 HTTP and authority

`CustomerSubjectClaimsController.createCustomerSubjectClaim(Principal principal, HttpServletRequest request)` returns `CustomerSubjectClaimDtos.ClaimResult`. Constructor takes `@Qualifier("customerSubjectClaimCommandService") ObjectProvider<CustomerSubjectClaimCommandService>` and `CustomerProblemWriter`. It is `@RestController`, `@Profile("web")`, servlet-only, with `@PostMapping(path="/api/v1/customer/subject-claims", produces="application/json")`. Do not add a `consumes` constraint that turns malformed media types into an accidental framework 415; the filter implements the frozen 422 boundary.

Filter admission adds only exact POST, with its existing context-path/request-URI equality check. Case changes, trailing slash, encoded slash/semicolon, subpaths and all other HTTP verbs remain undispatched/not-found under the existing firewall. “Malformed method” in the operation input means `verificationMethod`; unsupported transport verbs are not a new API operation. Do not admit a GET for this path.

Reuse the existing bearer authenticator and audience `onecall:customer-session:v1`. Missing, duplicated, malformed, invalid or wrong-audience bearer =>401 UNAUTHENTICATED; missing/ambiguous/unavailable verifier =>503 SERVICE_UNAVAILABLE, as existing infrastructure already distinguishes invalid from unavailable verification. Never mint or alter a session. Workspace comes only from `CustomerRequestPrincipal.caller().selectedWorkspace()`. Missing/malformed trusted workspace =>503; `PERSONAL` with null organization is eligible; organization-selected or inconsistent PERSONAL+organization =>403 FORBIDDEN. The composer neither calls `getCustomerMe` nor accepts workspace/account selectors. Owner command rechecks current account existence/status; missing or non-ACTIVE account =>403 FORBIDDEN, with no acceptance.

`Idempotency-Key`: exactly one header, 1–128 Unicode code points, valid UTF-8, no NUL, nonblank (and satisfies PostgreSQL `btrim` check). Preserve exact characters; never trim, lowercase, split commas or hash into another idempotency scope. Missing/duplicate/blank/overlong =>422. Commas in one actual header are opaque key text, not a second header. No body request ID. Use existing filter request attribute accessor. Query parameters are not authority; they cannot override any command value and introduce no alternative selector.

Body: exactly four required, non-null strings: `subjectId`, `bindingId` canonical UUID textual shape (hex case accepted, normalize server UUID values); `verificationMethod` exact enum `SMS|WECHAT_PHONE|CALLER_CHALLENGE|MANUAL_REVIEW`; `verificationProofId` opaque 1–128 code points, nonblank, valid UTF-8, no NUL. Strict duplicate detection, no additional properties, no coercion, no trailing JSON; no arrays/numeric UUIDs. Reject any account/owner/organization/role/raw-number/HMAC/ciphertext/OTP/proof-content property. Content-Type must be one application/json media type, existing charset handling; malformed/missing/duplicate =>422. Retain existing Accept negotiation behavior without broadening other routes. Invalid schema/header/method/reference =>422 VALIDATION_FAILED, never a new code.

Boundary precedence: exact route firewall → session verification → Idempotency-Key / media / existing negotiation validation → trusted personal-workspace check → strict body → owner service availability → single authoritative transaction with local proof/reference validation → locked current predicates → OPEN risk → request row identity then candidate payload identity → accepted-owner integrity → same-account duplicate → owner/fresh acceptance. Within locked predicates check subject/account existence, binding association/activity/verification, account ACTIVE. These current predicates preempt stored success; OPEN risk preempts every replay or duplicate result. Owner integrity is checked before any accepted result or new claim/owner write, even when otherwise an exact duplicate or changed-context duplicate. A request-identity mismatch still returns its non-disclosing replay conflict without processing that stored outcome.

### 5.2 Result and problems

200 JSON is exactly `claimId`, `status`, `subjectId`, and `verifiedAt` only when status ACCEPTED. Project from stored first row, not new UUID/time on replay. `@JsonInclude(NON_NULL)` on `verifiedAt`; no verification details, booleans like created/replayed, account/owner/binding/number, Location or other response additions. Initial rejection/conflict uses the problem matrix below; an identical replay of its stored REJECTED/CONFLICT row returns the frozen 200 `ClaimResult` with no verifiedAt once current predicates/risk permit replay. This is stored-outcome replay, not reinterpretation of history.

Keep `Cache-Control: no-store`, `Pragma: no-cache`, `X-Content-Type-Options: nosniff`; 401 has `WWW-Authenticate: Bearer`. Problem is only existing `type,title,status,code,traceId`, `application/problem+json`, server trace UUID, no detail/SQL/cause/identifiers. Extend the writer's closed switch with claim codes and sanitized titles: `Claim risk blocked`, `Claim replay conflict`, `Claim context conflict`. Existing common titles remain unchanged. 429 RATE_LIMITED remains the frozen infrastructure mapping; no new limiter or public quota is invented.

### 5.3 Locked decision and history matrix

“Fresh row” means no existing `request_id`, all FK parents exist and input reference/binding are resolvable. Rows and audited denials are committed by returning a transaction decision, **not by throwing inside the transaction**; only after commit does the owner throw the sanitized public exception. Infrastructure/integrity rollback paths cannot commit tentative claim/owner writes. No old outcome is returned on an error.

| Condition / internal reason | Public result | Fresh row | Existing request | Owner write |
| --- | --- | --- | --- | --- |
| Missing subject/binding; `CLAIM_SUBJECT_NOT_FOUND` / `CLAIM_BINDING_NOT_FOUND` | 422 VALIDATION_FAILED | none | no stored disclosure | none |
| Missing current account; `CLAIM_ACCOUNT_NOT_FOUND` | 403 FORBIDDEN | none | no stored disclosure | none |
| Binding mismatch; `CLAIM_SUBJECT_BINDING_MISMATCH` | 422 VALIDATION_FAILED | REJECTED if resolvable | audit only | none |
| Binding non-ACTIVE or verified_at null; `CLAIM_BINDING_NOT_ACTIVE` | 422 VALIDATION_FAILED | REJECTED if resolvable | audit only | none |
| Existing inactive account; `CLAIM_ACCOUNT_INACTIVE` | 403 FORBIDDEN | REJECTED if resolvable | audit only | none |
| Invalid/malformed/wrong-version/undecryptable/incomplete input reference; `CLAIM_VERIFICATION_REF_INVALID` | 422 VALIDATION_FAILED | none | none | none |
| Provider unavailability, thrown/null/ambiguous collaborator result, pool/role/permission unavailable | 503 SERVICE_UNAVAILABLE | none / rollback | no stored disclosure | none / rollback |
| Any OPEN risk; `CLAIM_RISK_BLOCKED` | 409 CLAIM_RISK_BLOCKED | exactly one REJECTED | audit only, including previously ACCEPTED | none |
| Same request, any logical identity component changed; `CLAIM_REPLAY_CONFLICT` | 409 CLAIM_REPLAY_CONFLICT | not applicable | no new claim, audit only | none |
| Stored reference unresolvable; `CLAIM_REPLAY_UNRESOLVABLE` | 422 VALIDATION_FAILED | none | no success, audit only | none |
| Exact request identity, current predicates/risk/accepted-owner checks pass | 200 first stored outcome | none | return original id/status/subject/time | none |
| No request replay; exact same-account accepted context and valid owner relation | 200 first ACCEPTED result | none | original accepted result | none |
| No replay; changed same-account accepted context; `CLAIM_CONTEXT_CONFLICT` | 409 CLAIM_CONTEXT_CONFLICT | exactly one CONFLICT | later identical replay may return its stored result | none |
| Another account owner or organization owner; `CLAIM_OWNER_CONFLICT` | 403 FORBIDDEN | exactly one CONFLICT | replay identity rules still apply first | none |
| Any accepted-owner inconsistency; `CLAIM_DATA_INTEGRITY` | 500 INTERNAL | no new claim | never disclose stored acceptance | none; no repair |
| Fresh personal claim, no accepted corruption, unowned subject | 200 ACCEPTED | exactly one ACCEPTED | not applicable | conditional null→account once |
| Fresh personal claim, already same-account owner, no accepted claim | 200 ACCEPTED | exactly one ACCEPTED | not applicable | none; do not bump revision |
| Retry budget exhausted; `CLAIM_CONCURRENCY_EXHAUSTED` | 503 SERVICE_UNAVAILABLE | failed attempts rolled back | no aborted-transaction read | none from failed attempts |

Same-account context comparison includes null organization and method as well as binding/HMAC/proof. Subject/account/null organization/method are row identity; binding/number/proof are decrypted payload identity. After current predicates and risk, compare immutable row subject/account/null-organization/method before any stored-reference open. A mismatch in any one returns generic replay conflict with zero opens of that row, even if its ciphertext is invalid/unavailable; never stage foreign plaintext. A request key owned by another account/subject is not a cross-account stored-result lookup. No `CLAIM_OWNER_CONFLICT`, `CLAIM_DATA_INTEGRITY`, `CLAIM_REPLAY_UNRESOLVABLE`, SQLSTATE or other internal reason escapes as a new public code. Corrupt accepted claims on an unowned subject or accepted claims inconsistent with either owner column always fail INTERNAL without repair; inspect **all** accepted claims for the locked subject before returning acceptance/duplicate, not only this account's row.

## 6. Exact packages, types and SPI contracts

### 6.1 Published owner boundary and customer composition

`subject_number.customerclaim` has `@NamedInterface("customer-subject-claim-command")`. Only two top-level types are exported:

```java
public interface CustomerSubjectClaimService {
    ClaimResult create(ClaimCommand command, PersonalClaimCaller caller);
    record PersonalClaimCaller(UUID accountId) { }
    record ClaimCommand(UUID subjectId, UUID bindingId, VerificationMethod verificationMethod,
                        String verificationProofId, String requestId, UUID traceId) { }
    enum VerificationMethod { SMS, WECHAT_PHONE, CALLER_CHALLENGE, MANUAL_REVIEW }
    enum ClaimStatus { ACCEPTED, REJECTED, CONFLICT }
    record ClaimResult(UUID claimId, ClaimStatus status, UUID subjectId, Instant verifiedAt) { }
}
public final class SubjectClaimException extends RuntimeException {
    public enum Code { UNAUTHENTICATED, FORBIDDEN, VALIDATION_FAILED, CLAIM_RISK_BLOCKED,
        CLAIM_REPLAY_CONFLICT, CLAIM_CONTEXT_CONFLICT, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL }
    public SubjectClaimException(Code code); // message = code.name(); no cause retention
    public Code code();
}
```

Enforce nonnulls, accepted/time shape and input bounds again in owner command validation. Override command `toString()` to constant redacted text; never render proof IDs/request contents. Owner caller intentionally has no organization/workspace/role selector. Personal authority is derived by the customer composer, not from HTTP fields.

`customer_iam.internal.application.CustomerSubjectClaimCommandService` constructor takes `@Qualifier("customerSubjectClaimService") ObjectProvider<CustomerSubjectClaimService>`. Its exact operation is `ClaimResult create(TrustedCustomerCaller caller, ClaimCommand command)`. Validate trusted caller/account/channel and selected personal workspace, construct `PersonalClaimCaller(caller.accountId())`, resolve exactly one qualified owner service and invoke. No SQL, account mutation, query credentials or provider lookup here. Missing/ambiguous bean =>503. `CustomerWebConfiguration` supplies qualified `customerSubjectClaimCommandService`; controller remains a lazy client. Binder returns `CustomerSubjectClaimDtos.ClaimRequest(UUID subjectId, UUID bindingId, VerificationMethod verificationMethod, String verificationProofId)`; DTO toString is redacted. `ClaimResult` web DTO has exactly the four frozen fields, with null time omitted. Customer imports only the named owner surface, never `subject_number.internal`.

### 6.2 Minimum proof/reference seam (all internal to subject-number)

```java
public interface SubjectClaimProofVerifier {
    SubjectClaimProviderResult<ApprovedProof> verify(ProofRequest request);
    record ProofRequest(UUID accountId, UUID subjectId, UUID bindingId,
                        VerificationMethod method, String verificationProofId) { }
    record ApprovedProof(UUID accountId, UUID subjectId, UUID bindingId,
                         VerificationMethod method, String proofRef) { }
}
public interface SubjectClaimReferenceCipher {
    SubjectClaimProviderResult<SealedReference> seal(byte[] canonicalV1Payload);
    SubjectClaimProviderResult<byte[]> open(SealedReference reference);
    record SealedReference(String envelope) { }
}
public sealed interface SubjectClaimProviderResult<T> {
    record Allowed<T>(T value) implements SubjectClaimProviderResult<T> { }
    record Invalid<T>() implements SubjectClaimProviderResult<T> { }
    record Unavailable<T>() implements SubjectClaimProviderResult<T> { }
}
```

These are operation-specific types, not a provider registry, verification-attempt repository, key provider or identity subsystem. SPI owners can later implement real mechanisms, but no such adapter is part of S1. Qualifiers are exactly `subjectClaimProofVerifier` and `subjectClaimReferenceCipher`; default beans guarded by `@ConditionalOnMissingBean` by type return Unavailable for every method, including open. Multiple replacements fail closed; do not select the first or fall back to a test fake.

Approval is meaningful only when every echoed account/subject/binding/method equals the trusted request and server-resolved `proofRef` is a valid stable 1–128 reference. An SPI must resolve/check its server-side evidence, not approve because client input looks opaque or equals a supplied string. All four methods have the same fail-closed foundation; no method gets a permissive special case. No raw OTP, raw proof or provider payload is accepted by these interfaces. Invalid proof/reference =>422; unavailable/exception/null/misbound approval =>503 for a broken collaborator (a verifier's explicit Invalid is 422). Do not persist or accept partially populated output.

`SubjectClaimVerificationReference` owns nested `Payload(int schemaVersion, UUID subjectId, UUID bindingId, String numberHmac, VerificationMethod verificationMethod, String proofRef)` and exact methods `byte[] encode(Payload payload)`, `Payload decode(byte[] canonicalJson)`. Construct only after approval and binding identity resolution. Plaintext keys in RFC8785-compatible sorted order are `bindingId,numberHmac,proofRef,schemaVersion,subjectId,verificationMethod`, UTF-8 without insignificant whitespace; v1 has only strings and integer 1, so no generic canonical-number library is necessary. Strict parser rejects extra/missing/duplicate fields, trailing tokens, malformed UTF-8, invalid UUID/HMAC/method/reference, wrong version and noncanonical encoding. Input round-trip payload must equal every originally approved/constructed field and the locked current binding id/HMAC. A stored payload must match its own row subject/method; its otherwise valid binding/HMAC/proof/method differing from the current command is a logical replay/context conflict, **not** malformed-reference422. Do not require a historical stored binding to equal the current binding before identity comparison or acquire earlier-order locks for that historical binding. Payload contains **no account field** because the frozen payload has none; bind account through verifier approval and the immutable row's `account_id` during every comparison.

The cipher SPI seals canonical bytes into `v1.<key_ref>.<base64url(nonce || authenticated_ciphertext_and_tag)>` and authenticates/opens them server-side. Production has no crypto algorithm, key source or fake envelope implementation: Q2 algorithm/KMS ownership remains deferred. Validate envelope framing/nonempty key ref/URL alphabet before open; permit the frozen key-ref character set, parsing its end at the last dot because the base64url payload has no dots. Cryptographic validity belongs to SPI. Invalid/wrong-version/undecryptable =>Invalid→422; provider unavailable or malformed success/throw =>503. Decode/open a stored invalid payload yields the same 422 boundary without stored success. Never interpret a prefix as successful encryption. Use defensive copies for plaintext byte arrays and redact every sensitive type's `toString()`, including `SubjectClaimVerificationReference.Payload`, `SubjectClaimProofVerifier.ProofRequest`, `ApprovedProof`, `SealedReference`, every repository row, every provider-result variant (including generic Allowed), public ClaimCommand and all private command/input/decision state. Exceptions never retain provider/SQL messages or sensitive causes.

Randomized encryption means two successful seals of the same payload differ. Replay/duplicate identity compares decoded fields plus row identity, **never ciphertext bytes or ciphertext digests**. No decoded-payload map, pre-opened envelope cache or staged provider result exists. Tests must prove different valid ciphertext for identical logical payload replays and equal-looking context with a different server proofRef conflicts.

### 6.3 One authoritative local transaction per attempt

Reject an ambient active Spring transaction with SERVICE_UNAVAILABLE before entering the loop; do not suspend arbitrary caller work. Each of at most three attempts invokes the **same write TransactionTemplate exactly once** (READ COMMITTED, REQUIRES_NEW, dedicated connection). There is no preliminary SELECT-only transaction, unlocked preparation phase, staged decoded-payload map, PreparationChanged restart, extra retry reason or second state machine.

Inside that transaction: assert role; resolve the immutable binding identity (zero => uniform422, no locks/writes); verify approved proof against authenticated account/request/binding/HMAC, construct canonical payload, seal and validate/open the new input reference with the local SPIs. No client number/HMAC or reference is trusted. Then acquire and reread exactly §7.3's locks/current state; evaluate predicates then risk; read request metadata; compare immutable row identity **before opening its stored reference**; open only a possible candidate, validate its own-row payload, compare logical identity; reread request/accepted rows and check accepted-owner integrity before projecting any stored success; decide replay, duplicate, owner or fresh insert; append safe audit; return decision and commit. §7.3 pins the detailed order and audit exceptions. Returned durable errors become public exceptions only after commit.

S1 SPI implementations are local fail-closed defaults or controlled in-process fakes, not external adapters. Verify/seal/open are repeatably resolvable local operations; tests assert an active transaction and the same connection/PID on each invocation, with stored opens occurring only after risk and row identity. No network, remote proof, KMS, delivery, charging or other consuming side effect may occur after **any** PostgreSQL/advisory/row lock is acquired. The S1 SPIs must not perform network I/O at all. Production defaults return Unavailable, never fabricated approval/encryption. Real provider/KMS integration requires a newly confirmed goal and transaction design; it is not a reason to retain preparatory state now.

Only §7.4's named uniqueness/serialization/deadlock failures retry after whole-transaction cleanup. Repeat local proof/reference validation, every lock, current predicate and risk evaluation on each attempt. Local proof/cipher failure before lifecycle rereads can return422/503 before account status is observed, following T-S1-06 server-validation order; once current account is read, inactive or unexpectedly missing always maps uniformly to403, no claim/owner write for missing account. No externally visible proof/provider facts are added.

## 7. Persistence, transaction, locks and SQL

### 7.1 Configuration and privileges

`SubjectClaimPersistenceProperties` is `@ConfigurationProperties("onecall.subject-claim-command.datasource")` record `(String url,String username,String password)` rejecting missing/blank components with a constant message. `SubjectClaimPersistenceConfiguration` is `@Profile("persistence")`, `@ConditionalOnProperty(prefix="onecall.subject-claim-command",name="enabled",havingValue="true")`, and enables those properties. No implicit enablement by `web`, existing persistence URL or an SPI replacement.

`application-persistence.yml` adds only:

```yaml
onecall:
  subject-claim-command:
    enabled: ${ONECALL_SUBJECT_CLAIM_COMMAND_ENABLED:false}
    datasource:
      url: ${ONECALL_SUBJECT_CLAIM_COMMAND_DB_URL:}
      username: ${ONECALL_SUBJECT_CLAIM_COMMAND_DB_USERNAME:}
      password: ${ONECALL_SUBJECT_CLAIM_COMMAND_DB_PASSWORD:}
```

Merge this beneath the existing onecall key; do not duplicate the YAML key. Runtime property names are exactly the prefix above. New names follow existing `onecall.sharing-command.datasource` / `ONECALL_SHARING_COMMAND_DB_*` conventions; they are new dedicated names, not claimed pre-existing deployment variables.

Qualified beans: `subjectClaimCommandDataSource` (Hikari, destroy close), `subjectClaimCommandTransactionManager` (DataSourceTransactionManager, rollbackOnCommitFailure true), `subjectClaimCommandTransactionTemplate` (READ_COMMITTED, REQUIRES_NEW, readOnly false), `subjectClaimCommandDsl` (PostgreSQL, indexed prepared statements, executeLogging false, TransactionAwareDataSourceProxy of the very same pool), `subjectClaimRepository`, `customerSubjectClaimService`, and the two SPI beans. Owner implementation constructor takes exactly `(TransactionTemplate transaction, DSLContext context, SubjectClaimRepository repository, SubjectClaimProofVerifier proofs, SubjectClaimReferenceCipher cipher)` with explicit bean qualifiers at configuration injection sites.

Each single authoritative transaction begins with literal `SET LOCAL ROLE subject_number_role`, then `SELECT current_user` equals exactly `subject_number_role`. Never session SET ROLE, search-path reliance, customerQueryDataSource, default DSL/TM, customer query credentials, evidence role, owner role or administrator fallback. All statements in one authoritative attempt share the transaction-bound connection/PID, including audit function, locks and inserts/owner updates. Prove cleanup after success, denial and exception with reused pool connections (pool max size 1 in cleanup tests). Credentials absent while flag true =>clean constant startup configuration error; role/pool errors at runtime =>503, not credential-bearing diagnostic. Flag absent/false or persistence profile absent =>no pool/owner service; web endpoint still authenticates and fails closed if service is absent.

Existing grants suffice: V003 SELECT/INSERT claim, SELECT subject/binding/risk, UPDATE owner_account_id/revision/updated_at; UPDATE grants already present on owner-local binding/risk permit their FOR SHARE locks. V002 SELECT account id/status plus advisory account lock requires no foreign UPDATE. V001 grants audit schema usage and append_fact EXECUTE. Built-in advisory lock EXECUTE is checked by ACL tests. No privilege grant/migration is needed. If that statement is false on the frozen chain, stop `BLOCKED_NEEDS_GOAL_RECONFIRMATION`; do not plan V018 or silently run as a privileged login.

Test-only login: `pa30_subject_claim_<random lowercase suffix> LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS`, synthetic password, membership **only** in `subject_number_role`; no INHERIT escalation or grant option. Before SET LOCAL ROLE direct domain access is denied; after it only existing role capabilities work. Existing V003 grants include other subject-number owner operations: document that fact, do not pretend this login is a newly column-restricted claim-only role. The application emits only this operation's approved SQL. Production provisioning/credential assignment: **NOT EXECUTED / NOT CLAIMED**. Do not edit/re-run production identity provisioning to grant this login.

### 7.2 Repository signatures and row shapes

`SubjectClaimRepository` exposes the following methods, all taking explicit `DSLContext tx` as first parameter; implementation never opens another connection or owns a transaction:

```text
void assertRole(tx)
Optional<BindingIdentity> findBindingIdentity(tx, UUID bindingId)
Optional<ClaimRow> findByRequestId(tx, String requestId)
Optional<ClaimRow> findAcceptedForAccount(tx, UUID subjectId, UUID accountId)
void lock(tx, String namespaceKey)
Optional<SubjectRow> lockSubject(tx, UUID subjectId)
Optional<BindingRow> lockBinding(tx, UUID bindingId)
List<RiskRow> lockRisks(tx, UUID bindingId)
Optional<AccountRow> readAccount(tx, UUID accountId)
List<AcceptedOwnerRow> readAcceptedOwners(tx, UUID subjectId)
Instant databaseTime(tx)
void insertClaim(tx, NewClaim row)
int setUnownedOwner(tx, UUID subjectId, UUID accountId, int expectedRevision, Instant updatedAt)
void appendAudit(tx, AuditDecision decision)
```

All row/decision records/enums are nested in this internal port or private in the owner service. `BindingIdentity(id,subjectId,numberHmac)`; `SubjectRow(id,ownerAccountId,ownerOrganizationId,revision)`; `BindingRow(id,subjectId,numberHmac,status,verifiedAt)`; `RiskRow(id,state)`; `AccountRow(id,status)`; `AcceptedOwnerRow(id,accountId,organizationId)`; `ClaimRow(id,subjectId,accountId,organizationId,verificationMethod,verificationRef,requestId,status,riskCheckedAt,verifiedAt)`; NewClaim same insert columns. Sensitive records have constant redacted representations. AuditDecision holds only decision time, authenticated account/requested-subject UUID, request/trace identity and §7.3 safe reason/outcome/riskEvaluation enums, no provider/reference data or old-outcome facts. Private `ClaimDecision` is either result or public error plus internal reason; no throwing for durable rejections.

### 7.3 Exact lock and query sequence

Use bound parameters and schema-qualified tables/functions. No SELECT *, inline values, JOOQ execution logging or SQL-binding collection with secrets. Starting each authoritative transaction:

```sql
SET LOCAL ROLE subject_number_role;
SELECT current_user;
SELECT id,subject_id,number_hmac FROM subject_number.caller_number_binding WHERE id=?;
-- if zero: validation, no locks/writes
-- local proof verification and input seal/open validation on this same transaction
SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0));
-- parameter: 'subject_number.number:' || immutable number_hmac
SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0));
-- parameter: 'subject_number.subject:' || requested subject UUID lowercase text
SELECT id,owner_account_id,owner_organization_id,revision
  FROM subject_number.business_subject WHERE id=? FOR UPDATE;
-- if zero: validation, no claim/owner write
SELECT id,subject_id,number_hmac,status,verified_at
  FROM subject_number.caller_number_binding WHERE id=? FOR SHARE;
SELECT id,state FROM subject_number.subject_risk_state
  WHERE caller_number_binding_id=? ORDER BY id FOR SHARE;
SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0));
-- parameter: 'customer_iam.account:' || authenticated account UUID lowercase text
SELECT id,status FROM customer_iam.customer_account WHERE id=?;
```

Lock binding before risk rows; all risk rows are locked in ascending UUID. No organization/membership, canonical-caller, session, activation, outbox or platform-idempotency locks. The initial binding identity is used only to select its immutable canonical-number key. Under the locks, reread identity/status/verifiedAt, owner/revision and account; verify the locked binding identity matches the key acquired. No pre-lock predicate authorizes a write. Subject row is already its locked reread. Read all risk states only from the locked result (test actual risk evaluation separately from lock acquisition); no risks means no OPEN, not evidence of a made-up CLEAR row. Canonical-number lock protects zero-risk insertion/reopen phantoms against conforming risk writers. Account is a plain SELECT under its lifecycle advisory, never foreign FOR SHARE/UPDATE.

For a fresh current-predicate rejection, find request key to decide append vs audit-only, but do not replay it; use `riskEvaluation=NOT_REQUIRED` and a fresh database decision time, never claim that merely locking risk rows evaluated risk. For current predicates that pass, sample `SELECT clock_timestamp()` for actual risk decision after reading states. OPEN preempts any logical replay comparison and first stored result. Only after this check read the request row. Inspect its immutable subject/account/null-organization/method first; on mismatch return CLAIM_REPLAY_CONFLICT and audit without passing its ciphertext to open. Otherwise open that candidate only, validate payload against its own row, compare current binding/HMAC/server proofRef and method, then reread the request and accepted rows on the same transaction before deciding. Append-only request/accepted rows plus the subject lock make candidate identity stable; a later cross-subject global-key winner is handled by rollback/retry, never a preparation restart.

If no request row, read same-account accepted metadata. Compare subject/account/null-organization/method first: a changed duplicate row context yields CLAIM_CONTEXT_CONFLICT without opening it; inspect accepted-owner integrity before any duplicate/conflict return. Only a metadata-compatible accepted candidate may be opened for binding/HMAC/proof comparison. Do not acquire locks for historical payload bindings. Reread request/accepted rows before final decision and inspect all accepted owners for the locked subject before any acceptance/duplicate. No stored reference is opened for predicate/risk denials or foreign request identities. Stored errors can win only after risk and candidate identity pass; an OPEN risk always wins over replay and stored-reference Invalid/Unavailable.

Claims are selected by explicit column list `id,subject_id,account_id,organization_id,verification_method,verification_ref_ciphertext,request_id,status,risk_checked_at,verified_at`, with `WHERE request_id=?`, or `WHERE subject_id=? AND account_id=? AND status='ACCEPTED'`. No arbitrary LIMIT hides duplicates. Integrity read is `SELECT id,account_id,organization_id FROM subject_number.subject_claim WHERE subject_id=? AND status='ACCEPTED' ORDER BY id`. Under subject lock, every accepted row must agree with the effective exclusive owner; any accepted row with both owners null or mismatched organization/account is corruption. For personal owner, accepted relation must have that account and null organization; an accepted organization relation cannot masquerade as personal. With a different non-corrupt owner, fresh request appends CONFLICT and returns403.

Insert exactly once, with application UUID and database decision times:

```sql
INSERT INTO subject_number.subject_claim
 (id,subject_id,account_id,organization_id,verification_method,
  verification_ref_ciphertext,request_id,status,risk_checked_at,verified_at)
VALUES (?,?,?,NULL,?,?,?,?,?,?);
UPDATE subject_number.business_subject
 SET owner_account_id=?,revision=revision+1,updated_at=?
 WHERE id=? AND owner_account_id IS NULL AND owner_organization_id IS NULL AND revision=?;
```

The insert has ten values for ten columns, with organization literal NULL and nine placeholders. Choose risk_checked_at after current risk evaluation, verified_at from a database time sampled at/after that timestamp; REJECTED/CONFLICT verified_at is NULL. Proof-attempt time never populates verified_at. New input reference is sealed/validated once per authoritative attempt and inserted unchanged when a fresh row is required. For acceptance, insert before conditional owner update in the same transaction; no accepted claim commits without its owner. Update only when both owner columns are null. Already same-account ownership causes **zero** UPDATEs and no revision/time bump. Conditional update count must equal one on the null-owner branch; if zero reread owner once under held lock, fail INTERNAL and roll back the whole attempt (never reuse an inconsistent accepted result or transfer). Database triggers remain the final backstop.

After each resolvable locked decision (including exact replay/duplicate and denial on an existing request), call existing function on this connection:

```sql
SELECT audit.append_fact(?::timestamptz,'ACCOUNT',?::uuid,'SUBJECT_CLAIM_DECIDED',
 'BUSINESS_SUBJECT',?::uuid,?::text,NULL::jsonb,?::jsonb,?::text,?::text);
```

Arguments: decision time, authenticated account, requested subject, stable internal reason, safe after_snapshot, request key and trace UUID text. Before_snapshot is NULL. Snapshot has **exactly** three keys `reason`, `outcome`, `riskEvaluation`; the reason equals the function reason argument. Its values come only from this closed mapping (never from a stored claim status or provider text):

| Locked decision | Exact reason | Exact outcome | Exact riskEvaluation | Durability |
| --- | --- | --- | --- | --- |
| Fresh accepted, unowned or same owner | `CLAIM_ACCEPTED` | `ACCEPTED` | `NO_OPEN` | Commit with claim/optional owner |
| Exact request replay, any stored status | `CLAIM_REPLAYED` | `REPLAYED` | `NO_OPEN` | Audit only; no old-status fact |
| Exact same-account accepted duplicate | `CLAIM_DUPLICATE` | `DUPLICATE` | `NO_OPEN` | Audit only |
| OPEN risk, fresh or existing request | `CLAIM_RISK_BLOCKED` | `DENIED` | `OPEN` | With fresh REJECTED or audit only |
| Request row/payload identity differs | `CLAIM_REPLAY_CONFLICT` | `CONFLICT` | `NO_OPEN` | Audit only |
| Accepted duplicate context differs | `CLAIM_CONTEXT_CONFLICT` | `CONFLICT` | `NO_OPEN` | With one fresh CONFLICT |
| Other account/organization owner | `CLAIM_OWNER_CONFLICT` | `CONFLICT` | `NO_OPEN` | With one fresh CONFLICT |
| Resolvable binding/subject mismatch | `CLAIM_SUBJECT_BINDING_MISMATCH` | `DENIED` | `NOT_REQUIRED` | Fresh REJECTED or audit only |
| Resolvable inactive/unverified binding | `CLAIM_BINDING_NOT_ACTIVE` | `DENIED` | `NOT_REQUIRED` | Fresh REJECTED or audit only |
| Resolvable inactive current account | `CLAIM_ACCOUNT_INACTIVE` | `DENIED` | `NOT_REQUIRED` | Fresh REJECTED or audit only |
| Candidate stored reference Invalid | `CLAIM_REPLAY_UNRESOLVABLE` | `DENIED` | `NO_OPEN` | Audit only;422 |
| Accepted-owner integrity violation detected before writes | `CLAIM_DATA_INTEGRITY` | `DENIED` | `NO_OPEN` | Audit-only commit;500 after commit |

No audit for missing binding/subject/account (including the uniform403 missing-account path), pre-transaction authentication/schema/composition failure, invalid/unavailable local proof/input reference, stored-cipher Unavailable, role/pool/SQL infrastructure failure or exhausted retries. Conditional owner-update miss and SQL integrity/trigger errors roll back **all** tentative claim/owner/audit writes; they produce no durable audit, and must not start a second audit transaction. A pre-write integrity decision can commit its safe audit only because it has made no claim/owner write; this is not permission to commit a partial failed mutation. Audit failure always rolls back and returns503. Infrastructure failure is not another audited business decision.

Snapshots contain only these safe enums/reason/riskEvaluation: no proof/reference/number/HMAC/ciphertext, binding identity, stored id/time/status, old outcome or provider diagnostics. Request/trace and authenticated/requested UUIDs are existing function metadata, never snapshot additions. No direct audit DML, outbox event or new audit framework. U07/U08/U10–U18 and I03/I07–I17/I23 assert exact function reason, exact three-key snapshot equality, riskEvaluation and commit/rollback/no-audit behavior for every applicable row; U18 enumerates every mapping, including all replay statuses with the identical REPLAYED audit. Boolean canary absence assertions inspect captured HTTP/log/exception/audit data without printing actual values.

### 7.4 Retry/rollback implementation

Wrap `transaction.execute(...)` in an owner-local loop with a literal maximum of **three total authoritative attempts**, not three retries plus the first. Catch only **after** execute has returned/thrown and transaction cleanup completed. Traverse bounded/cycle-safe cause chains for SQLSTATE/constraint without logging or propagating messages. Retry SQLSTATE 23505 only for `uq_claim_request` or `uq_claim_accepted_subject_account`. No ON CONFLICT DO NOTHING plus a read in the same failed transaction, nested transaction, savepoint or subtransaction. Other 23505/check/FK/trigger integrity errors fail INTERNAL with rollback; role/permission/connection/transaction infrastructure failures map503. SQLSTATE 40001/40P01 may consume the same whole-attempt budget and retry from the beginning of the single authoritative transaction; never acquire a later lock then an earlier one. Budget exhaustion =>503. No random sleeps or unbounded loops.

Rollback includes inserted claim, tentative owner/revision/time, audit and every transaction advisory lock. Commit returned decision before translating a durable rejection to its HTTP exception. Never retry a committed denial or a committed uncertain outcome within an aborted connection; a connection-loss/unknown commit maps503 and the client can replay its original key. No attempt may read winner state from the connection until rollback/cleanup completed; a new attempt acquires a new transaction (possibly the cleaned same pooled connection). No generic retry component is warranted: this is a bounded owner-local loop plus existing TransactionTemplate.

## 8. Concurrency acceptance scenarios

| Race | Serial-order proof / expected durable state |
| --- | --- |
| Same key and exact identity | At most one ACCEPTED insert/owner update. Loser after cleanup redoes predicates/risk and returns same id/time; any audit rows describe actual completed decisions. |
| Same key, different subject/account/binding/method/proof | Same-subject paths serialize; cross-subject keys may hit global request uniqueness. Loser rolls back **all** tentative writes, retries, returns replay conflict, never winner's result. |
| Same subject/account/context, different keys | Subject row serializes; first creates ACCEPTED, second returns first result without inserting its different key. Its audit can exist; its request key has no claim row. |
| Same subject/account, changed context, fresh different keys | Existing accepted row stays immutable. Each distinct changed request inserts exactly one CONFLICT and gets409; exact repeat of a stored conflict follows replay rules. |
| Two accounts claim unowned subject | One null→account winner/ACCEPTED. Other gets403 and one CONFLICT if resolvable; no owner transfer, exactly one revision increment. |
| Different bindings of one subject | Both canonical-number locks precede shared subject lock; no owner change races or inverse lock acquisition; context determined against first acceptance. |
| Risk OPEN/reopen first | Writer holds same number key; claim waits, rereads OPEN, gets risk409 and fresh REJECTED or existing-request audit only. Historical ACCEPTED never disclosed. |
| Claim first, risk writer later | Claim commits accepted once; later OPEN never rewrites claim, but blocks every later replay/duplicate. Clear then replay rechecks proof/current state and returns original outcome. |
| Binding disabled/unverified or account disabled first | Compatible lifecycle writer locks precede claim; current reread denies and never returns old acceptance. Claim-first is not retroactively rewritten. |
| Zero risk rows, concurrent new OPEN | Number advisory protects absence; claim-first can accept, risk-first denies. No fabricated gap lock or foreign privilege. |
| Corrupt accepted-owner relation | No repair or second accepted row under any timing;500 with no owner change. |
| Request-key unique loser / provider or audit failure | No query on aborted transaction; transaction-scoped role/locks cleaned; no tentative claim/owner persists; bounded retry or sanitized503. |

Use two independently checked-out JDBC connections, latches/barriers and bounded futures (10-second waits, 30-second test ceiling), not sleeps to prove ordering. Inspect SQLSTATE/SQL templates/PID without capturing sensitive binds. Mutator fixtures follow the same namespace/order; they are test SQL, not newly delivered production lifecycle services.

## 9. Exact test inventory (ordinary named @Test methods)

Identifiers W/U/R/C/A/I/L below are planning labels, not prefixes added to Java method names. No parameterized/repeated/dynamic tests, disabled/skipped tests or commented @Test lines. Each listed name is one method. Existing methods are adjusted in place and add **zero** methods.

### 9.1 Focused Surefire additions: 51 methods / 5 classes

`com.onecall.archtests.CustomerSubjectClaimsWebTests` (12):

| ID | Exact method | Required assertions |
| --- | --- | --- |
| W01 | `exactPostReturnsOnlyFrozenAcceptedFields` | POST route,200, exact keys, accepted timestamp, no owner/binding fields |
| W02 | `missingInvalidDuplicateAndWrongAudienceSessionAreUnauthenticated` |401 and bearer challenge; schema parsing never reached |
| W03 | `onlyTrustedPersonalWorkspaceCanReachOwnerCommand` | Organization403; client selectors cannot change account; no owner call |
| W04 | `missingTrustedWorkspaceAndUnavailableDependenciesFailClosed` |503 for malformed trusted context/absent owner/verifier |
| W05 | `closedBodyRejectsUnknownDuplicateMissingAndCoercedFields` | All closed-schema cases422 without command |
| W06 | `idempotencyHeaderHasExactBoundsAndOpaqueIdentity` | Missing/duplicates/blank/129 reject;1/128 accepted; opaque comma/spacing preserved |
| W07 | `methodUuidAndProofReferenceValidationUseUniform422` | Four methods accepted syntactically; invalid enum/UUID/ref boundaries422 |
| W08 | `domainProblemsExposeOnlyThreeFrozen409Codes` | All internal mapping paths; exactly three domain409s |
| W09 | `storedRejectedAndConflictReplayOmitVerifiedAt` |200 REJECTED/CONFLICT exact result keys, no null time property |
| W10 | `exactRouteMediaNegotiationAndAuthenticationPrecedenceHold` | Route/firewall, JSON422, existing Accept behavior, session first |
| W11 | `responsesProblemsAndFailuresNeverExposeSensitiveCanaries` | Constant failures/output/representations; no sensitive canary interpolation |
| W12 | `concurrentSessionsKeepCallerAndCommandContextIsolated` | Two trusted callers/workspaces never share request authority |

`com.onecall.archtests.subjectnumber.CustomerSubjectClaimServiceTests` (17; obsolete U06 deleted, remaining planning IDs retained):

| ID | Exact method | Required assertions |
| --- | --- | --- |
| U01 | `personalCallerAndCurrentActiveAccountAreMandatory` | Null inputs guarded; current inactive/missing account403 |
| U02 | `zeroInitialBindingStopsBeforeLocksAndWrites` | Uniform422; no further locks/claim/owner/audit |
| U03 | `proofApprovalMustBindEveryTrustedDimension` | Server stable ref; explicit Invalid422; misbound/null/throw503 |
| U04 | `unavailableDefaultProofAndCipherNeverAccept` | Every method unavailable; no synthetic production success |
| U05 | `locksUseFrozenNamespacesAndCurrentRereads` | One execute per attempt; local SPI same active transaction/PID; ambient tx rejected; exact locks/rereads, no foreign row/org locks or network |
| U07 | `currentPredicateFailuresPersistOnlyResolvableFreshRejections` | Matrix parent/reference existence; NOT_REQUIRED audit |
| U08 | `openRiskPreemptsEveryReplayAndDuplicateOutcome` | No old-result/reference error disclosure; fresh risk REJECTED |
| U09 | `riskTimesAndAcceptedTimesComeFromDatabaseDecisionClock` | verified>=risk, never proof timestamp |
| U10 | `exactRequestReplayUsesLogicalIdentityAndFirstOutcome` | Account/subject/null organization/method/payload checks; no new claim/owner |
| U11 | `changedRequestIdentityNeverDisclosesStoredOutcome` | Counting cipher: zero opens of foreign-account/different-subject/different-method stored envelopes (invalid and unavailable variants);409, no disclosure/write; OPEN risk wins |
| U12 | `exactAcceptedDuplicateReturnsFirstResultWithoutInsert` | Fresh unused key not inserted; audit only |
| U13 | `changedAcceptedContextAppendsExactlyOneConflict` | Method/binding/HMAC/proof change; no owner write/old result |
| U14 | `freshAcceptanceSetsOnlyNullOwnerWithExpectedRevision` | Insert + conditional owner update, exact columns/time/revision |
| U15 | `differentOwnerRefusesWithoutTransferOrRepair` | Other account/org403 with one CONFLICT |
| U16 | `corruptAcceptedOwnerRelationAndUpdateMissFailClosed` | All accepted relations scanned;500; rollback on conditional miss |
| U17 | `uniqueLoserRetriesOnlyAfterWholeTransactionCleanup` | Request/partial unique and40P01/40001 budget3; one execute per attempt; rollback/cleanup before new locks/risk |
| U18 | `exactAuditMappingsAndFailuresNeverLeakOrPartiallyCommit` | Enumerate every §7.3 mapping/exclusion; exact keys/enums;503 budget; boolean canaries; audit/owner-miss rollback |

`com.onecall.archtests.subjectnumber.SubjectClaimVerificationReferenceTests` (8):

| ID | Exact method | Required assertions |
| --- | --- | --- |
| R01 | `v1PayloadHasExactCanonicalKeysAndNoAccountOrRawProof` | Sorted fixed canonical bytes and shape |
| R02 | `strictPayloadRejectsMissingExtraDuplicateAndMalformedValues` | Shape, UTF-8, HMAC/UUID/ref limits, trailing/noncanonical JSON |
| R03 | `wrongVersionAndInvalidEnvelopeFailClosed` | Envelope and schema version both validated |
| R04 | `undecryptableAndUnavailableReferencesRemainDistinct` | Invalid422 vs unavailable503 |
| R05 | `randomizedCiphertextsCompareByDecodedLogicalIdentity` | Same logical payload/different ciphertext replays |
| R06 | `rowSubjectMethodAndBindingHmacMustMatchPayload` | Input round-trip substitution denied; stored own-row subject/method validated; valid historical binding/HMAC changes produce409, not422 |
| R07 | `serverResolvedProofReferenceNotClientTextDefinesIdentity` | Same approved ref vs changed ref distinction |
| R08 | `sensitiveValuesAndProviderFailuresHaveRedactedRepresentations` | Defensive copies; constant toString on Payload/ProofRequest/ApprovedProof/SealedReference/all rows/results/command state; boolean canaries in representations/logs/causes, no secret failure output |

`com.onecall.archtests.SubjectClaimPersistenceConfigurationTests` (8):

| ID | Exact method | Required assertions |
| --- | --- | --- |
| C01 | `defaultWebAndDisabledProfilesCreateNoCommandPool` | Existing contexts unchanged, no DB requirement |
| C02 | `explicitEnablementRequiresAllDedicatedCredentials` | No query/default URL/credential fallback; constant errors |
| C03 | `commandBeansUseExactQualifiersAndOneDedicatedDatasource` | Pool/TM/DSL/owner bindings |
| C04 | `writeTemplateUsesReadCommittedRequiresNewAndRollbackCleanup` | Transaction settings and transaction-aware provider |
| C05 | `defaultProvidersAreUnavailableAndControlledReplacementsAreUnique` | Missing/ambiguous fail closed |
| C06 | `customerComposerAndControllerUsePublishedOwnerProviderOnly` | Qualified lazy types, profile and exact mapping |
| C07 | `roleAssertionFailureNeverFallsBackToAnotherPrincipal` | SET LOCAL ROLE literal, exact current_user |
| C08 | `datasourceAndProviderDiagnosticsNeverExposeCredentialsOrReferences` | Constant errors/log suppression |

`com.onecall.archtests.architecture.CustomerSubjectClaimBoundaryTests` (6):

| ID | Exact method | Required assertions |
| --- | --- | --- |
| A01 | `namedCommandExportsOnlyFrozenOwnerTypesAndPersonalCaller` | Two top-level types plus exact nested members; no internal dependency |
| A02 | `controllerBinderAndProblemSurfaceMatchFrozenOperation` | Request/response/method/error enums exact |
| A03 | `customerCompositionHasNoSqlOrClientAuthorityEscape` | Only named owner imports; no getCustomerMe/internals/reflection/session creation |
| A04 | `ownerSqlUsesOnlyApprovedColumnsLocksAndAuditFunction` | Explicit SQL columns, no forbidden write/provider framework |
| A05 | `singleAuthoritativeTransactionAndLocalSpisPreserveBoundary` | One template execute per attempt; no prep/staged map/restart/savepoint/network/external provider/generic lock subsystem |
| A06 | `openApiMigrationsAndProductionProviderAbsenceRemainFrozen` | Exact20 unique raw+LF oracles: OpenAPI/docs14/docs15 + V001–V017 (V003 once); Control excluded; no real provider/KMS adapter |

### 9.2 PostgreSQL 16 additions: 34 methods / 2 classes

`com.onecall.archtests.container.CustomerSubjectClaimsIT` (24):

| ID | Exact method | Required assertions |
| --- | --- | --- |
| I01 | `postgres16FrozenSeventeenMigrationChainAndDedicatedLoginAreUsed` | Before any HTTP: fixture profile/qualified-owner/repository/pool/two-SPI preflight and role/PID; PG16,17 history entries/resources, Flyway validate, synthetic-only login |
| I02 | `roleLocksReadsWritesAndAuditShareOneConnection` | Fixture bean preflight first; effective role/PID, read-write READ COMMITTED; local SPI calls on same transaction; no network |
| I03 | `freshUnownedPersonalClaimCommitsOneOwnerAndAcceptedRow` | HTTP200, exact insert/owner revision+1/time/audit |
| I04 | `inactiveAccountAndOrganizationWorkspaceAreForbidden` |403 for inactive or missing current account and organization workspace; missing account has no claim/owner/audit write; trusted authority, no organization locks |
| I05 | `proofInvalidUnavailableAndUnresolvableReferencesFailClosed` |422/503, no accepted/owner write, stored invalid vs risk precedence |
| I06 | `missingSubjectBindingAndBindingMismatchUseUniformValidation` |422 no existence disclosure; permitted resolvable rejection history |
| I07 | `inactiveAndUnverifiedBindingsPersistOnlyRejectedHistory` |422; no owner/accepted mutation |
| I08 | `openRiskRejectsFreshRequestAndPreemptsAcceptedReplay` |409; fresh REJECTED; old accepted unchanged/undisclosed |
| I09 | `openRiskPreemptsSameAccountDuplicateAndStoredReferenceErrors` |409 before duplicate and stored Invalid/Unavailable |
| I10 | `clearedRiskReevaluatesAndReplaysFirstStoredOutcome` | Original accepted or rejected result; no reinterpretation/history rewrite |
| I11 | `exactReplayUsesFirstIdTimeAndRandomizedReferenceEquality` | Logical equality, no second claim/owner update |
| I12 | `changedReplayIdentityReturnsConflictWithoutStoredDisclosure` | Each identity component; counting cipher zero foreign-account/subject/method stored opens; malformed/unavailable foreign refs still409; OPEN risk409 first |
| I13 | `exactDuplicateWithNewKeyReturnsFirstAcceptedWithoutInsert` | No fresh-key row; audit only |
| I14 | `changedDuplicateContextAppendsOneConflictAndNeverUpdatesOwner` | Changed method/binding/proof,409 one row |
| I15 | `identicalRejectedAndConflictReplayReturnsFrozenStoredShape` |200 after current predicates/risk permit; no verifiedAt |
| I16 | `otherAccountAndOrganizationOwnersAreNeverTransferred` |403 one CONFLICT, owner/revision unchanged |
| I17 | `corruptAcceptedOwnerRelationsFailWithoutRepair` | Seed inconsistent data as fixture admin;500 no accepted/owner mutation |
| I18 | `concurrentExactKeyCallsHaveOneAcceptedWinner` | Two HTTP connections converge id/time, one owner update |
| I19 | `crossSubjectRequestKeyLoserRollsBackBeforeReplayConflict` | Real23505, loser cleanup before reread, no loser owner/claim write |
| I20 | `concurrentSameAccountDuplicateAndChangedContextSerialize` | Exact duplicate no row; changed context one CONFLICT |
| I21 | `twoAccountsClaimingUnownedSubjectHaveOneOwnerWinner` | One ACCEPTED/revision, loser403 CONFLICT |
| I22 | `riskBindingAndAccountWritersRespectBothSerialOrders` | Number/binding/account gates, zero-risk OPEN phantom, claim-first/risk-first |
| I23 | `auditFailureAndConditionalOwnerMissRollBackWithoutSecretLeakage` | Atomic rollback, safe audit fields, HTTP/log/value canaries |
| I24 | `boundedWholeTransactionRetriesRestorePoolAfterFailure` | Global request/accepted uniqueness and serialization/deadlock;3 attempts; same-tx local SPIs, fresh risk, no aborted reads/savepoints, pool cleanup |

`com.onecall.archtests.container.SubjectClaimRoleLeastPrivilegeIT` (10):

| ID | Exact method | Required assertions |
| --- | --- | --- |
| L01 | `syntheticLoginHasOnlyExistingSubjectNumberMembership` | Exact attributes/membership, no production provisioning |
| L02 | `directLoginCannotReadOrWriteBeforeLocalRole` | NOINHERIT default denied; role cleanup |
| L03 | `assumedRoleCanReadExactClaimPredicatesAndInsertHistory` | Current role; actual binding/risk FOR SHARE, subject FOR UPDATE, exact SELECT/INSERT and audit function usable |
| L04 | `ownerUpdatePermitsOnlyAccountRevisionAndTimeColumns` | Org owner/real-name changes denied; allowed conditional update |
| L05 | `claimUpdateDeleteAndTruncateAreDenied` | Runtime ACL denial; owner append-only trigger separately raises |
| L06 | `foreignAccountWritesLocksAndAdjacentReadsAreDenied` | Only id/status; FOR SHARE/UPDATE and PII columns denied |
| L07 | `unrelatedDomainWritesDdlAndOwnerEscalationAreDenied` | No evidence/sharing/activation/order/account etc write; no owner/admin role |
| L08 | `advisoryAndAuditFunctionsNeedNoNewGrantOrMigration` | Built-in advisory, append_fact usable, direct audit/outbox DML denied |
| L09 | `localRoleAndTransactionLocksClearOnCommitRollbackAndDenial` | Same pooled session reverts role, read-only/isolation/pid checks |
| L10 | `frozenAclsHistoryAndCustomerQueryPrincipalRemainUnchanged` | 17 migrations, unchanged logical ACLs/provisioning oracles; query writes denied |

`CustomerSubjectClaimFixture` composes `CustomerBusinessNumbersFixture.start(null)` for the frozen migration/provisioning baseline, then starts its **own** `SpringApplicationBuilder(OneCallApplication.class, CustomerBusinessNumbersFixture.SessionBeans.class, ControlledBeans.class).profiles("web", "persistence")`, port0. Never call base.web() (web-only). Supply high-priority test properties explicitly:

| Property | Fixture value / authority |
| --- | --- |
| `onecall.customer.persistence.url`, `.username`, `.password` | Container JDBC URL, existing `pa27_customer`, synthetic fixture password |
| `onecall.customer.business-numbers.cursor-hmac-key` | Base64 synthetic32-byte key |
| `onecall.customer.evidence.cursor-hmac-key` | Base64 synthetic32-byte key |
| `onecall.subject-claim-command.enabled` | `true` |
| `onecall.subject-claim-command.datasource.url`, `.username`, `.password` | Container JDBC URL, distinct `pa30_subject_claim_<suffix>`, its synthetic password |
| `onecall.persistence.url`, `.username`, `.password` | Container JDBC URL, existing `pa27_query`, synthetic fixture password; required by unrelated eager sharing-query pool under persistence |
| `onecall.sharing-command.enabled`, `onecall.sharing-admission.datasource.enabled`, `onecall.evidence-media-read.datasource.enabled` | `false`; unrelated opt-ins stay disabled; public-read configuration is gated by sharing-command.enabled too |

ControlledBeans installs exactly one `SubjectClaimProofVerifier` and one `SubjectClaimReferenceCipher`, suppressing fail-closed defaults via their missing-bean conditions, not bean overriding/primary ambiguity. Before **any endpoint call**, fixture preflight/I01 asserts profiles web+persistence, `customerSubjectClaimService` qualified owner implementation, `subjectClaimRepository`, `subjectClaimCommandDataSource`, its manager/template/DSL, and both unique controlled SPI beans exist and are wired to the dedicated pool. Assert query and command pools are distinct and command username is the synthetic subject-number login, never pa27_customer/pa27_query/admin/default; use a role/PID probe on the command template. I02 checks same-connection operations; C02/C03 remove each dedicated credential in turn and prove no fallback despite populated customer/sharing properties. Unrelated persistence pools are never implicit command credentials.

Fixture admin seeds unowned/same/foreign/org subjects, active/disabled/unverified bindings, OPEN/CLEARED/no risk, immutable accepted/rejected/conflict references and a verified session whose current account row is absent. Missing-account assertion is uniform403, no claim/owner/audit write. Historical accepted corruptions are deliberate negative fixtures, never repaired. Synthetic role creation is local to this fixture's isolated container and must not pollute existing ACL/provisioning oracle fixtures or justify changing their expected catalogs. No production provisioning evidence is claimed.

`ControlledSubjectClaimProviders` uses an in-memory immutable approval map keyed by synthetic attempt ID, binds all approved dimensions, and a test-only ephemeral JCA authenticated cipher with randomized nonce and test key ref. It is not component-scanned, is never in main sources/JAR, and must not use plaintext `v1.test.<json>` as evidence. Algorithm choice in a test fake does not settle production Q2. U11/I12 instrument open calls per synthetic stored-envelope identity: foreign-account, different-subject and different-method rows each have exactly zero opens, including Invalid/Unavailable variants. Count local input round-trip opens separately; never reset away a stored open. With OPEN risk assert zero stored opens even for metadata-compatible Invalid/Unavailable rows and CLAIM_RISK_BLOCKED rather than replay/reference failure. No foreign plaintext is constructed/staged. Assertions use numeric counts and safe booleans, never ciphertext-valued assertion keys. Safe boolean assertions verify secret canary absence; never AssertJ-print an actual sensitive payload, bind list, body or ciphertext on failure. R08/W11/U18/I23 exercise every named sensitive type and captured logs/exception diagnostics with `assertThat(rendered.contains(canary)).isFalse()`-style boolean assertions and constant descriptions, never `doesNotContain(canary)` on the actual secret-bearing value. Rejection/ACL probes run on their own rolled-back transactions; no savepoints are needed to continue after denial.

### 9.3 Exact arithmetic and historical fixtures

Exact existing-test edits (zero added/deleted/renamed existing methods):
- `ModuleBoundaryTests.EXPECTED_ALLOWED` customer set gains `subject_number::customer-subject-claim-command` (10→11). In **both** `verifiesNoCyclesOrBoundaryViolations` and `rejectsInternalPackageAccessFixture`, change `hasSize(10)` to `hasSize(11)` and add tuple `("subject_number", "customer-subject-claim-command")` to each exact element/edge list. Retain all ten old edges and all other module sets. Existing `assertsExactAllowedDependencyMapFromPlan62` and `discoveredProductionEdgesStayInsideTheConfiguredMap` validate the extended map, unchanged method identities.
- `CustomerApiBoundaryTests.customerDependenciesAndReadOnlySqlStayInsideBoundary`: `hasSize(39)`→`hasSize(43)` and narrowly allow the new published command imports without weakening read-only SQL/internal-import checks. `profileAndVerifierArtifactsCannotEscapeIntoProduction`: append `web/CustomerSubjectClaimsController.java` to its fixed profile/controller List.of; preserve all entries and profile/servlet assertions. `customerApiSignaturesAndProjectionAreExact` remains the same method.
- `CustomerBusinessNumberBoundaryTests.customerImportsOnlyPublicQuerySeamsAndNoForeignInternalImplementation`: change only the exact customer dependency `hasSize(10)`→`hasSize(11)`, with the package map gaining `subject_number::customer-subject-claim-command`.
- `CustomerEvidenceBoundaryTests.namedOwnerInterfacesAndTenCustomerDependenciesAreExact`: append only `subject_number::customer-subject-claim-command` to the exact ten-element customer dependency set/list, making eleven; retain the method identity including `Ten`.
- `CustomerSubjectDetailBoundaryTests.subjectEvidenceAndOrderExposeOnlyMinimalNamedQueryInterfaces`: change only the exact customer dependency `hasSize(10)`→`hasSize(11)`.
- `SharingQueryBoundaryTests.sharingQueryApiAndProjectionAreExact`: append only `subject_number::customer-subject-claim-command` to the exact ten-element customer dependency set/list, making eleven.
- In all four files retain all ten existing dependency entries wherever enumerated and every other assertion, method identity and method count. No weakening, renaming or new tests; REV-09 completes REV-05.
- `CustomerWebConfigurationTests` and `OneCallApplicationTests`: adjust only current default/profile/qualified-absence assertions; keep every existing method identity/count. No test or allowlist enforces provider-outside-transaction preparation anymore.

U06 `allProviderCallsRunOutsideTransactionsAndLocks` is removed, not padded/replaced: its contract contradicts S1 single-transaction local SPIs. U05/A05/I02 now assert the actual transaction contract. U18/A05/I24 are renamed only within the **proposed new** inventory to remove obsolete preparation/restart scenarios and retain needed audit/retry coverage. Thus additions fall from86 to85 methods, Surefire52→51; class counts stay unchanged; REV-09 expands only the implementation manifest to41, not the test inventory.

| Inventory | Current observed source/CI | Additions | Final source=CI=XML requirement |
| --- | --- | --- | --- |
| Surefire classes / methods | 49 / 417 | 5 / 51 | 54 / 468 |
| Failsafe classes / methods | 21 / 336 | 2 / 34 | 23 / 370 |
| Combined classes / methods | 70 / 753 | 7 / 85 | 77 / 838 |
| `@Test` annotation-line scan | 754 | 85 | 839 |
| POM Failsafe explicit includes | 21 | 2 | 23 |
| Production modules | 14 | 0 | 14 |
| Customer internal regular source files | 39 | 4 | 43 |
| Flyway migration resources/history | 17 | 0 | 17 |

Annotation-line arithmetic includes the existing single commented historical `@Test` line in PostgresContainerIT; preserve its exact whitelist and all historical stage totals. It is not an executable test. Extend CI with explicit `pa30_unit_additions` / `pa30_container_additions` maps containing exactly the names above. Preserve PA29-start 375+302=677 and PA30-start/current baseline 417+336=753 when computing historical inventories. Do not subtract PA30 names from older stages incorrectly or weaken exact class/name equality, duplicate detection or zero failures/errors/skips. Update **both** early Surefire report total 417→468 and final XML totals/class/POM/line arithmetic. Existing `PostgresContainerIT` stays exactly64; existing ACL catalogs and provisioning history stay unchanged.

POM adds `**/CustomerSubjectClaimsIT.java` and `**/SubjectClaimRoleLeastPrivilegeIT.java`, no broad wildcard. CI JAR smokes add the new route's unauthenticated401/closed problem/no-store probe on web profile; default profile still starts without web/Hikari. Preserve existing dependency allowlists and staged firewall totals (82 at its intermediate assertion,116 at its later assertion), not the README's older82-only description. No classpath dependency delta is expected. Existing test edits change assertions only: module map gains one named edge, CustomerApiBoundaryTests keeps explicit no-SQL/no-internal imports and file count43, CustomerWebConfigurationTests/OneCallApplicationTests prove the disabled default. Existing evidence/customer/sharing read-only checks remain strict; only the four exact customer dependency assertions specified above gain the command edge, with all other assertions unchanged.

## 10. Validation commands and expected evidence

Commands here are **for the later implementation gate**, unless explicitly listed as plan-only checks in section12. Run from repository root. Do not mistake a planned command for executed evidence. Use JDK Temurin21.0.13+9, wrapper Maven3.9.9, Node22.23.4 as CI pins; Docker must actually be usable. Source=CI=XML requires clean complete reports, never focused leftovers.

### 10.1 Focused and full builds

```bash
cd backend
./mvnw -B -ntp -Dtest=CustomerSubjectClaimsWebTests,CustomerSubjectClaimServiceTests,SubjectClaimVerificationReferenceTests,SubjectClaimPersistenceConfigurationTests,CustomerSubjectClaimBoundaryTests test
# Exactly51 methods /5 classes; zero failure/error/skip.
./mvnw -B -ntp -Dtest=ModuleBoundaryTests,CustomerApiBoundaryTests,CustomerWebConfigurationTests,OneCallApplicationTests,CustomerBusinessNumberBoundaryTests,CustomerEvidenceBoundaryTests,CustomerSubjectDetailBoundaryTests,SharingQueryBoundaryTests test
# Existing method identities/counts retained; all pass,14 modules, no forbidden edges.
docker info
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=CustomerSubjectClaimsIT,SubjectClaimRoleLeastPrivilegeIT -DfailIfNoTests=false verify
# Failsafe exactly34 methods /2 classes; PG16,17 migrations, real command login.
./mvnw -B -ntp clean verify
# Surefire468 /54 classes; default does not claim container execution.
./mvnw -B -ntp clean -Pcontainer-smoke verify
# Surefire468 /54 and Failsafe370 /23 =838 /77; zero skipped/errors/failures.
cd ..
```

The full run is mandatory before full XML inventory checks. Focused integration invocation may also run all Surefire tests by Maven lifecycle; its **Failsafe** target remains34. No report reuse to claim a skipped Docker suite. Capture bounded execution and completion logs outside tracked source.

### 10.2 Execute the repository's exact inventory, JAR smoke and firewall tools

These tools currently live as embedded CI Python, not standalone scripts; do not invent a new inventory framework. After the clean full build, this exact command first executes the CI firewall shell prerequisites in `backend`, requires both nonempty files, then extracts and runs the three current Python bodies with `cwd='backend'`. Fail on any prerequisite command failure or missing/empty file; never reuse a stale dependency list:

```bash
set -eu
(
cd backend
rm -f target/jar-index.txt target/pa11-runtime-dependencies.txt
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
./mvnw -B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa11-runtime-dependencies.txt
test -s target/jar-index.txt
test -s target/pa11-runtime-dependencies.txt
)
python3 - <<'PY'
from pathlib import Path
import re, subprocess, textwrap
for prerequisite in ('target/jar-index.txt', 'target/pa11-runtime-dependencies.txt'):
    artifact = Path('backend') / prerequisite
    assert artifact.is_file() and artifact.stat().st_size > 0, prerequisite
ci = Path('.github/workflows/ci.yml').read_text()
for name in ('assert container smoke actually ran',
             'default and opt-in web jar smokes',
             'production jar dependency firewall'):
    step = ci.split('      - name: ' + name + '\n', 1)[1]
    step = re.split(r'\n      - (?:name:|uses:)', step, maxsplit=1)[0]
    match = re.search(r"python3 - <<'([^']+)'\n(.*?)\n          \1(?:\n|$)", step, re.S)
    assert match, name
    subprocess.run(['python3', '-c', textwrap.dedent(match.group(2))],
                   cwd='backend', check=True)
PY
```

Expected: exact source/CI/XML names/classes match468+370=838; annotation line839 and sole existing comment whitelist; POM includes exactly23 and equals container-class names; all JAR default/web probes pass including new POST401, no Hikari in defaults; original dependency/firewall inventories and staged82/final116 adversarial probes remain intact. Any packaged secret/test-provider class is failure. These commands exercise actual packaged boot/HTTP/firewall, not just `jar tf` existence.

### 10.3 Frozen contract, schema and diff checks

```bash
(cd api/openapi && npm ci && npm run check:s3)
# Frozen operation/doc-map/client/typecheck/media-gateway lint all pass; no YAML edits.
git.exe diff --check
git.exe diff --name-status
git.exe diff --exit-code a4c6abacc8afeec14d0308e854a7daff442a770f -- \
  api/openapi/onecall-v1.openapi.yaml backend/src/main/resources/db/migration \
  docs/14-production-data-model-and-flyway-schema-spec.md \
  docs/15-production-openapi-surface-spec.md
git.exe status --short
```

Expected implementation diff is exactly the41-file manifest, plus this already-present plan only if comparing against the pre-plan checkpoint; no existing-file deletion/rename/unplanned changes. Verify branch/base and clean starting state using Windows Git before editing. If WSL interop is still unavailable, Controller must run Windows Git checks; never replace them with Linux Git on this worktree.

Hash verification must check **raw bytes and LF-canonical bytes**, not normalize a forbidden edit away. At implementation entry capture a private baseline of the three non-migration frozen inputs plus all17 migrations (20 unique paths; V003 counted once), and compare at completion. The three non-migration expected pairs and V003 evidence pair are in section2; fix-2 records all20 expected pairs. Control is checked for current goal/gate authority, never byte equality. Use:

```bash
python3 - <<'PY'
from pathlib import Path
import hashlib, json
names = ['api/openapi/onecall-v1.openapi.yaml',
         'docs/14-production-data-model-and-flyway-schema-spec.md',
         'docs/15-production-openapi-surface-spec.md']
names += [str(p) for p in sorted(Path('backend/src/main/resources/db/migration').glob('V*.sql'))]
assert len(names) == len(set(names)) == 20
actual = {}
for name in names:
    data = Path(name).read_bytes()
    actual[name] = [hashlib.sha256(data).hexdigest(),
                    hashlib.sha256(data.replace(b'\r\n', b'\n')).hexdigest()]
baseline = Path('/tmp/pa30-implementation-frozen.json')
if baseline.exists():
    assert json.loads(baseline.read_text()) == actual, 'Frozen input mismatch'
else:
    baseline.write_text(json.dumps(actual, sort_keys=True))
print('20 unique frozen files = 3 non-migration + 17 migrations; raw and LF verified')
PY
```

The first invocation is only baseline capture; manually/assertion-compare section2 on entry, never claim the capture alone proves the Controller hashes. A06 also embeds the exact20 implementation-frozen LF and raw oracles (including docs/15 and all17 migration identities; V003 counted once; excluding mutable Control) and rejects an added resource/version. L01–L10 are the ACL command, I01 the migration-history command, A01/A03 and existing ModuleBoundaryTests the module command; all are in focused/full invocations above. No provisioning/migration command beyond ordinary test-fixture Flyway17 is authorized.

### 10.4 README current truth

Update only current sections in `backend/README.md`: new personal claim POST and public codes; default503 without real proof/reference SPI; opt-in persistence/profile/flag and exact dedicated env variables; same-connection role/lock order/retry3; ciphertext logical equality; audited claim-history behavior; no owner transfer; actual468/370/838,54/23 classes,839 annotation lines,23 POM includes; full validation commands and staged firewall82/116. Keep historical checkpoints labeled historical, not retroactively modified. Explicitly state17 migrations, unchanged OpenAPI, no real provider/KMS or production credential provisioning, and no claim of acceptance/production readiness. No new operational runbook, Control update, existing design/doc-map edit or historical artifact churn.

## 11. Stop conditions and completion report contract

Stop immediately and report exact path/line evidence plus the minimum required Controller decision if: any frozen hash differs; the frozen schema cannot express a required public mapping; V003/V002/V001 permissions or built-in lock permissions require a grant/new migration; no clean role/pool can run with dedicated credentials; proof/reference SPI-only cannot preserve durable logical identity; transaction seams cannot provide same-connection cleanup/no-network-under-lock; requirements need organization/owner-transfer/proof-expiry/provider/KMS policy; file manifest/test arithmetic cannot be satisfied without extra scope. A migration/privilege need is `BLOCKED_NEEDS_GOAL_RECONFIRMATION`; never propose V018 as a workaround. Environment inability to run Docker/Windows Git is reported as NOT RUN, never as passing evidence.

Later implementation completion report format:

```text
Task/runtime/provider; gate and supplied base/checkpoint
Slice:1; manifest41 (new25/existing16), actual diff and any approved deviation
Behavior: exact operation/authority; state/error matrix; history/owner atomicity
Proof/reference: two SPIs, production unavailable, controlled fakes, no secret leakage
DB: dedicated pool/properties; role/PID/lock/retry evidence; no migration/grant/provisioning
Validation: each command exit/outcome, focused51 + PG34, full468+370=838,
            classes54+23=77, annotation839, POM23, source=CI=XML,
            JAR/firewall82/116 with both prerequisite files and JAR index checks,
            frozen20 raw/LF hashes (three contracts/design docs +17 migrations; no Control),
            module/ACL/diff results
NOT RUN/limitations; blockers; no production readiness claim
No commit/push/PR/review/approval/merge/later gate unless separately authorized
```

No goal drift proof: the only runtime authority entering SQL is authenticated account plus verified personal scope; all other caller dimensions are strictly bounded requests, not ownership proof. Proof approval/reference content stays server-side. Only subject_claim INSERT, conditional permitted owner columns and existing audit function are emitted. No login/account creation, subject real-name, activation/trial/order/evidence/share write, owner transfer, new migration/OpenAPI change, real provider/KMS/provisioning or generic framework is delivered. Module and ACL tests enforce this negative scope, not just prose.

## 12. Plan-fix execution record and gate boundary

Fix2 read the fixed plan, immutable historical fix-1, current Control PA30 goal/re-review adjudication and both accepted re-review artifacts. All five exact dispatch input hashes matched before editing. All prior accepted REV-01 through REV-07 findings remain binding; REV-09 completes REV-05 by adding four assertion-only existing test edits, REV-10 supplies the firewall prerequisites and REV-11 removes mutable Control from implementation freezing without weakening docs/15. The single-authoritative-transaction design, exact fixture/audit/redaction contracts, public surface and V001–V017 ACL reuse remain unchanged. Private before-write filesystem and frozen-input snapshots support the two-file write boundary. No delegation or external mutation occurred.

Plan-only checks and actual outcomes belong in fix-2: UTF-8/LF/final-newline/no trailing whitespace, plan/fix-2 hashes, all five exact input hashes, manifest41=25+16 with contiguous expanded allowlist/status checks, named-test uniqueness51+34=85, source/CI/POM baseline and final arithmetic, frozen20 raw/LF checks (three non-migration +17 migrations, V003 once), exact §10.2 preamble/extractor and fail-closed prerequisite checks, `git.exe diff --check` and status attempt. Historical fix-1/re-review manifest/freeze/command claims are superseded by this plan and fix-2, not edited. Windows Git remains unavailable due WSL interop; do not call that passing diff/status or branch/base proof. Private filesystem hashes confirm only the two authorized documents changed during fix2; Controller/input artifacts remain unchanged by this task. Maven/Docker/XML/JAR/firewall/OpenAPI toolchain/endpoint execution are **NOT RUN at plan fix**. Final source=CI=XML counts and actual firewall82/116/JAR index evidence remain future implementation requirements, not executed tests.

Design blockers/open policy questions: **none** after the bound missing-account403, docs/15 freeze, single-transaction local-SPI, exact audit and constant-redaction resolutions. Remaining operational verification risks are implementation-time database/concurrency/fixture/secret-leak/CI behavior and the Windows Git limitation, not permission to alter the goal. Real provider/KMS, external I/O transaction design and production provisioning remain deferred. No source/test/config/migration/OpenAPI/CI/POM/README/Control edit, commit/push/PR/reviewer request/approval/ready/external mutation or later gate is authorized or performed. Stop at the fix gate; do not request fresh review yourself.
