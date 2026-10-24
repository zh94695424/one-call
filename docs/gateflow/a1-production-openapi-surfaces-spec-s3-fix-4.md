# A1 S3 fix-4: approved opaque request walker correction

- Gate: `fix` (S3 implementation slice)
- Work unit: `A1-production-openapi-surfaces-spec`
- Task label: `a1-openapi-s3-fix4-ds-20261024-01`
- Role: bounded fix author
- Date: 2026-10-24

## Controller finding

S3 fix-3's OQ-4 approved-node exemption in `walkRequestSchema` was correct at the
parent node (`OpaqueProviderPayload` / `IvrEventPayload` recognized by node
identity), but the walker then descended into each approved node's own
`additionalProperties: true` openness declaration and reported it through the
generic boolean-schema invariant:

- `POST /api/v1/ivr/calls/{callId}/events: request schema boolean true is forbidden: it admits arbitrary data`
- `POST /api/v1/callbacks/payment-events: request schema boolean true is forbidden: it admits arbitrary data`

That is a false positive: the boolean is the approved openness of the named
component, not an ordinary schema-valued traversal position.

## Exact change (only `api/openapi/contract-rules.mjs`)

1. `walkRequestSchema` additionalProperties descent:

   - Added `const approvedOpaqueOpen = opaqueRequestException && node.additionalProperties === true;`
   - The descent guard is now
     `if (node.additionalProperties && !typeDefinitivelyExcludesObject && !approvedOpaqueOpen)`.
   - The exemption remains based on node identity with the two approved
     component nodes (`opaqueRequestException`), never on the
     `x-onecall-opaque` annotation alone. Only the approved node's exact
     boolean `additionalProperties: true` declaration is skipped; direct
     boolean `true` schemas and every non-approved additionalProperties
     position still flow through the unchanged rejection.

2. OQ-4 probes in `validateS3FixProbes`:

   - Approved-component probe strengthened from "no annotation problem" to
     `approvedProblems.length > 0` failing with the full problem list; each
     approved component must now produce zero walker problems. This is the
     probe that would have caught the fix-3 regression.
   - The inline unapproved-opaque probe now uses
     `additionalProperties: true` and additionally asserts the ordinary
     closure message, so annotation alone demonstrably does not grant the
     additionalProperties exemption.
   - Added an ordinary `{ type: 'object', additionalProperties: true }`
     negative probe requiring `request object schema must set additionalProperties false`.
   - Added a direct `walkRequestSchema(root, true, ...)` negative probe
     requiring `request schema boolean true is forbidden`.

No trusted-field, closure, array, ref, `$ref`-sibling, sensitive-field, or
operation-count rule was changed. No OpenAPI YAML, generated client, docs/15,
controller, plan, package/config, prototype, screenshot, or Git state was
modified.

## Positive probe evidence

- `OpaqueProviderPayload` and `IvrEventPayload` are still admitted only by
  identity: `node === root.components.schemas.OpaqueProviderPayload` (or
  `IvrEventPayload`), and `resolveRequestSchemaRef` returns the same object
  reference for `$ref` hops, so the identity check holds on the real request
  paths.
- For each approved node, `type: object` makes
  `typeDefinitivelyExcludesArray` true (no `items` problem),
  `opaqueRequestException` suppresses the `x-onecall-opaque` and closure
  problems, and `approvedOpaqueOpen` suppresses only the boolean
  additionalProperties descent. The strengthened probe now requires the
  actual walker to return zero problems for each approved node.

## Negative probe evidence

Executable probes added/preserved in `validateS3FixProbes`:

| Probe | Expected walker output |
| --- | --- |
| inline `{ type: 'object', additionalProperties: true, 'x-onecall-opaque': true }` | annotation rejection + `request object schema must set additionalProperties false` + boolean-true rejection |
| ordinary `{ type: 'object', additionalProperties: true }` | `request object schema must set additionalProperties false` |
| direct boolean `true` | `request schema boolean true is forbidden: it admits arbitrary data` |
| unapproved component carrying `x-onecall-opaque: true` | `x-onecall-opaque is approved only on OpaqueProviderPayload and IvrEventPayload` (via `validateSensitiveFields`) |

## Commands and outcomes

Working directory: `/mnt/d/codex-call/api/openapi`.

| Command | Outcome |
| --- | --- |
| `node --check contract-rules.mjs` | **Not executed.** Harness permission returns `This command requires approval`; the author session cannot grant it. |
| `npm run check:s3` | **Not executed** for the same permission reason. Dynamic evidence remains controller-owned. |
| `npm run check:s1` | **Not executed** for the same permission reason. |
| `npm run check:s2` | **Not executed** for the same permission reason. |
| focused negative/positive probe execution | **Not executed** (node execution permission-blocked). Probe code is in-process and runs under controller validation. |
| `git diff --check -- contract-rules.mjs` | Passed (no output, exit 0). |
| conflict-marker / U+FFFD / CR / trailing-whitespace scans on `contract-rules.mjs` | 0 matches each. |
| `git diff --cached --name-only` | Empty; index unchanged. |
| `git diff --numstat -- contract-rules.mjs` | `848 17` (includes pre-existing uncommitted fix-3 work plus this fix-4 correction). |

Because npm/Node execution is denied to this author session, the expected
`check:s3` green result and the `check:s1` 35 later-operation/count mismatches
and `check:s2` 17 S3-operation/count mismatches are **not confirmed here**.
The controller must rerun them; this report does not claim they passed.

## Changed files

- `api/openapi/contract-rules.mjs` (walker guard + strengthened/added probes)
- `docs/gateflow/a1-production-openapi-surfaces-spec-s3-fix-4.md` (this artifact)

## Residual risks

- Runtime validation and Redocly lint are unverified in this author session due
  to the permission denial; correctness is argued statically plus by the
  in-process probes the controller will execute.
- The guard is intentionally narrow (`approved node` + `additionalProperties === true`).
  If an approved opaque component ever declares a schema-valued
  `additionalProperties`, that subschema is still walked under the ordinary
  rules — no exemption is granted beyond the sanctioned boolean openness.

## Status statement

No review, acceptance, commit, stage, push, or PR operation occurred. The
controller will validate and dispatch an independent re-review.
