# PA38 aggregate re-review3 onward Controller intake and hash chain

## Scope

This Controller bookkeeping artifact records the peer-isolated aggregate
re-review3 onward intake, adjudication and the Control hash-chain endpoint after the
accepted documentation corrections for `PA38-AGG-RR3-01..04`,
`PA38-AGG-RR4-01..06`, `PA38-AGG-RR5-01..02`, all later label findings and the
current ledger verification.

It is supplemental evidence outside the original 32 implementation paths. No
product, test, migration, OpenAPI, POM, CI or evidence byte changes.

The current intake scope covers every aggregate re-review from RR3 onward. The
detailed rows below cover the RR3–RR6 freezes and adjudications; later
provenance is retained in Control and the durable review artifacts cited below.
Historical values are append-only and are not rewritten by later Controller
passes.

## Frozen review inputs

- Snapshot: `/tmp/pa38-aggregate-rereview.final.kiBVGJ/{a,b}`
- Snapshot inventory: 2,307 files / 70,424,483 bytes /
  `b2d80e8c14d9a14d877227ec699c6894b069d0984cdcd3811b93503d0a1d5891`
- Range diff: 1,825,917 bytes /
  `695cb988cb5704ea0e3255e28c7b4890114ba3b105462c2209826b638ca3da06`
- Reviewer A: `docs/reviews/code-review-20261119-164656.md`,
  12,835 bytes /
  `42d55293dae11227fd917849215220c8c94bf1454a16d67ed10735c6581fda64`,
  `pass-with-risks`, one Low
- Reviewer B: `docs/reviews/code-review-20261119-170056.md`,
  28,125 bytes /
  `6a7e1785756512467454cb540c760f52b56a1cc74bfe6bb9ffbcd48934f1b7c3`,
  `pass-with-risks`, one Medium and three Low
- Re-review4 freeze: `/tmp/pa38-aggregate-rereview.final.iqTsOs/{a,b}`,
  2,310 files / 70,473,262 bytes /
  `acb54f37354e8e90c902bc04dc81a8f1d4ec7fffe46cf6feba91f73af03d59d4`.
- Re-review5 freeze: `/tmp/pa38-aggregate-rereview.final.v7Zv0o/{a,b}`,
  2,312 files / 70,519,786 bytes /
  `bed4e85470969c5dbbe44f3899b502729d0ed697aedc5dc370be429c92a05241`.
  Its A/B artifacts were lost after host restart; Control preserves their hashes
  and outcomes.
- Re-review6 freeze: `/tmp/pa38-aggregate-rereview.final.zK2BOA/{a,b}`,
  2,312 files / 70,521,531 bytes /
  `08d01b2358612728784f911e672f5d2749e59f2f1b0c06e45725499a83ce4731`;
  durable A: `docs/reviews/code-review-20261119-194019.md`,
  17,206 / `e53f2d47e654dc157a42fab676babfdecdf95c85533ba694d6ec1db5988549c7`;
  durable B: `docs/reviews/code-review-20261119-195303.md`,
  31,039 / `1abde4c9d5b10fe01b8888b10cd36e71972413821f6a2f10c35d28f65d69cb1a`.
- Later peer-isolated review artifacts, all submitted before Controller intake:
  - RR7: `code-review-20261119-200113.md`,
    `4929bbd8650bf9b7c116a314fce2f2ca1eb98889f1dce3dcec290c4c88a52ad0`;
    `code-review-20261119-201606.md`,
    `79d548d51f6f1acee7bc7105e411a9a8ea155dd34661e48e081cfa0b4009ab87`.
  - RR8: `code-review-20261119-202446.md`,
    `3168868745a0c00e27127d78a76d4de96b6d25b6bd356b224be5f22367a65045`;
    `code-review-20261119-203543.md`,
    `468eb04a4fa0dcc16a9ab2f42323a1e32381bb22da0f4274a3b8e6e2e4963384`.
  - RR9: `code-review-20261119-204409.md`,
    `9be9cabadc09512f90fd2124be7946d6fbf95ae6ad6a2e637e65c420722c5011`;
    `code-review-20261119-205935.md`,
    `e2465c1e358c791189c32eea38fc95d4d5eb43e2e629617c5126f67510fbe354`.
  - RR10: `code-review-20261119-210900.md`,
    `b7c34f89cc5aa8c5ef96b43e2f9058244d8674cef892847b8d663d7e2e82179f`;
    `code-review-20261119-211846.md`,
    `fe9834f57b40b0f1c4a66d0abab1f773e7239fbc6a5d7b7acbbd7370229b9b1b`.
  - RR11: `code-review-20261119-212710.md`,
    `f42e468169ed8ec4befd2de6e4be254eeae9ae590892f2298ad0b2fdbc799d0a`;
    `code-review-20261119-213411.md`,
    `8ad8a550b12631be88946d5ae01cbe0cb6b1f534033d1eadfd12bc2efa92a8a6`.
  - RR12: `code-review-20261119-214350.md`,
    `29e48c9c0293dd2a4610dd6e64661270da750ce029f2ac76574e78cc06371cb6`;
    `code-review-20261119-214609.md`,
    `3f21c224baa50c5995fd7a147b6e16285363922756a5234876f4c312f53e48eb`.
  - RR13: `code-review-20261119-215316.md`,
    `ba8310d4212390c9b14235059434000d056cd914a3ed69803827cbd59abc18e8`;
    `code-review-20261119-220037.md`,
    `7eb10e1aecb92d280e8bc0370b8084b6b82a72cb044bcef05c5e66c68fc6c03d`.
- RR12 freeze: 2,325 files / 70,807,921 bytes /
  `1bba69514d5d0efe0990ffcfe7f8173505990e6647a098fb4ef007aec6405de6`.
- RR13 freeze: 2,327 files / 70,843,859 bytes /
  `a6dccf860de2fb06ec723de226976c0e4fdfbc43042bf8629f1dce5f38a190c0`.
- RR14 freeze: `/tmp/pa38-aggregate-rereview.final.vawSGk/{a,b}`,
  2,329 files / 70,882,372 bytes /
  `2e66d3dfba522c0ec44d30bb5260254570321d1f5c0c9313c9af218cfe0840c4`;
  range diff 2,288,400 bytes /
  `ff51c0bb3b3a5cd2522ab481f2744b8b1dbdf8ccd519364a93e848a632c605bb`.
  A artifact `code-review-20261119-221121.md`, 18,015 bytes /
  `7c6a1398a1989a1ca1c47ea83069a7cb8bface7485d66feedbaedde11dd04f68`;
  B artifact `code-review-20261119-221914.md`, 24,822 bytes /
  `07549f71f5938eeb3a656bf711c5696ee284bd08753e38824b8bc20285b319cf`.
- RR15 freeze: `/tmp/pa38-aggregate-rereview15.final.VuXqSk/{a,b}`,
  2,332 files / 70,933,884 bytes /
  `9cbceb5c7cf09ee7c1417170639b7dc1d4ef4099107d4cf28806b8cdf00eb130`;
  range diff 2,340,886 bytes /
  `4c2ec0ea2f410131438b6b282a1b11db856020bbec6341bd11b2e6dfdb5468e7`.
  A artifact `code-review-20261119-222945.md`, 11,494 bytes /
  `987595770cb07f057af42783daa63a486371178312f35d548aee8a7196c574fb`;
  B artifact `code-review-20261119-224139.md`, 23,817 bytes /
  `6d2abbc909ea2f3be2ed772ceac971a1807ad465cffdb6c6c6762628db2e1e31`.
- RR16 freeze: `/tmp/pa38-aggregate-rereview16.final.haKG1j/{a,b}`,
  2,335 files / 70,975,080 bytes /
  `e8f55b9b5c2308c2f7d03fadc85ca2bc9c583dd4fc62619fcf0f6c3ae98568eb`;
  range diff 2,382,954 bytes /
  `2e8daaa9b436299f4d7be26b0e84b16e0435ddbb12d2e6954418ea92ce695d53`.
  A artifact `code-review-20261119-230106.md`, 13,462 bytes /
  `d095eb088e4292bb113ca9800e7fabda601b1ab5fa60d3d06b7e153487623622`;
  B artifact `code-review-20261119-231244.md`, 27,500 bytes /
  `ea03cc8c7908c15061342b22384eeb66f212f9ecec6e7fb89010ba086cf6dfb3`.
- RR17 freeze: `/tmp/pa38-aggregate-rereview17.final.9KfMB4/{a,b}`,
  2,338 files / 71,020,458 bytes /
  `d92f8ff6fa4f692fc45b8618bb2347dbe34a310f5622da5c7b0d2dc8edde48de`;
  range diff 2,429,283 bytes /
  `e446dc81ca9c0aa8f30b05c499c592fcf10c304e7280d2e4fb37b4cb05cfa834`.
  A artifact `code-review-20261119-232234.md`, 14,315 bytes /
  `0eb426df6aeb6168a7e5372ca880dfd5daaff4905aec9ea33536d6a07c6d29be`;
  B artifact `code-review-20261119-232844.md`, 29,393 bytes /
  `c8ed150d9abaf053e0cb516f7935ecbf8022e500a0a3661f21207fd6e212f721`.
- RR18 freeze: `/tmp/pa38-aggregate-rereview18.final.I05Qr2/{a,b}`,
  2,341 files / 71,070,019 bytes /
  `cbcf2bd37ea432a232c74819eb67b82d3d86ac3c362c5d0b7d7083acb8eeef2b`;
  range diff 2,479,868 bytes /
  `65d4036bfa5d1100939a9098c2b20420947bf5c5679cfefbeb213e10713264a7`.
  A artifact `code-review-20261119-233813.md`, 17,252 bytes /
  `3c02ffd5227601da7ae27b967f9be04f231e30de970932a47aa2132f95f0943f`;
  B artifact `code-review-20261119-235421.md`, 27,431 bytes /
  `35963e87c5af1c9cf9b1f63da8e2723447f09944d87feb50da545432c8b6194c`.

Both reviewers submitted before Controller read either artifact.

## Adjudication

- `PA38-AGG-RR3-01` (`accepted`, Medium): relabel every remaining point-in-time
  Control custody/report/manifest claim. Values stay unchanged and historical;
  the latest explicit current line governs.
- `PA38-AGG-RR3-02` (`accepted`, Low): record the Control relabel policy and the
  predecessor digest.
- `PA38-AGG-RR3-03` (`accepted`, Low): pin the post-write Control digest here.
- `PA38-AGG-RR3-04` (`accepted`, Low): clarify that `P0001` can arise from the
  same-key/different-payload collision, the defensive missing-row branch and
  explicit validation branches; do not imply line 371 is the only source.
- `PA38-AGG-RR4-01` (`accepted`, Medium): correct the malformed 63-hex fix2
  digest in Control and this intake to the true 64-hex value at the RR4
  snapshot, `00c25cace29bebe6293b6ab7980ca37ac510a2a72e097a6480cbcf31503f2d64`.
  Later RR4 edits produce the current fix2 row below.
- `PA38-AGG-RR4-02` (`accepted`, Medium): restore the historical RR2 document
  row verbatim in Control.
- `PA38-AGG-RR4-03..06` (`accepted`, Low): add RR3 scope traces, complete the
  report's `P0001` enumeration, canonicalize finding IDs and make the intake path
  discoverable from Control.
- `PA38-AGG-RR5-01..02` (`accepted`, Low): remove the last historical-fix2
  current/next-gate claims, name the entered final gate dynamically from Control,
  and make every current section's linkage resolvable.
- `PA38-AGG-RR6-01..05` (`accepted`, Medium/Medium/Low/Low/Low): gate-anchor
  the remaining Control custody wording, mark the lost review5 artifacts,
  replace static final-gate references with Control pointers, and complete the
  Controller-intake scope/hash chain.
- `PA38-AGG-RR7-01..05` and subsequent RR8 closure checks are documentation-only
  label/pointer corrections recorded in Control and the plan/fix review
  artifacts; they do not change the 32 candidate bytes.
- RR9–RR13 added no product changes. Their durable review artifacts are recorded
  in Control; this intake retains their purpose as repeated checks that
  historical labels cannot override the current-status anchor.
- RR14 findings `PA38-RR14-A-01..04` and
  `PA38-AGG-RR14-B-01..05` are (`accepted`, documentation/ledger only). This
  pass appends the missing RR12/RR13 predecessor rows and freeze inventories,
  records RR13/RR14 provenance, and replaces completeness claims with explicit
  pointers. No product, test, migration, OpenAPI, POM, CI or evidence byte
  changes.
- RR15 A reported `pass`; RR15 B reported two accepted Controller-documentation
  findings. `PA38-RR15-B-01` corrected the pre-RR11 Control size to 2,734,495
  bytes and anchored each predecessor to its actual review freeze.
  `PA38-RR15-B-02` assigned floating `postgres:16` digest pinning to
  Platform/CI supply-chain hardening. Product bytes remain frozen.
- RR16 A reported `pass`; RR16 B reported two accepted Low Controller-label
  findings. This pass makes the intake scope gate-relative and uses explicit
  pre-review correction labels for every 2,737,446-byte Control state.
- RR17 A reported `pass`; RR17 B reported two accepted Low Controller-chain
  findings. This pass appends the RR16 and RR17 freeze/artifact rows and their
  predecessor Control states, and aligns the pre-review prefixes with the
  freeze that actually contained each state.
- RR18 A and B both reported `pass` with zero findings after independently
  reproducing the repaired chain, all live pins and the unchanged product
  boundary. No further fix loop is required.

## Control hash chain

- Pre-review3 freeze Control:
  `3e78deba78d1eef45ff05b5c0ba85c21363d32097e26d1ae1b0da7803118061d`
- The pre-RR4 intake snapshot of Control was 2,725,946 bytes /
  `065a77d3c8b087708f45f7a0002febff10fb7b675f2d51d6280facb26192aa9d`.
- Pre-RR5 Control (after RR4 correction): 2,728,818 bytes /
  `3ce3d4561c93a6a736695983eb9976ce817da137b9e374ee14697f06a4c0399b`
- Pre-RR6 Control: 2,730,120 bytes /
  `d0c0b33d74a3b5f77e231125a4ed0b3dd1c5471e7a0b14ad0d33f71e76a17711`
- Additional preserved Control predecessors encountered while fixing the chain:
  `0bd063b030c26c490ce06d209f66b874724806532424de62facda234b7b91700`,
  `ba924140d4e4989078aa01b7473a333bbdbec91b84be6784fcaadac3efc6aca2`,
  `696200dd991254a8bf58f2423224ec251ce86ca9a0a27e3f8764bb648adc9392`,
  `212f5d4d7c0c8141c0ad2923a46e2533cd526d0a9b4b8f75b76a81ec37148098`,
  `c0ef7bc4115641294ea2b728b4e530e0f93a2839cbb0b30679ebd78d10c5c044`,
  `8f0241fb213a2ccf6f0aa3a41b210ae7794906ca05bbb39e242b704c5db7f7aa`,
  `c6853deacba04d470fbbde9b71334704edd1caa7dc37075da187ec162a148169`,
  `dba5b50afce31e62c236a70e8dd70d9379ed844c3a0c5b0062b38340b8d137be`,
  `a6549d1de338cb01090af80296bb058edb8a98d9023069c33a76d4431da95d45`.
- Additional preserved Control predecessor for the RR9 correction:
  `268a608b6b4373976ff4a97aff8b32cec04216a6c9afbf8b0de21f53fb64b686`.
- Pre-re-review10 Control after RR9 corrections: 2,734,495 bytes /
  `937f3076412aaa7611c3e9dfd7ba19963b65fa6f7bf140d4d727d44a1fb1c380`
  (measured during the re-review10 freeze, `xj3xrN`).
- Pre-re-review11 Control after RR10 corrections: 2,736,455 bytes /
  `0b6892e934a6e426e087caafdbb8ebf2c001cfc964036bf99836e2d047439b2a`
  (measured during the re-review11 freeze, `Tf87Wj`).
- Pre-re-review12 Control after RR11 corrections: 2,737,446 bytes /
  `ab0326f91b59e657823a784584eb91571f2167d0aa37adb31e322136b3422838`.
- Pre-re-review13 Control after RR12 corrections: 2,737,446 bytes /
  `cfa82af2deed4fe9e72e3eb8760279fcbd10630c0f69aae061fe8ef6929796bd`.
- Pre-re-review14 Control after RR13 corrections: 2,738,108 bytes /
  `43bc968602912f4512f5e11d23dbce87d0b768504ebf57472c6753a5165ab16a`.
- Pre-re-review15 Control after RR14 corrections: 2,740,334 bytes /
  `f40cf68769e59f1be8d75de6cd916968a3f8569b9f1ac651c6e2b4f47601e10d`.
- Pre-re-review16 Control after RR15 corrections: 2,741,594 bytes /
  `0d9226ff1cafd21abeb0b5ef2d9466a5eb11cfb17177000988e93869a30efb17`.
- Pre-re-review17 Control after RR16 corrections: 2,742,700 bytes /
  `459ef73a924ba555510f9319a5f9697e230ae75341197540417d3da0d889b269`.
- Pre-re-review18 Control after RR17 corrections: 2,743,788 bytes /
  `e86b95216b2d88e7d91da78fe516a12c7fe2dc2648ebf9432a4a2b1d3abbe639`.
- Mutation policy: historical values are immutable; ambiguous status labels may
  be gate-anchored in place. Each pass appends its predecessor digest and
  freeze/artifact identities; terminal predecessor entries are never rotated or
  replaced. A supplemental artifact records the successor digest because a file
  cannot contain its own final hash.

## Corrected current documents

- Implementation report: 109,847 bytes /
  `4988956569b2376009cc517e6c412b56fae5d285f1f61ac9327e41c30dbaba2d`
- Aggregate fix2: 39,228 bytes /
  `cd04d368d0c8b42158ae4576e891df10a13c6ca40edad873fb41755f25c7937e`
- Aggregate re-review fix1: 22,920 bytes /
  `75cd97a0022d9b7d09d4c1fc3d8c5bdd7a9c38e8e717d241e5baa79255666b91`
- Aggregate re-review2 fix1: 25,519 bytes /
  `a2dbe55753189fdf9a29b3b6e291844d01baa0faa71254339866c81fee369139`

## Current candidate

- Original candidate: 32 paths / 572,661 raw bytes /
  `8b6ac652ecfab06f85d5166d71a8a275f824527057352fb4931c3bd31624e5f2`
- Only the implementation report differs from the prior
  `572,726 / 20fa30f3...`, `572,759 / 3e2e99f7...`,
  `572,762 / d5520ddd...`, `572,633 / 02da23e6...`,
  `572,633 / d091b8e9...`, `572,649 / ac6c90e4...`,
  `572,489 / d29c3465...`, `571,838 / 94a362d5...` and
  `571,762 / 235b0703...` bindings;
  reconciliation is `572,661 = 564,632 - 101,818 + 109,847`.
- Java remains 290/180, production manifest remains
  `310 / 1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045`,
  migrations remain V001-V018 with no V019, `git diff --check` passes and no
  tracked deletion exists.

## Residuals and next gate

`PA38-AGG-RR2-04` (mixed CRLF/LF), `PA38-AGG-RR2-05` (self-contained future XML
bundle custody), floating `postgres:16` digest pinning, Outbox delivery ordering,
platform retention/dispatch, shared-pool capacity, broad frozen ACLs, service
recovery and all other recorded risks retain their earlier
owners/destinations.

The RR18 peer-isolated aggregate review passed with zero findings and the
accepted deepreview checkpoint is complete. The current gate / next entry point
is `ready-to-open-draft-PR`. No merge, approval, mark-ready action, reviewer
request, external comment or issue mutation is authorized.
