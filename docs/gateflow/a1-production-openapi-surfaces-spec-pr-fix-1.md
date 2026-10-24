# A1 PR fix 1: canonical claim verification timestamp

- Task label: `a1-openapi-pr-fix1-ds-20261024-01`
- Work unit / gate: `A1-production-openapi-surfaces-spec` / `PR fix 1`
- Finding: **F-PR-01** from `docs/reviews/code-review-20261024-234446.md`
- Decision: controller accepted the finding and chose the canonical wire name `ClaimResult.verifiedAt` (rename of the optional `acceptedAt` alias).
- This artifact is not acceptance, a review, a merge, a PR mutation or a runtime claim.

## 1. Finding and chosen mapping

F-PR-01: optional `ClaimResult.acceptedAt` was an undocumented alias for A0
`subject_number.subject_claim.verified_at`, while sibling `ClaimSummary` already
exposed the same fact as `verifiedAt`. A0 has no `accepted_at` column.

Chosen canonical mapping (one name, no retained alias, still optional):

```
ClaimResult.verifiedAt  <-  subject_number.subject_claim.verified_at
```

- Optional, not required: the field is absent for non-`ACCEPTED` results,
  consistent with nullable A0 `verified_at` and
  `ck_claim_verified_shape = ((status = 'ACCEPTED') = (verified_at IS NOT NULL))`
  (`docs/14` §4.14, lines 899/907).
- When present: non-nullable `type: string`, `format: date-time`.
- Historical accepted plan (`docs/gateflow/a1-production-openapi-surfaces-spec-plan.md` §5.6)
  keeps its `acceptedAt?` spelling and is not rewritten; docs/15 records the
  supersession instead.

## 2. Exact changes

| Path | Change |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `ClaimResult.acceptedAt` renamed to `ClaimResult.verifiedAt` (`:4568-4571`); property remains outside `required` (`required: [claimId, status, subjectId]` unchanged). Description now maps to `subject_number.subject_claim.verified_at`, says present only for `ACCEPTED`, and references `ck_claim_verified_shape`. |
| `api/openapi/client-compat.ts` | Added generated-client assertions (`:298-311`, exported `:1038-1041`): `AssertClaimResultVerifiedAtKey`, `AssertClaimResultVerifiedAtOptional`, `AssertClaimResultVerifiedAtNonNullable` (`string \| undefined`), `ForbiddenClaimResultAcceptedAtKey` + `AssertClaimResultAcceptedAtAbsent`. |
| `api/openapi/contract-rules.mjs` | Added `validatePrFix1ClaimResult` (`:2903-2939`), wired into the `sensitive-fields` rule chain (`:3509-3510`). Added a positive control (`:2954`) and six adversarial probes in `validateAggregateFix1Probes` (`:3253-3318`). |
| `docs/15-production-openapi-surface-spec.md` | Added the §3.5 supersession bullet (`:169`): accepted plan `acceptedAt?` is superseded by optional `verifiedAt <- subject_number.subject_claim.verified_at` (A0 §4.14), present only for `ACCEPTED` (`ck_claim_verified_shape`), matching `ClaimSummary.verifiedAt`; historical plan not rewritten. |
| `docs/gateflow/a1-production-openapi-surfaces-spec-pr-fix-1.md` | This artifact (new file). |

No other file was modified by this fix pass.

## 3. Rule and probe pins

`validatePrFix1ClaimResult` enforces, against the canonical `components.schemas.ClaimResult`:

1. `acceptedAt` must not appear in `properties` or `required` (reintroduction rejected).
2. `verifiedAt` must be declared; absence is rejected.
3. `verifiedAt` must **not** be in `required` (optionality pin; absent for non-`ACCEPTED`).
4. `verifiedAt.type` must be the bare `string` (no `null`, no type array).
5. `verifiedAt.format` must be `date-time`.
6. `verifiedAt.description` must contain `subject_number.subject_claim.verified_at`
   and `ACCEPTED` (explicit A0 mapping / presence condition).

The probe block adds negative fixtures for each of those six failure modes and a
positive control that the canonical contract satisfies the rule. The validator
is registered both as its own chain entry under the `sensitive-fields` Redocly
rule and as a positive control in the aggregate fix probe set, so drift fails
`lint`/`check` rather than passing silently.

Generated-client pins (compile-time, run by `npm run typecheck`):

- `'verifiedAt' extends keyof ClaimResult` is `true`.
- `undefined extends ClaimResult['verifiedAt']` is `true` (optional).
- `ClaimResult['verifiedAt']` equals `string | undefined` (non-nullable when present).
- `Extract<keyof ClaimResult, 'acceptedAt'>` is `never` (stale key absent).

## 4. Validation evidence

### Static checks executed in this session

- Grep: `operationId:` count = **49** (unchanged).
- Grep: `x-onecall-surface:` count = **54** = 49 operations + 5 root tag surfaces (unchanged).
- Grep: registry `httpStatus:` entries = **59** (unchanged).
- Grep: docs/15 operation-map rows matching `| <op> | <METHOD> |` = **49** (unchanged).
- Grep: `acceptedAt` in `onecall-v1.openapi.yaml` = **0 occurrences**; `ClaimResult` occurs only at the response `$ref` (`:509`) and the schema definition (`:4554`).
- Grep/Read: canonical `ClaimResult.verifiedAt` is optional `string` + `date-time` with the A0 mapping description; `required` remains `[claimId, status, subjectId]`.
- Manual review of the new JS validator/probe logic and the TS assertions for syntax and containment (no dynamic parse available; see blockers).

### Dynamic commands attempted and BLOCKED

All `node`/`npm`/`git` invocations in this environment were refused by the
approval gate (`This command requires approval`). They did **not** run, so no
green result is claimed:

| Command (from `api/openapi`) | Result |
| --- | --- |
| `node --check contract-rules.mjs` | BLOCKED (approval gate) |
| `npm run check:s3` | BLOCKED (approval gate) |
| `npm run generate` (run 1, SHA-256 comparison) | BLOCKED (approval gate) |
| `npm run generate` (run 2, SHA-256 comparison) | BLOCKED (approval gate) |
| `npm run check:s1` (expected only 35 later operations) | BLOCKED (approval gate) |
| `npm run check:s2` (expected only 17 S3 operations) | BLOCKED (approval gate) |
| `git diff --check` | BLOCKED (approval gate) |
| `git status` / `git diff --cached --name-only` (write-boundary + empty-index) | BLOCKED (approval gate) |

Controller must replay the full validation suite. Expected outcomes relative to
the current contract: `check:s3` `customer=18 operations=21 ivr=6
public-share=3 provider-callback=1 total=49` with `PASS check-doc-map (s3)`;
`check:s1` S1 14 with 35 later operations; `check:s2` S2 32 with 17 S3
operations; dual `generate` producing identical SHA-256; `tsc --noEmit` strict
passing with the new assertions.

## 5. Preserved invariants

- 49 operations, five surfaces (`18/21/6/3/1`), 59-code Problem registry,
  44-table A0 catalog, stage sets and all error/status behavior unchanged.
- No wire change beyond the optional property rename; `required`, `status`
  enum, `additionalProperties: false`, response statuses and Problem sets are
  untouched.
- No alias retained; `acceptedAt` appears nowhere in the canonical YAML.
- Aggregate fixes 1 and 2, S1/S2/S3 prior fixes and the read-filter exception
  are untouched.

## 6. Residuals

- Dynamic validation is unverified in this session because `node`/`npm`/`git`
  are approval-blocked; controller replay is required before acceptance.
- The generated `tmp/openapi-client/onecall-v1.d.ts` on disk still reflects the
  pre-fix contract (`acceptedAt`) because `npm run generate` could not run. It
  is ignored/untracked and will refresh on replay; the committed `client-compat.ts`
  assertions target the regenerated declaration.
- The description pin is a substring check (`verified_at` + `ACCEPTED`), not a
  full prose equality; future wording changes that remove the mapping phrase
  will fail, but harmless rewording that keeps both tokens will pass.
- The historical accepted plan continues to say `acceptedAt?` by design; docs/15
  carries the supersession record.

## 7. Write-boundary and stop confirmation

- Write boundary respected: only `api/openapi/onecall-v1.openapi.yaml`,
  `api/openapi/contract-rules.mjs`, `api/openapi/client-compat.ts`,
  `docs/15-production-openapi-surface-spec.md` were modified, and
  `docs/gateflow/a1-production-openapi-surfaces-spec-pr-fix-1.md` was created.
- No plan, controller, prior artifact/review, package/config/lock, generated
  tracked file, prototype/screenshot or Git state was modified.
- **No review, no acceptance, no staging, no commit, no push, no PR
  edit/comment/review, no merge, and no gate advance was performed.** The Git
  index was not changed (empty-index check could not be executed due to the
  approval gate; no staging command was issued).
