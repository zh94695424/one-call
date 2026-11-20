# PA38 aggregate re-review17 Controller fix 1

## Gate and scope

- Work unit: `PA38-operations-catalog-sale-lifecycle`
- Gate: aggregate deepreview fix after peer-isolated re-review17
- Frozen batch: `/tmp/pa38-aggregate-rereview17.final.9KfMB4/{a,b}`
- Snapshot: 2,338 files / 71,020,458 bytes /
  `d92f8ff6fa4f692fc45b8618bb2347dbe34a310f5622da5c7b0d2dc8edde48de`
- Range diff: 2,429,283 bytes /
  `e446dc81ca9c0aa8f30b05c499c592fcf10c304e7280d2e4fb37b4cb05cfa834`
- Reviewer A: `docs/reviews/code-review-20261119-232234.md`, 14,315 bytes /
  `0eb426df6aeb6168a7e5372ca880dfd5daaff4905aec9ea33536d6a07c6d29be`,
  `pass`.
- Reviewer B: `docs/reviews/code-review-20261119-232844.md`, 29,393 bytes /
  `c8ed150d9abaf053e0cb516f7935ecbf8022e500a0a3661f21207fd6e212f721`,
  `pass-with-risks`: two Low Controller-chain findings.

This fix changes Controller documentation only.

## Adjudication

- `PA38-RR17-B-01` (`accepted`, Low): the intake now appends the RR16 and RR17
  freeze triples, all four review artifact identities, and their predecessor
  Control states (2,741,594 / `0d9226ff…` and 2,742,700 / `459ef73a…`).
- `PA38-RR17-B-02` (`accepted`, Low): the two surviving older states are now
  labelled `Pre-re-review10 Control after RR9 corrections` and
  `Pre-re-review11 Control after RR10 corrections`, matching the freeze that
  contained each state and eliminating the duplicate starter label.

RR17 A reported no findings. B's two Low findings are reproduced against the
surviving freeze bytes and accepted.

## Changes

- `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview3-intake.md`
  completes the RR16/RR17 append-only chain and corrects the round prefixes.
- `docs/gateflow/pa38-operations-catalog-sale-lifecycle-current-status.md`
  records the RR17 reviews and pending re-review18 gate.
- `docs/gateflow/project-execution-control.md` records the RR17 freeze/reviews,
  accepted findings, live pins and next gate.

## Validation

- RR17 A/B sizes and SHA256 values match the frozen outputs.
- The Control chain now includes each discovered predecessor once, in order,
  through the Control measured by re-review17.
- The 32-path candidate remains 572,661 raw bytes /
  `8b6ac652ecfab06f85d5166d71a8a275f824527057352fb4931c3bd31624e5f2`;
  Java remains 290/180; migrations remain V001–V018 with no V019.
- `git diff --check` and zero tracked deletions remain required after the final
  Control pin.

## Residuals

The existing product and operational residuals retain their explicit
owners/destinations. The two RR17 findings are closed subject to re-review18.

## Next gate

Run targeted aggregate re-review18 against the repaired batch. Do not merge,
approve, mark ready, request reviewers, send external comments or mutate an
issue.
