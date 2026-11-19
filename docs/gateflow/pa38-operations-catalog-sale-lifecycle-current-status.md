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
- Historical RR18-review candidate: 32 paths / 572,661 raw bytes / ordinal aggregate
  `8b6ac652ecfab06f85d5166d71a8a275f824527057352fb4931c3bd31624e5f2`
- Current PR-review candidate: 32 paths / 572,735 raw bytes / ordinal aggregate
  `db38b4375c29addd13bfbb8cacde90a172bc32469a736b6b70d7190768500555`
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

## Current gate

RR14–RR18 found only Controller-ledger inconsistencies and no product defect.
The accepted fixes append the missing predecessor/freeze rows, correct
predecessor sizes and correction-round labels, assign every residual owner and
record RR13–RR18 provenance. RR18 A and B both returned `pass` with zero
findings. Product code remains frozen. The accepted aggregate-deepreview
checkpoint is complete and Draft PR #49 is open against `main`. The current
gate is the first PR-review fix: deterministic checkout EOL, push, and a green
exact-head GitHub Actions run before fresh PR re-review.
