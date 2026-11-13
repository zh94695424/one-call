# PA31 Customer Personal Subject Activation Foundation — Final Closeout

- Work unit: `PA31-customer-personal-subject-activation-foundation`
- Type: architecture-sensitive authenticated customer command over existing PostgreSQL persistence
- Branch: `codex/pa31-next-work-unit`
- Base: `main` at `210e8b31667c39fb24641ac38b4571c6b599b6db`
- Draft PR: #42 `https://github.com/zh94695424/one-call/pull/42`
- Accepted PR review head: `4ca81281f9072330e8e02d2b3a4c5067ea8fd964`
- Status: `final closeout pass`; production readiness remains NOT CLAIMED

## What changed

Implemented only `createCustomerSubjectActivation`:

`POST /api/v1/customer/subjects/{subjectId}/activations`

The command now:

- derives account, channel and selected personal/organization workspace from the verified session;
- enforces personal ownership or active organization membership and the frozen customer subject-access roles;
- validates the exact six-field body against the one enabled/published FIRST-TRIAL policy snapshot;
- creates one ACCEPT consent, one `INACTIVE -> ACTIVE` revision transition and one permanent FIRST-TRIAL in one transaction;
- returns the frozen six-field result with original trial units/expiry;
- commits a replayable DECLINE consent and returns `409 ACTIVATION_CONSENT_DECLINED`;
- replays exact events, rejects same-event semantic conflicts with `409 CONSENT_REQUIRED`, and never reactivates a customer SUSPENDED subject;
- uses the existing same-connection `activation_consent_role` ↔ `entitlement_metering_role` owner handoff and existing privileges;
- bounds activation request input to 8 KiB before JSON parsing and preserves exact `422 VALIDATION_FAILED` behavior.

OpenAPI, docs/14, docs/15 and V001–V017 are byte-identical. No V018, new grant, provider/KMS, production credential or live acceptance was added.

## Verification

- Clean PostgreSQL16: 59 Surefire classes/520 methods + 25 Failsafe classes/404 methods = 84/924, zero failures/errors/skips.
- Focused activation PostgreSQL16: 34/34.
- Source=CI=XML 84/924; annotation lines 925; POM Failsafe includes 25.
- Default/web packaged JAR smokes passed, including activation 401/503 probes.
- Dependency firewall passed exact 72 resolved/65 packaged inventories and 116 adversarial probes.
- OpenAPI `check:s3` passed all lint/generation/typecheck/doc-map checks.
- Frozen20 raw/Git checks passed; exactly V001–V017 with no V018; exact 40 implementation paths.
- Independent code review A/B and re-review A/B completed peer-free. Final re-reviews were both `pass`, zero findings.
- Aggregate GPT deepreview over the accepted 50-path range returned `pass`, zero material findings.
- Hosted final PR head checks: `java-runtime` passed in 23m47s and `contract-regressions` passed in 36s (run `43408716541`).

## Finding status

- `PA31-PLAN-REV-01..06`: closed.
- `PA31-CR-A-01` organization/cross-organization HTTP isolation: `已修复`.
- `PA31-CR-A-02` exact success value/type contract: `已修复`.
- `PA31-CR-A-03` bounded request body: `已修复`.
- Aggregate deepreview: no material finding.

No accepted finding or blocking open question remains.

## Residual risks and owners

| Residual | Owner / destination |
| --- | --- |
| Production FIRST-TRIAL policy publication/content and any future publisher conformance | Operations/product policy owner |
| Dedicated activation login, two non-inherited SET memberships, credentials, TLS, rotation, UTC, pool drain | Platform/Database/Security/Operations |
| Descriptor/anchor/original-trial corruption detection and incident response | Database/Operations; `INTERNAL`, no repair/synthesis |
| Historical replay must not be interpreted as live authorization while current state is SUSPENDED | Client/product; Operations owns resume |
| Deployment-wide body/rate/read-timeout protections beyond this route-local 8 KiB bound | Platform/Security |
| Production provider/KMS, policy management, UI, IVR confirmation and live acceptance | Separately authorized future owners/work units |

## PR and issue state

PR #42 is Open and Draft against `main`. The workflow did not merge, approve, mark ready for review, request reviewers, send an external comment or mutate an issue.

This work unit is not tied to a GitHub issue; issue link, closing keyword and closeout comment are not applicable.

## Next entry point

After the user reviews and merges PR #42:

1. fetch the updated target `main`;
2. create a fresh managed worktree and `codex/` branch from that merge commit;
3. reread this Control and the applicable design sources;
4. run a fresh Phaseflow goal confirmation for the next bounded work unit.

No next work unit is authorized or defined by this closeout.
