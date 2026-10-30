# PA7 V007 Aggregate Fix 1

- Task label: `pa7-s2impl-mimo26flash-20261030-135416`
- Runtime / provider / model: Linux WSL2; Claude Code harness; MiMo-V2.6-Flash (implementation author)
- Author role: implementation author only — no review, merge, or gate authority
- Date: `2026-10-30`; workspace `/mnt/c/Users/Administrator/.codex/worktrees/pa7-v007-activation-consent/codex-call`
- Binding inputs: accepted B Finding 1 in `docs/reviews/code-review-20261030-133045.md`; binding plan `docs/gateflow/pa7-v007-activation-consent-foundation-plan.md` §4.7

## 1. Changed paths (exactly three allowed writes)

| Path | Action |
| --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | modified (+42/−3: ACL negatives + one comment; 51 `@Test` unchanged) |
| `docs/gateflow/pa7-v007-activation-consent-foundation-s2-implementation.md` | modified (single typo: resume revision `1/2` → `2/3`) |
| `docs/gateflow/pa7-v007-activation-consent-foundation-aggregate-fix-1.md` | new (this report) |

No CI, README, controller, plan, or SQL file written. No GRANT/REVOKE issued; no privilege granted or changed.

## 2. Fix 1 — operations_iam table/column negatives (B Finding 1)

- In `assertV007SchemaUsageMatrix`, schema USAGE/CREATE pins stay unchanged; a new block enumerates every `operations_iam` BASE TABLE under the migration identity via `information_schema.tables` with a non-empty assert (currently `operations_iam.employee_account`).
- Per table: `has_table_privilege` = false for SELECT/INSERT/UPDATE/DELETE/TRUNCATE/REFERENCES/TRIGGER.
- Per actual column (`information_schema.columns`, ordinal order): `has_column_privilege` = false for SELECT/INSERT/UPDATE/REFERENCES.
- Zero is asserted despite the schema USAGE denial: `has_*_privilege` resolves the object as the migration identity (superuser `onecall`) and reports the target role's ACL, so a leaked grant fails the gate even without name resolution.

## 3. Corrections 2–3 (two directly identified inaccurate claims)

- Consent check probes: comment now states probes may run under the migration identity after check isolation (CHECK enforcement is role-independent); the old claim that non-overlapping probes "run as runtime role" is true only before the first savepoint isolation.
- S2 report: resume positive control uses from/to revision `2/3` (code sets from=2, to=3), not `1/2`. Historical S1 reports preserved untouched.

## 4. Runtime proof (actual)

| Step | Command | Result |
| --- | --- | --- |
| Compile | `/opt/maven-3.9.9/bin/mvn -B -ntp -DskipTests test-compile` | BUILD SUCCESS |
| Final full verify (exactly one) | `/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke verify` | Surefire 10/0/0/0; Failsafe 51/0/0/0; BUILD SUCCESS (30.756 s; PostgreSQL 16.15 Testcontainers) |

No `#` method filters needed or attempted. V001–V007 migrations untouched; the 51 method names remain source/CI/XML-identical.

## 5. Denied commands (honest record)

- `cd <repo> && git status --porcelain ... && git diff --stat ...` — denied by the compound cd+git guard; substituted plain `git` invocations from the current directory, which succeeded.
- Shell redirection of verify output to `/tmp/...log` — blocked by the harness write scope; substituted harness-captured background execution. No denied command was retried unchanged.

## 6. Stop

Stopped after: code + tests green and artifact written. No stage/commit/push/PR, no self-review entry, no next-gate action; the controller will independently validate.
