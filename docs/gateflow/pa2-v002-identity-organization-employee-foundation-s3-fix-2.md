# PA2 V002 S3 fix 2 — evidence-only F-03 manifest and F-04 guard SQLSTATE corrections

- Task label: `pa2-v002-s3-fix2-mimo26flash-20261027-01`
- Work unit: `PA2-v002-identity-organization-employee-foundation`
- Gate: `S3 fix 2` (strictly bounded evidence-only correction after independent re-review; no re-review, no later gate)
- Binding controller: `docs/gateflow/project-execution-control.md` current PA2 S3 rows (read-only)
- Binding review: `docs/reviews/code-review-20261027-195649.md`, SHA-256 `55CC42F32C7296963CAFC578E6F221FC608584B031943DD3A8709A10A2CFA78B` (read-only, unchanged)
- Initial review (read-only, unchanged): `docs/reviews/code-review-20261027-192100.md`, SHA-256 `F9CEC7FDEEE20F85D73D926A8255133EA23B734AB8A92C263BAAA5BF252C29B3`
- Accepted S2 base: `7d00831d24c079c50bd3f24a5261d73dcc72238f`
- Plan/design and all code/migrations/tests/CI/README are read-only
- Date: 2026-10-27 (Asia/Shanghai)
- Author: controller-dispatched MiMo 2.6 Flash author for one bounded evidence-only fix
- Artifact: this file (`docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-fix-2.md`)

## 0. Conclusion

**COMPLETE — bounded S3 fix 2 is author-complete; `F-03` and `F-04` are both `已修复` (fixed).** Exactly the two controller-accepted Low evidence-only findings were corrected in exactly two allowed existing documents plus this new artifact. No code, test, migration, CI, README, POM, controller, plan, or review path was modified. No stage, commit, push, PR, merge, re-review, S3 acceptance, or aggregate review was performed.

## 1. Accepted findings and disposition

Binding source: `docs/reviews/code-review-20261027-195649.md` (re-review) and controller S3 re-review / S3 fix 2 rows.

| Finding | Severity | Accepted content | This fix |
| --- | --- | --- | --- |
| `F-03` | Low (evidence-only) | S3 evidence §4 content-only IT diff manifest still stated pre-fix `667` added / `3` deleted at `...-s3-implementation.md:67`; actual post-fix content-only numstat is `672/3` (= `667/3` from S3 plus 5 lines from fix 1) | Manifest row corrected to `672 | 3`; following sentence records the exact `672 = 667 + 5` split (and still explains the three IT deletions) |
| `F-04` | Low (evidence-only) | S3 evidence §8 owning-guard row said PG16 `38000`-class behavior at `...-s3-implementation.md:145`; PostgreSQL 16.15 actual direct invocation is SQLSTATE `0A000` (`trigger functions can only be called as triggers`); IT correctly asserts non-`42501` plus message containing `trigger` | Row corrected to observed `0A000` with the exact trigger message; executable assertion wording (`not 42501`, message contains `trigger`) left unchanged |

Only these two findings were in scope. RR-09 (`wait_event='advisory'`) was not acted on (controller deferred it to aggregate review). F-01/F-02 remain `已修复` from fix 1 and were not reopened.

## 2. Exact before/after wording and line references

### 2.1 F-03 — S3 evidence §4 content-only manifest

File: `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-implementation.md`

Before (pre-fix-2 line `:67`):

```text
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | 667 | 3 |
```

After (post-fix-2 line `:67`):

```text
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | 672 | 3 |
```

Immediately following sentence (post-fix-2 `:69`) — split explanation preserved:

```text
The IT content-only row is the post-fix total `672/3` = `667/3` from the S3 slice plus 5 added lines from S3 fix 1. The three IT deletions are only the class-javadoc tail lines rewritten to mention the S3 methods; all sixteen S1/S2 `@Test` methods and their helpers are otherwise untouched (additions only, plus that javadoc wording). No file outside the four allowed paths changed.
```

Independent confirmation before edit: `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call2 --work-tree=. diff --ignore-cr-at-eol --numstat HEAD -- backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` → `672	3`. Fix-1 already recorded `672/3` at `...-s3-fix-1.md:28` and `:127`.

### 2.2 F-04 — S3 evidence §8 owning guard invocation

File: same S3 implementation evidence.

Before (pre-fix-2 line `:145`):

```text
| Owning guard invocation | not `42501`, message contains `trigger` | owning runtime role passes EXECUTE and reaches the trigger-context error (PG16 `38000`-class behavior), savepoint-rolled-back |
```

After (post-fix-2 line `:145`):

```text
| Owning guard invocation | not `42501`, message contains `trigger` | owning runtime role passes EXECUTE and reaches the trigger-context error (PostgreSQL 16.15 observed SQLSTATE `0A000`: `trigger functions can only be called as triggers`), savepoint-rolled-back |
```

Executable assertion left untouched: `PostgresContainerIT.java:1381-1394` still asserts SQLSTATE is not `42501` and the message contains `trigger`.

Independent disposable probe before edit (this task, container `s3fix2g`, PostgreSQL `16.15`, actual V001+V002 applied, container removed):

```text
ERROR:  0A000: trigger functions can only be called as triggers
CONTEXT:  compilation of PL/pgSQL function "guard_customer_account" near line 1
LOCATION:  do_compile, pl_comp.c:554
NOTICE:  SQLSTATE=0A000 MSG=trigger functions can only be called as triggers
```

### 2.3 Fix-1 stale hash/content references (allowed write #2 only)

File: `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-fix-1.md`

| Line | Before | After |
| --- | --- | --- |
| `:88` | `(before SHA-256 `2B684EFE…1F45`, after `EBCD9549…7A71A`):` | `(before SHA-256 `2B684EFE…1F45`, after this fix-1 `EBCD9549…7A71A`; further revised only for F-03/F-04 by S3 fix 2 — current hash is recorded in `...-s3-fix-2.md`):` |
| `:157` | after-column pure `EBCD9549CC4119A3057D764F657F4AFFD63989656D1ED8F50F3ADE418147A71A` | same hash annotated `(fix-1 after-hash; superseded for the F-03/F-04 lines by S3 fix 2 — see `...-s3-fix-2.md`)` |

No other fix-1 text, conclusion, or prior finding status was rewritten. Historical fix-1 content claims (including its own `672/3` accounting of the IT edit) remain intact.

## 3. Changed-file inventory

| # | Path | Action | SHA-256 before | SHA-256 after |
| --- | --- | --- | --- | --- |
| 1 | `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-implementation.md` | edited (F-03 `:67`/`:69`, F-04 `:145` only) | `EBCD9549CC4119A3057D764F657F4AFFD63989656D1ED8F50F3ADE418147A71A` | `80ADC22354375B8119F7A4807FFE17A368C6F4D04E64B3A7B2A73C7A907DA460` |
| 2 | `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-fix-1.md` | edited (stale evidence-hash references at `:88` and `:157` only) | `E13A8B90E17D3E5E13DFD186A13FF2FB916AEC3AA048E71B46A0DAF4F1A8DE37` | `90849D1D6C8B58364DFDFBE2B5472B14DC4EF271C269147C74FD784CF415CB83` |
| 3 | `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-fix-2.md` | new (this artifact, LF) | n/a | recorded in the task return report (self-hash not embedded) |

Exact paths changed by this task (and only these three):

1. `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-implementation.md`
2. `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-fix-1.md`
3. `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-fix-2.md`

## 4. Hashes (required set)

| Item | SHA-256 | Match required frozen value? |
| --- | --- | --- |
| Revised S3 evidence | `80ADC22354375B8119F7A4807FFE17A368C6F4D04E64B3A7B2A73C7A907DA460` | n/a (revised by this task) |
| Revised fix-1 artifact | `90849D1D6C8B58364DFDFBE2B5472B14DC4EF271C269147C74FD784CF415CB83` | n/a (revised by this task) |
| New fix-2 artifact | recorded in task return report (not embedded here) | n/a |
| Current IT (`PostgresContainerIT.java`, CRLF) | `0CEB33BAFA033128E4206F862CD03A5C8B85EEF5B0006B9D063CC4AEC0D8C4D8` | yes |
| V001 HEAD blob | `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045` | yes |
| V002 | `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5` | yes |
| Initial review `code-review-20261027-192100.md` | `F9CEC7FDEEE20F85D73D926A8255133EA23B734AB8A92C263BAAA5BF252C29B3` | yes |
| Re-review `code-review-20261027-195649.md` | `55CC42F32C7296963CAFC578E6F221FC608584B031943DD3A8709A10A2CFA78B` | yes |
| CI `.github/workflows/ci.yml` | `1DC020BAEA6A37E2EDDF7C783AD39816C192F5360389F74CEF899AA5AD8C0E3B` | yes (unchanged this task) |
| README `backend/README.md` | `451FC79EE0B35AD1C66021269B93601FC9B0E527D34792E86C8DBB56F2CA3BDE` | yes (unchanged this task) |
| POM `backend/pom.xml` | `FBC2D9ECEDC2DAC942C3C460B1BFC17049D4CB5FFBDDA39C7B1496EBC6DDFDA5` | yes (unchanged this task) |

## 5. Validation commands and exact results

Environment: WSL2 Linux `6.18.33.2-microsoft-standard-WSL2`; Temurin JDK `21.0.13+9` (`JAVA_HOME=/opt/jdk21`); Apache Maven `3.9.9`; Docker server `29.1.3`; floating `postgres:16` → PostgreSQL `16.15`. Git via `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call2 --work-tree=. <args>` only; no Git metadata edited.

| # | Check | Command / method | Result |
| --- | --- | --- | --- |
| 1 | Pre-read findings and target lines | Read binding re-review F-03/F-04 and S3 evidence `:67`/`:145` | Both stale claims present before edit |
| 2 | Actual IT content-only numstat | `git ... diff --ignore-cr-at-eol --numstat HEAD -- PostgresContainerIT.java` | `672	3` (matches F-03 actual) |
| 3 | Direct guard probe (disposable PG 16.15) | V001+V002 applied; `SET ROLE customer_iam_role; SELECT customer_iam.guard_customer_account();` with verbose psql + stacked `SQLSTATE` capture; containers removed | `ERROR: 0A000: trigger functions can only be called as triggers`; `NOTICE: SQLSTATE=0A000 MSG=trigger functions can only be called as triggers`; server `16.15` |
| 4 | IT guard assertion intact | Read `PostgresContainerIT.java:1381-1394` | asserts non-`42501` and message contains `trigger`; file hash unchanged |
| 5 | Maven validate | `backend/`: `JAVA_HOME=/opt/jdk21`, `PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH`, `mvn -B -ntp validate -Dmaven.repo.local=/mnt/d/tools/m2` | exit `0`; Enforcer rules 0–4 pass; `BUILD SUCCESS` |
| 6 | Frozen IT hash | `sha256sum` | `0CEB33BAFA033128E4206F862CD03A5C8B85EEF5B0006B9D063CC4AEC0D8C4D8` (required match) |
| 7 | Frozen V001 HEAD blob | `git ... cat-file blob HEAD:...V001...` \| `sha256sum` | `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045` (required match) |
| 8 | V001 CR-normalized diff vs HEAD | `git ... diff --ignore-cr-at-eol --exit-code HEAD -- ...V001...` | exit `0` |
| 9 | Frozen V002 hash | `sha256sum` | `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5` (required match) |
| 10 | Initial review hash | `sha256sum` | `F9CEC7FDEEE20F85D73D926A8255133EA23B734AB8A92C263BAAA5BF252C29B3` (required match) |
| 11 | Re-review hash | `sha256sum` | `55CC42F32C7296963CAFC578E6F221FC608584B031943DD3A8709A10A2CFA78B` (required match) |
| 12 | CI / README / POM hashes | `sha256sum` vs records | CI `1DC020BA…C0E3B`, README `451FC79E…A3BDE`, POM `FBC2D9EC…DDA5` — all unchanged this task |
| 13 | Forbidden-scope diff | `git ... diff --ignore-cr-at-eol --exit-code HEAD -- backend/pom.xml backend/src/main/java backend/src/test/java/com/onecall/archtests/architecture` | exit `0` |
| 14 | Migration path content vs HEAD | `git ... diff --ignore-cr-at-eol --name-only HEAD -- backend/src/main/resources/db/migration/` | empty for content-only (V002 identical; V001 only pre-existing CRLF checkout noise, CR-normalized exit `0`) |
| 15 | No code/config/README/migration change by this task | Before/after hashes of IT, V001, V002, CI, README, POM, both reviews all equal; only the three allowed docs differ from their pre-task hashes | proven (see §3/§4) |
| 16 | Residual false claims in S3 evidence | grep `38000` and manifest row `\| 667 \|` | no `38000`; no `667` manifest row; `672 | 3` and `0A000` present. Remaining substring `667/3` appears only inside the intentional split sentence (`672/3 = 667/3 + 5`), not as a current-total claim |
| 17 | Scoped whitespace (edited tracked docs) | `git -c core.whitespace=cr-at-eol diff --check HEAD -- <s3-implementation> <s3-fix-1>` | exit `0`, zero diagnostics |
| 18 | New-artifact whitespace | `git -c core.whitespace=cr-at-eol diff --no-index --check /dev/null <s3-fix-2>` | expected exit `1` for a new file with zero diagnostic output |
| 19 | Trailing whitespace / conflict markers | CR-aware scan of all three allowed docs | zero trailing-whitespace lines; no `<<<<<<<` / `=======` / `>>>>>>>` markers; S3 evidence and fix-1 remain LF (`CR=0`); fix-2 LF |
| 20 | Disposable probe containers | `docker ps -a` names matching `s3fix` | none left (`s3fix2-guard-probe*`, `s3fix2g` removed) |
| 21 | Name inventory of this task | Explicit allowed-write set | only the three `docs/gateflow/pa2-v002-...s3-{implementation,fix-1,fix-2}.md` paths written |

### Suite limitation (truthful)

Full/default/container suites were **not** rerun in this task. The accepted fix is documentation-only; every executable/config/migration hash remained byte-identical (IT `0CEB33BA…`, V001 `946A0A13…`, V002 `534A0A06…`, CI/README/POM unchanged), so re-running `clean verify` / `container-smoke` would not exercise any delta from this task. Maven `validate` was still run and passed. Local green from prior S3/fix-1/re-review runs is not claimed as a new test execution here, and no GitHub Actions or production acceptance is claimed.

## 6. No-goal-drift / no-code-change proof

- No V001/V002/V003+ edit; V002 hash and V001 HEAD blob + CR-normalized diff unchanged.
- No grant/role/function/schema/sequence/trigger change; migration content-only diff vs HEAD empty.
- No `PostgresContainerIT.java` edit (hash remains `0CEB33BA…`); no assertion weakened; exact 18-method set untouched; RR-09 not implemented.
- No CI, README, POM, production Java, application resource, or arch-test change (forbidden-scope diff exit `0`; content hashes unchanged).
- No plan, controller, initial review, re-review, or S1/S2 artifact edited (both review hashes frozen as required).
- Stop condition not triggered: neither correction required any executable/config/README/migration/controller/review change.
- Goal drift: none — only the two accepted evidence statements and unavoidable fix-1 hash-reference cascade.

## 7. Residual risks

| ID | Residual | Classification | Owner / destination |
| --- | --- | --- | --- |
| RR-F3F-01 | F-03/F-04 closed by this fix; fresh re-review must confirm the revised evidence hash and wording | Fixed-pending-re-review | Backend/QA; S3 re-review |
| RR-10 (from re-review) | Evidence SHA-256 changed from `EBCD9549…A71A` to `80ADC223…DA460`; controller record still cites the fix-1-era evidence hash until the controller updates its row | Evidence-hash cascade, Low (controller-owned update) | Controller / Backend QA |
| RR-03 (inherited) | Floating `postgres:16` tag (observed 16.15), no digest pin | Inherited (plan R-11) | Platform/CI supply-chain hardening |
| RR-04 (inherited) | Concurrency timing sensitivity; mitigated by bounded waits and prior focused repetitions | Bounded test risk (plan R-14) | Backend/QA |
| RR-05 (inherited) | Global enabled-admin precondition fails closed if another suite commits an enabled ADMIN | Test-only fail-closed (RSK-S3-01) | Backend/QA |
| RR-08 (inherited) | GitHub Actions not run at this gate; local validate is not production acceptance | Gate residual (plan R-16) | Phaseflow/CI |
| RR-09 (deferred) | `awaitBlockedBackend` asserts `wait_event_type='Lock'` but not `wait_event='advisory'`; not acted on here | Deferred by controller to aggregate review owner | Aggregate review / Backend QA |

## 8. Final finding status

| Finding | Status | Basis |
| --- | --- | --- |
| `F-03` S3 evidence content-only IT manifest `667/3` → `672/3` | **已修复 (fixed)** | §4 row now `672 | 3`; split sentence states `672/3 = 667/3` (S3) `+ 5` (fix 1); live numstat remains `672/3`; no manifest row still claims total `667` |
| `F-04` S3 evidence guard SQLSTATE class `38000` → `0A000` | **已修复 (fixed)** | §8 row now states PostgreSQL 16.15 observed SQLSTATE `0A000`: `trigger functions can only be called as triggers`; disposable probe reproduced exact `0A000`; IT non-`42501`/`trigger` assertion unchanged; no `38000` remains in the evidence |

## 9. Non-actions

No stage, no commit, no push, no PR, no merge, no re-review, no S3 acceptance, no aggregate review, no controller edit, no review edit, no RR-09 implementation, no test/migration/CI/README/POM/code change, no GitHub Actions claim, no production acceptance. Author completion is not acceptance; the next gate remains a fresh independent re-review owned by the controller.

## 10. Completion status

- Completion: **author_code_complete = true; bounded S3 fix 2 COMPLETE.**
- Validation: findings pre-read; numstat `672/3`; disposable PG16.15 guard probe `0A000`; Maven validate `BUILD SUCCESS`; frozen IT/V001/V002/CI/README/POM/both-review hashes exact; forbidden-scope diff exit `0`; scoped whitespace/conflict/trailing checks clean on all three allowed docs; residual-claim scan clean for `38000` and for any `667` manifest total.
- Suite limitation: full/default/container suites not rerun (documentation-only delta; executable hashes frozen) — stated truthfully above.
- Artifact path: `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-fix-2.md`.
- This fix did not stage, commit, push, open/update a PR, merge, re-review, accept S3 or enter aggregate review.
