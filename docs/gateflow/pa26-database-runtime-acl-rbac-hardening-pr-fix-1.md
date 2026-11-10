# PA26 PR #37 — Bounded Fix 1

## Task and Boundary

- Task: `pa26-acl-rbac-pr-fix1-gpt-20261111-01`; role: Phaseflow PR-review fix author.
- Provider: Codex / GPT; dispatch label `gpt-6.1-sol` / high. No independent underlying-model attestation is available from the workspace.
- Date: November 11, 2026 (Asia/Shanghai). Gate: fix after PR review; **stop before PR re-review**.
- Metadata-free isolated input at PR head `8bd89f7bda832fa19519b64a104807f014fcea45`. Required Control Active section, PR body/metadata, both PR reviews, implementation report, hosted Java failure log/JSON, IT and V014 were read. Accepted plan §9 raw hashes were also cross-checked.
- Exactly three authorized repository paths: `backend/src/test/java/com/onecall/archtests/container/DatabaseRuntimeAclRbacHardeningIT.java`, `docs/gateflow/pa26-database-runtime-acl-rbac-hardening-s1-implementation.md`, and this new report. Paths are relative to `candidate/`.

## Finding Dispositions

| Finding | Disposition | Bounded correction |
| --- | --- | --- |
| PA26-PR37-CI-01 (Medium) | **fixed candidate** | Canonical UTF-8/LF SHA256 with thirteen independent literal expectations; preserve original CRLF provenance, no migration edits or runtime Git dependency. |
| PA26-PR37-A-01 (Low) | **fixed candidate** | Literal expected category for every mutation; exact `P0001` and category-prefixed JDBC message, savepoint rollback and full catalog-unchanged equality preserved. Authority negatives also categorized. |

No independent review acceptance, hosted rerun, production readiness or later gate is asserted.

## Two-Form Hash Provenance

The accepted plan §9 and historical S1 manifest hash Windows CRLF **worktree bytes**. The portable oracle hashes UTF-8 content after CRLF -> LF and lone CR -> LF normalization, preserving all other characters and the final-newline state. The raw and canonical forms are not Git object IDs or Flyway checksums. Expectations are literal test data, never obtained from Git metadata, migration output/history, or the migration under test.

| Version | Accepted plan §9 CRLF worktree SHA256 | Canonical-LF SHA256 used by IT |
| --- | --- | --- |
| `V001` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| `V002` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| `V003` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| `V004` | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `V005` | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| `V006` | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| `V007` | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |
| `V008` | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| `V009` | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| `V010` | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` | `fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343` |
| `V011` | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` | `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` |
| `V012` | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` | `13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828` |
| `V013` | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` | `691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6` |

All thirteen supplied base/candidate migration files are byte-identical. The before/final audit preserves these bytes and V014 bytes, so the accepted Git blobs/content are not changed by the patch. Git metadata is intentionally unavailable; no Git object inspection is invented. Python bytes/hashlib and separate Perl `s/\r\n?/\n/g` plus GNU `sha256sum` independently verify the canonical values. V001 exactly equals the hosted LF failure's actual digest. The historical CRLF table in the implementation report is retained and explicitly labeled; the canonical table is added alongside it.

## Rejection Stage Mapping

V014 remains unchanged. Each pair below is literal in the IT, rather than inferred from SQL at runtime. `rejected` retains exact SQLSTATE `P0001` and requires the JDBC message to start with `ERROR: <expected fixed category>\n`, delimiting the entire primary error category rather than accepting arbitrary diagnostic text. `rejectsAtomically` still captures the full catalog, creates a savepoint, rolls the failed migration back to it, compares the full catalog unchanged, then rolls back the mutation transaction.

| Mutation | Expected first sanitized category |
| --- | --- |
| `ALTER ROLE customer_iam_role LOGIN` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE evidence_role SUPERUSER` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE customer_iam_role NOINHERIT` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE evidence_role PASSWORD 'synthetic-nonproduction'` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE customer_iam_role CONNECTION LIMIT 1` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE customer_iam_role CREATEDB` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE evidence_role CREATEROLE` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE customer_iam_role REPLICATION` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE evidence_role BYPASSRLS` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE evidence_role VALID UNTIL 'infinity'` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE evidence_role SET search_path TO evidence` | `V014_SETTINGS_REJECTED` |
| `ALTER DATABASE pa26 SET search_path TO evidence` | `V014_SETTINGS_REJECTED` |
| `ALTER TABLE customer_iam.customer_account OWNER TO evidence_owner` | `V014_OWNERSHIP_REJECTED` |
| `GRANT pg_read_all_data TO customer_iam_role` | `V014_TOPOLOGY_REJECTED` |
| `CREATE ROLE pa26_partial LOGIN NOINHERIT; GRANT customer_iam_role TO pa26_partial` | `V014_TOPOLOGY_REJECTED` |
| `CREATE ROLE pa26_partial LOGIN NOINHERIT; COMMENT ON ROLE pa26_partial IS 'onecall:pa25:v1:stale'` | `V014_PROVENANCE_REJECTED` |
| `REVOKE evidence_role FROM pa26_lw` | `V014_TOPOLOGY_REJECTED` |
| `DROP ROLE pa26_lq` | `V014_PROVENANCE_REJECTED` |
| `GRANT evidence_role TO pa26_lw WITH INHERIT TRUE` | `V014_TOPOLOGY_REJECTED` |
| `GRANT customer_iam_role TO pa26_lc WITH SET FALSE` | `V014_TOPOLOGY_REJECTED` |
| `GRANT sharing_role TO pa26_lq WITH ADMIN TRUE` | `V014_TOPOLOGY_REJECTED` |
| `COMMENT ON ROLE pa26_lc IS 'onecall:pa25:v1:wrong-slot'` | `V014_PROVENANCE_REJECTED` |
| `ALTER ROLE pa26_lc INHERIT` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE pa26_lw SET search_path TO evidence` | `V014_SETTINGS_REJECTED` |
| `ALTER ROLE pa26_lc RENAME TO pa26_renamed` | `V014_PROVENANCE_REJECTED` |
| `ALTER ROLE pa26_lw NOLOGIN` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE pa26_lc SUPERUSER` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE pa26_lw CREATEDB` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE pa26_lc CREATEROLE` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE pa26_lw REPLICATION` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE pa26_lc BYPASSRLS` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE pa26_lw VALID UNTIL 'infinity'` | `V014_ATTRIBUTES_REJECTED` |
| `ALTER ROLE pa26_lc CONNECTION LIMIT 2` | `V014_ATTRIBUTES_REJECTED` |
| `GRANT customer_iam_role TO evidence_role` | `V014_TOPOLOGY_REJECTED` |
| `GRANT pg_write_all_data TO pa26_lw` | `V014_TOPOLOGY_REJECTED` |
| `GRANT SELECT(id) ON customer_iam.customer_account TO pa26_lc` | `V014_ACL_REJECTED` |
| `GRANT USAGE ON SCHEMA evidence TO PUBLIC` | `V014_ACL_REJECTED` |
| `GRANT SELECT ON customer_iam.auth_identity TO PUBLIC` | `V014_ACL_REJECTED` |
| `CREATE SEQUENCE public.pa26_unexpected; GRANT USAGE ON SEQUENCE public.pa26_unexpected TO evidence_role` | `V014_ACL_REJECTED` |
| `ALTER DEFAULT PRIVILEGES FOR ROLE customer_iam_owner GRANT SELECT ON TABLES TO PUBLIC` | `V014_DEFAULT_ACL_REJECTED` |
| `GRANT SELECT(provider_subject_hmac) ON customer_iam.auth_identity TO PUBLIC` | `V014_ACL_REJECTED` |
| `GRANT EXECUTE ON FUNCTION evidence.guard_recording() TO PUBLIC` | `V014_ACL_REJECTED` |
| `ALTER DEFAULT PRIVILEGES FOR ROLE evidence_owner GRANT SELECT ON TABLES TO evidence_role` | `V014_DEFAULT_ACL_REJECTED` |
| `ALTER DEFAULT PRIVILEGES FOR ROLE evidence_owner IN SCHEMA evidence GRANT EXECUTE ON FUNCTIONS TO PUBLIC` | `V014_DEFAULT_ACL_REJECTED` |
| `GRANT SELECT(provider_subject_hmac) ON customer_iam.auth_identity TO customer_iam_role` | `V014_ACL_REJECTED` |
| `REVOKE INSERT ON evidence.evidence_event FROM evidence_role` | `V014_ACL_REJECTED` |
| `GRANT SELECT ON customer_iam.customer_account TO customer_iam_role WITH GRANT OPTION` | `V014_ACL_REJECTED` |
| `REVOKE SELECT(id) ON organization_access.organization FROM customer_iam_role` | `V014_ACL_REJECTED` |
| `GRANT INSERT ON customer_iam.customer_account TO customer_iam_role` | `V014_ACL_REJECTED` |
| `GRANT UPDATE(status) ON evidence.evidence_record TO evidence_role` | `V014_ACL_REJECTED` |
| `GRANT EXECUTE ON FUNCTION evidence.guard_recording() TO evidence_role` | `V014_ACL_REJECTED` |
| `REVOKE SELECT(id) ON evidence.evidence_record FROM evidence_role` | `V014_ACL_REJECTED` |
| `GRANT SELECT(bucket) ON evidence.media_object TO evidence_role` | `V014_ACL_REJECTED` |
| `GRANT USAGE ON SCHEMA platform TO customer_iam_role` | `V014_ACL_REJECTED` |

The five malformed-marker variants (wrong database OID, wrong role OID, wrong slot, wrong name and trailing space) each explicitly expect `V014_PROVENANCE_REJECTED`. Both non-superuser session and superuser-session/current-role mismatch calls explicitly expect `V014_AUTHORITY_REJECTED`.

Important first-stage distinctions: PUBLIC schema/table/column/routine and unexpected sequence grants fail the exact ACL comparison before dependency checking, hence **ACL**, not DEPENDENCY. Actual global/schema default grants pass the object ACL stage and fail **DEFAULT_ACL**. Dropping/renaming/malformed external identities fails marker cardinality/exact matching at **PROVENANCE** before later graph/attribute checks; an unmarked partial membership fails **TOPOLOGY**. Role/database settings fail **SETTINGS** before attributes. No requested later category is forced by weakening the earlier guards. Twelve test identities remain unchanged; no thirteenth IT is added.

## Validation and Evidence

Execution/evidence root outside the repository: `/tmp/pa26-pr37-fix1-evidence` (denoted `F/` here). The final logs, XML reports, commands, scripts and manifests are also preserved outside the repository at `/mnt/d/codex-temp/pa26-pr37-fix1-evidence-20261111-050751` (the durable handoff mirror; execution/build outputs remain under `/tmp`). Neither is committed or a hosted artifact. `F/before.json` pins all supplied candidate/base/metadata/evidence files. `F/prior-evidence/` is a separate byte-preserved copy of the supplied evidence, including the original initial-flake log/XML, later retry, hosted Java failure and validator failures. No prior failure log is overwritten.

Builds execute in `F/validation/backend`, an external copy of the candidate. Only that copy's V001-V013 migration files are converted to canonical LF to reproduce Linux checkout bytes; original repository migration bytes remain untouched. Test source/build configuration are unchanged between the candidate and execution copy. All generated Maven outputs stay outside the repository.

Environment: WSL2 Linux x86_64, `/opt/jdk21` Temurin **21.0.13+9-LTS**, wrapper-pinned Maven **3.9.9**, Docker **29.1.3**, PostgreSQL **16.15**; local `postgres:16` digest `sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`. `F/environment.log` records actual versions. The default PATH Java is older; commands explicitly pin `JAVA_HOME=/opt/jdk21` and prepend its bin. The exported CRLF wrapper remains unchanged; run its same body after in-memory CR removal, with `$0=./mvnw` so wrapper properties resolution stays correct:

```sh
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
sh -c "$(tr -d '\r' < ./mvnw)" ./mvnw -B -ntp -Pcontainer-smoke -Dit.test=DatabaseRuntimeAclRbacHardeningIT,ProductionDatabaseIdentityProvisioningIT verify
sh -c "$(tr -d '\r' < ./mvnw)" ./mvnw -B -ntp -Pcontainer-smoke clean verify
python3 /tmp/pa26-pr37-fix1-evidence/inventory.py
```

`F/independent-hashes.log` records Python/Perl/GNU agreement and base/candidate byte equality; `F/plan-and-literal-crosscheck.log` confirms all thirteen accepted plan §9 values and canonical literal test expectations. An external Java reflection probe invokes the **actual compiled private `assertFrozen` helper**, not a reimplementation, against LF, CRLF, lone-CR and mixed-ending resource copies: all pass. A non-line-ending content corruption is correctly rejected. The final probe exits 0 (`F/normalization-probe.log/.exit`), without adding an IT or changing the source method inventory.

Probe draft evidence is preserved separately in `F/normalization-probe-initial-fixture.log/.exit`: LF/CRLF/lone-CR passed, but the first synthetic mixed fixture placed lone CR immediately before LF across empty lines, collapsing two intended separators into one CRLF and therefore changing canonical content. That draft exited 1; it is not claimed as a pass. The final generator uses an unambiguous separator cycle and a separate probe workspace. Its four successful forms and negative content probe do not weaken or change the IT. No product-source patch was needed for this harness-fixture correction.

| Validation | Exit | Exact outcome | Evidence |
| --- | --- | --- | --- |
| Focused V014 + PA25 `verify` | 0 | Surefire 36 classes / 309 tests; Failsafe 2 classes / 32 tests (V014 12, PA25 20); zero failures/errors/skips in both phases | `F/focused.command`, `.log`, `.exit`, `F/focused-{surefire,failsafe}-reports/` |
| Final full `-Pcontainer-smoke clean verify` | 0 | Surefire 36/309 + Failsafe 15/246 = **51/555**, zero failures/errors/skips; no retry or source change needed | `F/full.command`, `.log`, `.exit`, `.started`, `.completed`, `F/full-{surefire,failsafe}-reports/` |
| Exact checked-in CI inventory body | 0 | **source = CI = XML**, 36/309 + 15/246 = 51/555; line scan **556**; POM **fifteen includes**; all twelve V014 identities unchanged | `F/inventory.py`, `.log`, `.exit` |
| Canonical content/helper probes | 0 | 13/13 two-implementation hashes; actual helper passes four line-ending forms, rejects content corruption; CRLF reconstruction matches all thirteen plan hashes | `F/independent-hashes.log`, `F/crlf-reconstruction.log`, `F/normalization-probe.log/.exit` |

`F/validation-summary.json` independently aggregates the saved XML and asserts exact class/test counts and zero failures/errors/skips, rather than relying only on Maven's exit. The actual test JVM is Eclipse Adoptium 21.0.13 at `/opt/jdk21`; focused and full XML confirm the pin. Full MediaPlayback passes locally; this does **not** close or erase the prior initial-flake classification. The complete unmodified CI inventory step was extracted from `candidate/.github/workflows/ci.yml` and run against the execution-copy source and newly generated XML; candidate POM/CI and Java source are independently audited unchanged/identical as applicable.

**Fix-author exit: 0.** Both findings remain **fixed candidates** awaiting PR re-review, not independently accepted fixes. Local focused/full/inventory acceptance is complete; this does not assert all-green project validators, hosted rerun success, production readiness or any later gate.

## Preserved Contract and Residuals

- V001-V013 bytes and V014 bytes/semantics stay unchanged. PA25 literal 247-tuple oracle, unrelated 219 tuples, six-login/seven-effective-role/four-membership graph, POM/CI source and fifteen includes stay unchanged.
- All twelve IT method identities, exact denials `42501`/`25006`/`P0001`/`23503`, public/runtime behavior and docs privilege contract remain. No production Java/configuration/API/deployment changes.
- Hosted `java-runtime` at the frozen head remains **failed** (November 10 UTC / November 11 Asia/Shanghai); hosted `contract-regressions` succeeded. Local fix verification does not rewrite hosted status or claim a new hosted run.
- Initial MediaPlayback `SCOPE_MISMATCH` remains an unresolved time/scope reliability residual, root cause unproven, owner Backend/Test/Sharing. Original failure and later passing retry remain separate evidence, not collapsed into a pass.
- Prior A2-s3 exit 1 and A5-s1/s2 exit 2 remain baseline-shaped validator failures owned by Protocol/Data/Backend; A3 previously passed. These validators are not rerun or repaired by this bounded fix.
- CRLF wrapper export/toolchain custody, prior temporary-evidence durability (Backend/CI), V014 fail-closed/exclusive-admin deployment prerequisites, search_path first-statement defense-in-depth (Backend/Database), cluster-wide marker administration boundary (Platform/Database), historical V013/latest-suite split and repeated literal/docs oracle drift remain owner-routed.
- Live credentials/verifiers, pg_hba/TLS, migrator custody, rotation, old-session termination, pool drain, production deployment and hosted resource/image policy remain external and **NOT EXECUTED**. Mutable image tag is pinned here only by recorded observed digest; no policy change or readiness claim.
- No PR body/metadata/Control edits, external comments, commits, staging, push, PR/issue mutation, merge, approval, mark-ready, reviewer request or later gate. **Stop before PR re-review**.

## Final Artifact Metadata

Final bytes, LF-line counts and SHA256 for exactly the three changed paths are measured after final writes in external `F/final-path-metadata.tsv` and the completion handoff. Historical S1 metadata remains historical; no self-referential hash is embedded here.
