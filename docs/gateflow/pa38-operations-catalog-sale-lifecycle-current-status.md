# PA38 operations catalog sale lifecycle — current status

## Authority

This file is the single current-status anchor for the PA38 work unit. Per-gate
plan, implementation, fix and review artifacts are immutable historical
evidence. Any words such as `current`, `final` or `next gate` inside those older
artifacts are subordinate to this file and to the current Control row; they must
not be used to infer present state.

## Source and candidate

- Branch: `codex/pa38-next-work-unit`
- Accepted slice commit: `f612c99589ca59124e341f984fc17f2209598167`
- Accepted aggregate-deepreview commit:
  `b791846e3ddc8d2e3292a7f1d517e9c207e5f088`
- Accepted aggregate-deepreview tree:
  `195e4a3e6aed4ee4cfe6049b41842049133d6d99`
- Draft PR: #49, `https://github.com/zh94695424/one-call/pull/49`
- Source/config CI evidence: run `44577373056`,
  `contract-regressions` pass and `java-runtime` pass at
  `3a0a3ea9c777dd16ba40dde970ac39d667b832bf`; tree
  `ddb5722f88d7cf6e433cd42b6fe8d3cd59318956`.
- Previous exact-head run `44570384137` also passed at `186b108…`; the two
  heads differ only by Controller documentation. Later PR review/closeout
  documentation commits likewise do not change the source/config candidate.
- Accepted PR-review commit:
  `07c7b056e3433ec7b0dbe93340a8311672a8c008`; exact-head CI run
  `44595668761` passed `contract-regressions` and `java-runtime`.
- Historical RR18-review candidate: 32 paths / 572,661 raw bytes / ordinal aggregate
  `8b6ac652ecfab06f85d5166d71a8a275f824527057352fb4931c3bd31624e5f2`
- Current PR-review candidate: 32 paths / 572,735 raw bytes / ordinal aggregate
  `db38b4375c29addd13bfbb8cacde90a172bc32469a736b6b70d7190768500555`
- Committed LF blob projection of the same 32 paths: 568,803 bytes /
  `53fccb695d05d796214fd8e15e8dfc3a915a614b4cd4a245979f8fdc17aebe79`.
  The raw shipped-snapshot and committed-blob identities intentionally differ.
- Production manifest: 310 /
  `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045`
- Java source/test: 290/180
- Migrations: V001-V018 only; no V019

## Current document bindings

| Document | Bytes | SHA256 |
| --- | ---: | --- |
| Implementation report | 109,847 | `4988956569b2376009cc517e6c412b56fae5d285f1f61ac9327e41c30dbaba2d` |
| Aggregate fix2 | 39,228 | `cd04d368d0c8b42158ae4576e891df10a13c6ca40edad873fb41755f25c7937e` |
| Aggregate re-review fix1 | 22,920 | `75cd97a0022d9b7d09d4c1fc3d8c5bdd7a9c38e8e717d241e5baa79255666b91` |
| Aggregate re-review2 fix1 | 25,519 | `a2dbe55753189fdf9a29b3b6e291844d01baa0faa71254339866c81fee369139` |

Historical artifacts remain available under their original paths and hashes.
The Controller intake
`docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview3-intake.md`
records the append-only correction chain, round-freeze inventories and review
artifacts. Control pins the live intake digest; a reader should follow that
chain section rather than infer completeness from historical prose.

## Review and validation status

- Aggregate reviews and re-reviews through RR18 were executed by independent
  peers under the Gateflow sequence. All material product findings are closed.
- Latest durable reviews:
  - `docs/reviews/pr-49-review-20261120-024321.md`, 14,681 bytes /
    `a8751117f3deef343fc7892cb012472159e86283003853d8512675e351395dd1`
  - `docs/reviews/pr-49-review-20261120-030147.md`, 34,783 bytes /
    `65028d42816d8e751e6080026e9c906237259110303099281d4634e8d21f82ee`
  - `docs/reviews/pr-49-review-20261120-021717.md`, 18,189 bytes /
    `4a53165dfd82f255d5397f33c6797d5399ceb9e6020ddf58bb8eedcf2044b9b4`
  - `docs/reviews/pr-49-review-20261120-023454.md`, 33,725 bytes /
    `221bbb5baf1a9d89b90746dece52ec30cdab2ff09930dcb0a301127d62ae05a5`
  - `docs/reviews/pr-49-review-20261120-001534.md`, 16,651 bytes /
    `b0b19bf97a8379700282f1e359181bb050ab9fd63bf4a99ec303e7e8fead815d`
  - `docs/reviews/pr-49-review-20261120-003856.md`, 39,095 bytes /
    `e8e4fad85d164e0845962be0c6c1d487efb79f04bb7d64a28487580ccd7017be`
  - `docs/reviews/code-review-20261119-233813.md`, 17,252 bytes /
    `3c02ffd5227601da7ae27b967f9be04f231e30de970932a47aa2132f95f0943f`
  - `docs/reviews/code-review-20261119-235421.md`, 27,431 bytes /
    `35963e87c5af1c9cf9b1f63da8e2723447f09944d87feb50da545432c8b6194c`
  - `docs/reviews/code-review-20261119-232234.md`, 14,315 bytes /
    `0eb426df6aeb6168a7e5372ca880dfd5daaff4905aec9ea33536d6a07c6d29be`
  - `docs/reviews/code-review-20261119-232844.md`, 29,393 bytes /
    `c8ed150d9abaf053e0cb516f7935ecbf8022e500a0a3661f21207fd6e212f721`
  - `docs/reviews/code-review-20261119-230106.md`, 13,462 bytes /
    `d095eb088e4292bb113ca9800e7fabda601b1ab5fa60d3d06b7e153487623622`
  - `docs/reviews/code-review-20261119-231244.md`, 27,500 bytes /
    `ea03cc8c7908c15061342b22384eeb66f212f9ecec6e7fb89010ba086cf6dfb3`
  - `docs/reviews/code-review-20261119-222945.md`, 11,494 bytes /
    `987595770cb07f057af42783daa63a486371178312f35d548aee8a7196c574fb`
  - `docs/reviews/code-review-20261119-224139.md`, 23,817 bytes /
    `6d2abbc909ea2f3be2ed772ceac971a1807ad465cffdb6c6c6762628db2e1e31`
  - `docs/reviews/code-review-20261119-221121.md`, 18,015 bytes /
    `7c6a1398a1989a1ca1c47ea83069a7cb8bface7485d66feedbaedde11dd04f68`
  - `docs/reviews/code-review-20261119-221914.md`, 24,822 bytes /
    `07549f71f5938eeb3a656bf711c5696ee284bd08753e38824b8bc20285b319cf`
  - `docs/reviews/code-review-20261119-215316.md`, 12,020 bytes /
    `ba8310d4212390c9b14235059434000d056cd914a3ed69803827cbd59abc18e8`
  - `docs/reviews/code-review-20261119-220037.md`, 25,691 bytes /
    `7eb10e1aecb92d280e8bc0370b8084b6b82a72cb044bcef05c5e66c68fc6c03d`
- Canonical code validation previously passed: smoke-class 8/8, focused 86/86,
  guards 17/17, PostgreSQL PA38 38/38, exact observer three sequential 1/1
  runs, clean full 97/934 + 38/630, and the unchanged 135-report CI/XML oracle.
- The host restart removed the WSL `/tmp` validation roots and the two
  re-review5 review files before import. Their recorded hashes and outcomes
  remain in Control; the current repository-contained review chain and source
  hashes are authoritative for continuity.
- No merge, approval, ready-for-review action, reviewer request, external
  comment, issue mutation, push or commit has been performed in this gate.
- PR re-review freeze: 2,347 files / 71,183,242 bytes /
  `b107b2fb83711cef033f0117f38c755da918d185e7e32d89bc2fa4e9e6a95a82`;
  PR diff 2,587,463 bytes /
  `31b95fda177b53eb5f8c22bb2d2fcbbc349569f1d3fc53f12916a3be7c85673b`.
- Accepted blocker-fix carriers:
  `.gitattributes` 231 bytes /
  `e5eceadf7d117a07044a7b4134eba98e20a51027eca21f25d86418fe8b1e4ad6`;
  PR-review fix1 5,000 bytes /
  `14203706cc1111557dba0342ce1e618f4494da492baae1f0aa081e263676dc4a`.
- PR re-review3 freeze: 2,349 files / 71,237,678 bytes /
  `47f9ac8f5d6801223824b8d95fac0db5a9c5fdad4289adbdea7a306717b0d0aa`;
  PR diff 2,642,787 bytes /
  `258335db62611305cee66086d1d80ef1bfd02aa4ff745cd91ada53f6819598eb`.

## Residuals

All deferred items retain explicit owners/destinations in Control and the
immutable review artifacts: mixed CRLF/LF repository hygiene (Backend/CI),
future self-contained XML bundle custody (Controller/Backend evidence
maintenance), floating `postgres:16` digest pinning (Platform/CI), Outbox
delivery ordering (Platform/Operations), audit/Outbox retention and dispatch
(Platform/Operations), shared pool capacity (Platform/Operations + Security),
broad frozen ACLs (Database/Security + Catalog-order), recovery-shape
validation and service test hardening (Backend), and later API/business slices
(API governance + Catalog-order).

`PA38-PRR49-B3-01` (aggregate-custody derivation wording) is deferred to
Controller evidence maintenance. `PA38-PRR49-B3-02` (`java-runtime` timeout
headroom) is deferred to Platform/CI and must be delivered as a source/config
change with its own exact-head green run, not as documentation-only work.

## Current gate

RR14–RR18 found only Controller-ledger inconsistencies and no product defect.
The accepted fixes append the missing predecessor/freeze rows, correct
predecessor sizes and correction-round labels, assign every residual owner and
record RR13–RR18 provenance. RR18 A and B both returned `pass` with zero
findings. Product code remains frozen. The accepted aggregate-deepreview
checkpoint is complete and Draft PR #49 is open against `main`. The current
gate is final closeout. The accepted PR-review checkpoint and exact-head CI
are green; final closeout records all findings, residuals and the next-entry
condition. The `java-runtime` timeout concern remains deferred to Platform/CI.
