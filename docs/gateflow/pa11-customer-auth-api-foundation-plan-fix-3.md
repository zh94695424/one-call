# PA11 customer authentication and API foundation plan fix 3

- task label: `pa11-plan-fix3-gpt-20261102-01`
- work unit: `PA11-customer-auth-api-foundation`
- workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa11-customer-auth-api-foundation/codex-call`
- branch: `codex/pa11-customer-auth-api-foundation`
- start HEAD: `3ca8c89ed7f361e77df3b067f3d802ccf36302eb`
- authorized writes: `docs/gateflow/pa11-customer-auth-api-foundation-plan.md`; this new fix artifact
- status: `plan-fix-3-complete`

## Exact contradiction

Accepted plan section 8 required every resolved runtime artifact to map one-to-one to a `BOOT-INF/lib` entry while also freezing 55 resolved runtime coordinates and 48 packaged JARs. The isolated entry evidence proves that requirement is unsatisfiable: baseline is 37 resolved / 34 packaged; web is 55 resolved / 48 packaged; web adds exactly 18 resolved coordinates and 14 packaged JARs; eight resolved coordinates are dependency-only and have no packaged library JAR; and the Boot repackager adds exactly `BOOT-INF/lib/spring-boot-jarmode-tools-4.1.2.jar` without a project runtime-dependency coordinate. The cardinality identity is `55 - 8 + 1 = 48`.

The eight dependency-only coordinates are:

- `org.springframework.boot:spring-boot-starter:4.1.2`
- `org.springframework.boot:spring-boot-starter-logging:4.1.2`
- `org.springframework.boot:spring-boot-starter-jackson:4.1.2`
- `org.springframework.boot:spring-boot-starter-tomcat:4.1.2`
- `org.springframework.boot:spring-boot-starter-tomcat-runtime:4.1.2`
- `org.springframework.boot:spring-boot-starter-webmvc:4.1.2`
- `org.springframework.modulith:spring-modulith-starter-core:2.1.2`
- `org.springframework.modulith:spring-modulith-apt:2.1.2`

## Correction and adjudication rationale

Section 8 now defines two exact related inventories instead of asserting a false bijection:

1. A complete literal allowlist of 55 unique resolved runtime tuples: group, artifact, version, type, classifier, and scope. The accepted plan contains all 55 evidence-derived tuples, with `type=jar` and an empty classifier where recorded.
2. An exact packaged-library inventory of 48 `BOOT-INF/lib` JARs: 47 exact counterparts of resolved tuples, matched by full tuple and exact filename/version/classifier/scope, plus one independently exact packager-added JAR, `spring-boot-jarmode-tools-4.1.2.jar`.

The eight dependency-only rows are explicitly marked as having no packaged JAR and are not packaged-artifact approvals. The jarmode-tools JAR is handled by its own exact packaged entry and is not a project runtime dependency approval. Neither exception grants any other starter, tools, test, support, or broad-prefix artifact.

The executable cross-check now asserts all identities and rejects malformed, missing, unknown, duplicate, colliding, filename-only, wrong-coordinate, wrong-version, wrong-type, wrong-classifier, wrong-scope, dependency-only-as-JAR, or missing-counterpart cases. Positive exact resolved-coordinate-and-filename members bypass the unchanged legacy negative regex; the exact packager-added entry follows its own exact branch; every nonmember is rejected regardless of regex outcome. Resolved-group Jackson 2 core/databind rejection, Jackson annotations and Jackson 3 exact allowances, all adversarial fixtures, fixed versions, no wildcard/broad-prefix exemption, and the unchanged 30-minute CI ceiling remain binding.

## Unchanged boundary

The accepted goal, API, schema, profile behavior, ownership, ACL/security/no-write boundary, exact test classes/methods and counts, adversarial checks, default/web smoke requirements, no-production-adapter rule, and 30-minute CI ceiling are unchanged. This fix only resolves the accepted-plan contradiction and updates the associated implementation/validation/README language from a single one-to-one inventory to the two-inventory model. No source, test, POM, CI, README, migration, canonical document, controller, prior fix, review artifact, private file, or implementation blocker report was edited.

## Changed sections

- `docs/gateflow/pa11-customer-auth-api-foundation-plan.md`: section 8, `### CI and dependency firewall`, replacing the contradictory one-to-one firewall requirement with the complete 55-tuple table, explicit dependency-only set, exact 47+1 packaged inventory, deterministic cross-check, positive-first ordering, and preserved adversarial/Jackson/firewall rules.
- The same section explicitly requires later implementation, validation, and README/report evidence to retain both inventories and their 37/34 baseline and 18/14 web-delta identities. No unrelated section was intentionally changed.
- `docs/gateflow/pa11-customer-auth-api-foundation-plan-fix-3.md`: this author artifact.

## Input/output hashes

Input hashes captured before the correction:

| Input | SHA256 |
| --- | --- |
| Accepted plan before edit | `ffb3c9e5fd0e1ca6b3535934e0b9ea9b05cb8723f1490186545a6ee6daa2d764` |
| Plan fix 1 | `ffee8ff46f6b97bfa0638ce854179c366e21105c992ae93268fa33e922c89725` |
| Plan fix 2 | `39751c5fde1762b54b89f0883471349e2c0e72654bf2aea7a96e149c87c34bef` |
| Implementation blocker report | `ea523b20e2a3bcd6a434987596655d710412821606a0f946632fd103bd5d22df` |
| Baseline runtime evidence | `6f7a1005ee5c3f12887783cadcc4292d1bb1c9a2c13f447a6c0c67dad05f1aae` |
| Web runtime evidence | `62593cda848af62606e2fcabb8a224b0c7eb525e77e0c6bba3cbc5bcc54ad8c6` |
| Web JAR index evidence | `8c227833a186fc63d4de6aedf2c46d7a0abd01abeb3cc682bca138b72648f176` |
| Dependency tree evidence | `1f8863f147138f673af890f873644bffaf83b16b655059a4364e1485087d670e` |
| Effective POM evidence | `e1ddd76d8e9e236e39ebd8c635b12c6cbf22dc51574fba7b7d4440329ea7560b` |

Output hash after the plan correction, before this artifact was created:

| Output | SHA256 |
| --- | --- |
| Accepted plan after edit | `70fce4d677e9c77bd9389d3ec3db9ecbdeacd127a91930c107658db118bc5f5e` |

The artifact hash is intentionally not self-recorded.

## Commands, results, and evidence paths

- `pwd` -> `/mnt/c/Users/Administrator/.codex/worktrees/pa11-customer-auth-api-foundation/codex-call`.
- `find .. -name AGENTS.md -print` -> no additional `AGENTS.md` found.
- `find /mnt/c/Users/Administrator/AppData/Local/Temp/pa11-web-probe-20261101-233703 ...` -> isolated evidence directory confirmed.
- `nl -ba .../baseline-runtime-dependencies.txt` -> 37 resolved lines after the header.
- `nl -ba .../runtime-dependencies.txt` -> 55 resolved lines after the header.
- `rg -n 'BOOT-INF/lib' .../jar-index.txt` -> 48 web `BOOT-INF/lib/*.jar` entries, including the exact jarmode-tools entry.
- Evidence paths: `/mnt/c/Users/Administrator/AppData/Local/Temp/pa11-web-probe-20261101-233703/baseline-runtime-dependencies.txt`, `runtime-dependencies.txt`, `jar-index.txt`, `dependency-tree.txt`, `effective-pom.xml`, `baseline-repo/backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar`, and `repo/backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar`.
- `sha256sum` over the authorized input documents and evidence files -> hashes recorded above.
- A generated table check parsed the web runtime evidence into exactly 55 full tuples and the plan now contains all 55 rows.
- No Maven, product build, runtime smoke, remote CI, implementation, or external review was run or claimed.

## Arithmetic and evidence identities

- Baseline resolved: 37.
- Web resolved: 55.
- Web resolved additions: 18; removals: 0.
- Baseline packaged: 34.
- Web packaged: 48.
- Web packaged additions: 14; removals: 0.
- Dependency-only resolved coordinates: 8, exactly the listed set.
- Resolved packaged counterparts: `55 - 8 = 47`.
- Exact packager-added JARs: 1, only `spring-boot-jarmode-tools-4.1.2.jar`.
- Total packaged JARs: `47 + 1 = 48`.

These identities were checked against the isolated evidence files, not inferred from memory. Fresh implementation-time CI must execute the literal cross-check required by corrected section 8.

## Residual risks and owners

- Backend/CI owns implementation of the two literal inventories and deterministic cross-check in the existing CI script; no new manifest file is authorized.
- Backend/CI with Platform/Database/Security owns actual support-JAR/image provenance, adversarial firewall execution, and measured suite/startup/process/total timing.
- Backend/QA owns implementation-time source/XML/CI/README reconciliation and preservation of exact test counts.
- Controller owns fresh independent A/B re-review, current control hash capture, acceptance checkpoint, and any separately authorized timeout decision if measured CI exceeds 30 minutes.
- This fix does not claim a passing implementation firewall, remote CI success, production verification, or review acceptance.

status: plan-fix-3-complete

## Final validation addendum

- Evidence-set validation script -> PASS for unique 37/55 resolved sets, 18 additions / 0 removals, unique 34/48 packaged sets, 14 additions / 0 removals, exact eight absent resolved counterparts, 47 resolved packaged counterparts, exact single jarmode-tools addition, and all 55 plan table rows.
- Trailing-whitespace scan over both authorized documents -> PASS.
- `git diff --check` and `git diff --name-status` could not execute: the workspace `.git` file points to `D:/codex-call/.git/worktrees/codex-call9`, which is unavailable in this mounted environment; Git reports “not a git repository.” No staging/commit command was run. The changed-file mtime check identifies only the authorized plan and fix-3 artifact. This Git-metadata limitation prevents a definitive index/status assertion and is disclosed rather than represented as a passing Git check.
- The pre-existing `backend/target` directory was observed; no command in this task wrote there. No `.codex` or `.pa11-*` paths were created in the repository.
- Final accepted-plan SHA256: `70fce4d677e9c77bd9389d3ec3db9ecbdeacd127a91930c107658db118bc5f5e`.
- Final fix-3 artifact SHA256, computed externally: `2448fde95047fe768f75f9cef935b61a3f4b6f99a13d55862a554be3b0d6afe9`.
