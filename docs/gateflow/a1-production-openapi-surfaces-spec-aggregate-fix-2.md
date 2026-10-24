# A1 aggregate fix 2 — read-filter parameter trust boundary

- Task label: `a1-openapi-aggregate-fix2-ds-20261024-01`
- Work unit / gate: `A1-production-openapi-surfaces-spec` / aggregate `fix`
- Author: bounded DeepSeek fix author
- Date: 2026-10-24
- Accepted S3 HEAD: `58587f8e1bdf62b11e62f0add01e4fa76c149320`
- Preceding artifact: `docs/gateflow/a1-production-openapi-surfaces-spec-aggregate-fix-1.md`
- Finding owner: controller aggregate `fix` gate (`npm run check:s3` lint-only failure)

This artifact records one bounded correction to the parameter-trust rule. It is not a review, an acceptance, an S3 closeout, a runtime claim, or a commit/push/PR approval.

## Finding

After aggregate fix-1, `validateParameterTrust` rejects every top-level parameter whose exact name is in `TRUSTED_REQUEST_FACT_NAMES`, regardless of method or location. That conflates a read filter with a write assertion.

The canonical O15 operation `listOperationsOrders` (`GET /api/v1/operations/orders`) documents `paymentStatus` as a structured query filter and binds it in `x-onecall-filter-digest` (`onecall-v1.openapi.yaml:2169-2186`), yet the rule reported:

```
GET /api/v1/operations/orders: parameter must not accept trusted fact paymentStatus
```

The same false positive was repeated by the aggregate positive-control probe. A query filter selects rows; it does not write or assert payment state.

## Exact predicate

`api/openapi/contract-rules.mjs::isApprovedReadFilterParameter(operation, method, parameter)` returns true only when all of the following hold:

1. `method === 'get' || method === 'head'`;
2. `parameter.in === 'query'`;
3. `operation['x-onecall-filter-digest']` is an array and contains a value strictly equal to `parameter.name` (exact string equality).

`validateParameterTrust` now skips the top-level trusted-fact rejection only when this predicate is true. The predicate is evaluated per operation/method, so a path-item parameter inherited by a write method is still rejected.

All other paths remain fail-closed and unchanged:

- trusted fact names on write methods (including digest-bound ones) → rejected;
- trusted fact names in `path`, `header` or `cookie` → rejected;
- trusted query names absent from `x-onecall-filter-digest` → rejected;
- nested trusted properties inside a parameter schema → rejected by the unchanged `walkRequestSchema` nested-property walk, including for an approved top-level read filter;
- `FORBIDDEN_PROPERTY_NAMES` → still checked unconditionally in every location and method, independent of the read-filter predicate;
- no generic allowlist, no schema change, no wire change.

## Executable probes

Added `validateAggregateFix2Probes(root)` and invoked it from the `onecall/sensitive-fields` rule immediately after `validateAggregateFix1Probes(root)`. All probes are in-process synthetic roots; no fixture files are written.

Positive controls:

- `validateParameterTrust(canonicalRoot)` must return no problems (covers `listOperationsOrders` `paymentStatus`);
- a synthetic `GET` and `HEAD` operation with query parameter `paymentStatus` and digest `['paymentStatus']` must return no problems.

Negative probes (each asserts an exact message needle):

| Probe shape | Expected rejection |
| --- | --- |
| `POST` query `paymentStatus`, no digest | trusted fact on a write method |
| `POST` query `paymentStatus`, digest-bound | digest-bound trusted fact on a write method |
| `GET` path `paymentStatus`, digest-bound | trusted fact in a path parameter |
| `GET` header `paymentStatus`, digest-bound | trusted fact in a header parameter |
| `GET` cookie `paymentStatus`, digest-bound | trusted fact in a cookie parameter |
| `GET` query `paymentStatus`, no digest | unbound trusted query parameter |
| `GET` query `paymentStatus`, digest-bound, schema property `paymentStatus` | nested trusted property inside an approved read filter |
| `GET` query `objectKey`, digest-bound | forbidden secret in a digest-bound read filter |
| `POST` header `objectKey` | forbidden secret on a write method |

## Validation evidence

### Dynamic commands — blocked by the approval gate

The session's Bash approval gate rejected every Node/npm execution attempt with “This command requires approval”. Each required command was attempted once and was not silently skipped.

| Command (from `api/openapi`) | Outcome |
| --- | --- |
| `node --check contract-rules.mjs` | BLOCKED (approval gate); not executed. |
| `npm run check:s3` | BLOCKED (approval gate); not executed. |
| `npm run check:s1` | BLOCKED (approval gate); not executed. |
| `npm run check:s2` | BLOCKED (approval gate); not executed. |

No lint result, probe result, stage result or generated artifact is claimed. The controller must execute the full dynamic suite. Expected stage outcome after the fix is unchanged from the adjudication: `check:s1` reports only the 35 later-operation/count mismatches and `check:s2` only the 17 S3-operation/count mismatches, once shared checks pass.

### Static evidence performed

- Only one canonical parameter name across the whole YAML matches `TRUSTED_REQUEST_FACT_NAMES`: `paymentStatus` (`onecall-v1.openapi.yaml:2182`), and it is bound into that operation's `x-onecall-filter-digest` (`:2169-2171`). No other trusted-name parameter exists, so the canonical positive control covers the full current surface.
- The read-filter predicate requires `get`/`head` (`METHODS` includes `head`), `in: query`, and exact-string membership in the digest array; nested-schema and forbidden-secret checks are untouched and run unconditionally.
- `git status --short` shows `api/openapi/contract-rules.mjs` modified and the new fix-2 artifact untracked; no other path was touched by this fix. The pre-existing aggregate fix-1 modifications and unrelated pre-existing changes remain untouched.
- Git index empty: `git diff --cached --name-only` produced no output.
- No trailing whitespace in `contract-rules.mjs`.
- SHA-256 of `api/openapi/contract-rules.mjs` post-fix: `13625e6d40def5dfefbcd2fe62a6b2360fd45b9a509b831b72f12d77f9437e91`.

## Changed paths

- Modified: `api/openapi/contract-rules.mjs`
- Created: `docs/gateflow/a1-production-openapi-surfaces-spec-aggregate-fix-2.md`

No YAML, client compatibility, docs/15, controller, plan, prior artifacts/reviews, config/package, generated tracked file, prototype/screenshot or Git state was modified.

## Residual risks and explicit limits

- Dynamic syntax/lint/probe/stage validation was blocked; the predicate and probes are statically reviewed only. The controller must run `node --check`, `npm run check:s3`, `npm run check:s1` and `npm run check:s2`.
- The exception is intentionally narrow: any future read filter must be added to the operation's `x-onecall-filter-digest` to be admitted, and any non-GET/HEAD or non-query use remains rejected.
- Aggregate fix-1 behavior is preserved; the only semantic change is the approved read-filter exception described above.
- No runtime service, database, auth, payment, telephony, storage or provider behavior is claimed.

## Status statement

No independent review was performed, no aggregate acceptance is claimed, no commit was created, nothing was staged, nothing was pushed, no PR was created or mutated, and no gate was advanced. The gate remains at the controller's aggregate `fix` step.
