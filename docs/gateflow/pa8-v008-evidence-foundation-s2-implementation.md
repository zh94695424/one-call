# PA8 V008 Slice2 implementation completion record

## Status and authority

- Task: `pa8-s2-gpt-impl-20261030T182409Z`; isolated workspace only.
- Goal: implement only the approved evidence-foundation Slice2 behavioral witnesses, retaining the accepted Slice1 catalog, ACL and FK contracts and all existing 54 test methods.
- Accepted S1 commit: `de4890dbd7c9e6e7392510359bd4ef5edc54df3f`. Isolated starting HEAD: `4d339a1e8ce39ec30a9342bf79efc1aa4b64ea8a` (S1 raw-byte snapshot).
- **Implementation complete; container runtime validation pending, blocked by Docker socket permission. This is not a Slice2 acceptance or runtime PASS record.**
- Governing documents read: accepted plan in full (especially §7.3 and §9), plan-fix-3, spec-reconciliation, doc-14 §4.39–4.43/§10.21–10.24/§11.5 and related contract sections, and doc-12 architecture. No applicable repository/ancestor `AGENTS.md` or `CLAUDE.md` was found.
- No frozen migration defect or material contract contradiction was identified during implementation. Database execution is still required; this statement does not substitute for runtime validation.

## Exact deliverable scope

Only these four deliverable paths change:

| Path | Change |
| --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | Exactly three approved tests, localized S2 fixtures/probes/rollback helpers, and a list-based overload of existing local cleanup; existing single-record cleanup callers retain their signature. |
| `.github/workflows/ci.yml` | Exact expected-name set grows from 54 to 57, S2 count/error labels; existing migration-method rename remains intact. |
| `backend/README.md` | PA8 V008 S1–S2 operational scope, 57-method inventory, three previously omitted S1 names and three S2 names, behavioral evidence limitations; Surefire expectation stays 10. |
| `docs/gateflow/pa8-v008-evidence-foundation-s2-implementation.md` | This durable implementation/evidence record. |

No services, V009, sharing, cloud integration, APIs, dependencies, test framework, production DDL, authoritative design, accepted plan, prior artifacts or controller changes. No stage, commit, reset, checkout, push, delegation, independent review, approval request or later gate was performed. Pre-existing `.codex` was not touched. `.pa8-s2-validation/`, backend build outputs and generated root `build/` are local untracked validation/build material, not additional deliverables.

## Test and helper entry points

All Java references are in `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`:

| Entry point | Start line | Responsibility |
| --- | ---: | --- |
| `v008MediaRecordingShapeDefaultsAppendOnlyAndRollbackBackstops` | 2634 | Media/recording checks, defaults, owner guards, transitions, recording-only rollback. |
| `v008CreationCycleBothOrdersAndTerminalLifecycleBackstops` | 2762 | Runtime-role committed creation pairs/ORIGINAL, deferred cycle failure classes, remaining checks, evidence lifecycle/append-only, full rollback. |
| `concurrentV008EvidenceCreationAndRecordingIdentityHasOneWinner` | 2917 | Five bounded two-connection winner/loser cases. |
| Existing `expectCommitFailure` | 4175 | Reused deferred COMMIT failure assertion, unchanged. |
| `v008S2Fixture` | 25177 | Committed valid local subject/call/session/reservation/media/recording/base pair and ORIGINAL; reuses V008 constructors. |
| `v008S2CloneSql` / `v008S2Check` | 25227 / 25241 | INSERT-shaped probes with positive-row witness and exact named CHECK failure. |
| `v008S2IsolatedStatusCheck` / `v008S2ConstraintCatalog` | 25248 / 25262 | Savepoint-only preemptor isolation and full catalog restoration. |
| `v008S2Record` / `v008S2Event` / `v008S2Pair` / `v008S2Artifact` | 25268 / 25281 / 25289 / 25300 | Local fixture writes, including both creation orders. |
| `v008S2CycleFailures` | 25307 | Deferred FK and cycle-guard distinctions. |
| `v008S2RecordingRollback` / `v008S2FullRollback` / `v008S2SentinelFailure` | 25355 / 25394 / 25438 | Successful writes/counts before distinct `22012` sentinel and full rollback. |
| `v008S2Cleanup` / `cleanupV008LocalRows` overloads | 25447 / 25643 / 25657 | Nonempty local ID lists, FK-ordered cleanup, trigger restoration and fresh-connection zero checks. |

Existing concurrency helpers `configureBlockedWriter`, `awaitBlockedBackend` and `awaitFuture` are reused. Existing exact catalog/ACL/FK assertions remain in place.

## Complete 39-CHECK witness map

These are **implemented assertions, not observed container-test results**. Every row has a positive control from a committed valid fixture row (checked before the negative), supplemented by successful state/lifecycle writes. The source-reference exception additionally has exact catalog and real-runtime-role non-NULL committed witnesses. The grouping is 9 media + 11 recording + 13 evidence + 1 artifact + 5 event = 39. There are 38 distinct named `23514` negative targets; repeated shape probes do not inflate that count.

| # | Frozen CHECK name | Negative/classification |
| ---: | --- | --- |
| 1 | `ck_media_object_provider` | `OTHER`; exact `23514`. |
| 2 | `ck_media_object_bucket` | Whitespace-only bucket; exact `23514`. |
| 3 | `ck_media_object_key` | Empty key; exact `23514`. |
| 4 | `ck_media_object_version` | Empty object version; exact `23514`. |
| 5 | `ck_media_object_sha256` | Malformed non-NULL hash; exact `23514`. |
| 6 | `ck_media_object_size` | Zero size; exact `23514`. |
| 7 | `ck_media_object_mime` | Empty MIME; exact `23514`. |
| 8 | `ck_media_object_key_ref` | Empty key reference; exact `23514`. |
| 9 | `ck_media_object_worm_status` | `OTHER`; exact `23514`. |
| 10 | `ck_recording_protocol_id` | Empty recording protocol ID; exact `23514`. |
| 11 | `ck_recording_declared_format` | Empty format; exact `23514`. |
| 12 | `ck_recording_declared_size` | Zero declared size; exact `23514`. |
| 13 | `ck_recording_declared_duration` | Zero declared duration; exact `23514`. |
| 14 | `ck_recording_declared_sha256` | Malformed non-NULL hash; exact `23514`. |
| 15 | `ck_recording_validated_duration` | Zero validated duration with otherwise shape-valid QUARANTINED/validated-at values; exact `23514`. |
| 16 | `ck_recording_validated_sha256` | Malformed non-NULL hash with otherwise shape-valid QUARANTINED/validated-at values; exact `23514`. |
| 17 | `ck_recording_status` | `OTHER`, isolating only `ck_recording_state_shape`; exact `23514`. |
| 18 | `ck_recording_state_shape` | Illegal DECLARED/UPLOADED/VALIDATED/QUARANTINED shapes; exact `23514` each. |
| 19 | `ck_recording_upload_expiry` | Expiry equals created-at; exact `23514`. |
| 20 | `ck_recording_revision` | Zero revision; exact `23514`. |
| 21 | `ck_evidence_source_type` | `OTHER`; exact `23514`. |
| 22 | `ck_evidence_source_ref` | **Sole NOTNULL exception:** NULL `source_ref_id` yields exact `23502`/column name, not `23514`; exact CHECK catalog retained, non-NULL committed pair witness. |
| 23 | `ck_evidence_source_domain` | `OTHER`; exact `23514`. |
| 24 | `ck_evidence_source_event_id` | Empty ID, snapshot event ID aligned to avoid shape preemption; exact `23514`. |
| 25 | `ck_evidence_capture_shape` | Array capture; separately missing capturedAt/sourceChannel/subjectId; exact `23514`. |
| 26 | `ck_evidence_capture_subject` | Nonmatching non-NULL subjectId while preserving keys; exact `23514`. |
| 27 | `ck_evidence_consent_version` | Empty consent version; exact `23514`. |
| 28 | `ck_evidence_status` | `OTHER`, isolating only `ck_evidence_state_shape`; exact `23514`. |
| 29 | `ck_evidence_state_shape` | PENDING with solidified time, SOLIDIFIED without it, QUARANTINED/FAILED with it; exact `23514` each. |
| 30 | `ck_evidence_revision` | Zero revision; exact `23514`. |
| 31 | `ck_evidence_snapshot_shape` | Array snapshot; missing capture/content; non-NULL source call/event mismatches; exact `23514`. |
| 32 | `ck_evidence_snapshot_sha256` | Malformed non-NULL hash; exact `23514`. |
| 33 | `ck_evidence_link_shape` | NULL call with non-NULL session, snapshot call aligned; exact `23514`. |
| 34 | `ck_evidence_artifact_role` | `OTHER`; exact `23514`. |
| 35 | `ck_evidence_event_domain` | `OTHER`; exact `23514`. |
| 36 | `ck_evidence_event_id` | Empty event ID; exact `23514`. |
| 37 | `ck_evidence_event_type` | `OTHER`; exact `23514`. |
| 38 | `ck_evidence_event_payload` | Array payload; exact `23514`. |
| 39 | `ck_evidence_event_payload_sha256` | Malformed non-NULL hash; exact `23514`. |

CHECK probes use INSERT SELECT rather than immutable UPDATE. Status probes drop only their preempting shape CHECK inside a savepoint, roll back the savepoint and compare the complete constraint catalog plus the existing exact CHECK predicate inventory. Frozen PostgreSQL CHECK NULL/UNKNOWN behavior is not redesigned: deterministic violating non-NULL values are used where needed; no unsupported NULL-rejection claim is made. The source-reference NOT NULL constraint is never removed.

## Behavioral contracts and failure classes

- Role split: owner/migration role for `P0001` guard proofs; runtime `42501` ACL proof remains distinct in S1. Both record-first and event-first valid pairs plus ORIGINAL artifacts actually COMMIT as `evidence_role`, exercising INVOKER permissions rather than relying on catalog grants. Recording inserts have valid calls; fixtures provide committed valid subject/session/reservation prerequisites.
- Missing creation event: exact deferred `23503`, `fk_evidence_source_event`. Missing evidence record: exact deferred `23503`, `fk_evidence_event_record`, using a noncreation event to avoid guard preemption.
- Noncreation decoy: exact COMMIT `P0001` with `EVIDENCE_CREATION_CYCLE_VIOLATION`. Wrong-owner two-record/two-event events-first crosslink and three-row events-first R/D/C source mismatch: class-level COMMIT `P0001` only; no SQLSTATE unions and no over-specific named-trigger assertion. Fresh connections verify failed-transaction local record/event absence.
- Creation duplicate uses distinct PK and event ID to isolate immediate `23505`/`uq_evidence_created_event`. ORIGINAL duplicate uses a different media ID to isolate frozen `uq_evidence_original_artifact`; ordinary artifact uniqueness is also exercised.
- Recording defaults, immutable head fields, legal DECLARED→UPLOADED→VALIDATED and DECLARED/UPLOADED→QUARANTINED transitions, revision+1, media/validation locks and terminal absorption are asserted. Evidence PENDING→SOLIDIFIED/QUARANTINED/FAILED, revision+1, terminal absorption and lifecycle events are asserted. Media, event and artifact owner UPDATE/DELETE are append-only guard proofs. Frozen same-state revision behavior is retained (recording revision 7, evidence revision 9), not tightened.
- Distinct recording-only rollback: valid parent/media/recording writes and media binding succeed, scoped counts are observed positive, then deliberate `SELECT 1/0` (`22012`) fails and the entire transaction rolls back. A fresh connection observes zero local rows.
- Distinct full T-S3-12 rollback: record + creation event + ORIGINAL + raw simulated Outbox write (under `platform_reliability_owner`) succeed; deferred constraints are drained and all scoped counts are positive before the same distinct sentinel. Full rollback and fresh-connection zero assertions cover every fixture ID. This is a raw database atomicity witness, not application Outbox integration.
- Bounded concurrency cases: `uq_evidence_source_event`, `uq_recording_protocol_id`, `uq_media_object_version`, `uq_evidence_created_event`, and same-source record/event pair winner. Distinct application names, two owner connections, 20-second statement timeout, 15-second observed-lock bound, 30-second future bound and executor termination prevent unbounded waits. Each loser has an earlier local valid event-first pair; after exact named `23505`, the entire loser transaction rolls back, with fresh zero pair/orphan assertions and exactly one winner.
- Cleanup reuses `cleanupV008LocalRows` with nonempty artifact/recording/media/event/record/local-parent lists. Only method-local IDs are deleted, in FK order: artifact → event → record → recording → media → entitlement/IVR/subject parents. Deferred events are drained before trigger DDL. All three temporarily disabled immutable triggers are restored to `O`; fresh connections assert per-ID zero and trigger restoration. No shared rows are deleted and no constraint is disabled outside rollback-restored isolation.

## Commands and actual evidence

Local evidence directory: `.pa8-s2-validation/`. No Failsafe/Surefire XML result is claimed for this run.

1. Environment/preflight, once, with `JAVA_HOME=/opt/jdk21`, `/opt/jdk21/bin:/opt/maven-3.9.9/bin` prepended to PATH and `DOCKER_HOST=unix:///var/run/docker.sock`:
   - Commands: `java -version`; `/opt/maven-3.9.9/bin/mvn -version`; `docker info`.
   - `environment.log`: JDK 21.0.13 and Maven 3.9.9 available. Docker returned **`permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`**. The socket points to `/var/run/dify.sock`. No retry, workaround, escalation, installation or global configuration change was attempted.
2. Offline compile, run three times (initial, pre-artifact and final cycle-classification code state):
   - `/opt/maven-3.9.9/bin/mvn -o -B -ntp -f backend/pom.xml -Pcontainer-smoke test-compile`
   - `compile.log`: BUILD SUCCESS, 4.478 seconds. `compile-final.log`: BUILD SUCCESS, 4.147 seconds. `compile-final-cycle.log`: final BUILD SUCCESS, 3.938 seconds, after limiting crosslink/source-mismatch assertions to SQLSTATE only; 26 test source files compiled with release 21. Compile is not runtime evidence.
3. Inline Python static source/CI inventory and frozen-byte comparison, plus `git -c core.whitespace=cr-at-eol diff --check`:
   - `static-inventory.log`: source 57 = CI exact expected set 57, unique 57, duplicate names 0; original 54 retained; exactly the three approved names added; existing migration-method rename retained. Eight migrations and frozen plan/fix-3/reconciliation/spec/architecture/controller match starting HEAD raw bytes. Tracked modifications confined to allowed paths; CR-aware diff check PASS.
   - `entrypoints.log`: source line inventory. `frozen-sha256.log`: frozen file hashes below.
   - `final-static.log`: final deliverable scope, exact 39-name artifact map, 57-name equality, frozen-byte and CR-aware diff checks after this artifact was added.
4. Focused three new S2 tests: **NOT RUN**, because Docker preflight was denied. No repeated blocked commands. Full 57-test suite: **NOT RUN**, explicitly reserved to the controller after intake. Surefire 10: unchanged expected count, **NOT RUN**, not an observed PASS.

The following is the intended focused command for a Docker-capable controller, **not executed here**:

```bash
JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH DOCKER_HOST=unix:///var/run/docker.sock \
/opt/maven-3.9.9/bin/mvn -o -B -ntp -f backend/pom.xml -Pcontainer-smoke \
  '-Dit.test=PostgresContainerIT#v008MediaRecordingShapeDefaultsAppendOnlyAndRollbackBackstops+v008CreationCycleBothOrdersAndTerminalLifecycleBackstops+concurrentV008EvidenceCreationAndRecordingIdentityHasOneWinner' \
  test-compile failsafe:integration-test failsafe:verify
```

## Documentation decision, constraints and residual ownership

Authoritative specs, architecture, approved plan and reconciliation remain read-only. README changes describe operational counts and the bounded database-only evidence scope; they do not amend the design. No broad refactoring or framework/dependency addition was made. Existing S1 exact catalog, ACL and FK assertions are retained; S2 does not weaken expected SQLSTATEs to accommodate unobserved runtime behavior.

| Residual item | Owner / destination |
| --- | --- |
| Runtime SQL/role/trigger ordering, cleanup and concurrency assertions have compiled but are unexecuted in this workspace. | Controller: Docker-capable focused3 must PASS before full57 Failsafe + Surefire10; preserve real logs/XML and investigate any failure against frozen contracts. |
| Independent code/event/stderr/artifact assessment and A/B reviews are not performed by the author. | Controller-owned intake and later independent reviews, not started here. |
| Runtime discovery of frozen migration defect or material contradiction. | Stop and report to controller/design owner; no silent migration amendment, error weakening or scope extension. |
| Q11 WORM and Q22 TTL/legal retention remain unresolved production-policy matters, not claimed as solved by database tests. | Production/security/compliance design owners through their existing design decision destinations, outside PA8 S2. |

## Frozen SHA-256 inventory

Hashes were captured from unchanged workspace bytes and compared with starting HEAD; short labels below identify the frozen paths recorded in `frozen-sha256.log`.

| Frozen file | SHA-256 |
| --- | --- |
| V001 migration | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| V002 migration | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| V003 migration | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| V004 migration | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| V005 migration | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| V006 migration | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| V007 migration | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |
| V008 migration | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| `docs/gateflow/pa8-v008-evidence-foundation-plan.md` | `0461ac69f24d7994e7a357ce9a92d749ab0d1bf47414992b37f7edf23b9a7c04` |
| `docs/gateflow/pa8-v008-evidence-foundation-plan-fix-3.md` | `59cd0bea4fc9bcfc8c844ac046d62d6adefdcfcf281508cadafad3cb26588824` |
| `docs/gateflow/pa8-v008-evidence-foundation-spec-reconciliation.md` | `6032bff1c645fd23f6a118632a158e135efeef406ea9373d144f2704e6b47aa0` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `8a4574272a9e411185e00fe088e530bf561069c703c1ce9eee7313fa0bfed685` |
| `docs/12-technology-stack-and-system-architecture.md` | `9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db` |
| `docs/gateflow/project-execution-control.md` | `64552f7f8f0e36d79ea538eed2c3b6148ace3f47a60453cfb7fc382b8c8f9f3e` |

Author work stops at this implementation record. No later gate is initiated.
