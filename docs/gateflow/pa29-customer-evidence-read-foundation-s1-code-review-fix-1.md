# PA29 Customer Evidence Read Foundation — S1 Code-Review Fix 1

## 1. Identity, authority and status

- Task: `pa29-evidence-s1-code-review-fix1-gpt-20261112-10`; work unit `PA29-customer-evidence-read-foundation`, S1; date 2026-11-12; gate **fix**. Provider/runtime: Codex `gpt`; dispatch requested `gpt-6.1-sol` / `high`, one-shot `--no-persist` (not independently model-introspected).
- Managed HEAD/base: `d00c22c4f4b654fe392bd7c4a82e92e87ef0a2f7`; merged implementation base `ef9d0823975ef66d1df3b70e921709d8562b4dcb`; goal-confirmation base `73813534bb3a8ec8f3f3c38db6c16e0d12109b5c`. Only Windows `git.exe` read-only inspection is used; no Linux Git-pointer repair or fresh remote-state claim.
- Binding authority: current PA29 Control adjudication in `docs/gateflow/project-execution-control.md`, complete accepted plan `docs/gateflow/pa29-customer-evidence-read-foundation-plan.md` (SHA256 `6e7ebb64fd1e37a7d08e9ccd4b58d46ff4ecabd382a0a92255a5271193a2d2a2`), and S1 implementation report (entry SHA256 `ee4ca9d4eb5aff16ba3de8c058f083cc391708c1d91da4ff5492613545f60468`). These are inputs, not implicit permission to advance gates.
- Accepted review A: `docs/reviews/code-review-20261112-135351.md`, SHA256 `2369c58fff98aadcd931bd641f67353888faca4fdbf4e883d7331e2a947c89a9`; accepted review B: `docs/reviews/code-review-20261112-141511.md`, SHA256 `6f5ff6e9ae15ff11655669468e03a90376e7baeb13390e1abf0776a0466cccfb`. Both exact input digests are verified. B reports no material defect. Only A-LOW-1 and A-LOW-2 are implemented; no open question, speculative hardening, future work or unrelated style change is adopted.
- Status: **fix-complete — local implementation and validation only**. A-LOW-1/A-LOW-2 are repaired; focused40/40 and clean753/753 pass, with exact CI/XML and JAR gates. No independent re-review, acceptance, gate advancement or production readiness is claimed.

## 2. Accepted decisions and implementation

### A-LOW-1

V017 now adds exactly five validation lines after the existing exact explicit/effective ACL checks and before database/default/dependency checks, invariant capture and the first grant loop. On **pass 1 only**, history cardinality must equal 16 when the fully validated expected ACL is `old_acls`, or 17 when it is `final_acls`; mismatch raises **`V017_HISTORY_REJECTED`**, SQLSTATE **`P0001`**. Exact ACL validation deliberately precedes this correlation, so partial/missing/widened/wrong-grantor/grant-option states still fail with their existing ACL category instead of being relabeled as a hybrid.

The second internal pass deliberately skips this new correlation: a legitimate fresh V016-to-V017 migration has already granted the final ACL but Flyway has not yet inserted row 17. Exact final-state replay retains history17/final ACL and passes. No grant, role, owner, provenance, object definition, default ACL, database or dependency rule is loosened. Removing only these five lines reproduces the exact pre-fix raw SHA256; the rest of V017 is byte-identical to the reviewed migration.

Both adversarial witnesses are inside the existing `v017RejectsPartialWidenedWrongGrantorAndGrantOptionStatesAtomically` method; no new test identity or helper is added:

| Witness | Setup | Required result and preservation |
| --- | --- | --- |
| V016 ACL + valid-looking V017 row | Start at exact V016/269; insert rank17/version017/SQL/success row with correct script/description and the actual pending Flyway checksum | `P0001` / `V017_HISTORY_REJECTED`; mutated pre-invocation catalog, ACL/history and seeded business state unchanged |
| V017 ACL + only V016 history | Upgrade normally to exact V017/288; delete rank17 before direct replay | `P0001` / `V017_HISTORY_REJECTED`; final ACL remains present, row17 remains absent and seeded business state unchanged |

The existing `rejects` helper snapshots the entire catalog including ACL-bearing attributes, role/credential/provenance/membership/owner/definition/dependency catalogs and Flyway history, compares it after rejection/savepoint rollback against the **mutated** pre-invocation snapshot, then rolls back the adversarial setup. It also compares all-module business rows/sequences outside that setup transaction. This method now seeds both ordinary business rows and evidence/event rows before the witnesses. Existing nineteen partial-addition checks, missing tuple, table/extra-column widening, wrong owner, nineteen grant-option and nineteen wrong-grantor probes are preserved. All thirteen original rejection categories remain present and exercised by the unchanged companion identity.

The unchanged thirteen-category set is AUTHORITY, PROVENANCE, HISTORY, SETTINGS, ATTRIBUTES, TOPOLOGY, OWNERSHIP, ACL, DATABASE, DEFAULT_ACL, DEPENDENCY, PRESERVATION and RECONCILIATION, each prefixed `V017_` and suffixed `_REJECTED`. Both witnesses and their catalog/business no-repair assertions pass in the focused run and again in the clean full run.

### A-LOW-2

`CustomerSubjectDetailReadPrivilegeIT.v016AddsExactlyEightOwnerIssuedTuplesAndMatchesIndependent269Oracle` regains its own direct `assertThat(after).isEqualTo(oracle())` immediately after its 269-size assertion. The eight-tuple delta, 34/16/219 split, non-grantable assertions and every existing assertion remain. Historical V016 stays at checksum `1669606497`, history16 and 269; current V017 stays at history17 and 288. Both privilege suites retain their exact ten public method identities, names and order.

## 3. Final V017 reseal

| Seal | Reviewed input | Fixed final bytes |
| --- | --- | --- |
| Raw SHA256 | `84f991ee286ca5f329e55bac277862b542b2e3ddf859f7ab3bd147158f6bb7bc` | `ec909aa7028ffa0e282fb70653407075f6755ae147b3addca1414bddb94287b7` |
| Flyway checksum | `-1388182157` | `1509666842` |

The final checksum is independently calculated line-by-line using CRC32 and by the actual **Flyway 12.4.0 `ChecksumCalculator` / `FileSystemResource`**, then verified against actual migrated Flyway metadata by `CustomerEvidenceReadPrivilegeIT`'s unchanged fresh/upgrade/history assertions. It is not an invented literal or wildcard. `flyway-checksum-final.log/.exit` records the native calculation and exit0; the focused privilege suite passes all10 identities against the resealed resource.

All exact current-seal occurrences are synchronized in PA25, the boundary checksum assertion, both current-history privilege suites, the implementation report and three current-boundary docs. Immutable plan/Control/review historical seals are intentionally untouched; only this artifact records the old/new comparison. The implementation report explicitly separates original Worker/Controller executions from this fix's evidence rather than attributing the new seal to old logs.

Outer migration permissions remain exactly nineteen owner-issued column SELECT additions, no grant options: evidence_record8 + evidence_artifact3 + media_object4 + recording4. Arithmetic remains **269 + 19 = 288 = customer53 + evidence16 + others219**, customer53 = six schema USAGE +47 column SELECT, target-pair50-to-69. No table/write/routine/sequence/schema-CREATE/role/membership/default-ACL expansion occurs. PA25 retains exact `expected_resolved=288`, history17, predecessor literals including V016 `1669606497`, and apply/check rejection without repair.

## 4. Exact fix scope

Only these nine existing paths plus this one new artifact are writable/changed by this task:

1. `backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql`
2. `backend/ops/postgresql/pa25-runtime-identities.sql`
3. `backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java`
4. `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceReadPrivilegeIT.java`
5. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailReadPrivilegeIT.java`
6. `backend/README.md`
7. `docs/17-production-security-design-spec.md`
8. `docs/operations/pa25-production-database-identity-provisioning-runbook.md`
9. `docs/gateflow/pa29-customer-evidence-read-foundation-s1-implementation.md`
10. `docs/gateflow/pa29-customer-evidence-read-foundation-s1-code-review-fix-1.md`

The accepted original54 implementation candidates remain exact, with zero missing/unexpected, plus only this new fix artifact. Pre-existing Control/private/review inputs are separately classified, not implementation candidates. A pre-edit raw snapshot of1,437 relevant backend/docs/API/CI files is retained in `before.json`; non-owned files remain byte-identical. All26 frozen paths match the inherited pre-S1 resume snapshot and both accepted Git baselines canonically; all18 sealed raw OpenAPI/V001-V016 hashes match the accepted plan. The unchanged plan's exact frozen verify here-document passes against the inherited snapshot, without recapture after edits. Historical bodies in all three current-boundary docs remain unchanged; their identical current-section body SHA256 is `9b502cd4c1f3bfb4aead542d86576eb38bc571d5daa439dfd9ef2fd23a742d2e`.

No dependency, POM, CI, production Java, OpenAPI, V001-V016, accepted plan, Control, review, credential, private helper, Git metadata or remote state is an authored fix target. The only available patch interface in this session is the terminal `apply_patch` executable; no standalone patch tool is exposed. Temporary helpers/copies/logs live only outside the repository under `/tmp/pa29-code-review-fix1`.

## 5. Environment, commands and actual results

Evidence root: **`/tmp/pa29-code-review-fix1/evidence`**. Validation copy: **`/tmp/pa29-code-review-fix1/validation`**. A read-only Git tracked/nonignored inventory copied1,849 inputs byte-identically, excluding Git/private `.pa29-*`/`.codex`, target/build/cache/dependency-output paths. Maven writes only the external copy's `backend/target`; no repository ignored build output is used.

Toolchain: Eclipse Temurin/Adoptium **Java21.0.13**, Maven **3.9.9**, Linux `6.18.33.2-microsoft-standard-wsl2` amd64. Docker client/server **29.1.3**, API1.52 (minimum1.44). Current tests successfully use `DOCKER_HOST=unix:///var/run/docker.sock` (resolves to `/run/dify.sock`), socket override `/var/run/docker.sock` and host override `localhost`. No bridge is needed or created; the previous Desktop/loopback recipe is not misrepresented as this task's transport. No persistent environment or host-service configuration is changed.

PostgreSQL binary and actual Testcontainers server metadata: **16.15**, Debian **16.15-1.pgdg13+2**. `postgres:16` image ID/digest **`sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`**, repo digest `postgres@sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`. `postgres-image.json`, `postgres-version.log/.exit`, `docker-info.json`, `docker.log` and test logs retain the measured evidence, not an assumption from the floating tag.

Commands run from the external `backend` use `JAVA_HOME=/opt/jdk21`, PATH prefixed with `/opt/jdk21/bin:/opt/maven-3.9.9/bin`, `MAVEN_OPTS=-Dmaven.repo.local=/mnt/d/tools/m2/repository`, and `JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true`. Build/test commands use pinned Linux `mvn`; the repository's CRLF wrapper is not edited. For the unchanged exact CI firewall script's `./mvnw` invocation only, the **external copy's** wrapper is LF-normalized, verified as CRLF-to-LF-only in `wrapper-copy.json` (repository raw `825cdc62abd459da5efa6eeb0218f329c0adca3f7d4f7560f6ba2b43b9deb928`, external LF `7e6e5d26712efd78140f2f63dafe8d17028f6c5c97ac1f746a043110b7a1d9ad`). No document or source/test/migration/PA25 content is normalized or changed for validation. Each Maven label has exact `.command`, `.log`, `.stderr`, `.started`, `.finished`, `.exit` and retained XML copies under `<label>-xml/`.

| Label / exact command | Actual result |
| --- | --- |
| `toolchain`: `mvn -version` | exit0; versions above |
| `docker`: `docker version` | exit0; client/server versions above |
| `focused-boundary`: `mvn -B -ntp -Dtest=CustomerEvidenceBoundaryTests clean test` | exit0; clean compile and4/4 boundary tests, zero failures/errors/skips |
| `focused-container`: `mvn -B -ntp -Pcontainer-smoke -Dtest=CustomerEvidenceBoundaryTests -Dit.test=CustomerEvidenceReadPrivilegeIT,CustomerSubjectDetailReadPrivilegeIT,ProductionDatabaseIdentityProvisioningIT verify` | exit0; boundary4/4 plus evidence privilege10/10, subject-detail privilege10/10 and PA25 provisioning20/20 =40 ITs; zero failures/errors/skips |
| `full-clean`: `mvn -B -ntp -Pcontainer-smoke clean verify` | exit0; Surefire49/417 + Failsafe21/336 =70/753; zero failures/errors/skips; all19 predecessor suites302 plus PA29 two suites34 |
| `exact-ci-inventory`: `bash /tmp/pa29-code-review-fix1/ci-assert-container-smoke-actually-ran.sh` | exit0; unchanged current CI here-document proves exact source=CI=XML70/753, all677 baseline identities preserved, annotation754/POM21 |
| `default-and-opt-in-web-jar-smokes`: `bash /tmp/pa29-code-review-fix1/ci-default-and-opt-in-web-jar-smokes.sh` | exit0; exact current CI script passes default/nonweb and opt-in-web checks, no DB/verifier adapter, JVM cleanup |
| `production-jar-dependency-firewall`: `bash /tmp/pa29-code-review-fix1/ci-production-jar-dependency-firewall.sh` | exit0; exact current CI script passes approved72-coordinate/65-JAR set and all116 adversarial probes, legacy37/34 baseline, counterparts64, dependency-only8/packager1 |
| Native checksum: `/opt/jdk21/bin/java -cp /mnt/d/tools/m2/repository/org/flywaydb/flyway-core/12.4.0/flyway-core-12.4.0.jar /tmp/pa29-code-review-fix1/Seal.java <absolute-V017-path>` | exit0; `1509666842` |
| `python3 /tmp/pa29-code-review-fix1/audit.py` | exit0; artifact-inclusive55=54+1 scope, zero missing/unexpected/staged, unchanged non-owned bytes, frozen26/raw18/canonical26, methods10+10, synchronized docs and `git.exe diff --check` exit0; final rerun retained as `audit-final.log/.exit` |

Actual local timestamps (+08:00): clean boundary run `14:27:59–14:28:27`; focused container run `14:28:27–14:37:25`; full clean run `14:37:25–14:55:00` (Maven finished `14:54:59`, total17:33); exact CI/XML gate `14:55:02`. The JAR produced before Failsafe remained byte-stable during the full run; its independent default/web smokes ran `14:40:23–14:40:27` and firewall `14:40:27–14:40:34`. These are actual final-seal executions, not relabeled original Worker/Controller logs.

`focused-xml-inventory.json` and `full-xml-inventory.json` preserve exact class/case identities and per-XML SHA256; `full-xml-inventory.log/.exit` records an independent parse and exit0. Focused4 and40 are not added to the clean753 total. Final full XML has49/417 Surefire plus21/336 Failsafe, exactly70 reports, no duplicate/skipped/error/failure cases; source methods match each report. Exact current CI scripts were extracted without changing assertions; no GitHub workflow or remote CI run is implied. A1–A5 contract/static tools were not rerun by this bounded fix: their inputs are unchanged, and original Controller evidence remains historical rather than claimed as a fresh full workflow execution.

The first implementation iteration is **not hidden**: the unparenthesized SQL CASE expression failed PostgreSQL parsing with SQLSTATE42601, so `attempt-1/focused-container` exited1 (40 ITs:2 failures,31 errors,0 skips); its boundary4 tests had passed. The CASE is parenthesized in the final five-line fix and resealed everywhere. Original attempt logs/XML/exits remain under `attempt-1/`, separate from final runs. A preliminary external audit also rejected pre-existing mixed CRLF/LF boundary/PA25 files under an overly strict LF-only check; the audit was corrected to accept existing CRLF while rejecting bare CR, without normalizing repository files. Its original nonzero log is retained as `audit-pre-artifact.log`, followed by the passing `audit-pre-artifact-final.log`. The checksum helper's initial resource-location construction was corrected before the final native calculation; no failed helper attempt is counted as seal evidence.

## 6. Assessment and residual owners

- Frozen/candidate assessment: final audit passes frozen26/raw18/canonical26, exact54 original candidates plus only this new artifact, unchanged non-owned bytes, report/current-doc equality and `git.exe diff --check` exit0. All48 candidate source/test/migration/PA25 paths are byte-equal between repository and validation copy; no tested implementation byte differs from the candidate. All original source method identities remain; both privilege suites retain ten each.
- PA25 assessment: both focused and full `ProductionDatabaseIdentityProvisioningIT` pass20/20; exact new checksum/288/history17 is enforced. Evidence privilege10 includes PA25 apply/check rejection/no-repair, fresh/upgrade/replay, both new hybrid witnesses, thirteen categories, independent288 oracle, login/role/edge invariants and pool restoration. V016 remains269/history16 and checksum1669606497.
- CI/XML assessment: full source=CI=XML70/753, annotation754/POM21 and every affected predecessor/current-fixture regression pass. All19 predecessor Failsafe suites/302 identities pass, including historical migrations and current sharing/customer/media/PA25 fixtures; both PA29 suites add34. No method identity is introduced, renamed or removed by this fix. No GitHub workflow is triggered and no remote CI success is claimed.
- JAR assessment: full clean packaged JAR is39,394,544 bytes / SHA256 `4df74439bfd652bf1113d6d24a90cd42ddbcfbf72148f74b0a2df66bf05ee053`; `jar-seal.json` records all17 packaged migrations as raw-byte-equal to repository sources, including exact final V017. Actual startup and116-probe firewall checks pass, not merely package presence. Local test JVMs/PostgreSQL fixtures exit; final cleanup inventory has no remaining Testcontainers containers and no loopback23759 bridge. The first five-second cleanup inventory briefly showed Ryuk before its automatic exit; that original inventory is retained separately, not hidden.
- Unchanged residuals: trusted-application JSONB/raw-role sensitivity and non-RLS/nonlinearizable revocation (Security/Database/Product), tenant-capacity/SLO evidence (Backend/Database), floating image tag and Windows EOL portability (CI/Database/Controller), cursor-key custody/rotation and external production credentials/TLS/pg_hba/deployment/pool drain/session invalidation/Q17/Q23 (Platform/Database/Security/Operations). Production/live readiness is **NOT EXECUTED / NOT CLAIMED**. No new product-policy decision is made.
- Residual blocker: **none for the authorized fix**. All mandatory local focused/affected-suite checks and the feasible full clean gate pass. Controller owns subsequent independent re-review and acceptance; this author stops before either, without promoting this evidence to production/live readiness or remote CI acceptance.

No stage, commit, push, branch creation, fetch, PR creation/update, merge, approval, ready action, reviewer request, external comment, issue mutation or review acceptance occurs. Docker activity is local isolated validation only; no external production or workflow mutation occurs. Stop before independent re-review.
