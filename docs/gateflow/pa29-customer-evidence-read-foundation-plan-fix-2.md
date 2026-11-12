# PA29 Customer Evidence Read Foundation — Plan Fix 2

## 1. Authority and scope

- Task: `pa29-evidence-plan-fix2-gpt-20261112-01`; ONECALL: `PA29-customer-evidence-read-foundation`; date: 2026-11-12; current gate: **fix**.
- Authority: active PA29 checkpoint in `docs/gateflow/project-execution-control.md`, fix-1 and all four independent plan-review artifacts below. This repair closes exactly accepted findings `PA29-PLAN-REV-07..12` at **plan level**, preserving `PA29-PLAN-REV-01..06`; it is not implementation, acceptance, re-review dispatch or gate advancement.
- Exactly two repository writes: update `docs/gateflow/pa29-customer-evidence-read-foundation-plan.md`; add this artifact. Source, tests, migrations, OpenAPI, CI, POM, Control, all reviews, fix-1 and private/generated files remain unchanged.
- Preserve the confirmed two GET routes, exact projections/access/risk/cursor/transaction semantics, one composed S1, nineteen-column V017 delta, customer53/global288/history17, all76 proposed identities and final49/417 +21/336 =70/753 forecast. OpenAPI and V001–V016 stay frozen.

## 2. Input and output hashes

Digests are lowercase raw-byte SHA256. The current plan matched the Controller-specified input digest before edits.

| Input / changed path | Before / reviewed SHA256 | After SHA256 |
| --- | --- | --- |
| `docs/gateflow/pa29-customer-evidence-read-foundation-plan.md` | `6e8567fc5207e17bdb7a969339384c456610617dc4c208ddb27d76e61dc8e9e3` | `6e7ebb64fd1e37a7d08e9ccd4b58d46ff4ecabd382a0a92255a5271193a2d2a2` |
| `docs/gateflow/pa29-customer-evidence-read-foundation-plan-fix-2.md` | Absent; new authorized artifact | Final digest reported in handoff, not embedded recursively |
| `docs/gateflow/pa29-customer-evidence-read-foundation-plan-fix-1.md` | `d18e531fb2fc650c31ccd81ea7b4783d868c66e50e46a37e377380726c62fbfa` | Unchanged |
| `docs/reviews/plan-review-20261112-070311.md` | `7aabef2cff36f068e9adc94c032bbb312596b9cf4eeee1db39da4dbba2c659e1` | Unchanged |
| `docs/reviews/plan-review-20261112-071426.md` | `9234e732eeec18750fd5d0d1a7f8f0f88b82f48c97d3d0081bedb26653e8e456` | Unchanged |
| `docs/reviews/plan-review-20261112-074018.md` | `6cceb326e6313924f63d9eb8caa85aa1ab264270443f8260b69571c280d368c2` | Unchanged |
| `docs/reviews/plan-review-20261112-075427.md` | `08b9d530ae0a212c45d05bf1d44bcd2b22e6a96f37ca69f672a65ba36ca479d8` | Unchanged |

The active Control, cumulative plan, fix-1 and all four reviews were read. Source/test/migration/CI/POM/specification/runbook input reading additionally covered 303 files / 6,682,035 bytes, including every backend Java source/test and all sixteen migrations. Exact affected source assertions, method names, fixture startup/property wiring, CI inventory maps and POM includes were checked directly; historical prose was not substituted for executable source.

## 3. Accepted finding closures

### PA29-PLAN-REV-07 — Fixed at plan level (High)

Plan §8.3 explicitly changes only `DatabaseRuntimeAclRbacHardeningIT.assertFrozen()`'s unfiltered migration-resource assertion `assertThat(resources).hasSize(16)` to `hasSize(17)`. The filename filter/sort, full `FROZEN` map, `FROZEN.containsKey(version)` guard, normalization/hash comparison and every other frozen block/assertion remain unchanged. V017 is not appended to the historical hash map. Both previously planned current-chain upgrade paths still use migrate3/applied17/current288; pinned V013/V014 oracles and every historical snapshot/rejection check remain intact. All twelve methods are preserved.

### PA29-PLAN-REV-08 — Fixed at plan level (Medium)

Plan §8.3 names the actual method `CustomerBusinessNumberReadPrivilegeIT.pa25ApplyCheckRequireExactV015HistoryAndIndependent261TupleOracle`. After `latest(); fixture.provision()` and the forbidden `number_hmac` grant, change precisely `assertThat(widenedAcls).containsAll(cleanAcls).hasSize(270)` to `hasSize(289)` (clean288 + one forbidden tuple). Keep exact tuple membership, apply/check rejection, no-repair and cleanup/restoration assertions. The already planned current post-apply269->288 remains distinct; historical V015 oracles/literals, independent261 checkpoint and all eight methods stay unchanged. No global numeric replacement is authorized.

### PA29-PLAN-REV-09 — Fixed at plan level (Medium)

Plan §8.3 independently names `ProductionDatabaseIdentityProvisioningIT.frozenMigrationsLogicalAclsAndHistoryRemainUnchanged`: its post-migrate0/validate `assertThat(flyway.info().applied()).hasSize(16)` becomes `hasSize(17)`. Existing latest-fixture/current-oracle/final-row updates are retained, not used as a substitute for this assertion. Preserve migrationHashes equality, exact V001–V013 hash filter, comments/ACL/marker assertions, historical/predecessor probes and all twenty methods.

### PA29-PLAN-REV-10 — Fixed at plan level (Low)

Plan §8.3 removes the nonexistent size-assertion instruction for `SharingQueryBoundaryTests`. It identifies the actual customer `getAllowedDependencies(modules).stream().map(...)` / `containsExactlyInAnyOrder` assertion, enumerates all nine retained dependency literals and adds only `evidence::customer-evidence-query` as the tenth. Preserve every sharing/admission/gateway/playback assertion and all eight method identities. No method or path is added.

### PA29-PLAN-REV-11 — Fixed at plan level (Low)

Plan §§8.3/9.6 authorize exactly one additive property entry in the already allowlisted `CustomerBusinessNumbersFixture.web()` initializer map, before `.run()`: `"onecall.customer.evidence.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32])`. The canonical test value is `AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=` (32 zero bytes), the existing business-number test-key source, supplied separately under the evidence property. The business-number entry and all unrelated helpers remain unchanged; production never falls back to another endpoint's key.

New `CustomerEvidenceFixture` composes `base.start(null)` (latest migration and PA25 provisioning), then `base.web()` with both properties bound before context startup, then case seeding/reset and requests. Existing §6.3 lazy configuration enables `CustomerEvidenceCursorProperties` under `onecall.customer.evidence` and wires `customerEvidenceCursorCodec(CustomerEvidenceCursorProperties)`. The qualified controller ObjectProvider resolves the codec only on list after structural validation; detail does not resolve it. Missing/invalid key still yields sanitized list503. No system-property workaround, post-start binding, extra pool or unrelated fixture refactor is authorized. The existing single-connection pool and bounded ambient-transaction two-connection exception remain intact. This adds no path or test identity.

### PA29-PLAN-REV-12 — Fixed at plan level (Low)

Plan §11.2 replaces the raw frozen-path diff exit-code gate with an executable Python here-document using read-only Git subprocess byte output, canonical/raw SHA256 assertions and exact path/status inventories. §11.3 and the completion-report requirement distinguish LF repository blobs from the managed CRLF checkout: canonical equality against both accepted Git baselines plus independent raw checkout equality before/after, never raw LF-vs-CRLF equality or general whitespace suppression.

The exact frozen inventory is **26 paths**: all ten existing `api/openapi` files (two specifications and eight tooling/configuration files) plus V001–V016. These eight tooling files were already inside the old directory-wide gate; they are not S1 write candidates. Both base trees must agree, every canonical digest must equal the checkout, and raw hashes plus frozen statuses are sealed once outside the repository before implementation and checked afterward without recapture. Tracked and nonignored untracked path enumeration excludes ignored dependency/build outputs, not extra candidates. Only the specifically named new V017 may appear after implementation, with exact `??` status; staging/rename/copy/deletion/extra paths fail. The existing eighteen-entry raw manifest remains exact and unchanged; canonical OpenAPI digests are added. Windows `git.exe` is selected in this worktree, ordinary Git in an independent Linux clone. No Git repair or configuration mutation is authorized.

## 4. Executed static validation

| Check | Observed result |
| --- | --- |
| Input identity | Plan before edits exactly matched `6e8567fc...dc8e9e3`; fix-1 and all four review digests match Control |
| Existing CI/source inventories | In-memory evaluation of CI literal-map assignments/updates, compared to comment-stripped Java methods: Surefire44/375; Failsafe19/302; exact names and uniqueness pass |
| POM / annotation scan | Exactly19 Failsafe includes, matching CI classes; 678 annotation-line hits including the historical comment |
| Directly affected methods | Existing class counts12/8/20/8; each of the three newly identified baseline literal assertions occurs exactly once; actual named methods and dependency assertion exist |
| New identity preservation | Ordered76 names exactly equal the pre-fix plan; distribution12/12/8/6/4/24/10; ordered-name manifest SHA256 `2acad06d4ea5da0f83dea5e22cc87da8acd4b50557e56e9c2c29d799189c4470` (newline-joined UTF-8, no trailing newline) |
| Paths / forecasts | All23 existing test/fixture write paths exist; S1 stays53 =19 new +34 modified, production Java14/test Java31; additions42+34=76; final49/417 +21/336 =70/753; annotation forecast754/POM21 |
| Privilege arithmetic | Delta8+3+4+4=19; customer34+19=53; global269+19=288=53+16+219; widened current289; history17; no tuple change |
| Frozen inputs | All18 raw hashes in plan §11.3 match; canonical hashes match both accepted Git baselines; V016 normalized hash and both canonical OpenAPI digests match |
| Portable command, actual checkout | Final here-document parses; actual capture mode passes with exact26 paths, raw/canonical hashes and empty frozen-path status |
| Portable command, synthetic verification | In-memory-only successor path/status simulation exercises successful verify; negative probes reject raw/canonical/path/status drift, missing V017, extra path, staged V017 and missing successor status; no migration or source file created |
| Formatting / Git | UTF-8, newline termination, no NUL/trailing whitespace; no obsolete size instruction or raw diff exit-code command; Windows `git.exe diff --check` passes; exact frozen-directory status is empty |
| Scope | All1,831 existing files outside the two authorized paths have identical before/after paths and raw hashes, including fix-1, Control, all reviews and private inputs; no extra path outside scope |

Scope manifest algorithm: exclude `.git` path components and only the two authorized plan/fix-2 files; sort relative paths; join `path + NUL + lowercase raw SHA256` records with newline; hash UTF-8 bytes. Before/after digest is **`af07120b8f4c537ec36de6b5fca34ebe509d9dfce158f7062c6625d8a45165dd`**, 1,831 entries. Fix-1's 1,825-entry manifest remains historical, not substituted for this task's captured baseline. Scope validation uses local temporary evidence outside the repository; private file contents are neither modified nor reproduced.

The portable command was iterated after an initial static assertion correctly exposed that `api/openapi` contains eight tooling files in addition to the two specifications; the final command inventories all ten. Actual capture passed; later implementation verify was exercised only with an explicitly synthetic successor inventory/status, not claimed as a real V017 pass.

## 5. Limits and stop

- Managed HEAD is `73813534bb3a8ec8f3f3c38db6c16e0d12109b5c`, direct child of merged main `ef9d0823975ef66d1df3b70e921709d8562b4dcb`, verified with read-only Windows Git. Native Linux Git cannot resolve the Windows worktree pointer; no pointer repair, fetch or remote-state verification occurred.
- Initial Controller-owned dirt remains: modified Control and untracked plan/fix/review/private inputs. Before/after raw scope equality, not an invented clean Git status, proves this author's boundary. Git diff alone cannot show the untracked plan changes.
- Maven, Docker, PostgreSQL, PA25 execution, XML generation and full CI are **NOT EXECUTED** in this plan-fix gate. All runtime totals are preserved forecasts. Implementer/CI own actual source=CI=XML/runtime evidence later.
- Prior accepted policies and residual owners remain unchanged, including whole-list risk403, artifactRoles empty[], ORIGINAL-only duration, trusted-application JSONB residual, tenant-sized subject sets, repeatable-read revocation semantics and implementation-derived/sealed V017 checksum. No new policy question or production-readiness claim is introduced.
- Stop after the cumulative plan and this artifact, final raw hashes and truthful static completion report. No source/test/migration/OpenAPI/CI/POM/Control/review/private/generated edits, stage, commit, push, PR, merge, approval, mark-ready, reviewer request, external comment, issue mutation or later-gate dispatch occurred.
