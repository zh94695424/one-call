# PA8 S1 completion 11 — bounded accepted review fix12

Task: `pa8-s1-gpt-fix12-20261030T175228Z`.

## Accepted finding and scope

Controller accepts finding 1 (A1) in
`docs/reviews/code-review-20261031-013519.md`: V008 S1 catalog evidence
previously fenced names/counts but did not pin column types, nullability,
default expressions, key columns/order, or complete index definitions.
Review B's pass is context only and does not supersede that acceptance.
The current migration is correct; this is a test-evidence correction only.

Read accepted plan §9 Slice 1 and §3 inventory, frozen doc-14 §4.39–4.43,
current V008 DDL, A1, and existing V003–V007 dictionary/assertion patterns.
Only `PostgresContainerIT.java` and this new completion artifact are changed.
No SQL, schema, ACL, design, lifecycle, concurrency, or S2 implementation changes.

## Precise changes

- Supplement the existing V008 inventory helper with 56 independently frozen
  ordered column rows across five tables (11/15/17/5/8), comparing
  `column_name|data_type|is_nullable|column_default|character_maximum_length`.
  Pin all ten actual default expressions (`'PENDING'::text`,
  `'DECLARED'::text`, `1`, `now()`) and absence of defaults elsewhere.
  All frozen types are non-varchar; maximum character length is explicitly
  `<none>` for these types, rather than omitting the length dimension.
  Existing exact table names, ordered column names, and 56-column count remain.
- Add whole `pg_get_constraintdef` rows for all five primary keys and all five
  ordinary unique keys, with stable table/constraint identities and exact
  ordered key columns. The unfiltered PK/ordinary-unique collection also
  rejects extra keys; existing name fences remain unchanged.
- Add ten complete `pg_indexes.indexdef` literals, identified by table/index:
  two partial unique and eight secondary indexes. These pin btree columns,
  order, both `created_at DESC` occurrences, and both partial predicates with
  PostgreSQL 16 text casts. Existing exact partial/secondary membership fences
  remain; no PK-backing-index requirements are added.
- Correct the 39-check comparison to apply existing `normalizeCheckDef` to
  actual and expected whole definitions. Only trim/collapse ASCII whitespace
  is allowed; expressions, identifiers, literals, parentheses and casts stay
  fully pinned. No check dictionary literals or counts change.

Historical clarification: the S1 implementation report claimed the check
comparison already used `normalizeCheckDef`; the intake candidate actually
used raw equality (also recorded in completion 3). This fix restores the
requested whitespace-only comparison on both sides and updates its local
Javadoc. Historical reports are preserved, not rewritten.

## Actual author static validation

- Static Python inspection independently compared all 56 literal rows with
  frozen doc-14 column tables and the unchanged DDL: exact matches, including
  order, types, nullability, default expressions, and no varchar declarations.
- Static inspection matched all ten key definitions and all ten scoped index
  definitions against DDL, deriving canonical deparse with existing expected
  dictionary conventions; expected values are not inferred from a database.
- All 54 `@Test` method names and their order are unchanged; no new test methods.
  The annotation-name audit accounts for intervening method annotations.
- Byte-level comparison with the pre-edit source confines edits to the V008
  inventory/check helper regions and preserves every unchanged line's bytes.
  Source endings: before 24,838 CRLF / 176 LF-only; after 24,972 CRLF /
  176 LF-only. No whole-file line-ending normalization.
- A pre-edit SHA-256 manifest confirms all other pre-existing workspace files
  retain bytes, including migrations, plan/spec, README, CI, and historical
  reports. V008 SHA-256 before/after:
  `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e`.
- `git -c core.whitespace=cr-at-eol diff --check` passes; source diff is
  138 insertions / 4 deletions. No Maven, Docker, database, compilation, or
  tests invoked. Static inspection is not a runtime-pass claim.

## Pending controller validation and residual risks

- Controller owns intake and runtime: rerun focused3 first, then full
  54 Failsafe + 10 Surefire. Earlier focused3/full54+10 passes apply only to
  the pre-fix12 candidate, not this modified candidate.
- Controller owns PostgreSQL 16 canonical deparse verification. If actual
  formatting differs, one observational literal correction may be needed;
  do not weaken whole-definition equality, column/order/default pins, or
  exact object membership. No live formatting was observed by the author.
- S2 owner retains lifecycle/immutability, deferred-cycle behavior, rollback,
  concurrency, check-negative and default-positive behavioral coverage;
  this catalog fix neither implements nor certifies that slice.
- Design owner retains service-layer source/link/snapshot semantic validation
  and WORM/storage/retention decisions (including Q11); catalog assertions do
  not establish these application or infrastructure guarantees.

No delegation, staging, commit, push, PR, or gate advance performed.
