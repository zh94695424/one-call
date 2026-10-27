# PA4 V004 Catalog Order and Payment Foundation - Plan Fix 1 (F-PA4-PR-01)

- Task label: `pa4-v004-plan-fix1-ds-20261028-01`
- Work unit: `PA4-v004-catalog-order-payment-foundation`
- Gate: `fix` (plan review loop)
- Author/provider: DeepSeek (plan author/fixer)
- Date: 2026-10-28
- Artifact: `docs/gateflow/pa4-v004-catalog-order-payment-foundation-plan-fix-1.md`
- Revised plan: `docs/gateflow/pa4-v004-catalog-order-payment-foundation-plan.md`
- Reviewed artifact SHA-256 before fix: `416F1B25A0A354573A4AEAE7510EF4C8CEC9DC5CA6AAE4F355BC04FA3E672C3E`
- Revised plan SHA-256 after fix: `B950D539FFAAFCC72AFDE97AB5363018694C873898B41A432DDECC297DF84359`
- Scope: fix only accepted finding `F-PA4-PR-01`; no implementation, no self-review, no re-review, no accepted-plan gate, no Git action.

## 1. Accepted finding and exact adjudicated scope

Accepted finding `F-PA4-PR-01` (from `docs/reviews/plan-review-20261028-053250.md`, low severity): plan §6.6.1 incorrectly grants `catalog_order_role` `USAGE` on schema `entitlement_metering` in V004. docs/14 §11.4 assigns that role its first `entitlement_metering` schema `USAGE` in V006, and plan §6.6.3/§6.8 already defer the corresponding table reads.

Controller adjudication (`docs/gateflow/project-execution-control.md` PA4 gate record, plan review row) is binding and limited to:

1. Remove `catalog_order_role` from the §6.6.1 `entitlement_metering` `USAGE`-at-V004 row.
2. Add an explicit §6.6.5 V004 denial that `catalog_order_role` lacks `entitlement_metering` schema `USAGE` and that its first grant is V006.
3. Align §6.8/no-forward-grant wording so it clearly covers schema `USAGE` as well as table/column privileges.
4. Search the whole plan for any other statement/assertion/helper/test expectation that still requires or implies the early grant, and make only necessary consistency corrections.
5. Keep `catalog_order_role` `USAGE` on `activation_consent` in V004 unchanged.
6. Do not alter any frozen catalog total, FK count, slice count, test method count, trigger/function/error identifier, other grant, scope boundary, non-goal or residual owner/destination.

No broader change was authorized, and none was made.

## 2. Direct binding design evidence

### 2.1 docs/14 §11.4 V004 step 1 (V004 wave assignment)

> Create schemas `catalog_order`, `ivr`, `entitlement_metering` and `activation_consent`; create the four owner/runtime role pairs from §2.9; revoke `CREATE` on all business schemas from `PUBLIC`. Grant the four S2 runtime roles `USAGE` without `CREATE` on `platform` and `audit` and `EXECUTE` on the four S1 functions per §10.8-§10.12; grant `USAGE` without `CREATE` on `catalog_order` to `catalog_order_role`/`entitlement_metering_role`, on `activation_consent` to `activation_consent_role`/`ivr_role`/`catalog_order_role`, and on `entitlement_metering` to `entitlement_metering_role`/`activation_consent_role`/`ivr_role` according to §10.12. No table privilege is implied by schema `USAGE`.

`catalog_order_role` is absent from the V004 `entitlement_metering` grantee list and present in the V004 `activation_consent` grantee list.

### 2.2 docs/14 §11.4 V006 step 5 (first grant for `catalog_order_role`)

> Apply the V006 owned-table and cross-module grants from §10.12. ... Grant `catalog_order_role` `USAGE` without `CREATE` on `entitlement_metering` and the exact read-only columns on `entitlement_issuance(order_id, installment_no, entitlement_type, status, grant_id, issued_at, last_error_code)` and `entitlement_grant(id, subject_id, entitlement_type, granted_units, valid_from, expires_at)` for the computed order-level entitlement projection (R-04); no write or foreign row lock is granted.

This is the first wave that grants `catalog_order_role` `USAGE` on `entitlement_metering`.

### 2.3 docs/14 §10.12 (steady-state matrix versus wave assignment)

§10.12 shows the steady-state `entitlement_metering` schema row including `catalog_order_role` (read-only entitlement projections), and its preamble requires the containing-schema `USAGE` to be paired with each declared read "in that role's creation/first-read wave (F-01)". §10.12 is not the wave schedule; §11.4 is. V006 step 5 above is that first-read wave for `catalog_order_role`.

### 2.4 Plan-internal evidence

Plan §6.6.3 already says `catalog_order_role` receives no `activation_consent` or `entitlement_metering` table privilege at V004 and that "its future reads are V006/V007". Plan §6.8 already classified `catalog_order_role` entitlement/activation reads as absent at V004. The §6.6.1 row was the only statement that contradicted those sections.

## 3. Exact edits (before/after semantics)

All five edits are in-place single-line ACL prose corrections. The plan line count is unchanged at 1010 lines.

### Edit 1 - §6.6.1 `entitlement_metering` `USAGE` row (line 361)

Before:

```text
| `entitlement_metering` | `entitlement_metering_owner` | owner, `entitlement_metering_role`, `activation_consent_role`, `ivr_role`, `catalog_order_role` | owner only |
```

After:

```text
| `entitlement_metering` | `entitlement_metering_owner` | owner, `entitlement_metering_role`, `activation_consent_role`, `ivr_role` (**`catalog_order_role` is deferred to V006**) | owner only |
```

Semantics: the V004 `USAGE` grantee list now matches docs/14 §11.4 V004 step 1; `catalog_order_role` is explicitly deferred to its V006 first grant.

### Edit 2 - §6.6.1 narrative denial (line 369)

Before:

```text
`catalog_order_role` has no `USAGE` on `ivr`. `entitlement_metering_role` has no `USAGE` on `customer_iam`, `organization_access` or `subject_number` at V004; the subject read is V006.
```

After:

```text
`catalog_order_role` has no `USAGE` on `ivr` or `entitlement_metering` at V004 (its first `entitlement_metering` `USAGE` is V006); its `activation_consent` `USAGE` is granted at V004. `entitlement_metering_role` has no `USAGE` on `customer_iam`, `organization_access` or `subject_number` at V004; the subject read is V006.
```

Semantics: §6.6.1 is internally consistent with its corrected row and preserves the granted `activation_consent` capability.

### Edit 3 - §6.6.5 explicit V004 denial (line 426)

Before:

```text
- `catalog_order_role` has no `USAGE` on `ivr` and no table privilege on any `activation_consent` or `entitlement_metering` table (they do not exist).
```

After:

```text
- `catalog_order_role` has no `USAGE` on `ivr` and no `USAGE` on `entitlement_metering` at V004 (its first `USAGE` on that schema is V006); it has no table privilege on any `activation_consent` or `entitlement_metering` table (those tables do not exist). Its `activation_consent` schema `USAGE` is granted at V004 and is not a denial.
```

Semantics: this is the adjudicated explicit denial and first-wave assertion; it also records the unchanged positive `activation_consent` grant so the denial bullets cannot be misread as revoking it.

### Edit 4 - §6.8 no-forward-grant wording (line 455)

Before:

```text
- No forward grant: `ivr_role` S1/ivr grants, `activation_consent_role` S1 grants, `entitlement_metering_role` subject grants and `catalog_order_role` entitlement/activation reads are all absent at V004.
```

After:

```text
- No forward grant at V004: `ivr_role` S1/ivr grants, `activation_consent_role` S1 grants, `entitlement_metering_role` subject grants, and `catalog_order_role` `entitlement_metering` schema `USAGE` plus all `catalog_order_role` entitlement/activation table and column reads are absent. `catalog_order_role` `activation_consent` schema `USAGE` is granted at V004; its `entitlement_metering` schema `USAGE` and reads are V006.
```

Semantics: "no forward grant" now explicitly covers schema `USAGE` as well as table/column privileges, distinguishes the granted V004 `activation_consent` `USAGE`, and places the `entitlement_metering` `USAGE` and reads in V006.

### Edit 5 - §8.3 exact ACL test expectation (line 747)

Before:

```text
- Schema `USAGE`/`CREATE`: assert every row in 6.6.1 for the 12 V004/S1/production roles and `PUBLIC`; assert no `CREATE` for any runtime role; assert `ivr_role` no `ivr`/`catalog_order`/S1 `USAGE`; `catalog_order_role` no `ivr`; `activation_consent_role` no S1 `USAGE`; `entitlement_metering_role` no S1 `USAGE`.
```

After:

```text
- Schema `USAGE`/`CREATE`: assert every row in 6.6.1 for the 12 V004/S1/production roles and `PUBLIC`; assert no `CREATE` for any runtime role; assert `ivr_role` no `ivr`/`catalog_order`/S1 `USAGE`; `catalog_order_role` no `ivr` and no `entitlement_metering` `USAGE` at V004 (its first `entitlement_metering` `USAGE` is V006), while `has_schema_privilege('catalog_order_role','activation_consent','USAGE')` is true; `activation_consent_role` no S1 `USAGE`; `entitlement_metering_role` no S1 `USAGE`.
```

Semantics: the S1 ACL method now carries the reviewer-required validation point - `catalog_order_role` `entitlement_metering` `USAGE` false at V004 and `activation_consent` `USAGE` true - so the wrong grant cannot become a required success signal.

## 4. Occurrence audit in the revised plan

Every revised-plan line containing both `catalog_order_role` and `entitlement_metering` was inspected. Classification below shows no V004 `entitlement_metering` schema `USAGE` grant to `catalog_order_role` remains.

| Line | Context | Classification |
| --- | --- | --- |
| 22 | Frozen schema/role inventory names | Role name only; no grant |
| 67 | §1.6 item 2: V006 applies V006-only grants, including `catalog_order_role` reads of V006 tables | V006 assignment; no V004 grant |
| 147 | §6.1 step 5 owner-issued grants: `entitlement_metering_owner` for the exact §6.6.1 matrix | Matrix reference; role not listed in the V004 `entitlement_metering` row |
| 359 | §6.6.1 `catalog_order` row | Correct V004 `catalog_order_role` `USAGE` on `catalog_order` |
| 361 | §6.6.1 `entitlement_metering` row | Explicitly deferred to V006; no V004 grant |
| 369 | §6.6.1 narrative | Explicit V004 denial plus V006 first-grant statement |
| 400 | §6.6.3 note | No `entitlement_metering` table privilege; reads V006/V007 |
| 406 | §6.6.4 function `EXECUTE` row | Function grant naming both role names; unrelated to schema `USAGE` |
| 426 | §6.6.5 denial | Explicit V004 `entitlement_metering` `USAGE` denial; first grant V006 |
| 455 | §6.8 absence boundary | `catalog_order_role` `entitlement_metering` schema `USAGE` and reads absent at V004 |
| 747 | §8.3 ACL expectation | Asserts `catalog_order_role` `entitlement_metering` `USAGE` false and `activation_consent` `USAGE` true at V004 |
| 749 | §8.3 cross-module expectation | No table read of absent `entitlement_metering` tables |

No line grants `USAGE` on schema `entitlement_metering` to `catalog_order_role` at V004.

## 5. `activation_consent` V004 `USAGE` preserved

- §6.6.1 line 362 still lists `catalog_order_role` among the V004 `activation_consent` `USAGE` grantees.
- §6.6.1 line 369 and §6.6.5 line 426 now state that the `activation_consent` `USAGE` is granted at V004.
- §6.8 line 455 distinguishes the granted `activation_consent` `USAGE` from the absent `entitlement_metering` `USAGE`.
- §8.3 line 747 asserts `has_schema_privilege('catalog_order_role','activation_consent','USAGE')` is true.

This grant was not changed, matching docs/14 §11.4 V004 step 1.

## 6. Frozen totals confirmation (unchanged)

The fix edits only ACL prose. Line count is unchanged (1010 lines before and after), and no numeric or identifier token was edited. The revised plan still records:

| Item | Value | Revised-plan locations |
| --- | --- | --- |
| S2 schemas | 4 | §1.2 line 22; §1.5 line 44 |
| NOLOGIN roles | 8 | §1.2 line 22; §1.5 line 45 |
| V004 tables | 7 | §1.2 line 23; §1.5 line 46 |
| Columns | 78 | §1.2 line 24; §1.5 line 47 |
| Named checks | 51 | §1.2 line 24; §1.5 line 48 |
| PKs | 7 | §1.2 line 24; §1.5 line 49 |
| Ordinary/partial unique keys | 11 | §1.2 line 24; §1.5 line 50 |
| Total indexes | 28 | §1.2 line 24; §1.5 line 51 |
| V004 FKs | 11 | §1.2 line 24; §1.5 line 52 |
| Deferred V007 FK | 1 (`fk_customer_order_consent`) | §1.2 line 25; §1.5 line 53 |
| Trigger functions | 5 | §1.2 line 24; §1.5 line 54 |
| Triggers | 7 | §1.2 line 24; §1.5 line 55 |
| Test methods | 25 -> 27 -> 31 | §5 line 127; §8.1 lines 591, 595-596; §8.7 lines 919-920 |
| Slices | 2 | §4 lines 112, 117 |

No catalog total, FK count, slice count, test method count, trigger/function/error identifier, other grant, scope boundary, non-goal or residual owner/destination was altered.

## 7. Validation commands and results

1. Before-fix plan SHA-256:
   `sha256sum docs/gateflow/pa4-v004-catalog-order-payment-foundation-plan.md` -> `416f1b25a0a354573a4aeae7510ef4c8cec9dc5ca6aae4f355bc04fa3e672c3e` (matches the reviewed artifact SHA-256).
2. After-fix plan SHA-256:
   `sha256sum docs/gateflow/pa4-v004-catalog-order-payment-foundation-plan.md` -> `b950d539ffaafcc72afde97ab5363018694c873898b41a432ddecc297df84359`.
3. Encoding: `file docs/gateflow/pa4-v004-catalog-order-payment-foundation-plan.md` reports UTF-8 text (`Unicode text, UTF-8 text`; long-line annotation omitted).
4. Line count: `wc -l` -> 1010 before and after; edits are in-place only.
5. Trailing whitespace: scoped search for `[ \t]+$` -> 0 matches.
6. Conflict markers: scoped search for `^(<<<<<<<|=======|>>>>>>>)` -> 0 matches.
7. CRLF: scoped search for `\r$` -> 0 matches; the revised plan is LF-only.
8. Occurrence audit: §4 above; the only `entitlement_metering` row that could grant schema `USAGE` (§6.6.1) no longer lists `catalog_order_role`; all other co-occurrences are denials, V006 deferrals, function grants or table-read boundaries.
9. `activation_consent` preservation: §5 above; the V004 grant remains in §6.6.1 line 362 and is asserted true in §8.3 line 747.
10. Totals: §6 above; the revised plan's inventory and method/slice counts are unchanged. Independent counts over the revised plan's embedded expectation maps return 51 named-check literals, 16 PK/unique constraint-definition rows (7 PK + 9 ordinary unique), 11 FK definition rows, and 28 `CREATE [UNIQUE] INDEX` rows (including the two partial uniques); these match the frozen totals, and the 11-versus-1 deferred-FK distinction and 5-function/7-trigger and 25->27->31 method counts are unchanged.
11. Artifact integrity: this fix artifact is UTF-8, LF-only, with 0 trailing-whitespace lines and 0 conflict markers; its post-write SHA-256 is reported in the completion response so the artifact does not contain a self-referential hash.

## 8. Residual risks

No new residual risk is introduced. The change is a low-risk ACL wave-list correction with direct binding evidence from docs/14 §11.4 V004 step 1 and V006 step 5. The direct evidence does not contradict the accepted finding and does not require a broader change; the stop condition was not triggered.

The reviewer-assigned residuals in `docs/reviews/plan-review-20261028-053250.md` §8 (RSK-PA4-PR-01..07) retain their existing owners and destinations and are unchanged by this fix. No residual owner or destination was edited.

## 9. Changed files and forbidden-action confirmation

Exact writes performed by this task:

- Modified: `docs/gateflow/pa4-v004-catalog-order-payment-foundation-plan.md` (five in-place ACL prose edits; before `416F1B25...672C3E`, after `B950D539...84359`).
- Created: `docs/gateflow/pa4-v004-catalog-order-payment-foundation-plan-fix-1.md` (this artifact).

No other file was created or modified. No source, migration, test, CI, README, POM, docs/12, docs/14, controller, review artifact or other Gateflow file was touched. No file was staged, committed, pushed, amended, merged or reverted; no PR/issue was opened or updated; no approval or re-review was entered; no Git config/metadata was changed.

## 10. Completion statement

`F-PA4-PR-01` is fixed exactly within the adjudicated scope: the V004 `entitlement_metering` schema `USAGE` row no longer grants `catalog_order_role`, the §6.6.5 denial and V006 first-wave assertion are explicit, §6.8 covers schema `USAGE`, the §8.3 ACL expectation encodes the required true/false checks, and the V004 `activation_consent` `USAGE` remains unchanged. All frozen totals and identifiers are unchanged and the required artifact/hash/encoding/whitespace validations pass.

This fix does not review itself, does not enter re-review or the accepted-plan/implementation gate, and performs no later gate or Git action.
