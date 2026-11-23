# PA40A Aggregate Deepreview Fix 1 — Canonical Current Content Oracle

- Task: `pa40a-customer-catalog-offer-read-acl-aggregate-fix1-gpt-20261124-01`.
- Date: 2026-11-24, Asia/Shanghai. Gate: aggregate deepreview fix 1; stop before re-review.
- Read-only HEAD: `f20becef3d0f5a4293d8a23f8218d1e080cdf8a1`; accepted slice: `9881f9166690dd319d453a73fa9fde933f388300`.
- Binding inputs: active PA40A Control checkpoint, accepted cumulative plan and plan fixes 1/2, implementation report, implementation fixes 1–5, final-validation report, both prior code reviews, and aggregate reviews `code-review-20261124-001448.md` / `code-review-20261124-002459.md` in the supplied `/tmp/pa40a-aggregate-deepreview-freeze.lVbD6N/{a,b}` snapshots.
- Findings addressed: `PA40A-AGG-A-01` / `PA40A-AGG-B-01`, the same accepted test/provenance defect. Implementation delivered; independent re-review remains pending, not performed here.

## 1. Root Cause And Correction

The old current oracle required raw hashes measured from the pre-commit mixed-EOL primary worktree. Those raw representations are not the accepted Git blobs or a clean checkout. The temporary fixtures bypassed the raw assertion through a root-path conditional, so their success did not establish equivalent live-root acceptance.

Every current C27 row now uses its committed canonical-LF digest in **both** digest slots. Both IVR consumers build current inherited pairs from the preserved historical LF column, overlay the same four pinned current identities, generate exact27 table/bare views, and pin the same recomputed binary aggregate. Materialized bytes are accepted **only** when CRLF-to-LF normalization hashes to that fixed canonical identity. There is no root-dependent exemption, fallback hash list, whitespace stripping, expected digest learned from the fixture, or removal of the content check.

The catalog-version and catalog-draft guards retain their exact current PA25 canonical pin and remove only the incompatible current raw assertion. Their historical OpenAPI/PA25 assertions, independent reconstruction and exact migration-name rejection checks remain intact. The repository's existing `eol=crlf` attributes still govern the historical OpenAPI raw proof; attributes are not changed.

Both IVR methods exercise clean-checkout fixtures in LF and CRLF using the **same checker** as the live repository. Their explicit one-byte content-mutation rejection loop is expanded from the four changed inputs to **all 27 current paths**, with restoration after each rejection. Existing table/bare missing/duplicate/fake/hash mutations, missing V019, extra V020, extra wrong-name V019, and same-count wrong-name replacement probes remain executable.

Historical F26/H26 literal pairs and all historical reconstruction assertions remain unchanged. Independent source comparison proves each IVR method's entire prefix before `currentPins` is text-identical to HEAD after EOL normalization. Historical aggregates remain:

- F26: `afda283795031bd226b3d48db7b9384ffacc5592798cd410769c9ec7ff8bc761`.
- H26: `ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409`.
- Historical PA25 LF/raw: `8f02d4d4842e734f10eac0c15b431afa16eb062790773b8696ae0de67d63b451` / `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9`.

## 2. Exact Changed Paths

1. `backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java` — canonical C27 pairs/aggregate, uniform checker and all-path mutation fixture.
2. `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java` — identical current representation rule and probes.
3. `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java` — current PA25 canonical-only check; historical assertions retained.
4. `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogDraftBoundaryTests.java` — same bounded current PA25 correction.
5. `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation.md` — current semantics, aggregate and both mirrored current tables corrected, without relabeling historical runtime evidence.
6. `docs/gateflow/pa40a-customer-catalog-offer-read-acl-aggregate-fix-1.md` — this new report.

No helper, CI, POM, README or runbook edit is needed. README/runbook do not make the stale current-raw identity claim. Prior plan/fix/review artifacts and Control remain immutable historical records; the cumulative report's current mixed-EOL requirement is explicitly superseded, not applied to a clean checkout.

## 3. Canonical Identity Measurements

**Current C27 aggregate: `58161cad19014b05911bb068d8cd120a7f4a3630c9f6c2c98b7a3883132cef98`.**

The former current aggregate `fea4453e24c987221253f659e65a7ed7e270a74a2b81fe087b2b2c895e0c0712` is superseded, not a permitted alternative. Sort all27 paths by unsigned UTF-8 bytes; append UTF8(path), NUL, decoded canonical SHA256, decoded canonical SHA256; hash the concatenation. Historical aggregates retain raw+LF serialization. Current table27/bare27 and binary views in both consumers use the canonical pair. The cumulative report has four changed-input rows plus the full27 rows; all31 table rows independently match both canonical slots.

| Current input | Canonical SHA256, repeated in both current slots |
| --- | --- |
| PA25 SQL | `ae0261bf90f916323bd387ca5dc314ab425576b6a17560408e9bf9fee2db7e47` |
| PA25 runbook | `a74827f87821353365cf48b0f5ebb794a902ca0ee4710229f057078a0f2d7430` |
| Backend README | `9c094a00f691fd8849363355a12c174cc289ce57841a63d0f25d04f8a30faf7b` |
| V019 | `32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e` |

All27 pins are checked independently against `git show HEAD:<path>`, LF-normalized working-candidate bytes and the clean projection. None of those27 inputs is edited by this fix. The complete canonical map is in the cumulative report and `/tmp/pa40a-aggregate-fix1-evidence/current.json`.

Production manifests independently reproduce **320** entries / `c290aac2bc7662d978e5a516e23d706a1b5195f8601055c3e8a917c21a4065e7` and **319** excluding exactly V019 / `57990aa0bc0886c45a58f4e63698d05f860f94a667bc047b2072b6251f3a66fd`. Production Java298, resources22 and migrations19 remain unchanged.

## 4. Clean Projection And Exact Validation Commands

Evidence directory: `/tmp/pa40a-aggregate-fix1-evidence`; executable Python helpers, canonical map, initial tracked-file snapshot, logs and clean projection remain outside the repository.

The projection helper archives exact HEAD with read-only explicit Git-directory/worktree overrides, extracts into `clean/`, and overlays **only the four changed Java tests**, normalized to LF. It copies no dirty-worktree test, private helper, target or prior XML. The archive/materialization honors the two existing PA25/OpenAPI CRLF attributes; its canonical content is checked against the blobs. The two changed documentation files are synchronized into the completed projection separately; they are not inputs to the tests or C27. No Git metadata is created or edited.

From the primary repository root:

```bash
python3 /tmp/pa40a-aggregate-fix1-evidence/projection.py
python3 /tmp/pa40a-aggregate-fix1-evidence/oracles.py
python3 /tmp/pa40a-aggregate-fix1-evidence/scope.py
```

The projection helper's exact Git invocations are `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call10 --work-tree="$PWD" archive HEAD` (stdout saved as `head.tar`) and `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call10 --work-tree="$PWD" show HEAD:<path>` for each of the27 inputs. It extracts the archive with Python `tarfile`, then overlays only the four named test paths with CRLF-to-LF normalization. No index, branch or commit is created for this projection.

Successful outputs:

```text
attributes: 27 canonical pins / 19 migrations / 27 mutations rejected; aggregate=58161cad19014b05911bb068d8cd120a7f4a3630c9f6c2c98b7a3883132cef98
LF: 27 canonical pins / 19 migrations / 27 mutations rejected; aggregate=58161cad19014b05911bb068d8cd120a7f4a3630c9f6c2c98b7a3883132cef98
CRLF: 27 canonical pins / 19 migrations / 27 mutations rejected; aggregate=58161cad19014b05911bb068d8cd120a7f4a3630c9f6c2c98b7a3883132cef98
```

Thus the candidate, committed content and clean projection agree; three independent materializations reject **81** one-byte content mutations. These are fixture probes, not additional ordinary JUnit identities. Both mirrored source-map recomputations also pass F26/H26 and canonical C27 equality with no live-root exemption.

From `clean/backend`, with native Temurin21.0.13 / Maven3.9.9, cached dependencies and Docker29.1.3:

```bash
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH
mvn -o -B -ntp -Dtest=OperationsIvrSessionReadBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsCatalogDraftBoundaryTests,CustomerCatalogOfferReadAclBoundaryTests test
mvn -o -B -ntp -Pcontainer-smoke -Dit.test=OperationsIvrSessionReadsIT,CustomerCatalogOfferReadPrivilegesIT failsafe:integration-test failsafe:verify
python3 /tmp/pa40a-aggregate-fix1-evidence/inventory.py
```

The same four-class Maven unit command also runs from the primary `backend` to verify actual mixed host materialization. Logs: `unit.log` (clean projection), `it.log` (clean projection), `primary-unit.log` (primary confirmation), `projection.log`, `oracles.log`, `scope.log`, `inventory.log`.

| Clean-projection class | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| OperationsIvrSessionReadBoundaryTests | 8 | 0 | 0 | 0 |
| OperationsCatalogVersionBoundaryTests | 8 | 0 | 0 | 0 |
| OperationsCatalogDraftBoundaryTests | 8 | 0 | 0 | 0 |
| CustomerCatalogOfferReadAclBoundaryTests | 8 | 0 | 0 | 0 |
| OperationsIvrSessionReadsIT | 24 | 0 | 0 | 0 |
| CustomerCatalogOfferReadPrivilegesIT | 16 | 0 | 0 | 0 |
| **Total** | **72** | **0** | **0** | **0** |

Clean-projection Surefire reports **32 tests, 0 failures, 0 errors, 0 skipped**, `BUILD SUCCESS`, exit0, elapsed23.441s. Clean-projection Failsafe reports **40 tests, 0 failures, 0 errors, 0 skipped**, `BUILD SUCCESS`, exit0, elapsed14:48min, finished2026-11-24T00:48:32+08:00. The exhaustive customer ACL class takes864.8s and the Operations IVR class21.96s. The separate primary confirmation also reports **32/0/0/0**, `BUILD SUCCESS`, exit0, elapsed01:30min; those32 overlap and are not additional distinct identities. Focused XML filenames/classes, individual `FQCN#method` sets and absence of failure/error/skip nodes are independently checked against unchanged CI source definitions: **six classes / 72 unique identities** pass exactly.

The PostgreSQL16.16 integration image is the preexisting local `postgres:16`, image SHA256 `3950ad7c3148ca2175ef1b6c36c9efbf78957ae8fbe8b3a5d7c35af50500382f`; no image replacement, dependency install or fixture/source workaround is introduced.

## 5. Inventory And Scope Evidence

The unchanged CI **source-inventory sections** execute from the clean projection: **105/1026 + 41/682 = 146/1708**, annotation-line scan **1709**, POM **41** and the full historical ladder, including PA40A-start1018/666 and1685, all pass. Identity SHA256 is unchanged: `fe582055c9a28067aa156231f5aabfece21f91510a909a56a4c189c2ba4ca55c` (1708 unique sorted LF `FQCN#method` records). The one historical comment-only annotation and its exact CI assertion are untouched.

CI has no C27 aggregate literal requiring a change; no authoritative CI literal changes. Consequently the conditional full unchanged inventory-body rerun is not triggered. The helper extracts the original current/historical source identity definitions and source/POM/ladder sections without changing their literals, omitting the full-suite XML preamble and final completeness calls. This is **not** a new 146-class XML/CI pass: only the six focused clean-projection reports are newly produced. Earlier146/1708 split-run evidence remains historical, not a full-suite rerun of this fix.

Initial raw-byte snapshot: **2418 tracked paths**, including preexisting dirty files. Final comparison: **2413 unchanged**, five permitted existing files changed, plus this single new report. Within the accepted52-path ACL implementation, **48 paths are raw-byte identical to the initial snapshot** and only the four permitted test files change. V001–V018, V019, PA25 SQL, OpenAPI, application Java, CI/POM, README/runbook, shared ACL helper, accepted plans, prior reports/reviews and Control are unchanged by this fix. Existing dirty `CustomerSubjectActivationsIT.java`, `CustomerSubjectClaimFixture.java` and Control are preserved, not included as candidate overlays or silently repaired.

Whitespace validation, from the primary root:

```bash
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call10 GIT_WORK_TREE="$PWD" git -c core.safecrlf=false diff --check
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call10 GIT_WORK_TREE="$PWD" git -c core.safecrlf=false diff --no-index --check -- /dev/null docs/gateflow/pa40a-customer-catalog-offer-read-acl-aggregate-fix-1.md
```

Tracked diff check exits0. New-file check exits1 for ordinary content differences, with **zero diagnostic bytes**, hence no whitespace error. Both checks are repeated after final report edits.

## 6. Limitations, Exploratory Corrections And Handoff

- The inherited Windows `.git` pointer makes default `git status` fail; read-only `GIT_DIR` / `GIT_WORK_TREE` overrides address it without editing metadata. No dependency, Docker or PostgreSQL availability blocker is observed for these focused commands.
- An initial projection helper incorrectly assumed `git archive` would leave every extracted file raw LF; its OpenAPI assertion exposed the existing CRLF attribute. The helper was corrected to compare canonical content, and the complete blob/candidate/projection check was rerun successfully. No pin was changed to fit that representation. The initial patch helper also stopped at a draft-test regex mismatch; the exact current raw assertion was then removed before compilation or validation.
- The first inventory-prefix trial reached the full-suite XML preamble and correctly stopped at missing `PostgresContainerIT.xml`. The explicitly source-only extraction then passed. No missing XML was fabricated, borrowed from the primary worktree or reclassified as a green full inventory.
- Native Windows execution, hosted CI, all146 classes and packaging/firewall/contract gates are not rerun here. This is focused clean-projection validation, **not one uninterrupted full clean verify**, not aggregate re-review approval and not production readiness. Controller/Test/CI own any later exact-head/full-suite or native-Windows validation.
- The accepted deferred comment-only convention remains owned by Test/CI maintenance. Production role-wide column reads versus future endpoint/row filtering and operational credential/TLS/pool/rollback acceptance remain with their existing Backend/Database/Platform/Security owners; this fix changes none of those boundaries.
- No change to the confirmed22-column grant, V019 behavior, PA25 behavior, seven-login/eleven-edge topology or product scope. No staging, commit, branch creation, push, PR action, merge, approval, mark-ready, reviewer request, external comment, issue mutation, or re-review dispatch is performed. Stop after this fix/report delivery.
