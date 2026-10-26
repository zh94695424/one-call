# A4 Production Capacity and Deployment Specification Final Closeout

- Work unit: `A4-production-capacity-deployment-spec`
- Type: architecture-sensitive capacity/deployment specification deliverable
- Gate: `final closeout`
- Status: `final closeout pass`
- Date: 2026-10-26
- Draft PR: `https://github.com/zh94695424/one-call/pull/6`
- Accepted PR review checkpoint: `c942669c3fa66d967ec58749c7905b182a8c6c20`
- Reviewed-content push verified at: `c942669c3fa66d967ec58749c7905b182a8c6c20`

## What changed

- Delivered a parameterized production capacity and deployment specification for the approved Tencent Cloud, private COS and existing self-developed carrier-room IVR architecture.
- Added a dependency-free deterministic calculator and strict input template covering IVR call legs and bridged calls, business API demand, recording/storage, upload/playback, encrypted local spool, metadata/audit/queue/AI growth, measured node fit/failover, backup/recovery closure and cloud-capability evidence.
- Added nine S1 fixture groups and three S2 fixture groups with strict missing/zero/invalid ordering, provenance and approval rules, programmatic nonfinite-number probes, repeatability checks, exact fixed-set checks and fail-closed readiness gates.
- Added deployment placement, release/rollback, scaling, backup/recovery and capability-evidence checklists plus the documentation index entry.
- Preserved the accepted A0/A1/A2/A3 contracts and ownership boundaries. No runtime Java/UI implementation, cloud resource, procurement choice, Region/AZ/SKU, price, SLO/RPO/RTO, retention, line-concurrency value, public API/schema change or production-readiness claim was added.

## Verification

- `node --check deployment/capacity/calc.mjs`: pass.
- `node --check deployment/capacity/validate.mjs`: pass.
- Stage S1: pass with 86/86 fixture cases, 459 programmatic NaN/+Infinity/-Infinity probes and one deterministic double-run check.
- Stage S2: pass with 113/113 fixture cases, 471 nonfinite probes, three repeat checks, one deterministic double-run check and `readmeLink=true`.
- Both stages keep `productionReady=false`, `procurementReady=false`, `cloudVerified=false` and `a3RiskClosed=false`.
- S1 independent review and re-review passed after bounded fixes.
- S2 independent review found five issues; the DeepSeek bounded fix closed all five and fresh MiMo Flash re-review passed.
- Aggregate deepreview `docs/reviews/code-review-20261026-122613.md`: pass with zero material findings and all nine aggregate checks green.
- PR review `docs/reviews/pr-6-review-20261026-125332.md`: pass with zero material findings, no regression and no scope drift.
- GitHub branch and Draft PR #6 head were verified at `c942669c3fa66d967ec58749c7905b182a8c6c20`. The PR remains OPEN/Draft/Clean and reports no configured checks; CI absence is not a green signal.

These checks prove the static specification, formulas, fail-closed evidence model and deterministic reference calculator only. They do not prove real carrier-line capacity, IVR/media performance, Tencent cloud capabilities, restore success, SLO/RPO/RTO compliance, pricing or production readiness.

## Finding status

- Plan review `F-01..F-09` and re-review `N-01..N-05`: fixed before the accepted plan checkpoint.
- S1 accepted review findings `R2-02` and `R2-03`: fixed and independently re-reviewed. `R2-04` was rejected with reason as formula-faithful behavior, not a defect.
- S2 `F-S2-R1-01..05`: fixed and independently re-reviewed; all final states are `已修复`.
- Aggregate deepreview: zero material findings. N1 is a classified low-risk fixture coverage gap; N2 is deliberately stricter validation; N3 is a reviewer-environment limitation closed by controller evidence.
- PR review: zero material findings; the same three non-blocking notes retain their aggregate dispositions.
- No accepted finding remains unresolved and no blocking open question remains.

## Residual risks and destinations

| Residual | Owner | Destination |
| --- | --- | --- |
| Complete-call concurrency, inbound/outbound/shared line limits, arrival distributions, ringing and holding durations are not approved measurements | Telephony / IVR | Carrier-line measurement and load evidence before production sizing; map failure/limit scenarios into A5 |
| Recording format, bitrate, channel count, daily volume, upload peak/catch-up and playback/AI load remain unmeasured | Telephony / IVR / Backend / Evidence | Media pipeline benchmarks and production implementation tests; A5 evidence rows |
| Media, metadata, audit, backup, version and legal-hold retention values are undecided | Product / Operations / Legal / Security | Retention decision and launch review before storage sizing or deletion policy |
| IVR encrypted-spool outage window, disk snapshot and recovery-upload throughput need real measurements | Telephony / IVR / Infrastructure | Carrier-room outage/recovery drill and A5 timeout/recovery scenarios |
| Per-node throughput, CPU/RAM/connections/IOPS, N-1 behavior and stateful normal/degraded topology are unmeasured | Backend / Platform / Database / Security | Benchmarks, failure tests and deployment qualification before node/SKU selection |
| Region/AZ/SKU, price, budget and Tencent KMS/Secret Manager/CAM/COS/storage-class/object-lock capabilities are unverified | Infrastructure / Security / Finance | Cloud capability/price validation; keep `cloudVerified=false` and `procurementReady=false` until evidence exists |
| SLO/RPO/RTO targets and six-class restore observations are not approved or proven | Product / Operations / Database / Backend / Infrastructure / Security | Policy approval plus restore drills; map normal/timeout/recovery/permission-denied paths into A5 |
| A3 COS, WORM, Region/SKU, media-gateway transport and audit-volume risks remain open | Their existing A3 owners | Carry into cloud qualification, deployment validation and A5; a static A4 checklist cannot close them |
| Same-source `peak < average` correctly fails closed but lacks a dedicated fixture | Backend / QA | Add to `A5-production-acceptance-matrix-spec` or the production calculator implementation test suite |
| GitHub PR #6 has no configured CI checks | Platform / CI | Add repository CI before production merge policy is enforced |

Every remaining risk has an owner and destination. None is an unclassified blocker for this static A4 work unit.

## Documentation and external state

- Canonical specification: `docs/18-production-capacity-and-deployment-spec.md`.
- Deterministic calculator and validator: `deployment/capacity/calc.mjs` and `deployment/capacity/validate.mjs`.
- Template and behavioral fixtures: `deployment/capacity/inputs.template.json` and `deployment/capacity/fixtures/`.
- Execution truth: `docs/gateflow/project-execution-control.md`.
- Draft PR #6 contains the accepted plan, both accepted slices, aggregate deepreview and accepted PR review checkpoint.
- Initial Git HTTPS transport failed because the local DNS answer for `github.com` was unreachable. The controller used a one-command `http.curloptResolve` override to a reachable official GitHub edge for both non-force pushes. No global Git configuration, hosts entry or remote URL was changed.
- This work unit is not a GitHub issue. No issue link, closing keyword, closeout comment or issue mutation is required or claimed.
- No merge, approval, mark-ready action, reviewer request, external PR comment or branch deletion was performed.

## Next entry point

After the user merges Draft PR #6, update local `main` from `origin/main`. Then start a new Phaseflow goal confirmation for `A5-production-acceptance-matrix-spec`, following architecture section 18 item 6: map every critical state machine to normal, duplicate, timeout, out-of-order, recovery and permission-denied scenarios.

Use accepted A0/A1/A2/A3/A4 contracts as binding inputs. Do not begin A5 from this unmerged A4 branch, and do not inherit A4 authorization to invent production measurements, policies, cloud capabilities or readiness conclusions.
