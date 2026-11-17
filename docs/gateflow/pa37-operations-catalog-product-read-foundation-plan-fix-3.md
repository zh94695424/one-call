# PA37 Operations Catalog Product Read Foundation — Plan Fix 3

Task label: `pa37-operations-catalog-product-read-plan-fix3-gpt-20261117-01`
Date: 2026-11-17 (Asia/Shanghai)
Scope: only accepted second-round re-review findings RR2-01 through RR2-09.
Status: bounded plan-author correction; no acceptance, implementation or later gate.

## 1. Inputs and provenance

- Input cumulative plan SHA-256:
  `7d9cb67c07485655783b90ed74857df336b3c8a91e4613daa2dc626ae69098c6`
  (19,346 bytes / 270 lines), verified before editing.
- Output cumulative plan:
  `docs/gateflow/pa37-operations-catalog-product-read-foundation-plan.md`
  SHA-256 `1fb2216b4046b0ca21ad03643fcc6a4a0d5037e912e7feb60d266df390812486`
  (33,356 bytes / 435 lines, canonical UTF-8/LF).
- Reviewed binding Control: `docs/gateflow/project-execution-control.md` PA37
  checkpoint; second-round reviews `docs/reviews/plan-review-20261117-193036.md`
  and `docs/reviews/plan-review-20261117-193924.md`; current frozen OpenAPI,
  V004/V002, Operations context/request/codec/configuration/order-counter sources,
  architecture mirrors, CI inventory and POM.
- Historical fix1 remains SHA-256
  `2f539bcd7e22fac208a2bafe51f29c27499df57c79c34725ae6c08ce1da1ecb4`
  (5,529 bytes / 68 lines); fix2 remains SHA-256
  `b0cc4a98dce4b692ba402ea72dc3a10bd0ab1f2c96a6e0f465e5c266543efec7`
  (10,410 bytes / 160 lines). Neither was edited.
- Authoritative pre-fix1 plan remains SHA-256
  `e0a5d3435cecd2e60919021757326bf6bab4c36d68a787eb3ef8f14b3ee8e069`
  (35,714 bytes / 514 lines). Fix2's correction of fix1's unavailability claim
  remains binding and is now also explicit in cumulative-plan §8.
- This new artifact has no before hash. Its final bytes/lines/SHA-256 are reported
  after writing, not embedded recursively in its own content.

## 2. Accepted second-round closures

| Finding | Exact bounded correction |
|---|---|
| `PA37-PLAN-RR2-01` | §§3/5 rename the one IT method to `assertsNoAdvisoryLocks` in the authoritative FQCN/method dictionary. The class stays16 methods; all CI identity consumers use that dictionary. Read-only PostgreSQL denies reachable DML/DDL/`FOR UPDATE` as25006, not advisory-lock functions. Static no-use plus backend-scoped lock observations inside/before/after success and failure prove the read path neither uses nor leaves an advisory lock. |
| `PA37-PLAN-RR2-02` | §4 separates exact outer version/key-id-length/key-id/nonce/ciphertext/tag from encrypted purpose-version/limit/digest-prefix/UUID/code-length/code plaintext. AAD length-prefixes the catalog purpose, exact GET endpoint and exact sort, then authenticates the key-id header. Active-key issuance, retained-old-key decode and removed/unknown-key rejection use exact key selection. Raw anchor fields are encrypted, not merely base64url-encoded. Maximum plaintext102 + outer overhead62 =164 bytes, unpadded base64url219≤512 at key-id32/product-code64. Eight existing codec methods cover target round-trip, rotation, wrong purpose, malformed frames, tampering and mismatch. |
| `PA37-PLAN-RR2-03` | §5 gives the exact CI checklist: six `pa37_unit_additions` entries `[8,8,18,10,8,8]`, two `pa37_container_additions` entries `[20,16]`; OR class-local `pa37_names` into all earlier method/source/line-scan subtractions and additions/baseline unions. Only real new historical assertions are PA37-start788/556 and scan1345; all current method/class/scan/XML totals, early Surefire aggregate, aggregate scan tail and diagnostic tail advance. Architecture class/include mirrors replace old literals with89/36/36; no fictitious historical mirror variants. Failsafe container-smoke34→36, all-POM include elements36→38, only two Failsafe include lines added. |
| `PA37-PLAN-RR2-04` | §5 enumerates all ten exact `backend/src/test/java/...` paths with FQCNs, exact eight test-bearing FQCN/method dictionaries and two excluded helper identities. §6 focused Maven selectors use those exact FQCNs. CI dictionaries/source equality/XML and the implementation report must use the same table, never simple-name aliases or alternate placement. |
| `PA37-PLAN-RR2-05` | §5 pins the catalog-owner `@Component("operationsCatalogProductReader")`, persistence profile, existing enabled-property condition and lazy registration used by the production order counter. Operations resolves the port, never imports/constructs the catalog concrete class or adds a module edge. The read configuration pins PA35's `@Configuration(proxyBeanMethods = false)`, same profile/property condition, class/service-bean lazy semantics and existing qualified resources. |
| `PA37-PLAN-RR2-06` | §2 pins `SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = CAST(? AS uuid)`. §4 pins canonical SHA-256 digest bytes with fixed ordered presence-aware length-prefixed purpose/endpoint/sort/limit fields. Default20 is normalized and encoded exactly like explicit20; limit is covered. After authority SQL, authenticated digest/limit mismatch is400 `CURSOR_FILTER_MISMATCH` before any catalog SQL. |
| `PA37-PLAN-RR2-07` | §5 removes context from writable paths and pins byte-unchanged SHA-256 `75c2e3b4b3cc251e8fe43223512be89e510c711059bf280ffbd466a9848adcdf`, existing `Role.ORDER`, and absence of PA37 tokens/branches. §3 states INSERT and selected UPDATE are granted, DELETE is not; read-only25006 masks reachable mutation attempts while the grant matrix is independently asserted. |
| `PA37-PLAN-RR2-08` | §§5/7 allowlist `docs/gateflow/pa37-operations-catalog-product-read-foundation-implementation.md` solely as non-code evidence; exclude it from production/test/CI/scan/POM counts. Require exact paths/FQCNs, counts, digest, immutable-context hash, commands and residual owners there. |
| `PA37-PLAN-RR2-09` | §3 phase2 reads versions only for the≤limit emitted roots, never the lookahead root. Anchor is `(product_code COLLATE "C", id) > (CAST(? AS text) COLLATE "C", CAST(? AS uuid))`. §5 no-new grant/role/schema/function checks are empty deltas relative to existing PA36 catalog/410-tuple/topology oracles, not duplicate whole-cluster scans. All totals and the six-unit/two-IT/two-helper inventory remain fixed. |

No finding creates a new test class/method or expands business scope. The sole
method-identity change preserves the existing 16-method IT inventory. The
cumulative plan is the implementation authority; historical fix1/fix2 wording is
superseded only where these accepted corrections require it.

## 3. Prior closures retained

| Finding | Preserved closure |
|---|---|
| `PA37-PLAN-REV-01` | Existing architecture tree/CI lockstep; guarded291→299/full normalized manifest digest; no new exclusions. |
| `PA37-PLAN-REV-02` | Exact eight production files/ten test files, six-unit/two-IT/two-helper identities and counts, POM and CI reconciliation. |
| `PA37-PLAN-REV-03` | No `catalog_offer` read/join/EXISTS; offer-less DRAFT and multi-channel membership invariance, inventory annotation only. |
| `PA37-PLAN-REV-04` | Root-bounded two-phase snapshot paging; zero-version roots, exclusive anchor, lookahead and no duplicate/skipped roots. |
| `PA37-PLAN-REV-05` | Truthful frozen V004 matrix, approved projections, read-only denial attribution and broad-role deferred owner. |
| `PA37-PLAN-REV-06` | C collation/typed anchor/UTF-8 comparison and legal AB/A-1/A_1/A0 fixtures. |
| `PA37-PLAN-REV-07` | Authority-first/ring×authority precedence, blank/detail/error/429 rules and no rate-limiter implementation. |
| `PA37-PLAN-REV-08` | Repository `check:s3` and focused Maven no-test guards remain; exact six-unit/two-IT FQCN selectors. |
| `PA37-PLAN-REV-09` | Declared nulls, QUOTE/CNY, exact authority projection, deterministic ordering and opaque bounded cursor. |
| `PA37-PLAN-FIX1-REV-01` | Six unit60/two IT36/two helper0, no redundant ACL-only class/selector or extra identity. |
| `PA37-PLAN-FIX1-REV-02` | Correct authoritative pre-fix1 hash and provenance; historical artifacts remain unchanged. |

The binding goal remains exactly the two frozen catalog product reads and one S1
end-to-end behavior family, no S2. OpenAPI, migrations, schema, grants, roles,
datasource/pool/key ring and mutation boundaries remain frozen. All residual owners
in cumulative-plan §7 remain assigned; nothing here represents acceptance.

## 4. Recomputed inventory

| Inventory | Current | Delta | Final |
|---|---:|---:|---:|
| Unit classes / methods | 83 / 788 | 6 / 60 | 89 / 848 |
| Container classes / methods | 34 / 556 | 2 / 36 | 36 / 592 |
| Total test-bearing classes / methods | 117 / 1,344 | 8 / 96 | 125 / 1,440 |
| Annotation-line scan | 1,345 | 96 | 1,441 |
| All test Java files (helpers included) | 159 | 10 | 169 |
| Failsafe container-smoke includes | 34 | 2 | 36 |
| All-POM `<include>` elements | 36 | 2 | 38 |
| Guarded production manifest files | 291 | 8 | 299 |
| Raw production files, V018 included | 292 | 8 | 300 |

Unit lengths8+8+18+10+8+8=60; container20+16=36. PA37-only historical
subtraction `[848-60,592-36] == [788,556]`, scan1441-96=1345. The only
existing architecture-test class/include mirror forms become89/36/36; they do
not acquire historical variants. Existing earlier historical values remain unchanged
because PA37 names join every subtraction. The current single comment-hit remains
unchanged, explaining scan1441 versus method1440.

The implementation allowlist is exactly five existing paths (two production filters,
one architecture test, POM, CI), eight new production files, ten new test files and
one non-code evidence report =24 paths. The context is not writable. Current guarded
manifest digest was reproduced as
`95c4eef4059fcc92d2408a493bbd89420a7fa5d5a5a3e5cdbd4dc164cc1623e0`;
the future final digest must be recomputed after implementation, not invented here.
No future XML/runtime evidence is claimed:848/592 are the required final exact XML
inventories, not reports produced at this plan gate.

## 5. Actual validation commands and stop

Commands run from the repository root (scratch validators/artifacts under `/tmp`):

```text
cp docs/gateflow/pa37-operations-catalog-product-read-foundation-plan.md /tmp/pa37-fix3-before.md
python3 /tmp/pa37-fix3-validate.py
diff -u /tmp/pa37-fix3-before.md docs/gateflow/pa37-operations-catalog-product-read-foundation-plan.md
wc -c -l docs/gateflow/pa37-operations-catalog-product-read-foundation-plan.md docs/gateflow/pa37-operations-catalog-product-read-foundation-plan-fix-3.md
sha256sum docs/gateflow/pa37-operations-catalog-product-read-foundation-plan.md docs/gateflow/pa37-operations-catalog-product-read-foundation-plan-fix-3.md docs/gateflow/pa37-operations-catalog-product-read-foundation-plan-fix-1.md docs/gateflow/pa37-operations-catalog-product-read-foundation-plan-fix-2.md backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsReadContext.java
```

An inline Python hash snapshot also recorded2,211 pre-edit repository files in
`/tmp/pa37-fix3-repo-before.json`; the validator compares every current file against
it, proving only the two permitted output paths differ. No Git cleanliness claim
depends on the managed worktree's unavailable Windows gitdir.

The validator parses all ten paths/FQCNs, method dictionaries and exact selectors;
compares current source to the actual CI dictionaries; recomputes all current/final
counts, earlier historical subtraction arithmetic, POM scopes, guarded manifest/digest,
context immutability and cursor size. It confirms every prior/new closure row and
retained scope boundary, rejects stale advisory-denial identity and obsolete POM/
historical-mirror wording, and verifies both outputs as strict UTF-8/LF with no
BOM, U+FFFD, conflict markers or trailing whitespace. `diff` exit1 is expected for
the bounded plan change. An initial scratch-validator inventory extractor skipped
the existing CI dictionary references; it was corrected to evaluate only the
existing dictionary-construction block. No repository implementation was changed
to satisfy the validator.

No npm/Maven/runtime/container/hosted test was run. Those cumulative-plan commands
remain implementation obligations. No source, test, POM, CI, migration, OpenAPI,
Control, review, prior fix artifact or product document was edited. No implementation,
staging, commit, push, PR, approval, ready transition, reviewer request, external
comment, merge, close or later-gate action was performed. Stop for Controller intake.
