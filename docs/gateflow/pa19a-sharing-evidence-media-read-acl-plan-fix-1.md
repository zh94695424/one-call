# PA19A sharing evidence media read ACL plan fix 1

Task: `pa19a-sharing-evidence-media-read-acl-plan-fix1-gpt-20261108`  
Gate: `fix`  
Runtime/provider: Codex/GPT `gpt-6.1-sol`, requested reasoning effort `high`  
Binding Control: current PA19A section, including adjudication lines immediately before this gate.  
Input plan SHA-256: `D0AEF36E60F2B5A2063B37B823C5D2B3D2B7EE5DA72E25F1608A538CDF808DEB`.  
Inputs: Review A `DEF1CA0B7CFB128FB4610CBE0B7958B29E3EF2A373648E6BD77A2566E494EAAF`; Review B `781ECA7D8214B84C586F76675F9F7B5FC6DB4CC9B12BBFDE60AD81084D497393`.

## Accepted findings and decisions mapped

| Finding / decision | Status | Fixed plan location | Resolution |
|---|---|---|---|
| PR-01 migration witnesses | accepted / fixed | “Migration witnesses and inventory update sites”; allowlist rows 13–19 | Added `SharingQueryServiceIT.java` and `PublicShareAdmissionIT.java`; explicit 12→13; preserved V012 index-11 assertion; pinned PostgresContainer post-V008 4→5, final V013 history/script lists, and exhaustive role tuple. |
| PR-02 unconditional POM and Failsafe | accepted / fixed | allowlist row 20; “Source = CI = XML arithmetic”; “Commands” | POM unconditional; both named IT includes mandatory; exact `-Pcontainer-smoke verify` command used for PostgreSQL integration. |
| PR-03 Missing/Unavailable query | accepted / fixed | “Fixed decisions” §4; reader proof | Explicit `evidence_record` anchor and outer joins; predicates stay in `ON`; zero rows alone is Missing; any existing invalid/missing/ambiguous relation maps Unavailable. |
| PR-04 remove storage-location access | accepted / fixed | “Fixed decisions” §2, §4; privilege matrix; probes | V013/query/probes/API omit `bucket`, `object_key`, `object_version_id`; opaque reference is media-object UUID only. |
| PR-05 exact unconditional 22 paths | accepted / fixed | “Module boundary”; exact allowlist | Removed evidence/internal/orchestration package-info edits and `SharingQueryBoundaryTests`; retained actual ModuleBoundaryTests assertion; exactly 22 unconditional paths. |
| PR-06 complete CI arithmetic | accepted / fixed | “Source = CI = XML arithmetic”; “Commands” | Pins 22/128, 11/192, 33/320; baseline 112/178 (290) to 128/192 (320); line scan 291→321; early Surefire 119→128; includes 9→11; each new IT exactly 8 methods; historical PA18 text remains historical. |
| A-Q1 migration inventory meaning | accepted / fixed | “Migration witnesses and inventory update sites”; arithmetic | Migration evidence is the enumerated source assertions; no nonexistent CI migration script is assumed. Dependency firewall is separate. |
| A-Q2/B-Q2 CI totals | accepted / fixed | “Source = CI = XML arithmetic” | Directly pins every derived total and echo-server count rather than asking implementer to recalculate. |
| A-Q3/B-Q3 query join semantics | accepted / fixed | “Fixed decisions” §4 | One unambiguous LEFT JOIN shape and result cardinality mapping. |
| B-Q4 null exception | accepted / fixed | “Fixed decisions” §3; reader tests | Null UUID throws `NullPointerException` via `Objects.requireNonNull` before DB access. |
| B-Q5 migration preconditions | accepted / fixed | “Fixed decisions” §8; privilege matrix | Explicit PG16/superuser/V012 preconditions and pre/postcondition blocks, safe compatible replay, deterministic incompatible-role failure. |
| A-Q4 hash consistency | deferred-with-owner | “No unsupported integrity claim”; residual risks | No unsupported recording hash/subject cross-check; later Evidence service/integrity slice owns it. |
| A-Q5 external credentials | accepted / fixed | “Fixed decisions” §6; commands | Explicit qualified properties, fail-closed behavior, external deployment provisioning, no secret and no persistence YAML change. |
| A-Q6 Missing/Unavailable consumer mapping | deferred-with-owner | residual risks | Later PA19 plan/implementation owns playback outcome mapping. |
| A5 orchestration narrowing | accepted removal | “Module boundary”; minimality demonstration | `ai_orchestration/package-info.java` remains untouched; no consumer exists in this unit. |

## Other required precision closure

- V013 precondition/role/grant/postcondition matrix, default ACLs, grant options, role settings, memberships/dependencies, PUBLIC/effective privileges, and actual-role test method are fixed under “V013 exact privilege matrix and assertions” and “Reader and configuration proof.”
- API, DTO constraints, read-only qualified configuration, redaction/toString/reflection/serialization assertions, safe columns, and exact query outcome mappings are fixed under “Fixed decisions” and “Reader and configuration proof.”
- One composed slice, stop condition, no sharing-admission widening, V001–V012 byte identity, residual owners, exact commands, and completion report are explicit in the fixed plan.

## Goal-drift determination

No goal expansion occurred. The accepted user goal, dedicated reader role, one privilege-only migration, evidence-owned fail-closed seam, one composed slice, and unchanged PA19/sharing boundaries are preserved. The edits remove unused location privileges and optional module edits while adding only witnesses and build/CI updates required to make V013 and both Failsafe tests executable and verifiable. No source, test, migration, POM, CI, Control, or review input was modified in this fix gate.

## Gate completion

Only the fixed plan and this mapping artifact are authorized outputs. Stop after document validation. No implementation, commit, push, PR, review dispatch, approval, readiness action, or external comment is authorized.
