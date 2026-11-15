# PA34 Operations activation history read foundation - final closeout

## Delivery

PA34 delivered exactly one authenticated Operations read route:

- `GET /api/v1/operations/activations`

The endpoint returns the frozen `ActivationPage` with an exact eleven-field
`ActivationSummary` projection. It uses the independent opaque employee session
and exact audience, requires a current server-derived ACTIVE employee with fixed
role `ADMIN` or `OPS`, validates only optional `cursor|limit`, and uses stable
`occurredAt desc, transitionId desc` keyset pagination. Cursors are opaque,
integrity-protected and endpoint/sort-specific; invalid cursors are sanitized
`CURSOR_INVALID` while missing configuration fails closed.

The request performs one read-only PostgreSQL `REPEATABLE_READ` snapshot through
an owner-local activation reader with exact column projection and asserted /
restored owner roles. It creates no activation, consent, trial, grant, balance,
audit or Outbox write. V001-V017, OpenAPI, docs/14, docs/15 and docs/17 remain
byte-frozen; no V018 or new grant exists.

The route preserves exact concealment for other Operations methods/paths and
uses Boot 4.1.2's effective `spring.web.error.path` for the ERROR-dispatch
boundary. The removed `server.error.path` is not treated as effective.

Reviewed product/test head:

`aa8fd29b7c608ceda1da113432e27a56d660c601`

Draft PR:

https://github.com/zh94695424/one-call/pull/45

## Verification

- Focused six Surefire classes: `60/60`; two PostgreSQL16 Failsafe classes:
  `28/28`; zero failures/errors/skips.
- Serial full clean PostgreSQL16: `76/704` Surefire + `31/504` Failsafe
  = `107/1208`; zero failures/errors/skips.
- Exact source=CI=POM=XML: 88 PA34 identities (`60+28`), source1208,
  `@Test` token lines1209 and POM includes31; focused/full XML names are exact.
- All21 frozen raw/LF hash entries match; exactly V001-V017 exist; no V018.
- Packaged JAR, default/web custom-error probes, Spring-key-only activation
  probes, PA11 dependency firewall and accepted A1-A5 commands pass.
- Hosted run `43719752829` on reviewed head `aa8fd29`:
  - `contract-regressions`: PASS in35s, including A1-A5.
  - `java-runtime`: PASS in22m49s, including build, module checks, full
    container smoke, JAR smokes and dependency firewall.
- Final branch and PR refs both resolve `aa8fd29`; PR #45 remains OPEN/Draft.

## Reviews and finding status

- Plan A/B reviews and re-reviews closed `PA34-PLAN-REV-01..06` and the bounded
  plan correction cycle.
- Code A/B reviews and re-reviews closed `PA34-CODE-REV-01..04` and
  `PA34-REREVIEW-01..02`.
- Aggregate deepreview found no material composed defect.
- Initial PR reviews accepted `PA34-PR-REV-01`, `02`, `04`; `03` was
  rejected-with-reason. The fixes and fresh re-review closed the accepted items.
- Second PR re-review closed `PA34-PR-REREVIEW-01..03`; `04` remains
  rejected-with-reason.
- Final re-review closed `PA34-PR-REREVIEW2-A-M01`, `A-L02` and `B-L01`.
- `PA34-PR-REREVIEW3-A-EOL01` is rejected-with-reason: the exact raw candidate
  bytes are the reviewed/tested identity; Git's normalized patch and stored blob
  are a representation layer, with no runtime, contract or security effect.
- `PA34-PR-REREVIEW3-B-01` is deferred-with-owner: consuming the bound
  `WebProperties.Error` should be considered in later Platform/Backend
  hardening. Current Boot4.1.2 behavior is correct and pinned against the bound
  path by real servlet/IT evidence.

No accepted finding remains unfixed. No blocking open question remains for the
confirmed PA34 goal.

## Residual risks and owners

| Residual | Owner / destination |
| --- | --- |
| Mixed-EOL source representation is not reproduced byte-for-byte by the normalized diff patch | Backend/gate maintainers; exact raw bytes and normalized Git blob remain separately identified |
| ERROR registration re-derives the supported property/default instead of consuming bound `WebProperties.Error` | Platform/Backend post-PA34 hardening |
| Unchanged customer error controller still uses removed `server.error.path`; custom-path CI coordinates both keys | Platform/Backend configuration and CI hardening |
| PA33 subject cursor still classifies every checked JCE failure as400 while PA34 classifies checked provider failures as503 | Backend/Security separately authorized PA33 correction |
| Non-default JCE provider could classify availability faults differently from SunJCE | Backend/Security provider deployment decision |
| Global activation order has no supporting global index; statement timeout/request budget return sanitized503 at scale | Backend/DBA + Operations capacity/index work; V018 needs new authorization |
| Corrupt/domain-impossible persisted facts fail the whole page with no repair path | Backend/DBA + Operations remediation |
| `REPEATABLE_READ` is snapshot-relative; continuation pages use fresh snapshots | Backend/Security accepted read semantics |
| Production employee verifier/session/MFA, runtime credentials and cursor-ring custody/rotation are external | Security/Operations IAM/DBA deployment readiness |
| Local development toolchain reports existing npm vulnerabilities/deprecations; hosted action versions emit deprecation warnings | Backend tooling + Platform/CI maintenance; no dependency or workflow change authorized here |

Production readiness, live IdP/key provisioning, arbitrary-cardinality success
and live Operations acceptance are not claimed.

## Issue and remote state

PA34 is not tied to a GitHub issue, so no issue link, closing keyword or
closeout comment is required. No issue mutation occurred.

PR #45 remains OPEN and Draft. No merge, approval, mark-ready action, reviewer
request, external comment or branch deletion was performed. The branch
`codex/pa34-next-work-unit` is based on merged PA33 main
`e423b4b7e3f0d673bd2ad3fa579c36ca585af13c`.

## Completion and next entry point

PA34 is complete at `final closeout pass`. Its controller-only closeout commit
contains this summary and the final Control update and does not alter the
reviewed product/test bytes.

After the user merges PR #45, fetch the updated `main`, create a fresh managed
worktree and `codex/` branch from that merge commit, reread
`docs/gateflow/project-execution-control.md` and applicable design sources, and
run a fresh Phaseflow goal confirmation for the next bounded work unit. No next
work-unit scope is defined or authorized by PA34; do not start from this
unmerged branch or reuse PA34 goal confirmation.
