# PA25 Plan Fix 1 — accepted provenance/adoption finding

## 1. Task, authority and status

- Task: `pa25-production-db-role-provisioning-plan-fix1-gpt-20261110-01`; date: 2026-11-10; Phaseflow role: fix author.
- Runtime/provider routing requested: Codex CLI / GPT `gpt-6.1-sol` / high. Observed execution: Codex CLI tools, local Bash, read-only static analysis and two documentation writes; no delegation. The session cannot independently attest the underlying model identifier or provider routing. Runner-level final exit/JSONL evidence belongs to the Controller after this turn, not to this artifact.
- Binding contract: PA25 Goal Confirmation and accepted review adjudication in `docs/gateflow/project-execution-control.md`. Only Medium `PA25-PLAN-A-01` is accepted for repair; A OQ-1 is subsumed. No other question, finding, goal or provisioning behavior is redesigned.
- Status: **PA25-PLAN-A-01 fixed candidate; independent re-review required**. This is author evidence, not independent acceptance, implementation authorization or production-readiness evidence. Stop before re-review.
- Sole task writes: modify `docs/gateflow/pa25-production-database-role-and-credential-readiness-plan.md`; create this fix artifact. Control, both reviews, source/tests/POM/CI/README/migrations and private `.codex` / `.pa25-plan-prompt.txt` are preserved.

## 2. Reviewed input identities

| Input | SHA256 | Verification / meaning |
| --- | --- | --- |
| Frozen pre-fix plan | `23FEF94FC8E67592DD2CA2E8B31C4F2D098713A654405FFD639BFD5D2A226971` | Entry raw bytes verified; 57,274 bytes, 301 LF characters. Complete plan read before editing. |
| Post-review Control at dispatch | `A72BC7F12F8E67542739A14A746DC909FDB3D93B342DE27229C1DB351CCC2735` | Entry raw bytes verified; binding Goal Confirmation/adjudication read; preserved. |
| Control in the reviewers' frozen snapshots | `612CA70AB5557F83A746B56EC9420B545CCD5230A570944D59D83325661C0FC7` | Historical identity reported in both reviews, not the current dispatch Control hash. |
| Review A: `docs/reviews/plan-review-20261110-194130.md` | `945572C2A92965FE5C32481C7579CCA0FD9AD988D1552A2BBC871E0C655224C1` | Read in full, independently submitted report preserved; one Medium finding. |
| Review B: `docs/reviews/plan-review-20261110-195726.md` | `FEC319268AFBE15F7856F893C5898C7B903468AECC355658EFDC47915024D706` | Read in full, independently submitted report preserved; zero material findings. |

## 3. Decision and alternatives

The accepted defect was implicit adoption: a compatible unrelated chosen-name login could receive approved memberships and pass structural check without proof it was intended. The decision is **creation-only durable role-comment provenance, no adoption or operator-attestation interface**.

Alternatives considered within that finding only:

- Name/attribute compatibility alone: rejected; it is the accepted counterexample.
- Exact approved memberships as provenance: rejected; structural compatibility is not administrative intent and cannot distinguish an unrelated compatible identity. It also does not independently justify missing-edge repair.
- Explicit nonsecret operator attestation for existing-role adoption/repair: not chosen; it adds an adoption authorization/input path and external attestation lifecycle when first creation plus bounded replay suffices. There is no override flag or inferred attestation in this contract.
- Checked `COMMENT ON ROLE` on newly created external logins: chosen; durable existing-catalog metadata, no helper schema/table or extra datasource, and compatible with first apply and replay of genuinely script-created roles.

## 4. Exact cumulative plan changes

| Plan location | Change |
| --- | --- |
| §1 | Record current fix-candidate status and the exact two-path Fix-1 authorization; retain all no-implementation/no-remote boundaries. |
| §4.1 | Permit only creation-time comments; require provenance before any mutation; make check comment-read-only; add fixed redacted `PA25_PROVENANCE_REJECTED`. Four existing script variables remain unchanged. |
| §4.2 steps 3–4 | Extend global preflight to all existing external login markers before any DDL; create/mark atomically; reaffirm attributes and repair approved missing edges only for proven existing or newly created-and-marked logins; verify provenance again before commit. |
| §4.3 | Make provenance an explicit required shape for existing LQ/LC/LW; retain every flag/password/validity rule. |
| New §4.4 | Answer A OQ-1 with exact marker grammar, slot mapping, catalog lookup, creation-only authority, both-mode validation, prohibited adoption, tamper/loss semantics and administrator trust boundary. |
| §5.1 | Add shared-comment pre/postcondition checks; clarify metadata is not an ACL tuple or a permission to relax any graph/dependency/history rule. |
| §6.2 methods 1/10/18/19/20 | Strengthen creation/replay, marked missing-edge repair, unrelated-compatible/tampered-marker rejection, metadata preservation and redaction within existing names. No new method or parameterization. |
| §8.2 / §9 / closing status | Add provenance-specific stop/recovery and custody/restore risk ownership; require independent re-review. |

### Provenance and adoption behavior

The exact durable value is `onecall:pa25:v1:db=<database_oid>:role=<role_oid>:slot=<slot>:name=<login_name>`, with no whitespace/newline, canonical decimal OIDs, the actual current database OID, actual login OID and validated chosen name. Fixed slots are sharing_login → sharing_query, customer_login → customer_context, command_login → sharing_command. Only the trusted provisioner constructs this value, after creating a missing role, using safe identifier/literal quoting. It writes the comment before granting any membership in that same transaction.

For every existing external login, apply and check read `shobj_description(role_oid, 'pg_authid')` / the corresponding shared `pg_shdescription` entry and compare byte-for-byte to the value reconstructed independently from the current catalog identity and requested slot/name. All three existing-role checks precede any creation, attribute reaffirmation, comment write or membership grant. A compatible unrelated role with no marker rejects, even with apparently approved edges. Missing/empty/malformed/version-mismatched or altered markers reject; a copied marker fails if database/role/slot/name differs. Check requires all roles/edges; apply may create absent roles or repair approved missing edges only with valid provenance. Replay never rewrites an existing comment. The postcondition repeats marker validation.

No existing-role adoption is supported. Direct-login and NOLOGIN migration roles retain their frozen migration/history provenance and receive no PA25 marker. Marker fields are nonsecret but are not emitted in success/error diagnostics. Shared comments bind the external login to one database; role renames, OID-changing restores and drop/recreate are not automatic replay/adoption paths.

### Failure and rollback semantics

- Missing or invalid provenance rejects both modes using only `PA25_PROVENANCE_REJECTED`; expected/actual comments, OIDs/names and raw SQL/exception details are not printed.
- Global preflight rejects unrelated compatible roles before any DDL, even if an earlier external slot is absent. An existing role's comment is never overwritten or reconstructed as a repair.
- Any later failure rolls back created roles/comments, attribute reaffirmations and membership grants together. Check has no persistent mutation. No ACL/history change is permitted in either mode.
- Stop rollout and escalate marker loss/collision/tampering to Platform/Database/Security. Do not auto-adopt, restamp, revoke, rename, drop, rotate credentials or replay old migrations. External recovery requires separate disposition and does not authorize an override here.

## 5. Test-method mapping — still exactly twenty

| Existing method number / name | Added or clarified proof |
| --- | --- |
| 1 / `createsExactIdentityGraphAndReplaysWithoutDrift` | Three exact creation-time markers independently reconstructed from catalog facts; unchanged bytes/OIDs across replay and password handoff. |
| 10 / `missingAndStaleMembershipsFailCheckAndOnlyAbsentEdgesAreReconciled` | Missing-edge repair remains permitted for genuinely script-created marked logins only; marker bytes preserved. |
| 18 / `invalidInputsHistoryAndAuthorityRejectAtomically` | Loop each external slot through a compatible unrelated existing login with no marker/zero edges and separately apparently approved edges. Both modes reject; include an earlier absent slot. Compare exact pre/post OIDs, attributes, opaque passwords, comments, memberships, ACLs and history in memory, proving no creation or mutation. Also cover lost/empty/malformed/wrong-version/copied markers, rename and drop/recreate with an old marker. |
| 19 / `frozenMigrationsLogicalAclsAndHistoryRemainUnchanged` | Only the three newly created external roles gain comments; all other comments, migration/history/300-tuple ACL facts preserved. |
| 20 / `credentialsEndpointsAndSqlDiagnosticsAreRedacted` | Provenance rejection is an explicit failure category; comments and their identity fields never leak through diagnostics. |

The other fifteen methods and all twenty names/order are unchanged. These are future ordinary methods with internal loops, not twenty-one methods or parameterized expansion. PostgreSQL/runtime assertions remain proposed implementation acceptance, not executed evidence from this documentation-only fix.

## 6. Preserved invariants and static validation

- Thirteen V001–V013 raw-byte SHA256 values recomputed against §10: **13/13 PASS**. No V014 or migration write.
- §3 graph/configuration contract compared verbatim to the pre-fix plan: **PASS**. Six logins, seven effective roles, ten managed nodes, four ADMIN FALSE / INHERIT FALSE / SET TRUE edges unchanged.
- §§5.2–5.3 compared verbatim: **PASS**. Exact seven-role 300-tuple ACL oracle, PUBLIC/default/database rules unchanged; comments are metadata outside ACL normalization. No object ACL modification, password/secret, helper schema/table, runtime Java/config, seventh datasource or identity platform added.
- §7 compared verbatim: **PASS**. Exactly six future implementation paths, one S1, unchanged CI/POM instructions and no-V014/runtime-change prohibitions.
- §6.2 named-method extraction compared to pre-fix: **PASS**, exactly 20 unique sequentially numbered methods, identical names/order; no parameterized expansion.
- Read-only source/POM scan using CI's method and annotation word-boundary regexes: **PASS**, current 36/309 Surefire + 13/214 Failsafe, 49 classes/523 methods, 13 includes, 524 annotation lines. Planned delta remains [309,234] = 543, 50 classes, 14 includes, annotation scan 544. No XML/runtime result is claimed.
- Initial exploratory annotation substring counting returned 538 and failed its assertion (exit 1), because it also counted longer annotation-name substrings. Corrected to the checked-in CI `@Test` word-boundary rule, not a source/test change; the full corrected static pass exited 0 and confirmed 524 → 544. This is a validator correction, not an inventory exception.
- Strict UTF-8 decoding, LF-only/final LF and no trailing whitespace: **PASS** for both task documents. Changed text manually reviewed and scanned for credential assignment/private-key/verifier patterns: no real credential or secret introduced; marker values are grammar placeholders only.
- Full repository file-content manifest taken before editing and compared after both writes: **PASS**, exactly the two allowed paths changed/created; Control/review/private/product files byte-preserved, no repository launcher/log/cache created.
- Git limitation: native worktree commands fail exit 128 because `.git` points to Windows `D:/codex-call/...`, unresolved from Linux. No pointer/index repair. Read-only `GIT_OPTIONAL_LOCKS=0 git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call --work-tree="$PWD" diff --check` succeeds exit 0; it covers tracked changes only, since the task documents are untracked at entry.
- Supplemental `git -C /tmp diff --no-index --check` checks use the saved pre-fix plan versus the final plan and `/dev/null` versus the new fix artifact. No whitespace diagnostics; exit 1 is Git's expected content-difference status, not a whitespace failure. Independent strict-byte whitespace checks pass. No-index execution from the broken worktree initially failed exit 128; running from `/tmp` avoids discovery of that pointer.
- No Maven, Docker, psql, JDBC, credentials, live database, runtime tests, staging/commit/push/merge/approval/ready/reviewer request or external comment used. Final static validation command exits are handoff evidence only, not a fabricated runner exit.

### Final artifact identity

Final cumulative plan: **64,558 bytes**, SHA256 `02635A174F9D2ADD867951463284B29F6AB518E4FE102B1AB3F483F05C1BA940`. Final fix-artifact bytes/SHA256 are measured after this artifact is complete and reported in the completion response; no impossible self-referential artifact digest is embedded here.

## 7. Residual risks / open questions and owners

| Residual / question | Owner and disposition |
| --- | --- |
| Catalog-writing administrator can forge an exact matching nonsecret comment; it is not cryptographic proof against a malicious superuser. Cooperative advisory lock does not stop unrelated superusers. | Platform/Database/Security: custody/audit of administration authority and exclusive cluster-wide change window. Existing trust boundary, no new identity platform. |
| Unmarked compatible existing name, lost marker, rename or OID-changing restore cannot be auto-adopted/repaired. | Platform/Database/Security: stop deployment, investigate and provide separate external disposition. Absent approved names preserve first-time apply. No marker override in PA25. |
| Runtime SQL/psql precision and negative snapshot proofs have not run in this static fix gate. | Backend implements within existing S1/twenty methods; independent reviewer rechecks this candidate before authorization. |
| Other initial review questions / external credentials, TLS, pg_hba, cluster and live acceptance. | Existing Control adjudication and plan owners remain unchanged; deliberately not reopened or resolved by this fix. |

A OQ-1 is explicitly answered by creation-only role-comment provenance with no adoption. No new blocking authoring question remains within accepted `PA25-PLAN-A-01`; no claim of live production acceptance is made.

**PA25-PLAN-A-01 fixed candidate; independent re-review required**

**STOP BEFORE RE-REVIEW**
