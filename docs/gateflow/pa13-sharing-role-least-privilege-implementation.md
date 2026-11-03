# PA13 sharing-role least-privilege implementation

- Task: `pa13-sharing-role-least-privilege-implementation-retry-gpt-20261103-222332`
- Gate: implementation / author validation only
- Date: 2026-11-03
- status: implementation-complete

## Pre-edit frozen migration manifest

Captured before any migration/source edit. Raw hashes identify actual bytes, not a synthesized CRLF serialization. Canonical-LF replaces CRLF with LF only. Exact byte copies and JSON manifest are preserved at `/tmp/pa13-implementation-20261103/`.

```json
[
  {
    "path": "backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql",
    "bytes": 25597,
    "raw_sha256": "96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6",
    "canonical_lf_sha256": "946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045",
    "crlf_count": 536
  },
  {
    "path": "backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql",
    "bytes": 16799,
    "raw_sha256": "4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505",
    "canonical_lf_sha256": "534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5",
    "crlf_count": 352
  },
  {
    "path": "backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql",
    "bytes": 12873,
    "raw_sha256": "4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7",
    "canonical_lf_sha256": "1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1",
    "crlf_count": 259
  },
  {
    "path": "backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql",
    "bytes": 26195,
    "raw_sha256": "8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030",
    "canonical_lf_sha256": "a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc",
    "crlf_count": 550
  },
  {
    "path": "backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql",
    "bytes": 33724,
    "raw_sha256": "6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065",
    "canonical_lf_sha256": "2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310",
    "crlf_count": 703
  },
  {
    "path": "backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql",
    "bytes": 35016,
    "raw_sha256": "7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d",
    "canonical_lf_sha256": "830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475",
    "crlf_count": 772
  },
  {
    "path": "backend/src/main/resources/db/migration/V007__activation_and_consent.sql",
    "bytes": 14486,
    "raw_sha256": "bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81",
    "canonical_lf_sha256": "bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659",
    "crlf_count": 312
  },
  {
    "path": "backend/src/main/resources/db/migration/V008__evidence_media.sql",
    "bytes": 22488,
    "raw_sha256": "32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde",
    "canonical_lf_sha256": "2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e",
    "crlf_count": 547
  },
  {
    "path": "backend/src/main/resources/db/migration/V009__sharing.sql",
    "bytes": 19320,
    "raw_sha256": "3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1",
    "canonical_lf_sha256": "ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae",
    "crlf_count": 378
  }
]
```

Manifest SHA-256: `1bd3170473f0a2fd7db576f4a771a54fb7d5891a4def1a63982be0911428882f`.

## Execution result

The accepted PA13 implementation was completed within scope. `backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql` narrows `sharing_role` and adds the unwired `sharing_command_role`; no view or `SECURITY DEFINER` object was introduced. V001–V009 remain byte-identical. Production Java, endpoints, API/OpenAPI, `control`, review records, and unrelated documentation were not changed. The final changed files are:

- `.github/workflows/ci.yml`
- `backend/README.md`
- `backend/pom.xml`
- `backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql`
- `backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerSharesFixture.java`
- `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
- `backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java`
- `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java`
- `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java`
- `docs/14-production-data-model-and-flyway-schema-spec.md`

The standalone security IT has exactly eight plain tests. It pins bootstrap administrator `fixture_admin`, uses test-only synthetic LOGIN memberships, verifies PostgreSQL 16/Flyway 10, role attributes/membership containment, exact catalog and effective ACLs, permitted/denied behavior, expected SQLSTATEs (`42501`, `25006`), and canonical V001–V009 digests. Existing fixtures retain endpoint/query membership only in `sharing_role`; database behavior probes use `sharing_command_role`. No production wiring was added.

## Validation

Runtime: Java 21.0.13. Testcontainers connected to Docker Server 29.1.3. The standalone suite reports PostgreSQL `16.15 (Debian 16.15-1.pgdg13+2)`, image `postgres:16`, image ID `sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`.

The repository wrapper has CRLF line endings in this WSL worktree. For each required Maven run, it was temporarily LF-normalized and restored byte-for-byte from the index afterward; the wrapper is not a product change.

- `cd backend && ./mvnw -B -ntp -Pcontainer-smoke -Dit.test=SharingRoleLeastPrivilegeIT verify` — exit 0; 8 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`.
- `cd backend && ./mvnw -B -ntp -Pcontainer-smoke verify` — final-tree exit 0; Surefire 67/67 and Failsafe 100/100, 0 failures/errors/skips; `BUILD SUCCESS`.
- Full Failsafe class totals: `PostgresContainerIT` 64, `CustomerMeIT` 10, `CustomerSharesIT` 8, `SharingQueryServiceIT` 10, `SharingRoleLeastPrivilegeIT` 8; total 100 across five classes. Combined inventory is 167 (67 Surefire + 100 Failsafe).
- Extracted and executed the exact inline inventory assertion from `.github/workflows/ci.yml` against final source and XML reports — exit 0; `PA13 exact source/CI/XML inventory passed: Surefire 67; Failsafe 64+10+10+8+8=100; combined 167; zero skips/errors/failures`.
- `git diff --check` with the resolved worktree Git directory — exit 0, no whitespace errors. Git emitted only its configured LF-to-CRLF normalization notices for existing tracked text files; these are line-ending notices, not whitespace failures. New-file whitespace was also checked.
- Frozen V001–V009 verification — all nine raw byte streams compare equal to `/tmp/pa13-implementation-20261103/` copies, and raw SHA-256, canonical-LF SHA-256, byte counts, and CRLF counts match the pre-edit manifest above.

Logs: `/tmp/pa13-sharing-focused-final.log` and `/tmp/pa13-sharing-full-final.log`. Reports: `backend/target/failsafe-reports/TEST-com.onecall.archtests.container.SharingRoleLeastPrivilegeIT.xml`, `backend/target/failsafe-reports/TEST-com.onecall.archtests.container.PostgresContainerIT.xml`, all remaining Failsafe reports under `backend/target/failsafe-reports/`, and Surefire reports under `backend/target/surefire-reports/`.

## Hashes and residuals

- V010 raw SHA-256: `b83545b2110e53b47515d3d6e3eefd084f7b3965552f8f7760040564a36dff4c`.
- Standalone IT raw SHA-256: `fa1e280df7b9b0abd960107da296e091fe118cc808416abcffdb6706c9e11d1a`.
- Final implementation artifact raw/canonical-LF SHA-256 will be computed after this final edit and reported in the runner completion summary to avoid self-reference.
- No implementation or validation blocker remains. The untracked `.codex` path was present before implementation work and was not inspected or modified. No files were staged or committed; no PR, review, merge, approval, or later gate was initiated.

## Completion boundary

Author implementation and validation only. Stop here; do not stage, commit, push, open or mutate a PR, request reviewers, comment externally, dispatch review, approve findings, mark ready, merge, or enter a later gate.

### Code-review fix 2 — author validation completion

- Task: `pa13-sharing-role-least-privilege-code-fix2-retry-gpt-20261104-004103`; gate `fix`, entry `code-review-fix-2`. Scope is the accepted re-review corrections; this is author validation, not another review.
- V010's reset and final relation, column, schema-PUBLIC, procedure-PUBLIC, and grantor/grantability checks cover all nine bounded schemas: `sharing`, `evidence`, `customer_iam`, `organization_access`, `subject_number`, `platform`, `audit`, `ivr`, and `entitlement_metering`. Exact totals are zero query relation ACL rows, six command relation ACL rows, 20 query SELECT columns, 34 command SELECT columns, and eight command UPDATE columns. Function ACL/grantor checks also span all nine.
- Direct V010 replay runs while the query login is a member of `sharing_role` and succeeds. Membership involving `sharing_command_role` remains rejected. The existing first IT method now includes a test-owned PostgreSQL 16 container, filesystem copies of V001–V010, an injected `SELECT 1/0` after V010's ACL work, and before/after snapshots of all bounded-schema namespace, relation, column, procedure, PUBLIC/grantor/grantability, role-attribute, and membership state. The failed migration leaves V010 absent and restores the snapshot. The dedicated suite remains eight methods; `PostgresContainerIT` remains 64.
- `pg_default_acl` is intentionally untouched. Residual provisioning risk: Platform/Database/Security must ensure future default privileges do not reintroduce grants outside this bounded migration's explicit object ACL reset; this test does not validate deployment-wide default-privilege governance.
- Focused validation: `cd backend && JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=SharingRoleLeastPrivilegeIT verify` — exit 0, 8 tests, zero failures/errors/skips. Full validation: `cd backend && JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke verify` — exit 0, `BUILD SUCCESS`; Surefire 67 and Failsafe 100 (`64+10+10+8+8`), 167 total, zero failures/errors/skips. Runtime used PostgreSQL 16.15.
- Exact CI/source/XML inventory passed: `PA13 exact source/CI/XML inventory passed: Surefire 67; Failsafe 64+10+10+8+8=100; combined 167; zero skips/errors/failures`. V001–V009 have no tracked diff from HEAD. `git diff --check` passed (only configured LF/CRLF normalization notices). The product wrapper was not modified.
- Focused log: `/tmp/pa13-sharing-fix2-focused.log`; full log: `/tmp/pa13-sharing-fix2-full.log`. XML: `backend/target/failsafe-reports/TEST-com.onecall.archtests.container.SharingRoleLeastPrivilegeIT.xml`. All Failsafe XML is in `backend/target/failsafe-reports/`; Surefire XML is in `backend/target/surefire-reports/`.
- Final raw SHA-256: V010 `46113453016350075387575ccd35429e0559df38d3daf3d97b59fa4180549a1c`; `SharingRoleLeastPrivilegeIT.java` `5a37807b704c3ea3c4b391ffff1d1c25dd9e4d8f1f101f4916172b21e7c91707`; `PostgresContainerIT.java` `6ff33fe690c702cd1aaee0e66ac70eb5828d5ff8dfe93a862459146e5538c156`; this implementation record is hashed in the author handoff. No production Java/API/OpenAPI/wiring change, stage, commit, or external review action was made.

## Code-review fix 1 — author validation

- Task: `pa13-sharing-role-least-privilege-code-fix1-gpt-20261103-234338`; gate `fix`, entry `code-review-fix-1`. This records only the accepted corrective findings from `docs/reviews/code-review-20261103-233248.md` and `docs/reviews/code-review-20261103-234127.md`; no independent review is claimed.
- V010 now requires the complete canonical `sharing_command_role` attribute tuple (including replication, BYPASSRLS, connection limit, and NULL password), rejects any pre-existing membership edge involving that role, and verifies final sharing-role membership and role attributes. A superuser pass clears PUBLIC and both sharing principals from each target schema, all exact target relations, every target column ACL, and the five relevant procedures/functions before owner-context regrants. No CASCADE or persistent helper object is used.
- Final migration assertions check the 3/7 schema-USAGE totals, no CREATE, zero query relation ACLs / six command SELECT+INSERT relation ACLs, 20 query SELECT columns / 34 command cross-module SELECT columns / eight command UPDATE columns, owner grantors/no grant options, PUBLIC absence on target ACLs, role memberships and attributes, function allow-list, sequence denial, and role-effective prohibited relation/function access. Exact test catalog assertions additionally enumerate schema, relation, attribute and procedure ACL rows and verify grantors, grantability, PUBLIC, and the query/command matrices.
- The dedicated IT remains exactly eight plain `@Test` methods; `PostgresContainerIT` remains exactly 64. Existing methods now replay V010 directly after removing fixture login memberships, and use an isolated PostgreSQL 16 container to prove a stale command-role membership and BYPASSRLS attribute each fail migration, leave V010 unapplied, and leave no command-role schema/relation ACL residue.
- `backend/README.md` now labels the V009 transaction/race path as historical and identifies `sharing_command_role` as the current, unwired command-side ACL principal. V001–V009 remain byte-identical; raw SHA-256 values match the pre-edit manifest above. Production Java/API/OpenAPI and accepted plan/control/review artifacts are unchanged.

### Fix validation

- Focused command `cd backend && ./mvnw -B -ntp -Pcontainer-smoke -Dit.test=SharingRoleLeastPrivilegeIT verify`: exit 0; 8 Failsafe tests, 0 failures/errors/skips; PostgreSQL 16.15. Includes successful direct V010 replay and both stale-state rollback probes.
- Full command `cd backend && ./mvnw -B -ntp -Pcontainer-smoke verify`: exit 0, `BUILD SUCCESS`; Surefire 67/67 and Failsafe 100/100 with zero failures/errors/skips. PostgreSQL container suites: `PostgresContainerIT` 64, `CustomerMeIT` 10, `CustomerSharesIT` 8, `SharingQueryServiceIT` 10, `SharingRoleLeastPrivilegeIT` 8. Combined count remains 167.
- Exact CI source/expected/XML inventory assertion exited 0: `PA13 exact source/CI/XML inventory passed: Surefire 67; Failsafe 64+10+10+8+8=100; combined 167; zero skips/errors/failures`. Source annotation counts independently remain 64 and 8.
- V001–V009 byte streams match the frozen pre-edit manifest exactly, including raw hashes and lengths. Full-tree `git diff --check` exited 0; Git emitted only configured LF-to-CRLF normalization notices. The wrapper was temporarily LF-normalized for WSL execution and restored byte-for-byte afterward; it is not a product change.
- Existing SQLSTATE probes remain green, including role ACL denials `42501`, read-only authorized write `25006`, V009 constraint/guard probes (`23502`, `23503`, `23505`, `23514`, `P0001`), and migration failure/rollback on stale state. Catalog assertions pass for namespace/relation/column/procedure ACLs, grantor, grantability, PUBLIC, query/command matrices, function and sequence rights, and memberships.
- Focused log: `/tmp/pa13-sharing-fix1-focused.log`; full log: `/tmp/pa13-sharing-fix1-full.log`. XML: `backend/target/failsafe-reports/TEST-com.onecall.archtests.container.SharingRoleLeastPrivilegeIT.xml`, `backend/target/failsafe-reports/TEST-com.onecall.archtests.container.PostgresContainerIT.xml`; remaining Failsafe/Surefire XML is retained under `backend/target/failsafe-reports/` and `backend/target/surefire-reports/`.

### Fix identities and residual risk

- V010 SHA-256: `e49021c6fcc4d7c61213673c41617827ce20d6af7ae042d270e10997b38ff997`.
- `SharingRoleLeastPrivilegeIT.java` SHA-256: `77827ce74b49a4f812e18d465a2b7e2948199218870b67e2fbb0696fe702b0ae` at the start of fix validation; refresh after final test edits if changed.
- `PostgresContainerIT.java` SHA-256: `7a53be652b109849b64b84bfcff0e554f1931435c45ffc807641d2262fc9ec3e`.
- `backend/README.md` SHA-256: `170340621515ec91534f2f9a54f08744d3fb1cc40ba0ac864d388b0ed882be32`.
- No production rollout or external review is claimed. The adversarial migration run covers stale command-role membership and BYPASSRLS state; this gate does not constitute a production-cluster rehearsal.

## Code-review fix 3 — author validation completion

- Task: `pa13-sharing-role-least-privilege-code-fix3-gpt-20261104-012425`; gate `fix`, entry `code-review-fix-3`. This records the accepted corrections from `docs/reviews/code-review-20261104-011323.md` and `docs/reviews/code-review-20261104-012217.md`; no independent re-review is claimed.
- V010 now resets every relation kind in all nine bounded schemas, every live column ACL, every procedure ACL, and separately enumerated sequences for PUBLIC, `sharing_role`, and `sharing_command_role`, without CASCADE. It enforces canonical attributes for all three sharing roles, rejects all outbound memberships, inbound memberships to owner/command, and stale query-role escalation while preserving inbound query-login membership. Postconditions cover all nine schemas, including sequence PUBLIC/grantor/grantability, and reject unexpected query/command column privilege types.
- Existing eight-method adversarial IT now covers stale `sharing_owner TO sharing_role`, an external member of `sharing_owner`, altered owner/query attributes, stale non-target column INSERT and PUBLIC/role sequence grants, and a successful legitimate query-login membership. Injected post-ACL failure snapshots and restores bounded namespace/relation/column/procedure/sequence ACLs, PUBLIC/grantors/grantability, role attributes, and memberships. `PostgresContainerIT` remains 64 methods; shared exact catalog assertions retain all nine schema inventories and add sequence ACL grantor/PUBLIC checks.
- Focused required command passed: `cd backend && JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=SharingRoleLeastPrivilegeIT verify` — 8 tests, zero failures/errors/skips. Full required command passed: `cd backend && JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke verify` — Surefire 67, Failsafe 100 (`64+10+10+8+8`), combined 167, zero failures/errors/skips; PostgreSQL 16.15.
- Exact CI source/CI/XML inventory passed with `PA13 exact source/CI/XML inventory passed: Surefire 67; Failsafe 64+10+10+8+8=100; combined 167; zero skips/errors/failures`. V001–V009 raw hashes, canonical-LF hashes, sizes, and CRLF counts match the frozen manifest. `git diff --check` passed; only configured LF/CRLF normalization notices were emitted.
- Logs: `/tmp/pa13-sharing-fix3-focused.log` and `/tmp/pa13-sharing-fix3-full.log`. Reports are retained under `backend/target/failsafe-reports/` and `backend/target/surefire-reports/`, including `backend/target/failsafe-reports/TEST-com.onecall.archtests.container.SharingRoleLeastPrivilegeIT.xml`.
- Fix-3 final raw SHA-256: V010 `eb7142c5f84ec9af6f34cb5bd634486c1494b0baf19e208f5df0ba017bfd9d29`; `SharingRoleLeastPrivilegeIT.java` `9a740328008d4bf82cb6a7d018bde1c62cde57e4adc6c32dcd358cd3978cb7a0`; `PostgresContainerIT.java` `2e2b9df818174b93f7729abc94c286a7c689c67f66368058bc1c405021884c60` (canonical-LF `34e2fedc80c477bd2f9046e50cbc22048a726cdd5833dc1f33f992441f6dc4f6`). The implementation artifact's final hash is reported in the author handoff after this update.
- Residual risk remains: `pg_default_acl` is intentionally untouched and remains owned by Platform/Database/Security provisioning. No V001–V009, production Java/API/OpenAPI, grant matrix, SQLSTATE, Flyway version, or production wiring change was made. No stage, commit, push, PR, review request, or external action was performed.

## Code-review fix 4 — author validation completion

- Task: `pa13-sharing-role-least-privilege-code-fix4-gpt-20261104-020900`; gate `fix`, entry `code-review-fix-4`. Applied only Controller-accepted F-01 and F-02; no independent re-review is claimed.
- F-01: V010's bounded mixed-`pg_proc` ACL reset now uses PostgreSQL 16-compatible `REVOKE ALL PRIVILEGES ON ROUTINE`, preserving function and procedure coverage. Existing postconditions inspect every bounded-schema routine ACL, including PUBLIC, both sharing principals, grantors and grantability, and preserve the exact two-function command execute allow-list.
- The existing `v010MigrationIdentityAndPostgres16` method now creates a synthetic procedure after V009, grants EXECUTE to PUBLIC and both sharing principals, applies V010, confirms zero residual grants on that procedure and confirms exactly two command-role executable functions remain. No ninth test method was added; test counts remain eight dedicated methods and 64 `PostgresContainerIT` methods.
- F-02: §4.44–§4.46 now describe `sharing_role` as having no sharing-table-level privilege and only its exact 13-column SELECT projection; `sharing_command_role` has table-level SELECT/INSERT on the three sharing tables and UPDATE only on eight grant columns for bounded future append/lifecycle work. The append-only immutability trigger remains the mutation backstop. The documentation explicitly makes no claim of production wiring.
- Focused required command passed: `cd backend && JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=SharingRoleLeastPrivilegeIT verify` — 8 tests, zero failures/errors/skips; PostgreSQL 16.15. Full required command passed: `cd backend && JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke verify` — Surefire 67, Failsafe 100 (`64+10+10+8+8`), combined 167, zero failures/errors/skips; Flyway history ends at V010.
- The exact source/CI/XML inventory from `.github/workflows/ci.yml` passed. V001–V009 raw SHA-256 values match the pre-edit frozen manifest. `git diff --check` passed; Git emitted only its configured LF-to-CRLF notices. Focused/full logs: `/tmp/pa13-sharing-fix4-focused.log` and `/tmp/pa13-sharing-fix4-full.log`; reports are retained under `backend/target/failsafe-reports/` and `backend/target/surefire-reports/`.
- Fix-4 final raw SHA-256: V010 `fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343`; `SharingRoleLeastPrivilegeIT.java` `dc52e1ecc5562c4380418a9ccfea4bbbbb9ccfa41241f28693c88482bdbb17ec`; `docs/14-production-data-model-and-flyway-schema-spec.md` `721f0ed541eadf244c0228508ea281c744c70b578cb159613f776f31f9d627ce`. The implementation artifact's post-update hash is reported in the author handoff to avoid self-reference.
- No V001–V009, `PostgresContainerIT`, CI, POM, README, production Java/API/OpenAPI, accepted plan, control document or prior review was changed for this fix. `pg_default_acl` remains the existing Platform/Database/Security residual. Author validation only; no staging, commit, push, PR, review, approval, merge or later gate action.
