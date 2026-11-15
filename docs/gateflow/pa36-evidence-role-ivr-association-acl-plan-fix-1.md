# PA36 evidence-role IVR association ACL — plan fix 1

Task: `pa36-evidence-role-ivr-acl-plan-fix1-gpt-20261116-01`

Date: 2026-11-16 (Asia/Shanghai). Author role: bounded plan-fix author, not implementer or approver.

Status: all seven Controller-accepted findings addressed **in the plan**; fresh independent re-review and any implementation authorization remain Controller gates. No gate is passed, marked ready or externally changed by this artifact. PA35 remains paused.

## 1. Authority and exact output boundary

Read the Active PA36 checkpoint in `docs/gateflow/project-execution-control.md`, cumulative plan, reviews `docs/reviews/plan-review-20261116-061519.md` and `docs/reviews/plan-review-20261116-063117.md`, their cited architecture/docs14/docs17/runbook/README, V001–V017, PA25 SQL, affected current/historical tests and shared Operations context/configuration/owner readers, CI and POM. The accepted checkpoint's seven-role shared-pool decision supersedes the original three-edge plan. Nothing here delegates work, edits implementation, or changes the authorized two-column delta.

Fix1 entry HEAD is `ab5424fb218642fe2037647d0d49cf87b04fcc0f` (review intake). Read-only Git reconciles merged PA34 base `5200355695d39b6aeba446631c8f3f041b96e9ba` → original Control-only `c89e7845f6b15b18fca2116bd3633b8e565b4aea` → plan intake `3a0872e` → review intake. The cumulative plan's former assertion that c89e784 was still local HEAD is corrected. Base→entry changes only plan, Control and both reviews; production/test baseline bytes remain untouched.

Only repository writes:

1. Update `docs/gateflow/pa36-evidence-role-ivr-association-acl-plan.md`.
2. Add this `docs/gateflow/pa36-evidence-role-ivr-association-acl-plan-fix-1.md`.

No source/test/migration/docs17/runbook/README/CI/POM/Control edits. No stage, commit, branch, push, PR, merge, approve, mark-ready, reviewer request, external comment, issue mutation or implementation action. Pre-existing untracked `.codex` remains unchanged. WSL's Windows `.git` pointer was not edited; read-only Git used explicit `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD"`.

## 2. Finding dispositions and changed sections

| Accepted finding | Review mapping | Plan disposition | Changed sections / concrete closure |
| --- | --- | --- | --- |
| PA36-PLAN-REV-01 | A-01 + B-01 High | Addressed | §§6,7.3,7.3.1,8: exact43-path manifest includes PostgresContainerIT, DatabaseRuntimeAclRbacHardeningIT, five omitted architecture tests, all equivalent existing latest-history/PA25/reflected-oracle/hash tails; split historical prefix from current inventory/docs17 body; complete selected test command and counts |
| PA36-PLAN-REV-02 | A-02 + B-02 High | Addressed with Controller-selected decision | §§1,2,4,5,7,9: same existing shared pool, exactly seven LO SET targets, eleven total edges; all five existing context enum scopes plus employee/IVR on real ordinary connection, restoration/failure/reuse and denial of any eighth role; old login/direct boundaries unchanged |
| PA36-PLAN-REV-03 | B-03 Medium | Addressed | §§3,5,7.1,7.2: V018 evidence-only16→18 literal oracle, exact target two tuples; full non-target preservation snapshots; no absolute408 migration predicate; safe non-target IVR EXECUTE variation preserved by migration but rejected by PA25 exact410; wrong-target/grantor/option/PUBLIC/topology drift still fails closed |
| PA36-PLAN-REV-04 | B-04 Medium | Addressed | §§6,7.3.1,7.4,8:43=3new+40existing,36 test/helper paths (34existing+2new),7 non-test;15 latest-history-only paths plus separate classpath/doc/hash responsibilities; explicit pa36_names merge/subtraction at every old checkpoint and exact focused/full totals |
| PA36-PLAN-REV-05 | B-05 Medium | Addressed | §§4,5,7.3.1,7.4: anchored psql input/settings/slots/name collision/cardinality/loop/direct-login slice/history18/oracle410/allowed-pair/mutation/edge11/output list; Operations/IVR actual true-INHERIT defaults; current history corruption sentinels rebased without losing historical coverage |
| PA36-PLAN-REV-06 | B-06 Low | Addressed, not deferred | §§5,6,7.1: backend/README.md is path43; prepend authoritative current section, explicitly scope retained undated three-input/six-seven-four prose historical; keep old bodies verbatim |
| PA36-PLAN-REV-07 | B-07 Low | Addressed | §§5,7.1,7.2,8,9: docs17 role-wide amplification reaches LW/customer sharing commands/projection and Operations evidence readers, not just LO; targeted LW/LO scalar access and unchanged customer/public identity matrix; unchanged sharing response/log regressions run, no payload output |

No accepted finding is deferred to implementation discovery or an unowned closeout edit. These are plan dispositions, not claims of passed runtime verification or reviewer agreement.

### Count and topology reconciliation

- Final implementation allowlist: **43 = 3 new +40 existing**. Existing34 test/helper paths are31 classes +3 fixtures; new paths are migration +2 test classes. Five omitted boundary tests, two mis-forbidden container suites and README add8 to the original35. OperationsActivationReadsIT additionally owns docs17/hash scoping, not just history.
- Old four edges remain LQ→sharing, LC→customer, LW→command and evidence. LO adds exactly Operations IAM, subject number, activation consent, entitlement metering, evidence, catalog order, IVR: **4+7=11**. Each edge has ADMIN false / INHERIT false / SET true, with exactly one approved unmanaged-superuser grantor row. No new pool/runtime role/hierarchy.
- Required success metric is `logins=7 effective_roles=9 memberships=11`. Nine is the exact-ACL-monitored role set, not the graph's total distinct targets. Four **already-existing** shared-context roles are separately topology/attribute validated, so total capability-role identities13; managed names13 (4external+9monitored), full boundary names17 (managed13+4context). Direct roles overlap login and capability counts:7+13−3=17. This definition preserves Controller's requested metric without falsely asserting only nine distinct graph targets or adding four extra ACL inventories to410.
- Migration-created roles31 remain31; four external PA25 login identities yield35 project roles (old34). Operations IAM and IVR rolcanlogin=false / rolinherit=true are preserved from V001/V004, not cosmetically altered. External logins remain NOINHERIT; edge inherit=false.
- Existing PA25 `$frozen$`288 = customer53 + evidence16 + sharing/admission/other219. Original owner-issued GRANT expansion independently yields Operations14 and IVR106. Expanded pre-count408 is accounting only; final410=288+120+2. Seven-role projection290, evidence-only16→18, evidence_record six→eight columns. Twelve-owner and43-routine metadata maps are already sufficient and unchanged.

## 3. Exact identities and CI/POM/XML arithmetic

Entry source methods were compared **by identity**, not just count, with the current CI literal expected maps. Failsafe POM includes were parsed as XML and compared as exact sets. There are147 Java test-tree files including non-test helpers,107 actual test classes /1208 ordinary methods. Annotation-line scan1209 includes exactly the frozen PostgresContainerIT comment. No test was executed by this plan fix.

| Inventory | Entry | Planned final | Delta |
| --- | --- | --- | --- |
| Surefire classes / methods |76 /704|77 /712|+1 /+8|
| Failsafe classes / methods |31 /504|32 /520|+1 /+16|
| All classes / methods |107 /1208|109 /1232|+2 /+24|
| Annotation lines |1209|1233|+24, same one comment|
| Exact Failsafe includes |31|32|sole new EvidenceRoleIvrAssociationAclIT include|
| Existing PA25 methods |20|20|identity set unchanged|
| Owned compatibility focus |31 /443 existing|33 /467|9 Surefire/56 +24 Failsafe/411|
| New-focused PA36+PA25 |not executed|3 /44|1 Surefire/8 +2 Failsafe/36|
| Amplification/customer-sharing focus |not executed|5 /74|1 Surefire/8 +4 Failsafe/66|

Canonical identity hashing sorts fully qualified class then method, emits each `class#method\n` in UTF-8/LF:

- Entry1208 identity SHA256: `89262b28603810de7858f39f2bcedf4e598137fc6cf54ecdb8c6249f8dc67843`.
- Planned final1232 identity SHA256: `a082db1f77d93e9c76ed20500ca52c9e5c8b96fdc97b7140f0fa7d1972f58b23` (source baseline plus exact8/16 named plan additions; not XML evidence).

The recomputation simulated new class/method sets against all historical suffix-union subtractions. Baseline/PA18, PA19,20,21,22,23,25,26-derived,27,28,29,30,31,32,33,34 and PA36 start pairs/scans all reproduce the plan's exact table. PA26's309/234/543 and line544 are printed/derived history, not an existing standalone CI accumulator. `pa36_names` enters every earlier subtraction and additions subset guard, while PA36-start subtracts only its24 methods. Earlier literal totals/maps/identities remain unchanged.

Plan §7.4 enumerates early Surefire totals, method totals, current scan, class/include lengths, exact XML calls and final success-summary token changes; package delta31 is deliberately not changed. Existing dependency/package firewall stays baseline37/34, approved72/65, deltas35/31, dependency-only8, counterparts64, packager1 and116 adversarial probes. Full runtime acceptance remains clean verify + exact source=CI=POM=XML109/1232, zero failures/errors/skips. Focused commands and exact selected unions are in plan §8, not executed here.

## 4. Before / after hash evidence

All hashes below are raw SHA256 unless stated otherwise. Before-plan bytes are the exact original54,269-byte /385-line artifact accepted at intake.

| Artifact | Before | After |
| --- | --- | --- |
| Cumulative plan |`2b79571ce3acbef10dee372627dfd989824faa3d756fae627f6c21e0ab75da0d`|`cad87e0e558a0db04678cfcb0ce547f085d31fb082fbe0afcf4f1ddae046ca21`|
| Fix1 artifact |absent|new planning artifact; its full-file SHA256 is computed externally after creation, not embedded self-referentially|
| Protected-entry manifest (2111 files; excludes cumulative plan and absent new artifact) |`9d81ff501dd8afaf395219c2bea75e102f1c61ca2021f6d43c0f53f5b20429ad`|`9d81ff501dd8afaf395219c2bea75e102f1c61ca2021f6d43c0f53f5b20429ad` (required and checked at final validation)|

Protected manifest convention: sort all entry repository file paths (excluding `.git` metadata and the two permitted output paths); emit each raw hash, two spaces, relative path, LF. This includes pre-existing `.codex`. Before/after equality is additionally checked per file, not inferred from Git status or line-ending-filtered diffs. No protected entry file may be removed or changed; the sole new repository path must be this artifact.

Selected protected input hashes (identical before/after):

| Path | Before = after SHA256 |
| --- | --- |
| `docs/gateflow/project-execution-control.md` | `de1fb95ff5a86ee3dfab7327566e1d5fd753b1747efcd202d1a1c3f506ad76b7` |
| `docs/reviews/plan-review-20261116-061519.md` | `adb3f3637f1de9fde2850a3b72d1fc28ffc0e83f37be0847261fdd6a0e30398d` |
| `docs/reviews/plan-review-20261116-063117.md` | `9df3bfa283a929614eaf6419f2105097f4f27d01e1444bb83f5bb7be48673f04` |
| `backend/ops/postgresql/pa25-runtime-identities.sql` | `e3d739e22d1c49dfca9b0a4dfa717c384adfc0b15f097f16795d1072253efdd8` |
| `docs/17-production-security-design-spec.md` | `7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e` |
| `docs/operations/pa25-production-database-identity-provisioning-runbook.md` | `588e7c1aafc3f2418a8341790eaae0692919b131cf4a76f414d848eb9dc9f63a` |
| `backend/README.md` | `0bd1b7b17f388a603a06dc47798748ab8df16199d67d9e243332dc183350a9c6` |
| `.github/workflows/ci.yml` | `2c4addc3a09c2418869c6252e06be9257011d0fc34751289db6c15292887faf0` |
| `backend/pom.xml` | `6a0e01aa11959149a55cd731118809d9efc9231ee11fd645292d490a2d9ea19a` |

### Frozen migration checksum and digest recheck

All seventeen raw hashes match plan §2, and all seventeen signed line-CRC32 values match the actual PA25 history tuples (version/script/checksum and rank order). The LF SHA256 values below distinguish normalized bytes from this checkout's raw bytes; normalization is an external validation projection only, never a repository edit.

| Version | Signed Flyway line CRC32 | LF SHA256 (raw unchanged, plan §2) |
| --- | --- | --- |
| `V001` | -709123726 | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| `V002` | -1229975166 | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| `V003` | -1355776370 | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| `V004` | -665258487 | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `V005` | -1005314773 | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| `V006` | 366834714 | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| `V007` | -1281725572 | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |
| `V008` | -245169007 | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| `V009` | -59961153 | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| `V010` | 247431460 | `fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343` |
| `V011` | 492686454 | `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` |
| `V012` | -291628548 | `13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828` |
| `V013` | -611381791 | `691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6` |
| `V014` | -340359494 | `e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a` |
| `V015` | -1316980027 | `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3` |
| `V016` | 1669606497 | `988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f` |
| `V017` | -41822162 | `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad` |

V018 does not exist in this planning-only tree; there is intentionally no invented checksum/hash, no production runtime claim and no history mutation. Plan §§3.3/5/7 require deriving its actual checksum after final bytes settle and pinning it in PA25/tests/docs. Current docs17 raw/LF hashes remain `7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e` / `8f96a69ce5309319a8c8fab1cc6cab419e79e90781945ddcd6fd2b41a311d38f`; future current pins must be separate from the three consumers' preserved PA34 historical-body pins.

## 5. Validation performed and limitations

Read-only/static validation performed using Python3 and existing repository formats; temporary scripts/manifests are outside the repository under `/tmp`:

1. Reconstructed and compared exact source methods with executable CI dictionary construction, including inherited addition maps; parsed POM's namespaced Failsafe includes and proved exact31-class equality. Result: PASS,76/704+31/504, canonical identity digest above.
2. Reconstructed all historical start totals with the exact new8/16 identities merged, validating suffix unions and the current annotation-comment offset; final109/1232 and scan1233. Result: PASS. This simulates inventories; it does not fabricate source methods or test XML.
3. Parsed actual PA25 `$frozen$`, `$history$`, `$owners$`, `$routines$`:288/17/12/43. Independently expanded original owner-issued Operations/IVR GRANT statements to14/106 unique tuples, checked120 total and408→410 arithmetic. Result: PASS.
4. Rehashed V001–V017 raw and LF bytes, recomputed signed CRC32 per UTF-8 line (Flyway convention), matched exact version/script/checksum tuples and all raw plan pins. Result: PASS. No bytes were normalized in the worktree.
5. Parsed the cumulative manifest: sequential unique rows1–43,40 existing/3 future-new implementation paths; complete changed-class set31 existing +3 helpers +2 new classes, and exact8/16 new identity lists. Checked literal command selections:8;8+36;56+411;8+66; full712+520. Result: PASS.
6. Reconciled shared pool role enum/employee entry and seven-role topology, actual default-INHERIT attributes, unchanged six fixture role lists/legacy pool names, role-wide evidence consumers, all current migration/hash/docs17/PA25 tails, and explicit exact future commands. Static plan checks reject stale memberships=7/three-role-live topology/absolute408 migration acceptance claims. Result: PASS; actual TCP/PG semantics remain runtime obligations.
7. Compared every protected entry file byte hash, repository new-path set and final Git diff scope after creating this artifact. Result: PASS, all2111 protected hashes unchanged, only the cumulative plan modified and this artifact added; `git -c core.safecrlf=false diff --check` with explicit read-only git-dir/work-tree passes, staged paths are empty and pre-existing `.codex` is unchanged. No repository plumbing/configuration changes.

**Not executed:** Maven/JUnit/Failsafe, Docker/PostgreSQL, Flyway migration/replay, psql apply/check, hosted CI, live deployment/TLS/credentials/rotation/pool drain, or external acceptance. This task writes plans only. PostgreSQL16 runtime evidence and clean full109/1232 zero-skip XML remain mandatory implementation gates; no source/static check substitutes for them. Tool prerequisites and exact future commands are in plan §8. No new role/pool/schema/table/column, broad/write/grant-option delta, PA35 API/application work or V001–V017 modification is authorized.

Handoff: stop at these two planning files. Controller owns any later isolated re-review, scope authorization and PA35 resumption; this task neither requests nor triggers those actions.
