# A1 S3 fix-5: TypeScript negative assertion placement

- Gate: `fix` (S3 implementation slice)
- Work unit: `A1-production-openapi-surfaces-spec`
- Task label: `a1-openapi-s3-fix5-ds-20261024-01`
- Role: bounded fix author
- Date: 2026-10-24

## Controller finding

After fix-3 and fix-4, Redocly lint, bundle, and generation pass, but
`npm run check:s3` fails only in `tsc --noEmit -p tsconfig.client-compat.json`.
Three `@ts-expect-error` directives sat on the `const` declaration line while
TypeScript reported the intended error on a nested object property line, so
each directive produced `TS2578` (unused) and the intended error surfaced:

| Declaration line (old directive) | Intended error line | Property |
| --- | --- | --- |
| `client-compat.ts(368,1)` | 376 | `extractionCode` |
| `client-compat.ts(378,1)` | 380 | `result` |
| `client-compat.ts(469,1)` | 471 | `mode` |

Generated types are correct: REPLAYED excludes `extractionCode`, rotation uses
canonical `ROTATED`, and P2 uses canonical `NONE`.

## Exact changes (only `api/openapi/client-compat.ts`)

Moved each directive down to the line immediately preceding the property line
TypeScript rejects. Fixtures, values, expected diagnostics, and directives are
otherwise unchanged; nothing was weakened, deleted, cast, or commented out.

1. `shareRotationReplayedWithExtractionCode` (now lines 368-377):

   ```ts
   const shareRotationReplayedWithExtractionCode: ShareCodeRotationReplayed = {
     // ...
     revocationGeneration: 0,
     // @ts-expect-error REPLAYED must never accept extractionCode.
     extractionCode: 'CODE-ONCE-001',
   };
   ```

   Directive moved from the former line 368 (above `const`) to line 375,
   directly above `extractionCode` at line 376.

2. `shareRotationComponentNameLiteral` (now lines 378-387):

   ```ts
   const shareRotationComponentNameLiteral: ShareCodeRotationRotated = {
     // @ts-expect-error component-name discriminator values are not wire values.
     result: 'ShareCodeRotationRotated',
     // ...
   };
   ```

   Directive moved from the former line 378 (above `const`) to line 379,
   directly above `result` at line 380.

3. `shareVerificationComponentNameLiteral` (now lines 469-472):

   ```ts
   const shareVerificationComponentNameLiteral: ShareAdmissionVerification = {
     // @ts-expect-error component-name mode values are not wire values.
     mode: 'ShareVerificationNone',
   };
   ```

   Directive moved from the former line 469 (above `const`) to line 470,
   directly above `mode` at line 471.

Each `@ts-expect-error` now sits on the exact property line TypeScript rejects.
A directive placed inside a multi-line object literal still applies to the
next source line, so the intended negative assertion remains enforced: if the
property or literal became legal, the directive would itself report `TS2578`.

## Commands and outcomes

Working directory: `/mnt/d/codex-call/api/openapi`.

| Command | Outcome |
| --- | --- |
| `npx tsc --noEmit -p tsconfig.client-compat.json` | **Not executed.** Harness permission returned `This command requires approval` for the `npx` invocation; the author session cannot grant it. |
| `npm run check:s3` | **Not executed** for the same permission reason. |
| `git diff --check -- api/openapi/client-compat.ts` | **Not executed.** Harness permission returned `This command requires approval` for scoped `git` invocations. |
| `git diff --cached --name-only` (index check) | **Not executed** for the same permission reason. |
| hygiene scans on `client-compat.ts` via dedicated search: conflict markers, U+FFFD, CR, trailing whitespace | 0 matches each. |

Because Node/npm and git execution are denied to this author session, the
expected `tsc` green result and green `check:s3` are **not confirmed here**.
The controller must rerun both. This report does not claim they passed.

No staging, commit, or index-mutating command was executed, so the Git index
was not modified by this fix; the index-empty result remains controller-owned
verification for the same permission reason.

## Changed files

- `api/openapi/client-compat.ts` (three directive relocations, no semantic
  change to fixtures or assertions)
- `docs/gateflow/a1-production-openapi-surfaces-spec-s3-fix-5.md` (this
  artifact)

No OpenAPI YAML, `contract-rules.mjs`, docs/15, controller, plan, prior
artifact, package/config, generated tracked file, prototype, screenshot, or
Git index/history was modified.

## Residual risks

- Static typing outcome is unverified in this author session due to the
  permission denial; placement is argued from TypeScript's next-line directive
  semantics and the controller-supplied diagnostic line numbers (368/376,
  378/380, 469/471).
- If TypeScript reports any additional diagnostic on a declaration line that
  the old directives were masking, removing them from those lines would
  surface it. The controller's finding states only the three `TS2578`/intended
  pairs remain, so none is expected.
- Fixture intent is unchanged: excess `extractionCode` on REPLAYED, a
  component-name discriminator literal for `result`, and a component-name mode
  literal for `mode` are still each asserted to be rejected.

## Status statement

No review, acceptance, staging, commit, push, or PR operation occurred. The
controller will validate and dispatch an independent re-review.
