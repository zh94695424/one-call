# PA34 Operations activation-history read foundation - draft PR readiness

## Status

- Work unit: `PA34-operations-activation-history-read-foundation`
- Gate at the original 2026-11-15 pre-push checkpoint: `ready-to-open-draft-PR`
- Branch: `codex/pa34-next-work-unit`
- Target base: `main` at merged PA33 `e423b4b7e3f0d673bd2ad3fa579c36ca585af13c`
- Accepted slice: `e48e830a7625fa57b244a675165df48c47b2060d`
- Accepted aggregate deepreview: `3d7de3a68b96cb9496ac0f850829356a3ac3ce0a`
- Historical remote status at that pre-push checkpoint on 2026-11-15: no PA34 push or Draft PR existed; the readiness evidence below records that local checkpoint, not future remote knowledge.
- PR-fix annotation (2026-11-15): Controller subsequently recorded branch `codex/pa34-next-work-unit` pushed and Draft PR #45 OPEN against `main`, reviewed remote head `a12d9bae70a864af8a72b2cda657a33c4674c069`. This annotation reports Controller evidence, not a new remote query or action; it does not relabel the historical verification below as hosted evidence.

## Delivered scope

- exactly `GET /api/v1/operations/activations`
- exactly `ActivationPage(items, nextCursor, hasMore)`
- exactly eleven `ActivationSummary` fields:
  `transitionId|subjectId|statusEventId|fromStatus|toStatus|fromRevision|toRevision|sourceChannel|sourceActionDomain|reasonCode|occurredAt`
- existing opaque employee session with independent audience
  `onecall:employee-session:v1`, current ACTIVE server-derived `ADMIN|OPS`
- stable `occurredAt desc, transitionId desc` seek pagination with `limit+1`
- endpoint-purpose AES-GCM cursor over the existing shared subject-read key ring
- one authoritative read-only PostgreSQL `REPEATABLE_READ` snapshot
- activation-owner eleven-column SELECT with asserted/restored owner roles
- five domain-legal transition shapes accepted; four database-shape-only
  combinations fail the whole page closed with sanitized 503
- no writes, audit fact, Outbox event, lock retention, business mutation or cursor
  table
- no OpenAPI, docs/14, docs/15, docs/17, V001-V017, schema, grant or migration
  delta; exactly V001-V017 exist and no V018 exists

The 24 authorized implementation paths contain 23 code/build paths plus the
implementation report. The cumulative branch diff also contains the accepted
plan/fix/review/Control artifacts. No generated `target`, XML, JAR, class,
`node_modules`, `.codex` or private `.pa*` path is tracked.

## Verification

- Focused PA34 Surefire: `60/60`, zero failures/errors/skips.
- Focused PostgreSQL 16 Failsafe: `28/28`, zero failures/errors/skips.
- Full clean `container-smoke verify`: `704/704` Surefire + `504/504` Failsafe
  = `1208/1208`, zero failures/errors/skips, BUILD SUCCESS.
- Exact source=CI=POM=XML: 76/704 +31/504 =107/1208 identities; 88 PA34
  identities; token lines1209; POM Failsafe includes31.
- Canonical LF frozen guard: architecture `1/1` + PostgreSQL `1/1`; 21 LF Git
  blob hashes match the frozen constants.
- Main worktree: all 21 raw frozen hashes match the accepted plan; exactly
  V001-V017/no V018.
- Packaged default/custom-error JAR probes: 120/120 pass.
- Production dependency firewall: 116/116 adversarial probes pass.
- Accepted A1-A5 stage commands pass in the authorized LF projection.
- Final independent code re-review and aggregate deepreview A/B both returned
  `pass-with-risks` with no material finding.

## Findings and residual ownership

- `PA34-CODE-REV-01..04`: fixed and re-reviewed.
- `PA34-REREVIEW-01..02`: fixed and re-reviewed.
- `PA34-FIX-HYGIENE-01`: rejected as a blocking finding after content, HEAD and
  staging verification; no Git history or index repair was performed.
- B-F04, broader Operations TRACE concealment and global route-literal
  centralization: deferred to later Operations route-boundary/contract work.
- Platform/Backend: coordinate `server.error.path` with Spring Boot
  `spring.web.error.path` for non-default error destinations; root/trailing-slash
  path policy remains deployment-owned.
- Backend/DBA: global order has no matching global V007 index; statement timeout
  and request budget may yield sanitized 503 at scale. Corrupt/domain-impossible
  history fails the whole page and requires a later remediation runbook.
- Backend/Security: revocation is snapshot-relative and continuation pages use
  fresh snapshots.
- Security/Operations IAM/DBA: production employee verifier/session/MFA,
  runtime role credentials and shared cursor-ring custody/rotation remain
  external prerequisites with fail-closed behavior.
- A1-A5 are LF-projection evidence; hosted Actions and live acceptance are not
  claimed. Existing npm development-tool vulnerabilities remain separately
  owned.

## Proposed Draft PR metadata

Title:

```text
PA34: add Operations activation history read foundation
```

Body:

```markdown
## Summary

Adds the single frozen Operations activation-history read endpoint
`GET /api/v1/operations/activations`.

- exact three-field page and eleven-field activation summary
- current ACTIVE ADMIN/OPS through the existing opaque employee session
- stable cursor pagination and one read-only Repeatable-Read PostgreSQL snapshot
- owner-local read with no writes, audit, Outbox, schema or grant delta
- frozen V001-V017 and OpenAPI/docs remain unchanged

## Validation

- focused Surefire `60/60` and PostgreSQL Failsafe `28/28`
- full clean PostgreSQL 16 `704/704 + 504/504 = 1208/1208`
- exact source=CI=POM=XML identity inventory
- 21 frozen raw/LF inputs and exact V001-V017/no V018
- packaged JAR, firewall and accepted A1-A5 checks
- independent code review and aggregate deepreview re-review passed

Production readiness is not claimed.

## Residual risks

- Platform/Backend owns non-default `server.error.path` /
  `spring.web.error.path` coordination and root/trailing-slash policy.
- Backend/DBA owns global-sort capacity/timeouts and corrupt-history remediation.
- Backend/Security owns snapshot-relative revocation semantics.
- Security/Operations IAM/DBA owns production verifier, credentials and cursor-key
  custody.
- B-F04 remains deferred to later Operations route-boundary/contract work.
```

## Remote-action boundary

At the original pre-push checkpoint the branch was locally ready for the proposed
Draft PR above, with remote actions awaiting explicit authorization. Controller
later authorized and recorded the push and creation of Draft PR #45. The current
bounded author-fix gate authorizes no stage, commit, push, PR update, merge,
approval, mark-ready action, reviewer request, external comment or re-review.
Stop for Controller; this historical readiness artifact grants no remote permission.
