# PA19A plan fix 2 correction 1

Task: `pa19a-sharing-evidence-media-read-acl-plan-fix2-correction1-gpt-20261108`  
Gate: `fix` correction `RR1-C1`

## Correction

Corrected only Commands item 1 in `pa19a-sharing-evidence-media-read-acl-plan.md`. The command is run from `backend/`, hashes V001–V012 with `sha256sum src/main/resources/db/migration/V0{01..12}__*.sql`, requires exactly 12 output rows in V001–V012 order, compares all rows to the pinned 12 digests, and requires byte-identical digests to pass.

## Preserved scope

No other semantic decision changed. `RR1-01` through `RR1-04`, the exact 22-path allowlist, query/API/grants, source=CI=XML arithmetic, goal/non-goals, and residual owners remain unchanged. V001–V012 were not modified. No source, migration, test, POM, CI, OpenAPI, Control, existing review/fix artifact, or implementation file was changed. No commit, push, review, implementation, or external action was performed.
