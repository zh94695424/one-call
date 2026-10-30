# PA8 V008 S1 Completion 2 — Runtime Corrections (controller-directed)

## 1. Task identity and verdict

| Field | Value |
| --- | --- |
| Gateflow work unit | `PA8-v008-evidence-foundation` |
| Task label | `pa8-s1-completion-2` (controller-driven runtime repair of blocked S1) |
| Provider / runtime | mimo / Claude Code agent session (`mimo-v2.6-flash`) |
| Slice | S1 only (catalog / ACL / FK / staged-prefix) |
| Binding plan | `docs/gateflow/pa8-v008-evidence-foundation-plan.md` |
| Prior artifact | `docs/gateflow/pa8-v008-evidence-foundation-s1-completion-1.md` (6 static corrections; present and preserved) |
| Controller findings repaired | 2 runtime defects (pg_index `relname` misuse; `"char"` catalog-column concatenation without cast) |
| **Verdict** | **`corrections applied; runtime still controller-owned`** — both evidenced runtime defects corrected in source; this session performed no runtime execution; controller must rerun focused then full verification |

## 2. Controller-executed evidence (actual, not author-run)

Controller artifact `/tmp/pa8-controller-compile.j1ndjmwp` (prior delivery = completion-6 corrected source):

| Check | Actual result |
| --- | --- |
| JDK21 Maven `test-compile` | **PASS** (4.877 s) |
| Failsafe focused run, 3 V008 methods, PostgreSQL 16.15 | **FAIL** — 3 tests / 0 failures / **2 errors** / 0 skipped |
| Flyway V001–V008 | All 8 migrations applied cleanly (log lines 38–65) |
| `v008OwnerPublicRuntimeAndCrossModuleAclsAreExact` | **PASS** (no error) |
| `v008EvidenceInventoryAndGuardShapesAreExact` | **ERROR** — `org.postgresql.util.PSQLException: ERROR: column i.relname does not exist` at `assertV008TablesColumnsChecksIndexesFks` (`PostgresContainerIT.java:23676`), via `queryStrings:18381` |
| `v008ForeignKeyAndDeferredCreationCycleFkBackstops` | **ERROR** — `org.postgresql.util.PSQLException: ERROR: operator is not unique: text || "char"` at `assertV008ForeignKeyInventory` (`PostgresContainerIT.java:24168`), via `prepareStatement.executeQuery()` |

Log: `/tmp/pa8-controller-compile.j1ndjmwp/focused.log`. No author-executed runtime verification is claimed in this session.

## 3. Corrections applied

### Finding 1 — `pg_index` has no `relname` column (ERROR 42703)

**Defect:** Two queries in `assertV008TablesColumnsChecksIndexesFks` selected `c.relname || '.' || i.relname FROM pg_index i`. `pg_index` exposes `indexrelid` (oid), not `relname`; the index relation name lives in `pg_class`. Both the partial-unique query (first error at line 23676) and the secondary-index query (same shape, immediately after) were affected.

**Fix:** Both queries now join a second `pg_class` alias on the index oid and select that alias's `relname`:

```sql
SELECT c.relname || '.' || index_alias.relname FROM pg_index i
JOIN pg_class c ON c.oid = i.indrelid
JOIN pg_class index_alias ON index_alias.oid = i.indexrelid
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE ...
```

Expected exact names are unchanged:

- Partial uniques: `evidence_artifact.uq_evidence_original_artifact`, `evidence_event.uq_evidence_created_event`
- Secondary indexes: the same eight `ix_*` names (`ix_evidence_artifact_media`, `ix_evidence_event_record`, `ix_evidence_call`, `ix_evidence_status`, `ix_evidence_subject`, `ix_media_object_sha256`, `ix_recording_call`, `ix_recording_pending`)

### Finding 2 — ambiguous `text || "char"` catalog concatenation (ERROR 42725)

**Defect:** `assertV008ForeignKeyInventory` built inventory rows with `c.conname || '|' || c.confupdtype || '|' || c.confdeltype || '|' || c.condeferrable || '|' || c.condeferred`. `confupdtype`/`confdeltype` are internal `"char"` catalog columns; PostgreSQL rejects `text || "char"` as operator-not-unique (runtime hint: "Could not choose a best candidate operator. You might need to add explicit type casts."). Boolean `condeferrable`/`condeferred` concatenate fine via `text || anynonarray` and are unchanged.

**Fix:** Both `"char"` columns are cast explicitly:

```sql
c.conname || '|' || c.confupdtype::text || '|' || c.confdeltype::text
  || '|' || c.condeferrable || '|' || c.condeferred
```

Expected catalog-pin strings are unchanged: ordinary FKs end `|r|r|false|false`; deferred cycle FKs end `|a|a|true|true` (boolean output remains `false`/`true`).

### Same-class scan of new V008 helpers (no broad legacy edits)

Searched all new V008 helpers for the same two misuse classes:

| Pattern | Result |
| --- | --- |
| `pg_index` alias reading `i.relname` | Only the two corrected sites; all other `pg_index` usages (legacy waves and `assertV008...`) reference `i.indexrelid`/`i.indpred`/`i.indisunique` correctly |
| `"char"` catalog columns concatenated to text without cast in V008 helpers | Only the corrected FK-inventory site; other V008 catalog queries use comparisons (`c.contype = 'c'`, `c.relkind = 'r'`, `c.confdeltype = 'r'` — literal coerces, no ambiguity), `pg_get_constraintdef(c.oid)`, or already-`::text`-cast patterns |
| Related boolean concatenation (`prosecdef`, `rolcanlogin`, `condeferrable`) in V008 helpers | Left unchanged: `text \|\| boolean` resolves via `anynonarray`; the evidenced runtime error was exclusively `text \|\| "char"` |

No other directly-evidenced related case was found in the new V008 helpers. No legacy (pre-V008) helper was edited.

## 4. Files changed

| Path | Action | Note |
| --- | --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | modify | Two catalog-query corrections only (see §3) |
| `docs/gateflow/pa8-v008-evidence-foundation-s1-completion-2.md` | create | This artifact |

All completion-1 (6 prior) corrections remain present and untouched: `V008_CHECK_DEF_MAP` parenthesis balance, LF digest witness, `expectCommitFailure` setup isolation, reservation fixture columns, snapshot JSON shape, `cleanupV008LocalRows` completeness.

No migration, spec, plan, controller, CI, README, V008 SQL, or historical artifact was modified.

## 5. Scope fence preserved

- S1 only; no S2 methods, no new tests, no weakened assertions, no SQLSTATE unions, no check/constraint drops.
- Expected exact names (index names, FK catalog-pin suffixes) unchanged.
- No commits/push/PR/gate transitions.

## 6. Validation status

| Check | Status |
| --- | --- |
| Correction 1 (pg_index join) | Applied; static source inspection only |
| Correction 2 (`::text` casts) | Applied; static source inspection only |
| Same-class scan of new V008 helpers | Complete; no further evidenced cases |
| JDK21 test-compile (this corrected source) | **Not executed in this session** — controller owns verification |
| Focused container tests (3 V008 methods) | **Not executed in this session** — controller owns verification |
| Full container suite (54) | **Not executed in this session** — controller owns verification |

## 7. Remaining unverified assertions

Runtime verification is still pending for the two corrected catalog queries and, transitively, everything after them in the two failing methods (check-def map whole-definition equality, trigger/function exactness, missing-parent and RESTRICT/deferred parent-delete probes, reservation fixtures, cleanup zero-residue). Controller reruns focused tests and handles any further one-shot observational failures.

## 8. Completion status

1. **Verdict:** corrections applied; runtime verification controller-owned
2. **Artifact paths:** this file + corrected `PostgresContainerIT.java`
3. **Exact files changed:** `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
4. **Validation performed:** static correction of the two controller-evidenced runtime defects; same-class scan of new V008 helpers; no runtime execution
5. **Slice count:** 1 (S1); S2 not started
6. **Blocking questions:** none from author side; controller must rerun compile + focused + full verification
7. **Residual owners:** controller owns plan §9 verification; S2 owns lifecycle/concurrency
