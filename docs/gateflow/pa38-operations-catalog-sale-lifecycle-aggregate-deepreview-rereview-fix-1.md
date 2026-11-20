# PA38 aggregate re-review fix1 — documentation truth and custody

## Gate, work unit and source state

- Gate: bounded PA38 aggregate re-review fix1; documentation-only correction of accepted `PA38-AGG-RR-01/02`, not independent review acceptance or a new implementation gate. The later bounded aggregate re-review2 fix1 gate extended the correction note below under `PA38-AGG-RR2-02` and relabeled historical headings in the implementation report under `PA38-AGG-RR2-03`; that gate is recorded in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview2-fix-1.md`, and every size/hash statement in the present artifact is this gate's own historical binding.
- Work unit: `pa38-operations-catalog-sale-lifecycle-aggregate-rereview-fix1-gpt-20261119-01`; November 19, 2026, Asia/Shanghai.
- Primary: `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call`; branch `codex/pa38-next-work-unit`; source HEAD `e3e49c4cf20c80078aa833a69efd3a9a2a832bfa`; HEAD tree `bdfe4736b08ff425747b4fb1e9a4c6cc56adb850`. The worktree contains prior uncommitted implementation/controller/Web/carrier and documentation changes; these are inputs, not changes authored by this task.
- Read-only Control checkpoint: `docs/gateflow/project-execution-control.md`, 2,715,686 raw bytes / SHA256 `0fe8865bb59e228f0183a564084711b4aaedcad0ded0ba87b0379cc00754bd81`. Current PA38 subsection and all seven Controller dispositions were read; Control is not edited.
- Final A/B aggregate re-review inputs: `docs/reviews/code-review-20261119-150142.md`, 17,486 bytes / SHA256 `b307ecf26d3f3a3cf4e80f515e41f52b801b95f4b932e4efcf23d2e3005e4213`; `docs/reviews/code-review-20261119-151607.md`, 32,383 bytes / SHA256 `f920be996bb13487bb9a4cfd2577d999042b8880b0a935925dd84f2818c2a933`. Both are advisory pass-with-risks, unchanged. The current implementation report and aggregate fix2 artifact were also read.

## Exact changed paths

Only these three paths changed relative to this gate's task-entry raw bytes (aggregate re-review2 fix1 later extended the third path and added a fourth supplemental artifact):

```text
docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md
docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-2.md
docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview-fix-1.md
```

The third path is new. All Java/test/migration/OpenAPI/POM/CI/review/evidence paths, fixture bytes, historical aggregate fix1 and Control are read-only and remain at task-entry bytes. No schema/index/migration or Java changes are made. Git uses process-local `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call25`, `GIT_WORK_TREE` equal to primary and `GIT_OPTIONAL_LOCKS=0`, without repairing the Windows `.git` pointer or changing configuration. Pre-existing changes are preserved, not reverted or represented as this fix's writes.

## Finding-by-finding disposition

| Finding | Controller disposition / severity | Result and rationale |
| --- | --- | --- |
| `PA38-AGG-RR-01` | accepted / Medium | Closed by documentation correction. Identical Outbox duplicates return the existing id without error/rollback; a same-key/different-payload collision raises `P0001`. Replay safety is attributed to the advisory lock, post-lock state short-circuit and revision/current-state CAS, not an Outbox `23505`. Hypothetical pairing/detection gap is explicitly owned below. |
| `PA38-AGG-RR-02` | accepted / Low | Closed by correcting the current LifecycleIT execution statement to PostgreSQL 16.16 and identifying floating `postgres:16` provenance. No pinned digest is claimed; clearly historical 16.15 runs remain historical. |
| `PA38-AGG-RR-03` | deferred-with-owner / Low | `(available_at,id)` does not guarantee paired delivery order. Both events commit atomically; no current state-machine defect is inferred. Owner Platform/Operations; destination later dispatcher/consumer contract. |
| `PA38-AGG-RR-04` | rejected-with-reason / Low | `candidate32*.txt` and `static-summary.json` explicitly preserve historical pre-final-report snapshots, not current drift. Accepted pre-edit final custody was 32 / 560,721 / `e7ab60382d33c307e363f3d7f9bee48e919559eeabcbe6763ede0eb74c63dbec`; this task independently reproduced it from entry bytes and recomputes the replacement below. Historical evidence is not overwritten. |
| `PA38-AGG-RR-05` | deferred-with-owner / Low | Stale foreign XML reports in per-gate bundles are filtered by `xml-audit.py`; exact 135-report custody remains in the accepted projection. Owner Controller/Backend evidence maintenance; future bundles must isolate their own reports and preserve a self-contained custody snapshot. Not a test rerun or evidence rewrite here. |
| `PA38-AGG-RR-06` | rejected-with-reason / Low | Transport `IOException` to sanitized `422 VALIDATION_FAILED` is the explicitly accepted `PA38-AGG-REV-02` contract, including the seven-byte partial-read probe. Changing it to distinguishable 503 would widen the frozen response matrix. Transport observability remains a classified residual, not a current defect. |
| `PA38-AGG-RR-07` | deferred-with-owner / Low | Unused authority-port `sqlState()` is interface maintenance, not a reachable production correctness defect. Owner Backend/Operations IAM; destination later interface cleanup. No widening of this docs-only fix. |

## Exact frozen Outbox semantics and correction note

Line evidence below refers to unchanged source files, not the line numbers of the edited reports.

1. `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql:344` inserts a new Outbox row; line 350 is exactly `ON CONFLICT (event_domain, dedupe_key) DO NOTHING`. Lines 351–354 return the newly inserted id on success.
2. The same migration at line 357 selects the existing `id, payload_sha256` for the same key, with `FOR UPDATE` at line 361. Lines 367–368 compare the supplied hash and return the existing id when equal. **Identical duplicate Outbox: idempotent existing-id return, no exception and no rollback.** This is not an audit uniqueness guarantee.
3. The migration at line 371 raises `OUTBOX_DEDUPE_CONFLICT` with `ERRCODE = 'P0001'` for a same-key/different-`payload_sha256` collision. The exception aborts PA38's mutation transaction, rolling back its audit and other writes. For completeness, lines 363–364 also raise `P0001` defensively if the post-conflict lookup finds no row; invalid-input branches earlier in the function have their own `P0001` validations. None makes identical dedupe an uncaught unique-index error.
4. The Outbox dedupe index cannot produce an uncaught SQLSTATE `23505` through this function's conflict-handled insert. `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java:27` classifies `23505`/`40001` for new-transaction recovery. PA38's `23505` branch is driven by the catalog active unique index, `uq_catalog_version_active` in the frozen V004 migration at line 112, not by Outbox dedupe.
5. `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java:110` obtains the product advisory transaction lock; lines 111–122 reload and short-circuit post-lock state/revision/current-active conflicts or current-state replay before facts. Lines 129–131 and 137 enforce revision/current-state CAS. **These product protections provide current PA38 replay safety.** Audit and paired Outbox appends share the mutation transaction; lines 133–135 append the audit then retirement/publication events, without implying dispatcher delivery order.
6. The repository at lines 184–187 does not inspect the returned Outbox id to distinguish insert from idempotent return. Under a hypothetical bypass of the product protections, an identical-payload Outbox skip could leave an unpaired audit fact with no Java-visible signal. Owner **Platform/Operations + Backend**; destination **later pairing/detection hardening**. This is a classified hypothetical boundary, not a claim of a currently reachable product failure. The frozen audit table at V001 lines 394–418 has no uniqueness constraint on `request_id`; line 416 only checks its length.

**Correction note for historical aggregate fix1 (extended by aggregate re-review2 fix1 under `PA38-AGG-RR2-02`):** two sentences in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-1.md` mis-describe the frozen Outbox function, and **both are superseded**:

- line 38 named the unique `platform.outbox_event(event_domain, dedupe_key)` constraint `uq_outbox_domain_dedupe` as part of "the durable replay backstop" alongside the product-scoped advisory transaction lock and the post-lock state-machine short-circuit; and
- line 40 overclaimed that a duplicate paired Outbox raises a uniqueness error and rolls back the transaction including its audit append.

Both are superseded by the frozen semantics enumerated above. An identical-payload duplicate on the same `(event_domain, dedupe_key)` returns the existing event id with **no error and no rollback**, so `uq_outbox_domain_dedupe` is not a replay backstop for identical payloads and contributes no durable replay protection on that branch. The collision path at line 371 raises `OUTBOX_DEDUPE_CONFLICT` with SQLSTATE `P0001` for same-key/different-`payload_sha256` and rolls back the mutation transaction, including its audit append. The function also has a defensive `P0001` at lines 363-364 when the post-conflict row cannot be re-read, and earlier invalid-input validation has its own explicit `P0001` branches. Current PA38 replay safety is therefore the product-scoped advisory transaction lock + post-lock state-machine short-circuit + revision/current-state CAS; the only uncaught `23505` recovery driver remains the catalog active unique index `uq_catalog_version_active`, not Outbox dedupe. The historical artifact is intentionally not rewritten. Current report and fix2 wording are corrected; no schema/index/migration is introduced.

## Runtime execution truth and floating-image provenance

The current 24 LifecycleIT / 14 RoleLeastPrivilegeIT evidence records PostgreSQL **16.16**, not 16.15. In `/tmp/pa38-aggregate-fix2-evidence/failsafe38.log`, lines 33–36 show creation of image `postgres:16` and the live Flyway observation `PostgreSQL 16.16`; lines 79–82 corroborate the second container. This log remains SHA256 `2e425244d8a0784e92b97d7541e13b10000201cecf4d39c775efa56741ebc8df`.

`postgres:16` is a **floating image tag**, not a pinned image digest. Container ids in the transcript are not digest-pinning evidence. No pinned digest is asserted and no image pull/inspection is executed here. The current implementation report's runtime paragraph now says 16.16 and identifies the tag; historical sections unmistakably describing older 16.15 diagnostics keep their historical wording. Digest pinning remains later test-infrastructure work, not part of this correction. Currency note added by aggregate re-review2 fix1 under `PA38-AGG-RR2-03`: at this gate the sentence above was aspirational rather than complete, because the report still carried nested `Current validation`, `Current … runtime` and `Current …` custody labels over 16.15 measurements inside historical gate sections; those labels are now renamed to gate-anchored historical/superseded forms and every 16.15 statement names its own historical gate, while current executed evidence remains PostgreSQL 16.16 from the floating `postgres:16` tag.

## Frozen green invocations and custody — not rerun

Evidence root `/tmp/pa38-aggregate-fix2-evidence/`; accepted LF projection `/tmp/pa38-lf-validation.hgmbb4/candidate`. Recorded launcher `/opt/maven-3.9.9/bin/mvn -B -ntp`, `JAVA_HOME=/opt/jdk21`, PATH prefixed `/opt/jdk21/bin`; Java 21.0.13, Docker Server 29.1.3. Existing commands run from that projection's `backend` are frozen inputs, reproduced here only as evidence:

```bash
/opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=EmployeeAuthorityReadPortTests,CatalogSaleLifecycleSemanticsTests,CatalogSaleLifecycleRepositoryTests,DefaultOperationsCatalogSaleCommandTests,CatalogVersionRequestBinderTests,OperationsCatalogVersionWebTests,OperationsCatalogVersionConfigurationTests,OperationsCatalogVersionBoundaryTests test
/opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=ModuleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests test
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT failsafe:integration-test failsafe:verify
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke '-Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner' failsafe:integration-test failsafe:verify
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke '-Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner' failsafe:integration-test failsafe:verify
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke '-Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner' failsafe:integration-test failsafe:verify
/opt/maven-3.9.9/bin/mvn -B -ntp clean -Pcontainer-smoke verify
```

The clean command is the success-gated `verify && python3` chain in `clean-full-oracle.sh`, which extracts and executes the unchanged shipped `.github/workflows/ci.yml` XML oracle and records custody. The exact expanded command files, seven named `EXIT=0` records and existing transcript hashes remain in fix2, not overwritten. No command in this block is executed by this work unit.

| Prior green evidence | Reports / tests | Preserved result |
| --- | --- | --- |
| Focused PA38 | 8 / 86 | Exit 0, zero failures/errors/skips |
| Existing guards | 2 / 17 | Exit 0, zero failures/errors/skips |
| PA38 live PostgreSQL | 2 / 38 (24 + 14) | Exit 0, zero failures/errors/skips |
| Exact observer, three sequential invocations | 1 / 1 each | Exits 0/0/0, zero failures/errors/skips |
| Canonical clean full | 97 / 934 Surefire + 38 / 630 Failsafe | PASS, zero failures/errors/skips |
| Smoke class in canonical XML | 1 / 8 | 8/8; no separate smoke transcript is present in this bundle |
| Shipped oracle and exact XML custody | 135 / 1564 | `clean-full-oracle EXIT=0`; custody PASS |

Final 135-report custody is SHA256 `316abf43769a81c897ab2c96733af20f3e35acf7e22983bf188ad7c3e505f422` over ordinal backend-relative XML path + space + raw SHA256 + LF. Lifecycle XML is `ebe22d6c4eb79b0073b1d0137f0b0b1b743d8083d7086f13ceb6ccc64606b908`; role XML is `b8f5805eb55c709d3124ed21526410bd2941e6dc344e78f7b90cf71f263ceb6c`. Existing `xml-audit.py` filters foreign reports and verifies identities/totals; exact reports remain in the unchanged projection. The root's `clean-full-oracle.log` records oracle/custody success, not the complete Maven transcript. Controller's completed canonical chain is the supplied execution authority; this fix does not infer a missing transcript or rerun Maven, tests, containers, PostgreSQL or the oracle.

## Read-only byte proof and final document relationships

Before editing, a raw task-entry inventory of all 2,395 tracked/untracked regular worktree files was captured outside the repository. It totals 70,556,123 bytes; its ordinal POSIX path + space + raw SHA256 + LF binding is `c4659417d3d261bcd5b9956694da1e74b7a3215082b7bd9258e471439897eff5`. This task-entry inventory is not the original 32-path candidate inventory or the prior A/B snapshot inventory. Final comparison against it permits only the three exact paths above; all other input bytes remain identical, including prior uncommitted product/test changes.

The evidence-root inventory remains exactly **69 files / 1,488,127 bytes**, binding `9edf2183654756e19f30e052a8103b56d3d76acf009715c6c2f66b8a87da9ed9`; the accepted projection remains **3,636 files / 126,137,740 bytes**, binding `d6869fd9d2641500c5b27c80e9d0a337cf310f7fb7ebd5548bd57d2794a5f1b9`. These are task-entry/final raw inventories using the same ordinal path/hash/LF algorithm, not substituted validation results. No evidence file is created, updated or deleted.

Read-only recomputation confirms:

- Java remains **290 production / 180 test** files; every product/test byte equals task entry.
- The unchanged production manifest algorithm enumerates backend `src/main/java` and `src/main/resources`, excluding only the existing V018 filename, normalizes CRLF to LF per file, and hashes ordinal backend-relative path + space + SHA256 + LF rows. It remains **310 / `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045`**, identical to frozen `manifest.txt` and the carrier pin.
- Exactly **V001–V018**, no V019; raw migration inventory remains `53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70`. No schema/index/grant/migration edit.
- OpenAPI, POM, CI, docs12/14/15/17/PA25, all review artifacts, historical aggregate fix1, Control and fixtures remain raw-byte identical to task entry. No executable byte changes; Maven/test rerun is neither required nor performed.

Final raw document bindings at this gate are intentionally one-way, and are now this gate's historical values: aggregate re-review2 fix1 changed both documents again, so the current sizes/hashes are those recorded in the fix2 artifact itself and in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview2-fix-1.md`.

| Document | Raw bytes | SHA256 |
| --- | --- | --- |
| Implementation report | 101,818 | `c0d98b3e30a7e903a7bb335a76a01d8f4ab3dbea3c29523ab33ead3fb4cf6585` |
| Aggregate fix2 artifact | 34,749 | `388f82010d62a12161f610ed51d2533dc91b327d19d479be3e44876416c7ecbe` |

The report is an original candidate path; fix2 and this artifact are supplemental. This artifact held this gate's report/fix2 final sizes and hashes as historical values; Control and the current fix2 listing record the values that govern later gates. **This artifact's own final raw size and SHA256 are emitted externally in the completion response**, not embedded in itself or in the report/fix2; there is no self-hash or cross-hash cycle.

## Aggregate re-review fix1 gate final 32-path custody — superseded for current bytes

The exact path set is the original 32 paths in immutable `candidate32.txt`, not an expanded worktree listing. After the report's final bytes, hash every original file raw, ordinally sort POSIX paths, serialize `path + single space + lowercase raw SHA256 + LF`, including final LF, and SHA256 the UTF-8 binding. Sum raw bytes over those same paths.

- **This gate's final original candidate: 32 paths / 564,632 raw bytes / `fb658be3b405c3af567be4174caa09428e07c8e533cf6f346927e47c88e7c8a2`.** That binding is historical for the aggregate re-review fix1 gate; the aggregate re-review2 fix1 report relabeling changed the report's bytes again, and the recomputed current total, aggregate and 32-row listing are recorded in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-2.md` and in `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview2-fix-1.md`.
- Only the report among these 32 changed in this fix: 97,907 to 101,818 raw bytes. All other 31 hashes remain at entry values. Final report raw hash is `c0d98b3e30a7e903a7bb335a76a01d8f4ab3dbea3c29523ab33ead3fb4cf6585`.
- The fix2 final candidate total, aggregate and report row are updated to these values. A second independent recomputation after all three documents' final bytes verifies the complete 32-row listing, report hash and custody still agree; supplemental-only updates cannot change a candidate path. Historical static-summary/candidate files remain historical and unchanged.

## Retained residual owners and destinations

| Owner | Classified residual / later destination |
| --- | --- |
| Platform/Operations + Backend | Hypothetical silent identical-payload Outbox skip / unpaired audit with no Java-visible signal; later pairing/detection hardening. |
| Platform/Operations | Delivery order not guaranteed by `(available_at,id)` despite atomic commit; later dispatcher/consumer contract. Audit/Outbox retention, dispatch, retries, pruning and no independent retirement audit remain future platform work. |
| Database/Security/Catalog-order | Broad frozen ACLs and DRAFT-content/revision TOCTOU; future authoring/privilege hardening, no current schema/grant change. |
| Security / Operations IAM | Lock-free authority/admission race and verifier availability/policy; later authority/security work. Forward/include behavior remains future filter work. |
| Platform/Security/Operations | Shared four-slot pool capacity, sustained load, deployment enablement and secrets; operational rollout gate. Slow-client time and unread-body/container swallow/close behavior remain operational/container hardening; the 8193-byte cap only bounds consumption/memory. |
| Backend + Platform | Commit-then-cleanup 503 and stale retry semantics; later cleanup/recovery hardening. |
| Backend | Recovery-shape validation; later recovery slice. |
| Backend/Operations IAM | Unused authority `sqlState()`; later interface cleanup. Transport-fault observability is classified under the accepted sanitized IOException-to-422 contract, not reopened as a defect. |
| Backend test-hardening / test-boundary / CI / Controller | Existing `PA38-RR3-01/02/03`, weak filtered-nonempty/allSatisfy witnesses, observer scheduling/starvation and primary PA35 CRLF; later assertion/evidence hardening, accepted LF projection only. |
| Controller/Backend evidence maintenance | Foreign per-gate XML filtering and durable/self-contained 135-report custody; future bundles isolate and preserve their own reports. Floating-image digest pinning remains later test-infrastructure work. |
| API governance/Catalog-order | UI second confirmation, DRAFT authoring, trial/customer filtering and later API/business slices. |
| Controller | Fresh independent A/B aggregate re-review and later acceptance/Control bookkeeping; not entered or granted by this fix author. |

## Completion and stop

**Accepted `PA38-AGG-RR-01/02` corrections complete; deferred/rejected dispositions and existing classified residuals preserved.** That gate's final raw custody and scope checks agreed; `git diff --check` passed. No staging, commit, push, PR mutation, merge, approval, mark-ready, reviewer request, external comment or issue mutation. No product/test/migration/OpenAPI/POM/CI/evidence bytes changed. This is documentation completion only, not aggregate acceptance or production readiness.

**That gate's next step was a fresh independent A/B aggregate re-review, subsequently delivered.** The current gate is the one explicitly named in Control; historical numeric review-stop instructions in sibling artifacts are superseded by that dynamic pointer.
