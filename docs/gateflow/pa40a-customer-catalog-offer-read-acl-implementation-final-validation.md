# PA40A — Implementation Final Validation Record

## Identity And Evidence Boundary

- Task label: `pa40a-customer-catalog-offer-read-acl-final-validation-gpt-20261123-01`.
- Work unit: `PA40A-customer-catalog-offer-read-acl`.
- Date: 2026-11-23; timezone: Asia/Shanghai.
- Current gate: `implementation` runtime validation evidence.
- Accepted plan commit / implementation HEAD: `0064c86d3df704aeb02b9084143049de089606a6`.
- Base `origin/main`: `ce9f26bd7caa91d69d1cbd7fcb3e058e763341b1`.

This bounded artifact records the supplied Controller-completed validation results. The documentation task did not rerun validation or independently remeasure the candidate. It updates only this artifact and `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation.md`. Historical authoring failures and pending command recipes in the cumulative report remain historical; the results below establish the current recorded evidence without converting limitations into passes.

## Candidate Integrity

- Implementation scope remains exactly **52 implementation paths: 49 modified + 3 new**, excluding private `.pa40a-*`, `.codex`, reports, and generated output.
- V019 raw and canonical-LF SHA256 both remain `32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e`.
- Migration count is **19**. V001–V018 have no diff against HEAD.
- The JAR exists; `jar tf` succeeded and shows exactly one V019 migration entry and no V020. Runtime dependency inventory succeeded.

## Final XML Aggregate

| Suite | Report classes | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Surefire | 105 | 1026 | 0 | 0 | 0 |
| Failsafe | 41 | 682 | 0 | 0 | 0 |
| Total | 146 | 1708 | 0 | 0 | 0 |

The final combined `mvn -B -ntp -Pcontainer-smoke failsafe:verify` exited **0** against **41 XML reports / 682 green tests**. The final aggregate is complete, but it is a split-run aggregate, not evidence of one uninterrupted all-green `clean verify`.

## Interruption And Bounded Completion

One uninterrupted `clean verify -Pcontainer-smoke` reached **16/41 green Failsafe classes**, then the Docker Linux engine disappeared and killed the Maven child. The Controller classified this as infrastructure interruption, not a test failure claim. After restarting Docker Desktop, the Controller completed missing/affected classes in bounded batches while preserving the **same compiled candidate**:

- **Batch A — 83 tests:** `EvidenceRoleIvrAssociationAclIT`, media, and sharing admission/read classes. `EvidenceRoleIvrAssociationAclIT` passed **16/16** and `PublicSharesIT` passed **8/8**. Initial media and ticket-TTL failures were classified as host/container clock or output-capture artifacts; affected classes were rerun after clock correction. The initial batch is not represented as uniformly green on its first attempt.
- **Media reruns:** `MediaGatewayTransportIT` passed **8/8** and `MediaPlaybackIT` passed **9/9** when Windows was approximately **640 ms ahead** of the container.
- **Sharing admission rerun:** `PublicShareAdmissionIT` passed **42/42** after the same clock correction and setting only an **in-process dummy Docker auth entry** to avoid Testcontainers' `https://index.docker.io` log false positive.
- **Batch C — sharing/identity/PA25:** **125/125** passed.
- **Clock-sensitive final rerun:** `CustomerSubjectActivationsIT` passed **24/24** and `OperationsSubjectLifecycleIT` passed **24/24** after Windows and PostgreSQL were observed within approximately **21 ms**.

These batch counts describe completion/rerun evidence and are not additive independent suite totals; the final XML inventory above is the aggregate. Neither Docker recovery nor clock correction is claimed as an implementation change. This record does **not** claim one uninterrupted all-green clean verify.

## Unchanged CI Python Bodies

### Body 1 — Exact Inventory

CI workflow Python body 1 was executed **unchanged** from **WSL Ubuntu 22.04 / Node-independent Python**, because it normalizes paths using `/`. It passed exact **source=CI=POM=XML** inventory, not merely matching counts:

- Surefire **105/1026** plus Failsafe **41/682** equals **146/1708**.
- PA40A start: **1018 + 666 = 1684**.
- Annotation scan: **1685 → 1709**.
- POM: **41 includes**.
- The historical ladder through PA39 and all subtraction checks passed.

### Body 2 — Packaged Runtime Smokes

CI Python body 2 was executed **unchanged with Windows Python**. The default JAR smoke, both web JAR smoke profiles, exact status/problem/header/redaction probes, and cleanup passed. **No DB/verifier adapter was activated.** These are the recorded packaged-runtime probes, not a claim of additional adapter validation.

### Body 3 — Dependency/Firewall Checks

CI Python body 3 was executed **unchanged with Windows Python**. The dependency/firewall check passed with **baseline 37/34, SSM 72/65, 64 counterparts, 8 dependency-only, one packager**. All **116 adversarial probes** passed.

## Contract And Document Validators

- `api/openapi npm ci --no-audit --no-fund` was executed only to provision **ignored local dependencies**. Tracked lockfile bytes did not change.
- `npm run check:s3` passed: Redocly lint and bundle, TypeScript generation/typecheck, doc-map, and media-gateway lint.
- Security **A3 S3** passed: **23/23**, including **10 negative** cases.
- Capacity **A4 S2** passed all **113 fixture cases** and **471 non-finite probes**.

**A2 IVR S3 and local A5 acceptance S2 were executed and did not pass.** They reproduced pre-existing repository/document validation failures unrelated to PA40A:

- **A2:** missing A0 named CHECK/column-dictionary coverage and model-to-A0 references, despite A0 table/operation drift checks and all **25 message fixtures** passing.
- **A5:** `state table header changed in section 7.1`.

These validator inputs are untouched by PA40A. The same A2/A5 local source-tree limitations were previously recorded in Control, including PA22. This documentation task does not modify Control or those inputs, does not suppress these failures, and does not classify them as passing gates.

## Final Disposition And Non-Actions

The Controller supplied complete green **146-class / 1708-test** XML evidence, including **41 Failsafe classes / 682 tests**, with **zero skipped/failures/errors** and final combined Failsafe verification exit **0**. Unchanged CI inventory/runtime/firewall bodies and the successful contract validators are recorded alongside the Docker split-run limitation and pre-existing A2/A5 failures. No separate PA25 command output or other result beyond the supplied evidence is inferred.

This is an **implementation runtime validation record**, not review approval, readiness, or production acceptance. Only the cumulative implementation report and this new bounded artifact are changed. No implementation source, tests, migrations, PA25, CI, POM, OpenAPI, plan/review artifact, or Control is changed. No staging, commit, push, PR creation/update, merge, approval, readiness marking, reviewer request, external comment, or code review is performed.
