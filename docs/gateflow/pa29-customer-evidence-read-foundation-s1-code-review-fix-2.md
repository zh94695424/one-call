# PA29 Customer Evidence Read Foundation — S1 Code-Review Fix 2

## 1. Identity and disposition

- Task: `pa29-evidence-s1-code-review-fix2-gpt-20261112-11`; date 2026-11-12; gate **fix**; local implementation and validation only.
- Authority: accepted PA29 plan, current Control fix-2 adjudication, cumulative S1 implementation report, fix-1 artifact, and re-reviews `docs/reviews/code-review-20261112-150725.md` (SHA256 `7E0A4E1FEABAA28627BB06E67B75FC2A780357E1E2391D19BD857F1C5F883DF1`) and `docs/reviews/code-review-20261112-152315.md` (SHA256 `A42AA522528A6093F897649A106184C45BC7E91E00D1992842EBC9B15D874F06`).
- Accepted finding: A-RR-LOW-1 only. A-LOW-1/A-LOW-2 remain closed; review B retains its zero-material-finding result.
- Disposition: **fix-complete** pending independent re-review. No commit, stage, push, PR, merge, approval, ready action, reviewer request, external comment, or re-review was performed.

## 2. Repair

V017 pass 1 now selects the history invariant by the exact validated ACL. With `old_acls`, it requires exactly 16 rows and no `installed_rank > 16`. With `final_acls`, it requires exactly 17 rows and an exact rank-17 row with version `017`, type `SQL`, success true, the V017 script, and description `customer evidence read privileges`. Any mismatch raises `P0001` with `V017_HISTORY_REJECTED` before grants. Pass 2 remains unchanged, preserving fresh V016-to-V017 migration where ACLs are final while history is still 16, and exact final replay.

The existing `v017RejectsPartialWidenedWrongGrantorAndGrantOptionStatesAtomically` identity gains the third hybrid witness: after exact V017, rank 17 is deleted and a structurally valid `BASELINE` row is inserted at installed rank 0, keeping total history cardinality at 17 without a valid rank-17 V017 row. The existing `rejects()` helper proves `P0001`/`V017_HISTORY_REJECTED`, catalog/business equality, and no repair. The first two hybrid witnesses and all prior rejection probes remain intact.

## 3. Reseal

| Value | Fix-1 current seal | Fix-2 final seal |
|---|---:|---:|
| V017 raw SHA256 | `ec909aa7028ffa0e282fb70653407075f6755ae147b3addca1414bddb94287b7` | `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad` |
| Flyway 12.4.0 checksum | `1509666842` | `-41822162` |

The new hash was computed from final migration bytes with `sha256sum`; the checksum was computed by Flyway 12.4.0 `ChecksumCalculator`/`FileSystemResource`. Current occurrences were synchronized in PA25, the boundary checksum test, both current-history privilege tests, the cumulative implementation report, and the three current-boundary documents. V016 checksum `1669606497`, V001–V016, OpenAPI, and prior seals remain frozen.

## 4. Scope

Changed paths are limited to the required existing V017 migration, PA25 runtime identities, boundary test, both current-history privilege tests, README, security design spec, provisioning runbook, cumulative implementation report, and this new artifact. No plan, Control, review, OpenAPI, V001–V016, POM/CI, unrelated source/test/doc, private helper, credential, or Git/remote state was changed.

## 5. Validation evidence

All Maven output was externalized under `/tmp/pa29-code-review-fix2/validation`.

- `mvn -B -ntp -Dtest=CustomerEvidenceBoundaryTests clean test`: exit 0, `4/4`.
- `mvn -B -ntp -Pcontainer-smoke -Dtest=NoSuchUnitTest -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=CustomerEvidenceReadPrivilegeIT,CustomerSubjectDetailReadPrivilegeIT,ProductionDatabaseIdentityProvisioningIT verify`: exit 0; affected PostgreSQL privilege/provisioning suites `40/40`, including all three hybrid witnesses.
- `mvn -B -ntp -Pcontainer-smoke clean verify`: started in the external copy but was terminated before completion; no final exit or `70/753` result is claimed.
- Toolchain observed: Temurin 21.0.13, Maven 3.9.9, Docker 29.1.3, PostgreSQL 16.15.

The initial combined selector failure (`NoClassDefFoundError` during Surefire discovery) is preserved as a command-level validation limitation; the separated focused/Failsafe command passed. CI source=CI=XML inventory, JAR smokes, dependency firewall, 54 original-candidate inventory, frozen/raw/canonical inventory, and `git.exe diff --check` are not claimed in this artifact until independently executed against the final files.

## 6. Residual owners

Independent reviewer owns re-review and acceptance. Controller owns gate advancement. Platform/Database/Security own production credentials, deployment, rotation, pool drain, live readiness, and external CI. No production or live-readiness claim is made.

Status: **blocked — full clean verify and remaining inventory gates incomplete; stop before re-review**.
