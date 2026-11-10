# PA24 final closeout - K20 Secret Manager live-rotation acceptance

## Status

- Work unit: `PA24-sharing-media-secret-manager-live-rotation-and-platform-acceptance`
- Gate: `final closeout`
- Status: **final closeout pass**
- Branch: `codex/pa24-sharing-media-secret-manager-live-rotation-and-platform-acceptance`
- Base/merge point: `d46140d012acb42574503dc049c2b8778fc116e5`
- Accepted PR-review head: `25f43af0c2b32f82297d80579be1728b0fa433c8`
- Draft PR: https://github.com/zh94695424/one-call/pull/35
- PR state at closeout: `OPEN`, `isDraft=true`, `mergeable=MERGEABLE`
- Hosted checks: `no checks reported`; nested `docs/gateflow/*.md` is outside the workflow's `docs/*.md` path filter. This is an explicit non-result, not a CI pass claim.

## What changed

The final PR range contains exactly 18 docs-only paths: the cumulative PA24 plan, Plan Fix 1, the composed S1 runbook/validation document, Control bookkeeping, four plan reviews, six code reviews, and four PR reviews. There are no product, test, POM, CI, migration, ACL, API, protocol, deployment, topology or V001-V008 changes.

The implementation-free acceptance decision remains justified. The merged PA23 K20 provider already supplies:

- exact explicit SecretName/VersionId selection through the direct CVM attached-instance role;
- strict schema-v1 bundle parsing with exactly one ACTIVE and zero/many VERIFY_ONLY keys;
- current-snapshot issue, replay and verification semantics;
- fail-closed error mapping with no file, last-good-key, alternate-version or dynamic fallback.

The S1 now defines the operator contract for immutable A/B/C versions, B time-valid overlap and full retention, C key absence and fresh K2, whole-cohort A-B/B-C/C-B transitions, rollback and retirement boundaries, protected evidence/redaction, and truthful timeout/retry/liveness limitations.

## Accepted findings

- Plan evidence-lifetime/classification finding: `fixed`. B valid-time overlap is separated from C key absence, full B retention is preserved, and optional time-valid C evidence is separately approved or `NOT EXECUTED`.
- CR-01 C-to-B whole-cohort recovery finding: `fixed`. The runbook requires whole-cohort quiesce/drain, complete restart, manifest/config verification and changed-state documentation.
- PR-35-A1 post-retirement recovery feasibility finding: `fixed`. Fixed-B restoration requires source/bundle/K1-presence attestation and fresh time-valid K2; expired old-K1 overlap is legitimately `NOT EXECUTED`. K1 issuance reactivation instead requires separately approved immutable R (`K1 ACTIVE + unchanged K2 VERIFY_ONLY`) and fresh time-valid K1. No single bundle is required to issue both K1 and K2.
- PR-35-A2 path-count finding: `fixed`. The accounting is 14 paths at initial PR review, 16 at the fix re-review candidate, and 18 in the final PR range.

Final independent gates passed with risks and zero remaining material findings: plan re-review, code re-review, aggregate Deepreview and PR re-review.

## Verification

The validation ran from a clean Linux extraction of accepted HEAD with no Tencent credentials or production keysource configuration.

- Toolchain: Temurin `21.0.13+9`, Maven wrapper `3.9.9`, Docker `29.1.3`, PostgreSQL `16.15`, Node `22.23.4`, npm `10.9.9`.
- Focused suite: `117/117`, failures/errors/skips `0/0/0`.
- Full `container-smoke clean verify`: Surefire `36/309` plus Failsafe `13/214` = `49/523`, failures/errors/skips `0/0/0`, exit `0`, 8m42s.
- Existing web-JAR smokes and complete dependency firewall: `72` approved coordinates, `65` packaged JARs, `8` dependency-only, `64` counterparts, one sole packager, seven approved collisions and `116` adversarial probes.
- All eight checked-in contract steps exited `0`.
- Production JAR: 39,257,237 bytes.

Durable evidence is retained under `D:\codex-temp\pa24-code-review-20261110.yhNPsc\a\evidence`:

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| `focused.log` | 12,235 | `7D4C181551098D7B4B69A9A2A9A2B616C49DBD6C2CE1BAF71B0A7B48AE52FC03` |
| `full-verify.log` | 458,026 | `EAB8020CE769C1FC718EFD23617DD2C2C64ACD2A341563CB4B3B01CF64EC502F` |
| `smokes-firewall.log` | 1,395 | `38C8AF65CC618CCD70598B58394A8F52509D87D60738121A850B18115C49F0D5` |
| `contract-regressions.log` | 26,688 | `7F5545EEEF80820B77D48801FCE9C0F8ED79FA1F61230E44F35D2EE730B6902F` |
| `validation-summary.json` | 957 | `57E754693B2772B3FF35A80E55E11D8FEDCD621ABE18398461E23A46FCAFBB29` |
| `inventory.log` | 348 | `76147467BD1F4988BF998069B48DC173E54A43229CDC5F738B8D39437850DF6C` |
| `report-summary.json` | 722 | `733E8EAEF0F432CFF27C4A6A0BDC2126B5A1FC7533803B987AA48B6F864BBD30` |

The final S1 is 65,308 bytes / SHA256 `B621E4C2F0876A71AD9C5A31E03E39FF17382FF98D43866F3B810E26A51C1265`.

## Residual risks and owners

All real cloud/platform rows remain `NOT EXECUTED`; this closeout makes no production-readiness, live-cloud, hosted-CI, approval or merge claim.

| Residual | Owner / destination |
| --- | --- |
| Real Secret Manager resource/version provisioning, immutable-version evidence and response identity | Platform/Security |
| CVM role attachment, CAM least privilege and environment isolation | Platform/Security |
| Protected harness/custody, validity/clock/skew/retention horizon and optional valid C cohort | Security/Sharing + Operations |
| Whole-cohort quiesce/drain, downtime and recovery authorization | Platform deployment + Security + Operations |
| SSM quota, latency, throttling, capacity and cost | Platform capacity/Security + Operations |
| CVM metadata/DNS/total-call liveness and transport retries | Platform/Security + Operations |
| Java/SDK String lifetime, logging/collector drift and advisory drift | Security + Build/Observability |
| Conditional R recovery bundle has no deterministic oracle | Security/Sharing + Platform deployment/Operations |
| Plan line 118's broad "validate both old credential populations" wording | Security/Sharing + S1 runbook owner; operative S1 validates only genuinely valid populations and treats expired/unknown overlap as `NOT EXECUTED` |
| Future repository revalidation after product/provider changes | Backend QA/build with a fresh approved runner |
| Q22 TTL/skew/propagation and outstanding replay/session horizon | Security/Sharing + Operations; deliberately unselected |

## Issue and merge handling

This was not a GitHub-issue work unit. No issue link, closing keyword, closeout comment or issue mutation was required or performed.

No merge, approval, ready-for-review action, reviewer request, external comment or branch deletion occurred. The repository is ready for the user to review and merge Draft PR #35 after acceptance.

## Next entry

After the user merges PR #35, start from the updated `main`, create a fresh managed worktree and `codex/` branch, and perform a new Phaseflow goal confirmation for the next independently scoped work unit. The retained next candidate is the database ACL/RBAC fix that was intentionally split from the sharing work, subject to fresh goal confirmation and its own Gateflow.
