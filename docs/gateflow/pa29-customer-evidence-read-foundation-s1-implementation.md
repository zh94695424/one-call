# PA29 Customer Evidence Read Foundation — S1 Implementation

> Code-review fix 1 update (2026-11-12): task `pa29-evidence-s1-code-review-fix1-gpt-20261112-10`, Codex `gpt`, gate **fix**, repairs only accepted A-LOW-1/A-LOW-2. Exact current V017 seals below supersede the reviewed implementation seal. Original worker/Controller execution records remain historical evidence, not executions of the resealed migration. The current fix commands, outcomes, old/new seals and bounded ten-path scope are recorded separately in `docs/gateflow/pa29-customer-evidence-read-foundation-s1-code-review-fix-1.md`; no review acceptance or gate advancement is asserted.

## 1. Authority, outcome and scope

- Task `pa29-evidence-s1-report-finalizer-gpt-20261112-09`, date 2026-11-12, work unit `PA29-customer-evidence-read-foundation`, sole approved slice S1, gate **implementation, final S1 report sealing**. Lineage: Workers A–C implementation/evidence → Worker D integration/audit → Worker E bounded regression repair (`pa29-evidence-s1-regression-fix-worker-e-gpt-20261112-08`) → Controller post-repair executions → this report-only finalizer.
- Integration outcome: **COMPLETE — implementation-only**. Controller-final focused18/18, all19 baseline302/302, clean Surefire417/417 + Failsafe336/336 =753/753, exact source=CI=XML inventory, JAR smokes/firewall and A1–A5 validators all pass after the two bounded repairs. This is not S1 acceptance, code-review acceptance/entry, later-gate authorization or production/live readiness.
- Binding accepted plan: `docs/gateflow/pa29-customer-evidence-read-foundation-plan.md`, raw SHA256 **6e7ebb64fd1e37a7d08e9ccd4b58d46ff4ecabd382a0a92255a5271193a2d2a2**. The read-only PA29 Current checkpoint authorizes final integration and the one typed-Mockito amendment `PA29-S1-IMPL-01`: 53→54 candidates, 19 new +35 existing. Plan references in the dispatch to §11.5 resolve to no heading in the sealed plan (it ends at §11.3); the complete executable runtime/frozen commands are in §11.2, used with §9.8 without editing the plan.
- Recorded managed HEAD `73813534bb3a8ec8f3f3c38db6c16e0d12109b5c`; merged base `ef9d0823975ef66d1df3b70e921709d8562b4dcb`. Worker D used Windows `git.exe` read-only; this finalizer's Linux Git read fails on the Windows pointer (exit128), with no pointer repair or independent remote-state claim.
- Worker E repaired only the following two already-allowlisted test paths and updated this report; both repairs preserve every public test identity (10 and 8 respectively):

- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailReadPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java`
- `docs/gateflow/pa29-customer-evidence-read-foundation-s1-implementation.md`

- Worker D's five authored paths were `.github/workflows/ci.yml`, `backend/README.md`, `docs/17-production-security-design-spec.md`, `docs/operations/pa25-production-database-identity-provisioning-runbook.md`, and this report. The exact cumulative candidate set is **54/54** (19 new +35 existing); the two Worker E repairs add no path. Controller report finalization is the only current repository write: `docs/gateflow/pa29-customer-evidence-read-foundation-s1-implementation.md`. No source/test/POM/CI/other-doc/Control/plan/review/private/generated-target path is writable by this finalizer.

### Goal / non-goal mapping

| Goal | Accepted implementation/evidence | Excluded expansion |
| --- | --- | --- |
| G1 | Only frozen list/detail GET routes; explicit safe DTOs; canonical route/auth/input/error precedence; WebTests12 and actual CustomerEvidenceIT HTTP cases | No new filters, owner selectors, alternate methods, routes or OpenAPI edits |
| G2 | Verified personal account/selected organization, ACTIVE lifecycle/four roles, owner seams, uniform404, all-binding whole-list OPEN403 | No share/creator/SOLIDIFIED eligibility, real-name/claim/activation gate or client authority |
| G3 | All four states, immutable capture scalar, current facts, role/hash/size/MIME artifact order and authoritative ORIGINAL duration | No live identity, raw JSON/snapshot/event output, media/storage/crypto/share credential |
| G4 | Descending native-UUID keyset, lookahead/last-returned anchor, endpoint/account/workspace/sort HMAC binding; limit20/1–100 not cursor-bound | No offset, total count, cache, cross-request snapshot or new rate limiter |
| G5 | Minimum named Subject/Evidence interfaces, one qualified customer pool/template read-only REQUIRES_NEW/REPEATABLE_READ snapshot, batched queries, pool cleanup | No foreign-owner SQL in Customer, N+1, generic framework, new resources, locks or business writes |
| G6 | Frozen26/18raw/26canonical, exact19-grant/history17/PA25 proof, literal inventory and honest actual/static separation | No predecessor rewrite, ACL repair, dependency/plugin upgrade, deployment/live acceptance or Git/remote mutation |

### Exact cumulative candidate allowlist (54)

The following is the independent literal §8 allowlist plus the Controller's single amendment. `scope.json` records exact status inventory, excludes pre-existing Controller/plan/review/private inputs and independently validated EOL-only frozen statuses. Worker D's raw non-owned manifest additionally seals 2,644 paths, including original generated targets. No extra candidate is authorized.

1. `.github/workflows/ci.yml`
2. `backend/README.md`
3. `backend/ops/postgresql/pa25-runtime-identities.sql`
4. `backend/pom.xml`
5. `backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerEvidenceQueryService.java`
6. `backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerPersistenceConfiguration.java`
7. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java`
8. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerEvidenceController.java`
9. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerEvidenceCursorCodec.java`
10. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerEvidenceCursorProperties.java`
11. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerEvidenceDtos.java`
12. `backend/src/main/java/com/onecall/core/customer_iam/package-info.java`
13. `backend/src/main/java/com/onecall/core/evidence/customerevidencequery/CustomerEvidenceReader.java`
14. `backend/src/main/java/com/onecall/core/evidence/customerevidencequery/package-info.java`
15. `backend/src/main/java/com/onecall/core/evidence/internal/customerevidencequery/CustomerEvidenceQueryConfiguration.java`
16. `backend/src/main/java/com/onecall/core/evidence/internal/customerevidencequery/JooqCustomerEvidenceReader.java`
17. `backend/src/main/java/com/onecall/core/subject_number/customersubjectquery/CustomerSubjectAccessReader.java`
18. `backend/src/main/java/com/onecall/core/subject_number/internal/customerquery/JooqCustomerSubjectAccessReader.java`
19. `backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql`
20. `backend/src/test/java/com/onecall/archtests/CustomerEvidenceConfigurationTests.java`
21. `backend/src/test/java/com/onecall/archtests/CustomerEvidenceWebTests.java`
22. `backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java`
23. `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java`
24. `backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java`
25. `backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java`
26. `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectDetailBoundaryTests.java`
27. `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`
28. `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`
29. `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java`
30. `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersFixture.java`
31. `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersIT.java`
32. `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceFixture.java`
33. `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceIT.java`
34. `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceReadPrivilegeIT.java`
35. `backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java`
36. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailReadPrivilegeIT.java`
37. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectsIT.java`
38. `backend/src/test/java/com/onecall/archtests/container/DatabaseRuntimeAclRbacHardeningIT.java`
39. `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java`
40. `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java`
41. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
42. `backend/src/test/java/com/onecall/archtests/container/ProductionDatabaseIdentityProvisioningIT.java`
43. `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java`
44. `backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java`
45. `backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java`
46. `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java`
47. `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java`
48. `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerBusinessNumberQueryServiceTests.java`
49. `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerEvidenceQueryServiceTests.java`
50. `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerSubjectDetailQueryServiceTests.java`
51. `backend/src/test/java/com/onecall/core/customer_iam/internal/web/CustomerEvidenceCursorCodecTests.java`
52. `docs/17-production-security-design-spec.md`
53. `docs/gateflow/pa29-customer-evidence-read-foundation-s1-implementation.md`
54. `docs/operations/pa25-production-database-identity-provisioning-runbook.md`

## 2. Current contract and synchronized documentation

The current contract below is text-identical to the prepended current-boundary section of all three owned boundary documents. The entire PA28 body and every older body remain unchanged after labeling PA28 historical; `independent-audit.log` compares historical text against HEAD. Current requirements are not inferred runtime passes.

## PA29 Current Runtime Boundary (V017)

This current boundary supersedes PA28 V016 and all older current capability/history/inventory statements below; their evidence remains historical. PA29 adds only `GET /api/v1/customer/evidence` / `listCustomerEvidence` and `GET /api/v1/customer/evidence/{evidenceId}` / `getCustomerEvidence`. OpenAPI and V001–V016 remain byte-frozen. Existing customer endpoints retain their prior contracts.

Exact safe shapes: page has only `items`, `nextCursor`, `hasMore`; summary has only `evidenceId`, `subjectId`, `status`, `sourceType`, `capturedAt`, `consentVersion`, `createdAt`, `artifactRoles` (always present, including `[]`). Detail has only `evidenceId`, `subjectId`, `status`, `sourceType`, `capturedAt`, `consentVersion`, optional `solidifiedAt`, `artifacts`; **detail has no `createdAt`**. Each artifact requires `artifactRole`; optional `mimeType`, `sizeBytes`, `validatedDurationSeconds`, `sha256` are omitted when absent, not fabricated. `capturedAt` is the validated immutable `identity_at_capture.capturedAt` scalar, never live identity or snapshot fallback. All four statuses PENDING, SOLIDIFIED, QUARANTINED, FAILED are visible; detail re-reads current facts. Artifacts order ORIGINAL then DERIVED, then hash/size/MIME; duration is authoritative only for ORIGINAL with a unique validated source/media match, never DERIVED.

Only the verified session supplies account/workspace authority. Personal scope matches subject `owner_account_id`; selected organization scope matches `owner_organization_id`, requires ACTIVE account/organization/membership and accepts ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER, AUDITOR. Inactive lifecycle is FORBIDDEN 403. Missing/unowned/foreign-workspace/other-subject detail is uniform NOT_FOUND 404. Any OPEN risk on any binding, regardless of binding status, of **any accessible subject having evidence** denies the **whole list with 403 before pagination**, including off-page subjects; owned subject without evidence does not deny the list. Detail checks requested subject risk after ownership. Malformed UUID/limit returns 422; malformed/tampered/context-replayed cursor returns 400; unsupported methods/aliases/extra route segments return 404. Authentication precedes input parsing; ignored query/header/body selectors never grant authority. Unrepresentable persisted detail facts fail closed as 404, list facts as sanitized 500; dependency/completion failure is sanitized 503.

Stable keyset order is `createdAt DESC, evidenceId DESC` using PostgreSQL UUID order, fetch limit+1, next anchor from the last returned item; tail has `nextCursor: null`. Default limit 20, inclusive 1–100. HMAC-SHA256 cursor binds version, fixed evidence endpoint, verified account, workspace type/organization, fixed sort and anchor; limit is deliberately not cursor-bound; no offset/count or cross-request snapshot promise. Explicit independent `onecall.customer.evidence.cursor-hmac-key` must be canonical base64 of exactly 32 bytes; there is no production default. Key/service resolution stays lazy after structural input validation; detail never resolves the cursor key.

Customer composes only named `customer-subject-query` and `customer-evidence-query` owner interfaces. Lifecycle, ownership, all-binding risk and batched safe facts share one customer-role **read-only REQUIRES_NEW / REPEATABLE_READ** transaction/connection; no N+1 or business/event/audit/outbox writes. Pool state and suspended transactions restore on success/failure. Authorization is application predicates plus exact ACLs, not RLS or linearizable revocation: later committed changes govern the next request.

V017 is privilege-only: **19** owner-issued column SELECT tuples, no grant options: `evidence_record(id,status,source_type,source_ref_id,identity_at_capture,consent_version,created_at,solidified_at)` (8), `evidence_artifact(evidence_id,artifact_role,media_object_id)` (3), `media_object(id,mime_type,size_bytes,sha256)` (4), `recording(id,status,media_object_id,validated_duration)` (4). All grantors are `evidence_owner`, grantee `customer_iam_role`. Arithmetic: **269 + 19 = 288 = customer53 + evidence16 + others219**; customer53 = 6 schema USAGE + 47 column SELECT, target pair50→69. No new table SELECT, writes, routines, sequences, schema CREATE, roles, memberships or pools. The JSONB column grant makes full capture JSON readable at the raw-role boundary: scalar-only SQL/output/logging is a trusted-application Security/Database residual, **not JSON-path security**.

Apply/Flyway-validate V001→V017 before current PA25 apply/check: exact **17** successful SQL history rows, V016-only rejected. V017 raw SHA256 `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad`; Flyway checksum **-41822162**. Replay accepts only exact V016 pre-state or complete V017 final state; partial/widened/wrong-grantor/grant-option/history drift rejects atomically without PA25 repair. Six logins/seven effective roles/four membership edges and credential/provenance/owner/PUBLIC/default-ACL invariants remain unchanged.

Required source=CI=POM=XML inventory is **49 classes / 417 Surefire + 21 / 336 Failsafe = 70 / 753**, 76 additions (42/34), annotation lines **754** with one historical comment, **21** exact Failsafe includes. Execution outcomes and any NOT EXECUTED gates belong to `docs/gateflow/pa29-customer-evidence-read-foundation-s1-implementation.md`, not an inferred pass from these requirements.

No media bytes/playback/download/URLs/storage/crypto references, share credentials/operations, live identity, snapshot/event payloads, lifecycle mutation or session issuance is added. External production credentials, TLS/pg_hba, migrator custody, deployment/rotation/pool drain/session invalidation and Q17/Q23 remain Platform/Database/Security/Operations work: **NOT EXECUTED; production/live readiness NOT CLAIMED**.


## 3. V017, exact tuples and independent database evidence

Current fix-1 V017 raw SHA256 **57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad**, Flyway checksum **-41822162** (independent line CRC32 plus actual Flyway 12.4.0 `ChecksumCalculator` on final bytes; retained V016 checksum1669606497). Current runtime metadata and PostgreSQL checks are recorded in the fix-1 artifact, not attributed to the original Worker C execution. History is exact17 successful SQL rows, rank17/version017/script `V017__customer_evidence_read_privileges.sql`/description `customer evidence read privileges`. Historical Worker C `postgresql-history.log` records PostgreSQL **16.15**, Debian16.15-1.pgdg13+2. Worker D used the same local `postgres:16` image; its exact digest/image ID remain preserved in `postgres-image.log` rather than inferred from a floating tag.

ACL arithmetic: old269 + delta19 = global288 = customer53 + evidence16 + others219; customer53 =6 schema USAGE +47 column SELECT; customer/evidence target50→69. All nineteen tuples have grantee `customer_iam_role`, object kind `column`, privilege SELECT, grantor `evidence_owner`, grantable false:

```text
customer_iam_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|consent_version|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|created_at|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|solidified_at|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false
customer_iam_role|column|evidence.media_object|id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.media_object|mime_type|SELECT|evidence_owner|false
customer_iam_role|column|evidence.media_object|sha256|SELECT|evidence_owner|false
customer_iam_role|column|evidence.media_object|size_bytes|SELECT|evidence_owner|false
customer_iam_role|column|evidence.recording|id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.recording|media_object_id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.recording|status|SELECT|evidence_owner|false
customer_iam_role|column|evidence.recording|validated_duration|SELECT|evidence_owner|false
```

`worker-c/acl-arithmetic.log` independently equates the literal IT oracle and PA25 expected ACL set, not just cardinality. Worker C privilege10/10, combined34/34 and provisioning20/20 all exit0 with zero failures/errors/skips. Its passing exact identities cover nineteen independent V016 SQLSTATE42501 gaps, fresh17/upgrade1/repeat0, frozen predecessors, all approved facts and denied sensitive/table/event/snapshot/media/declared-recording/write/routine/sequence/role capabilities; direct raw-role JSON readability is deliberately demonstrated, not misrepresented as JSON-path security. Exact replay preserves catalog/credential/definitions/business snapshots. Every partial/missing/wrong-owner/grant-option case and thirteen V017 rejection categories (AUTHORITY, PROVENANCE, HISTORY, SETTINGS, ATTRIBUTES, TOPOLOGY, OWNERSHIP, ACL, DATABASE, DEFAULT_ACL, DEPENDENCY, PRESERVATION, RECONCILIATION) rejects atomically without repair. PA25 apply/check reject V016-only and altered ACL/history; healthy repeat is mutation-free. Six logins, seven effective roles, four ADMIN FALSE/INHERIT FALSE/SET TRUE edges remain exact. The actual qualified customer pool restores role and transaction on success/not-found/permission failure and permits the next borrower without elevation.

Worker C's preserved host-only/first/second failures and final Windows transport recipe are historical execution evidence, not suppressed failures. Final Worker C Java21.0.12.1 is **not** pinned-JDK evidence; Worker D's separate toolchain log is the pinned proof.

## 4. Worker evidence and actual/static distinction

Evidence root is `D:\codex-temp\pa29-implementation-evidence-20261112-02`; authoritative roots are `worker-d` and `worker-e`. Controller-final post-repair logs/exits are in `worker-e`; Worker D audit/static/frozen evidence remains in `worker-d`. `worker-input-evidence-index.json` records raw size/SHA256 for all worker A–C/tooling inputs read. JSONL dispatch transcripts and original failures remain in their original directories.

| Worker | Actual evidence used | Limits |
| --- | --- | --- |
| A | Web12/configuration6/boundary4 exact source/plan/XML identities,22/22 exit0; exact shapes/auth/input/route precedence/redaction/default-profile/lazy-resource/module/frozen proof; diff/frozen checks | Windows Java21.0.12.1; no full CI or S1 acceptance |
| B | CustomerEvidenceIT24/24 exit0 on PostgreSQL16.15; verified HTTP personal/org/four roles/four states/whole-list risk404/403/cursor/artifact/current-state cases; deterministic snapshot concurrency, no-write snapshots, constant query counts and pool restoration | Process-local host transport, explicit selection before POM integration; no full CI; initial failures retained |
| C | Privilege10/10, combined evidence34/34, provisioning20/20 exit0; independent tuple/history/rejection/no-repair/effective-role/pool matrix; exact two POM includes | Unpinned Windows Java; no global completion; bridge stopped |
| D | Owned CI literal maps and historical arithmetic, synchronized current docs, independent predecessor/source/POM audit, pinned runtime, frozen26/raw18/canonical26 and exact candidate54 audit | Pre-repair baseline failure is retained as historical evidence, not the final integration result |
| E | Corrected historical/current migration targeting and exact288 oracle; moved V015 wrong-rank probe17→18; focused18/18 pass with all10+8 identities preserved | Two bounded existing test repairs only; intermediate failures retained |
| Controller final | Post-repair baseline302/302; clean753/753; exact70-class source=CI=XML/POM21; JAR smokes, firewall116 probes and A1–A5 validators exit0 | Local implementation evidence only; no external production runtime or readiness evidence |
| Report finalizer | Accepted-plan hash rechecked; actual final logs/exits and clean XML read directly; exact repository source=XML identities, repaired-source/copy equality and current-doc equality checked read-only | Writes only this report; no rerun/fabrication of runtime/XML and no Git/remote mutation |

Artifact/HTTP proof includes ORIGINAL/DERIVED shared-media duration exclusion, no fabricated duration/mime/hash facts, distinct summary/detail keys, empty arrays, immutable capture scalar and malformed legal persisted facts failing closed, uniform foreign/missing404 and no foreign-risk lookup. Cursor proof covers canonical93-byte/124-character envelope, domain isolation, every payload/MAC byte mutation, account/personal/org/endpoint replay, exact32-byte independent key and unbound limit. Transaction proof covers same backend PID/current_user/readOnly/isolation at owner calls, REQUIRES_NEW ambient suspension/restoration, deterministic later risk/status/artifact changes visible together only on the next request, fixed query counts for1 vs100 items, denied writes, business/event/audit/outbox byte-equivalence, and cleanup after empty/not-found/denial/database/completion paths. These are specific passing test assertions where XML is present, not live production evidence.

### Toolchain, external validation and transport

- Pinned local Linux Temurin **21.0.13+9**, implementor **Eclipse Adoptium**, `/opt/jdk21`; Maven **3.9.9**, Node **22.23.4**, Docker daemon **29.1.3**, PostgreSQL **16.15**. Existing local toolchains were searched first; no JDK download or repository version change. `toolchain.log` runs the exact pin/release/version greps, exit0.
- To obey the explicit prohibition on writes to repository target/generated/peer paths, runtime commands run in external `/tmp/pa29-worker-d-validation`. `validation-location.txt` records the copy; source/SQL/POM inputs were raw-byte identical at copy time; the two Worker E repairs were synchronized before Controller-final execution and their repository/executed-copy byte equality is rechecked by this finalizer. Only external `mvnw` and five documentation inputs used by contract parsers are CRLF→LF normalized to emulate Linux CI checkout. Repository raw frozen files are never normalized. This is an external execution limitation, not an assertion that commands ran in-place or raw CRLF passed.
- Raw Windows-checkout wrapper cannot execute under Linux (`/bin/sh^M`,126); raw A2 returns1 because line-anchored A0 headings/CHECK dictionaries retain CRLF. Exact failure is preserved in `raw-crlf-contract-attempt/`. The same commands pass using canonical-LF external docs, with every raw/canonical hash in `external-ci-doc-eol.json`. No parser or A0 source is silently fixed.
- First Maven attempt was deliberately interrupted143 while correcting process-local external Maven-cache selection; `interrupted-attempt/` preserves its command/log/stderr, and no pass is claimed for it. Final process-local `MAVEN_OPTS=-Dmaven.repo.local=/mnt/d/tools/m2/repository`, IPv4 preference and existing Worker C loopback Docker bridge recipe are in `validate.sh` and `temporary-docker-bridge.py`. Windows-only Unix-domain fallback is unnecessary for the pinned Linux JDK. Temporary listener binds127.0.0.1:23759 and forwards only to the existing local Docker socket; this host transport bridge is process-local and **stopped**, not a production adapter. Controller launchers trap bridge cleanup; Worker D cleanup logs and this finalizer's empty `ss -ltnp '( sport = :23759 )'` listener result corroborate shutdown. No repository runtime behavior, persistent environment or host-service policy is changed.
- Mockito dynamic-agent/CDS and existing compiler diagnostics remain in stderr. `npm ci` reports17 inherited toolchain vulnerabilities (1low/9moderate/5high/2critical); no audit fix/version upgrade is authorized or performed.

### Exact commands and exits

Named logs/exits and `.command`/`.stderr` where present retain actual executions; `validate.sh`, `contracts.sh`, `ci-steps.json`, Controller launchers and unmodified extracted here-documents retain full command arguments. Working directory is external `backend` for Java/JAR gates, external `api/openapi` for npm, external root for A2–A5. Unless prefixed `worker-e/`, earlier labels refer to `worker-d`. Four Controller `.exit` artifacts (`exact-ci-inventory`, `jar-web-smokes`, `jar-firewall`, `contracts-final`) literally contain `0n`, not a newline-terminated integer; this formatting defect is preserved, not silently rewritten. Controller-authoritative exit0 is corroborated by their success logs and, for inventory, direct clean XML inspection; it is not inferred by parsing `0n` as an integer.

| Command label | Actual exit | Exact command / gate |
| --- | --- | --- |
| `toolchain` | **0** | `bash /mnt/d/codex-temp/pa29-implementation-evidence-20261112-02/worker-d/ci-assert-toolchains-and-jdk-pin.sh` |
| `docker-info` | **0** | `docker info` |
| `focused-new-unit` | **0** | `./mvnw -B -ntp -Dtest=CustomerEvidenceWebTests\,CustomerEvidenceQueryServiceTests\,CustomerEvidenceCursorCodecTests\,CustomerEvidenceConfigurationTests\,CustomerEvidenceBoundaryTests test` |
| `focused-new-container` | **0** | `./mvnw -B -ntp -Pcontainer-smoke -Dit.test=CustomerEvidenceIT\,CustomerEvidenceReadPrivilegeIT verify` |
| `focused-existing-unit` | **0** | `./mvnw -B -ntp -Dtest=ModuleBoundaryTests\,CustomerBusinessNumberBoundaryTests\,CustomerSubjectDetailBoundaryTests\,SharingQueryBoundaryTests\,CustomerWebConfigurationTests\,CustomerBusinessNumberQueryServiceTests\,CustomerApiBoundaryTests test` |
| `baseline-container` | **1 (historical pre-repair)** | Original Worker D baseline attempt; superseded by the exact all19 post-repair command below, not erased or counted as a final pass |
| `focused-repair-final` | **0** | `./mvnw -B -ntp -Pcontainer-smoke -Dit.test=CustomerSubjectDetailReadPrivilegeIT,CustomerBusinessNumberReadPrivilegeIT verify`; 18/18, zero failures/errors/skips; `worker-e/focused-repair-final.log` |
| `baseline-repair` | **0** | `./mvnw -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT,SharingQueryServiceIT,CustomerMeIT,CustomerSharesIT,SharingRoleLeastPrivilegeIT,PublicSharesIT,PublicShareAdmissionIT,SharingAdmissionRoleLeastPrivilegeIT,EvidenceMediaReadIT,EvidenceMediaReadRoleLeastPrivilegeIT,MediaPlaybackIT,MediaGatewayTransportIT,CustomerShareCommandsIT,ProductionDatabaseIdentityProvisioningIT,DatabaseRuntimeAclRbacHardeningIT,CustomerBusinessNumbersIT,CustomerBusinessNumberReadPrivilegeIT,CustomerSubjectsIT,CustomerSubjectDetailReadPrivilegeIT verify`; all19 Failsafe302/302; `worker-e/baseline-repair.log/.exit` |
| `full-clean-repair` | **0** | `./mvnw -B -ntp -Pcontainer-smoke clean verify`; Surefire417/417 + Failsafe336/336 =753/753, zero skips/failures/errors; `worker-e/full-clean-repair.log/.stderr/.exit` |
| `exact-ci-inventory` | **0 (Controller; artifact caveat above)** | Exact current CI `assert container smoke actually ran` source/CI/POM/XML here-document; 49/417 +21/336 =70/753, scan754, exact historical POM21 includes, zero duplicates/skips/failures/errors; `worker-e/exact-ci-inventory.log/.exit` |
| `jar-web-smokes` | **0 (Controller; artifact caveat above)** | Extracted `worker-d/ci-default-and-opt-in-web-jar-smokes.sh` CI gate, default/nonweb and opt-in web smokes; `worker-e/jar-web-smokes.log/.exit` |
| `jar-firewall` | **0 (Controller; artifact caveat above)** | Extracted `worker-d/ci-production-jar-dependency-firewall.sh` CI gate; baseline37/34, SSM72/65, counterparts64, dependency-only8, packager1, 116 adversarial probes; `worker-e/jar-firewall.log/.exit` |
| `contracts-final` | **0 (Controller; artifact caveat above)** | `bash /mnt/d/codex-temp/pa29-implementation-evidence-20261112-02/worker-d/contracts.sh`; Node pin, npm ci, A1–A5 (including both A5 s1/s2) each exit0; `worker-e/contracts-final.log/.exit`; individual exact commands below |
| `node-pin` | **0** | `bash /mnt/d/codex-temp/pa29-implementation-evidence-20261112-02/worker-d/ci-assert-exact-node-version.sh` |
| `npm-ci` | **0** | `npm ci` |
| `a1-s3` | **0** | `npm run check:s3` |
| `a2-s3` | **0** | `bash /mnt/d/codex-temp/pa29-implementation-evidence-20261112-02/worker-d/ci-a2-s3.sh` |
| `a3-s3` | **0** | `bash /mnt/d/codex-temp/pa29-implementation-evidence-20261112-02/worker-d/ci-a3-s3.sh` |
| `a4-s2` | **0** | `bash /mnt/d/codex-temp/pa29-implementation-evidence-20261112-02/worker-d/ci-a4-s2.sh` |
| `a5-s1` | **0** | `bash /mnt/d/codex-temp/pa29-implementation-evidence-20261112-02/worker-d/ci-a5-s1.sh` |
| `a5-s2` | **0** | `bash /mnt/d/codex-temp/pa29-implementation-evidence-20261112-02/worker-d/ci-a5-s2.sh` |
| `ci-static` | **0** | `python3 ci-static.py` |
| `independent-audit` | **0** | `python3 independent-audit.py` |
| `frozen-exact-verify` | **0** | `frozen-exact-verify gate (see named log)` |
| `diff-check` | **0** | `diff-check gate (see named log)` |

### XML / inventory reconciliation

| Execution | Surefire classes/methods | Failsafe classes/methods | Skipped/failures/errors |
| --- | --- | --- | --- |
| `focused-new-unit` | 5/42 | 0/0 | 0/0/0 |
| Controller `focused-repair-final` | Not used as the final clean inventory | 2/18 (10+8) | 0/0/0 |
| Controller `baseline-repair` | Not used as the final clean inventory | 19/302 | 0/0/0 |
| Controller `full-clean-repair` | **49/417** | **21/336** | **0/0/0** |

Worker D's `xml-inventories.json` retains the earlier collected report SHA256/class/method identities; it is not relabeled as a Controller-final manifest. Controller-final clean reports are the actual `TEST-*.xml` files in `/tmp/pa29-worker-d-validation/backend/target/surefire-reports` and `failsafe-reports`, corroborated by `worker-e/full-clean-repair.log` and `exact-ci-inventory.log`. This finalizer directly parsed all70 XML files read-only and matched their actual class/method sets to repository sources: **49/417 +21/336 =70/753**, zero duplicates/skips/errors/failures. Focused18 and baseline302 log results are not added to753 or substituted for clean XML. Exact **source=CI=XML** filename/class/case identity and **21 exact historical Failsafe includes** passed the complete current CI `assert container smoke actually ran` here-document directly via stdin, not a fabricated XML/helper result. `ci-static.log` separately proves source[417,336], additions[12,12,8,6,4]/[24,10], maps49/21, POM21, annotation754 and the single byte-identical historical comment. `independent-audit.log` proves all677 predecessor class/method identities and unchanged dependency/plugin versions/POM except the two includes. Historical checkpoints remain112/178=290;128/192=320;162/198=360;196/204=400;231/214=445;266/214=480;309/214=523;309/234=543;309/246=555;341/272=613;375/302=677;current417/336=753. Historical scans291/321/361/401/446/481/524/544/556/614/678 precede754.

Controller-final JAR smokes pass default/nonweb database-free startup, opt-in web route denial/default behavior and safe error surface; the dependency firewall passes the exact72-coordinate/65-JAR approved set, historical37/34 baseline, counterparts64, dependency-only8/packager1 and all116 adversarial probes (deltas35/0 and31/0). These are actual named gate successes, not merely a packaged JAR's presence. A1 OpenAPI/client/doc-map/media-gateway, A2 protocol, A3 security, A4 deployment calculation and A5 acceptance s1/s2 static validators all pass in `contracts-final.log`. A5 explicitly reports runtime_evidence=NOT_RUN and production_accepted=false; static success is not live acceptance and supplies no external production runtime evidence.

## 5. Frozen, scope, encoding and equality evidence

- Sealed plan hash matches exactly; all18 raw hashes (two OpenAPI specs +V001–V016) equal §11.3. All26 frozen paths (ten OpenAPI/tooling files +sixteen migrations) have canonical SHA256 equality against **both** accepted Git baselines and unchanged raw path/hash/status inventory. `frozen-resume-before.json`, `frozen-exact-verify.log`, `audit.log` preserve proof.
- This is a resumed partial worktree with untracked V017 already present, not a fresh implementation. The exact plan capture command therefore returns1 at its pre-V017 inventory assertion; `frozen-exact-capture.log` preserves it. The external resume capture changes only the two successor-presence conditions to admit already-present V017 while sealing exactly the same26 predecessor raw/canonical/status entries; it does **not** delete/hide V017 or recapture after repository edits. The identical unmodified plan verify here-document subsequently returns0 against that snapshot. No original pre-S1 capture is invented.
- `nonowned-before.json` captures2,644 non-owned tracked/nonignored/generated-target paths at Worker D entry; `audit-final.log` records raw equality and `SCOPE implementation candidates 54 /54; missing []`. `scope.json` and `allowlist54.txt` account for the exact54 paths, not merely `git diff`. Worker E's two authorized test repairs remain within that set; Controller report finalization is the only current write. Controller Control/accepted-plan/fix/review/private inputs are excluded, not modified by this finalizer. A read-only comparison against Worker E's non-owned snapshot finds only `docs/gateflow/project-execution-control.md` differs, a Controller-owned excluded input; the earlier all-path audit is not misrepresented as a fresh unchanged-Control assertion. All other recorded non-owned hashes match. Finalizer raw/canonical checks confirm no frozen file changed after Worker D's successful frozen26/raw18/canonical26 audit.
- Current boundary sections agree text-for-text and match §2. All PA28 and older historical bodies are preserved. The original finalizer's normalized section SHA256 `6fca6916ac325c6242ae74d1841a07c488600136f8a38852e7ef1dca4a26031c` is historical; fix-1 resealed current section SHA256 is `9b502cd4c1f3bfb4aead542d86576eb38bc571d5daa439dfd9ef2fd23a742d2e`. Worker D `final-diff-check.exit` is0 (`git.exe diff --check`); only LF→CRLF advisories remain. Current fix-1 report/scope/encoding checks are in the separate fix artifact; original CRLF frozen bytes remain untouched.
- Report SHA256 is computed **after** this edit and provided in the final handoff, never embedded as a self-referential digest. Existing `worker-d/final-owned.sha256` and `worker-e/report.sha256` are historical pre-finalization manifests, not the digest of this newly sealed report; no external manifest is rewritten by this report-only task. XML/log evidence remains unmodified.

## 6. Findings, residual owners and NOT EXECUTED limits

**Closed implementation findings:** Worker E corrected behavior rather than weakening tests: historical subject-detail `current()` targets016; current PA25/provisioned-login/role/pool cases target017 with exact288 ACL/history17. All10 public identities remain. The business-number V015 wrong-rank probe alone moved from occupied17 to free18; all8 identities remain. Focused18, all19 baseline302, clean753 and exact inventory prove closure; no required S1 implementation runtime gate remains outstanding. The following are accepted policy/security/capacity residuals, tooling/evidence limitations or explicitly out-of-scope production work, not hidden implementation blockers.

| Residual | Owner | Destination / disposition |
| --- | --- | --- |
| Whole-list risk policy may deny all pages on one off-page subject | Backend/Product/Security | Confirmed policy; any change returns to Controller goal/plan authority, not this slice |
| Full JSONB capture is raw-role readable; scalar-only projection is trusted application policy | Security/Database | Security/data-boundary follow-up; no JSON-path ACL/RLS claim |
| Tenant-sized subject-set materialization/bind capacity, no large-tenant SLO evidence | Backend/Database | Capacity evidence in separately authorized work; no speculative index/cache/read model |
| Duration only unique matching validated ORIGINAL; DERIVED omitted | Evidence owner | Future association contract needs separate authority |
| Legal malformed capture/oversized MIME fails closed | Evidence/data-quality owner | Separately approved data repair; this read performs no repair |
| REPEATABLE_READ is not linearizable authorization; later changes apply next request | Security/Product | Stronger revocation requirement requires new decision |
| Key replacement invalidates prior cursors, no automatic key overlap | Backend/Operations/Security | Deployment key custody/rotation procedure; no live key assignment executed |
| Floating postgres:16 tag; observed patch/digest recorded | Database/CI | Future image-pinning decision; no version changes here |
| Windows CRLF/Unix launcher/parser difference; runtime tests use external copy | CI/Controller | Reproduce canonical Linux CI checkout; original raw-checkout failures retained, no out-of-scope edit |
| npm locked static-toolchain vulnerabilities and Mockito future-agent warning | API-tooling/Security and Backend/CI | Separate dependency/security maintenance; versions frozen in this slice |
| Original pre-S1 raw snapshot unavailable to Worker D; resume snapshot only | Controller/integration owner | Preserve original evidence if available; no fabricated historical capture |
| Four Controller exit artifacts contain literal `0n` | Controller/evidence custodian | Evidence-format limitation; retain original bytes and Controller exit0/success-log corroboration; normalize future capture tooling in separately authorized work, not this report-only task |
| Production credentials/TLS/pg_hba/migrator/comment custody | Platform/Database/Security | Production provisioning runbook and protected change process; **NOT EXECUTED** |
| Production deployment/rotation/pool-drain/session invalidation/Q17/Q23 | Platform/Database/Security/Operations | Production acceptance work; **NOT EXECUTED**, no production/live readiness claim |
| Live issuer/TTL/WeChat identity mapping, media/playback/share lifecycle, writes and operations APIs | Respective product/identity/media/sharing owners | Explicit non-goals, not missing PA29 deliverables; **NOT EXECUTED** |

All required local S1 implementation gates are closed by the Controller-final evidence. The historical nonzero attempts remain disclosed, not carried forward as final blockers. The NOT EXECUTED items above are external production/non-goal work: no deployment, production credentials, TLS, pg_hba or live-readiness validation occurred, and no runtime evidence exists here for external production. Controller owns any separately authorized next gate; this report does not enter/accept code review, authorize that transition or claim S1 acceptance/production readiness.

## 7. Prohibited actions

No Git or remote mutation occurred in this finalization: no reset, clean, revert, staging, commit, branch creation, fetch, push, PR, merge, approval, mark-ready action, review request, external comment, issue mutation or code-review entry. Maven `clean` was solely the explicitly required external build-directory cleanup, not Git clean. No peer-owned path is repaired or silently changed.

## 8. Controller-final implementation handoff

- **COMPLETE — implementation-only**, on the accepted sealed plan and exact54/54 allowlist. Worker D integration/audit, Worker E's two bounded repairs, and Controller-final execution evidence are distinguished above; this finalizer modifies only this report.
- Pinned WSL Temurin21.0.13, Maven3.9.9, Docker29.1.3 and PostgreSQL16.15; external validation copy `/tmp/pa29-worker-d-validation`; process-local host transport bridge stopped. Repository target/generated files were not used for runtime output.
- Focused18/18 exit0 (finished 2026-11-12T12:33:57+08:00), baseline302/302 exit0 (12:55:52+08:00), clean753/753 exit0 (13:20:56+08:00); all zero skips/failures/errors. Exact source=CI=XML70/753, scan754/POM21, JAR smokes, firewall116 probes and A1–A5 validators pass in `worker-e`; original failures/logs/XML remain preserved, with exit-artifact formatting caveat disclosed in §4.
- Current fix-1 V017 raw SHA256 `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad`, Flyway checksum `-41822162`, exact17 successful history rows; ACL269+19=288=customer53+evidence16+others219, customer53=6 schema+47 column, no grant options. Original final clean provisioning20/20 is historical; the separate fix-1 artifact records validation of this reseal.
- Frozen26/raw18/canonical26 and synchronized current-doc evidence remain valid; report encoding/whitespace checks and exact final SHA256 are supplied after editing in the final handoff. No production/live readiness or code-review acceptance is asserted, and no Git/remote mutation is performed.

## 9. Code-review fix 1 handoff (2026-11-12)

- Task `pa29-evidence-s1-code-review-fix1-gpt-20261112-10`, provider Codex `gpt`, gate **fix**: **fix-complete — local implementation and validation only**, not independent re-review or acceptance. Current managed HEAD is `d00c22c4f4b654fe392bd7c4a82e92e87ef0a2f7`; accepted plan/Control/reviews remain unchanged.
- Only accepted A-LOW-1/A-LOW-2 are repaired: V017 correlates exact old/final ACL with history16/17 on its first pass before grants, preserves the second in-migration final-ACL/history16 state, and rejects both hybrids with `P0001`/`V017_HISTORY_REJECTED`. Both new adversarial/no-repair witnesses stay inside the existing privilege rejection identity. V016's named269-oracle method regains its direct exact-set equality; all original assertions and both ten-method identity sets remain.
- Current exact V017 raw SHA256 `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad`, actual Flyway checksum `-41822162`; native Flyway12.4.0 calculation and migrated metadata agree. PA25/tests/three current-boundary docs/report are synchronized; V001-V016/OpenAPI bytes, V016 checksum1669606497, nineteen owner-issued column grants and269+19=288/customer53/evidence16/others219/history17 remain exact.
- Fresh isolated validation in `/tmp/pa29-code-review-fix1/validation`: clean boundary4/4; focused privilege10+subject-detail10+provisioning20=40/40; full `mvn -B -ntp -Pcontainer-smoke clean verify` passes49/417+21/336=70/753, zero skips/failures/errors, exit0 (2026-11-12T14:54:59+08:00). Exact source=CI=XML/scan754/POM21, all19 predecessor suites302 plus PA29 two suites34, JAR default/web smokes and116 firewall probes pass. Toolchain is Temurin21.0.13/Maven3.9.9/Docker29.1.3/PostgreSQL16.15; direct local Unix Docker socket, no bridge or repository target/build writes.
- Exact original54 candidates plus only the new fix artifact, zero missing/unexpected; final scope/frozen/encoding/`git.exe diff --check` checks pass. The first SQL CASE parsing failure and preliminary external-audit failure are preserved, not relabeled as passes. Old worker/Controller logs above remain historical; A1–A5 static validators and remote CI are not claimed as newly executed by this fix.
- Full authority, nine-existing-plus-one-new fix paths, old/new seals, commands/logs/exits/XML/JAR/frozen assessment and residual owners: `docs/gateflow/pa29-customer-evidence-read-foundation-s1-code-review-fix-1.md`. No local fix blocker remains. No stage/commit/push/PR/merge/approval/ready/reviewer-request/external comment or mutation occurs; stop before independent re-review. Production/live readiness remains NOT EXECUTED/NOT CLAIMED.
