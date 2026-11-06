# PA17 Sharing Public Admission And Ticket — Plan Fix 11

## Identity and status

- **Work unit:** `PA17-sharing-public-admission-ticket`.
- **Gate:** bounded `fix` after Controller runtime discovery failure `PA17-IMPL-01`.
- **Task label:** `pa17-public-admission-ticket-plan-fix11-gpt-20261107`.
- **Corrected plan checkpoint:** `660989196b411d2aaa4086a4bea1550f3d6f4a5e`.
- **Status:** `plan-fix-complete`.
- **Authorized writes:** only the target plan and this fix-11 artifact. No source, test, POM, migration, OpenAPI, CI, README, Control, review, or Git-state file was edited.

## Input and output identities

| Plan | Lines | Bytes | SHA-256 |
|---|---:|---:|---|
| Input, fix-10 checkpoint | 404 | 106,414 | `B8D0B99D639C5D99DF06A5B4035D76E7E626960E5C1414E14D656DED21711025` |
| Output, fix-11 | 406 | 107,832 | `6c00949299eacef2a37f3723b450fab77b8f8f341d279a620632713a8f262001` |

The input plan's measured line count, byte count, and SHA-256 matched the supplied identity before edits. Git metadata was not independently inspectable in this shell; no Git-state claim is made.

## Runtime discovery finding

The accepted full-host runtime finding is: Surefire ran 103/103; Failsafe discovered only the 130/130 baseline methods. `PublicShareAdmissionIT` (24 methods) and `SharingAdmissionRoleLeastPrivilegeIT` (8 methods) were not discovered because `backend/pom.xml` `container-smoke` hard-lists seven suites. This plan fix does not rerun Maven or edit the POM.

## Exact changed sections

- **§1:** identifies fix-11 and the accepted runtime-discovery gate.
- **§4 allowlist:** updates 37 to 38, keeps items 1–37, and adds item 38 as `backend/pom.xml`. Under the existing `container-smoke` Failsafe `<includes>`, retain all seven entries and add exactly `**/PublicShareAdmissionIT.java` and `**/SharingAdmissionRoleLeastPrivilegeIT.java`. POM is the sole path added beyond the prior 37. No test path or method is added.
- **§4 boundary:** removes the former POM exclusion and limits the implementation-gate POM change to those exact two includes.
- **§9 validation/history:** records the 37 -> 38 delta, unchanged 8 API + 10 implementation/configuration = 18 sharing production files, unchanged `PUBLIC_ADMISSION_FILES=8` and `ADMISSION_IMPLEMENTATION_FILES=10`, unchanged 12/24/8 = 44 additions, unchanged 20/103 + 9/162 = 265 and baseline 19/91 + 7/130 = 221. Adds actual Failsafe discovery of nine suites / 162 methods as the required acceptance check.
- **§9A:** preserves the full fix-10 replay-policy correction and adds fix-11's discovery correction as authoritative; items 1–37 and all prior contracts remain unchanged.
- **§10:** reports fix-11 status, artifact, allowlist delta, and runtime-discovery requirement.

## Bounded validation

- Parsed the numbered §4 allowlist: exactly 38 entries in sequence, with `backend/pom.xml` at item 38.
- Confirmed item 38 is the only new path beyond the prior 37; items 1–37 remain unchanged.
- Confirmed the two named production sets remain 8 and 10 files (18 total); no test path or method was added.
- Confirmed plan arithmetic remains 12/24/8 = 44 new methods, 20/103 + 9/162 = 265, and baseline 19/91 + 7/130 = 221.
- Confirmed no POM-forbidden statement remains. Historical fix-5's 36 -> 37 delta is identified as historical rather than a current allowlist claim.
- Actual Failsafe discovery must be validated at the later authorized implementation gate as nine suites / 162 methods; source/CI inventories alone do not satisfy this check.
- No Maven, PostgreSQL, staging, commit, push, PR, approval, ready action, reviewer request, external comment, or re-review was performed.

## Stop boundary

Stop after these two documents. Fresh A/B review follows Controller intake. No implementation or external workflow action is authorized by this fix.
