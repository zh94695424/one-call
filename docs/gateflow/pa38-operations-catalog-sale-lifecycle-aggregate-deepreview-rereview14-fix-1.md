# PA38 aggregate re-review14 Controller fix 1

## Gate and scope

- Work unit: `PA38-operations-catalog-sale-lifecycle`
- Gate: aggregate deepreview fix after peer-isolated re-review14
- Frozen review input:
  `/tmp/pa38-aggregate-rereview.final.vawSGk/{a,b}`
- Review snapshot: 2,329 files / 70,882,372 bytes /
  `2e66d3dfba522c0ec44d30bb5260254570321d1f5c0c9313c9af218cfe0840c4`
- Reviews:
  - A: `docs/reviews/code-review-20261119-221121.md`, 18,015 bytes /
    `7c6a1398a1989a1ca1c47ea83069a7cb8bface7485d66feedbaedde11dd04f68`
  - B: `docs/reviews/code-review-20261119-221914.md`, 24,822 bytes /
    `07549f71f5938eeb3a656bf711c5696ee284bd08753e38824b8bc20285b319cf`

This is a Controller documentation/ledger fix. It does not change the original
32 candidate paths, tests, migrations, OpenAPI, POM, CI or evidence bytes.

## Adjudication

- `PA38-RR14-A-01` (`accepted`, Medium) and
  `PA38-AGG-RR14-B-01` (`accepted`, Medium): the intake now appends the RR12
  Control `2,737,446 / ab0326f91b59e657823a784584eb91571f2167d0aa37adb31e322136b3422838`
  and RR13 Control `2,737,446 / cfa82af2deed4fe9e72e3eb8760279fcbd10630c0f69aae061fe8ef6929796bd`
  rows. The terminal entry is no longer treated as a rotating slot.
- `PA38-RR14-A-02` (`accepted`, Medium) and
  `PA38-AGG-RR14-B-02` (`accepted`, Medium): current Control/current-status
  wording now points to the repaired chain and no longer claims a complete
  predecessor set before the rows exist.
- `PA38-RR14-A-03` (`accepted`, Low) and
  `PA38-AGG-RR14-B-03` (`accepted`, Low): Control pins the final live intake
  size and SHA256 after the intake write.
- `PA38-RR14-A-04` (`accepted`, Low) and
  `PA38-AGG-RR14-B-04` (`accepted`, Low): the intake now states that detailed
  rows below cover RR3–RR6 and explicitly identifies Control/artifact
  provenance for RR7–RR14.
- `PA38-AGG-RR14-B-05` (`accepted`, Low): Control now records the RR12, RR13
  and RR14 file/byte/inventory identities and the RR14 range-diff identity.

No finding is rejected, deferred or left needing more evidence.

## Changes

- `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview3-intake.md`
  appends the missing predecessor states, RR12–RR14 freeze rows, RR13/RR14
  durable artifacts and accepted RR14 dispositions, and replaces the rotating
  terminal policy with an append-only rule.
- `docs/gateflow/pa38-operations-catalog-sale-lifecycle-current-status.md`
  records the RR14 durable reviews, clarifies that the intake chain is
  authoritative, and replaces the overstated completion wording with the
  pending re-review15 gate.
- `docs/gateflow/project-execution-control.md` records the fix, the RR12–RR14
  freeze inventories, the intake/current-status pins and the next gate.

## Validation

- Imported review copies reproduce the frozen artifact sizes and SHA256 values
  exactly.
- All accepted findings have a concrete disposition and durable destination.
- Product candidate identity remains 32 paths / 572,661 raw bytes /
  `8b6ac652ecfab06f85d5166d71a8a275f824527057352fb4931c3bd31624e5f2`;
  production manifest remains 310 /
  `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045`;
  Java remains 290/180 and migrations remain V001–V018 with no V019.
- `git diff --check` and the final no-deletion check are required after the
  Control pin is written.

## Residuals

The product and operational residuals recorded in Control remain unchanged and
retain their existing owners/destinations. The RR14 findings are closed by this
Controller fix, subject to peer-isolated re-review15 over the repaired batch.

## Next gate

Run fresh peer-isolated aggregate re-review15 against the repaired repository
batch. Do not merge, approve, mark ready, request reviewers, send external
comments, or mutate an issue.
