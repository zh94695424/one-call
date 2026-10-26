# A4 生产容量与部署规格 — Plan Fix 2 Report

> Gate: `plan fix` ONLY (bounded fix 2)
> Task: `a4-capacity-planfix-ds-20261026-02` / provider `ds` / model `deepseek-flash`
> Base: `f3bcbd99a6f49cb64977358cea08e028bc634a54`; branch `codex/a4-production-capacity-deployment-spec`
> Writable scope: revised `docs/gateflow/a4-production-capacity-deployment-spec-plan.md` and this report only.
> Read-only inputs: `docs/gateflow/a4-production-capacity-deployment-spec-plan-fix.md`, `docs/reviews/plan-review-20261026-062330.md`, `docs/reviews/plan-review-20261026-071809.md`, `docs/gateflow/project-execution-control.md` A4 section, upstream `docs/12`-`docs/17`.

## 1. Task, scope, and non-actions

This is the second bounded plan fix for the A4 capacity/deployment work unit. The binding goal is unchanged: a parameterized Tencent Cloud + private COS + carrier-room IVR capacity/deployment specification with a small deterministic Node 22 calculator, two behavioral slices (S1 arithmetic/unknown propagation and S2 deployment/recovery/capability evidence/discoverability), and no invented production numbers.

- No production values, region/AZ/SKU, price, retention, SLO/RPO/RTO, concurrency, or measured throughput was selected or synthesized as approved data.
- No new slice, public contract, endpoint, protocol event, security policy, or generic policy/validation engine was added.
- No calculator, validator, template, fixture, prototype, screenshot, controller, review, or upstream document was written. The only writes are the plan and this report.
- No stage, commit, push, PR, issue, review gate, cloud action, or runtime test was performed.
- `productionReady` and `procurementReady` remain `false`; `cloudVerified` and `a3RiskClosed` are now fixed root booleans with value `false`.

## 2. Closure of N-01..N-05

### N-01 (high) — spool snapshot contract

**Closed.** The plan now defines one coherent start-of-outage snapshot contract:

- `SPOOL_DISK_BYTES`, `SPOOL_FREE_BYTES`, and `SPOOL_EXISTING_BYTES` are all as-of outage start.
- `used = disk − free` already includes `existing`; `peakOccupancy = used + SPOOL_NEW_PEAK_BYTES_PS × outage` and `avgOccupancy = used + SPOOL_NEW_AVG_BYTES_PS × outage` therefore add outage production exactly once.
- The derived end-of-outage backlog is `SPOOL_RECOVERY_BACKLOG_BYTES = SPOOL_EXISTING_BYTES + SPOOL_NEW_AVG_BYTES_PS × SPOOL_OUTAGE_S`. It is explicitly a derived metric, not a caller input, and is the only value used for drain.
- `F-SPOOL-03` drains this backlog, not start `existing`; continuing production is subtracted from gross recovery payload throughput exactly once. Zero backlog drains in zero time; positive backlog with nonpositive net throughput yields `DRAIN_IMPOSSIBLE`/`NOT_MET`.
- All spool byte quantities and rates are explicitly **payload**, not wire.
- Fixture `s1-04` now asserts zero outage production (backlog = existing), zero existing plus positive outage production (backlog > 0 and drain > 0, never zero), existing plus zero production, no double count, consistency, and no negative/Infinity output.

### N-02 (high) — typed contract contradictions

**Closed.** The Unit enum and validation rules are now complete and scoped:

- Unit enum adds `hertz`, `bit`, `transactionsPerSecond`, `opsPerSecond`, `messagesPerSecond`; the alias map and `MEASURED_ROLE[].demandUnit` mapping are explicit.
- §4.1 defines named `scalarKind` contracts: `Ratio01`, `WireOverheadRatio`, `UtilizationRatio`, `VersionFactor`, `CopyFactor`, `DerivedByteRatio`, `NonNegativeRate`, `NonNegativeMeasure`, `DurationSeconds`, `DiscreteCount`, and `DailyVolume`.
- The old global `ratio 0..1` rule is removed. `RATIO_OUT_OF_RANGE` now applies only to the named bounded ratio/factor kinds. A **derived** utilization >1 is valid and yields `NOT_MET`; an **input** `Ratio01` utilization >1 is `RATIO_OUT_OF_RANGE`.
- `VersionFactor >= 1`, so fixture `versionFactor = 1.2` is valid. `CopyFactor >= 0` and `DerivedByteRatio >= 0`.
- `COUNT_NOT_INTEGER` is scoped to `DiscreteCount` fields; derived channels/in-flight/average-in-service values may be fractional (for example `0.5 calls/s × 7.4 s = 3.7 channels`).
- Fixtures `s1-02`, `s1-06`, and `s1-08` assert the version-factor, utilization, fractional-average, unit-enum, and integer-scope cases.

### N-03 (medium) — AI dimensional semantics

**Closed.** `F-AI-01` now separates two different quantities:

- `loadRatio = AI_TASK_ARRIVALS_PS / AI_TASK_SERVICE_RATE_PS` is a dimensionless load ratio. `AI_TASK_SERVICE_RATE_PS` is explicitly the measured **aggregate pool** completion rate, not a per-worker rate. `loadRatio` is not average in-service tasks; it is a ratio metric, may exceed 1, and carries `LOAD_RATIO_NOT_OCCUPANCY`.
- `avgInServiceTasks = AI_TASK_ARRIVALS_PS × AI_MEAN_SERVICE_S` uses the new typed `AI_MEAN_SERVICE_S` mean service-time input; it is the Little mean in-service task count, may be fractional, and is not a peak guarantee.
- `aiBytesPerS = AI_TASK_ARRIVALS_PS × AI_TASK_BYTES` remains independent.
- Zero semantics are explicit: arrivals=0 yields zero for all three metrics with `ZERO_FACTOR_SHORT_CIRCUIT`; missing service/mean-service/bytes are recorded as missing rather than turning zero demand into unknown demand; arrivals=0 with an explicit zero service rate adds `IDLE_ZERO_DENOMINATOR`; arrivals>0 with zero service rate gives `DIVISION_BY_ZERO`/`NOT_MET`; malformed leaves are still reported as `INVALID` even beside a zero arrival rate.
- Fixture `s1-05` asserts the ratio/task distinction, aggregate-pool definition, fractional values, and zero/malformed behavior.

### N-04 (medium) — upload catch-up basis

**Closed.** The upload catch-up path now shares the spool payload basis and never relabels payload as wire:

- `SPOOL_RECOVERY_TOTAL_BYTES_PS`, `SPOOL_CONTINUING_BYTES_PS`, and the end-of-outage backlog are payload bytes/bytes per second. Callers with only wire measurements must supply a payload measurement or an explicit overhead conversion; the calculator does not guess.
- `F-UP-04` reports `backlogCatchUpPayloadBits = (backlog > 0 && net > 0) ? net × 8 : 0` in payload bits/s.
- `backlogCatchUpWireEstimate = payloadBits / (1 − UPLOAD_WIRE_OVERHEAD_RATIO)` is produced only when a complete overhead ratio exists and is marked `ESTIMATE_ONLY`. If overhead is missing, payload remains `COMPLETE` and wire is `null` + `INCOMPLETE` with the missing input recorded; zero overhead is never assumed.
- `ongoingWire`, payload catch-up, and the explicitly measured combined wire value are reported separately; `backlog = 0` yields zero catch-up; positive backlog with nonpositive net yields zero plus `DRAIN_IMPOSSIBLE`/`NOT_MET`.
- Fixture `s1-03` asserts the basis, estimate labeling, missing-overhead behavior, and separation from ongoing/combined upload.

### N-05 (low) — DTO field mismatch

**Closed.** The fixed output DTO now contains root fields `cloudVerified: false` and `a3RiskClosed: false`. These are constants: caller input or capability assertions cannot change them; attempts are `CALLER_CONTROL_FORBIDDEN`. `F-CAP-01`, §5.2, §5.4, `s1-09`, `s2-02`, and `s2-03` assert the fields and the continued `NO_CLOUD_VERIFICATION` / `A3_RISK_NOT_CLOSED` limitations.

## 3. Prior finding regression check (F-01..F-09)

| Finding | Regression check in fix 2 | Status |
| --- | --- | --- |
| F-01 S1/S2 template count | S1 still asserts exactly 7 groups; S2 still owns the 9-group template assertion; no new slice or group was added. | no regression |
| F-02 spool catch-up/double subtraction | Gross recovery payload throughput minus continuing payload once; the new snapshot/backlog contract strengthens the fix without changing the gross-minus-continuing rule. | no regression; strengthened |
| F-03 N-1/headroom/topology | F-NODE-01..06 unchanged; `Ratio01` headroom and utilization contracts remain consistent with the same-headroom N-1 check. | no regression |
| F-04 backup truthiness/zero RPO/excluded vs missing | F-BACKUP-01/02, typed `included`, zero-RPO validity, `asOf`, and distinct `DEPENDENCY_EXCLUDED`/`DEPENDENCY_MISSING` are unchanged. | no regression |
| F-05 upload bandwidth surface | F-UP-01..04 preserved; F-UP-04 now reports payload and optional wire estimate separately. | no regression; strengthened |
| F-06 derived partial/global unknown | Metric-level partial outputs, independent derived ratio/retention/version, explicit-zero `DISABLED`, legal-hold increment, and backup-copy independence are unchanged; derived factor kinds are now explicitly `VersionFactor`/`DerivedByteRatio`. | no regression |
| F-07 capability self-attestation | `CLAIMED_VERIFIED` remains caller assertion and `NOT_VERIFIED`; root `cloudVerified`/`a3RiskClosed` are now explicit constants and never cloud verification or A3 closure. | no regression; strengthened |
| F-08 JSON cannot represent NaN/Infinity | JSON fixtures remain strict-JSON; programmatic nonfinite probes and no coercion of string `"NaN"`/`"Infinity"` remain required. | no regression |
| F-09 zero/failure wording | Explicit-zero validity and equation-specific division handling remain; a new “validate before short-circuit” rule ensures malformed leaves are never swallowed by a zero factor. | no regression; strengthened |

## 4. Exact changed paths and hashes

| Path | Change | SHA-256 | Lines / bytes |
| --- | --- | --- | --- |
| `docs/gateflow/a4-production-capacity-deployment-spec-plan.md` | revised in place (fix 2) | `671192F522153C982EB1CFAAABFBC076381D09FCE665D341446BDA703FA68D49` | 489 / 76533 |
| `docs/gateflow/a4-production-capacity-deployment-spec-plan-fix-2.md` | new fix-2 report | controller computes after this write (self-referential hash is not embedded) | this file |

No other path was written by fix 2. `docs/gateflow/a4-production-capacity-deployment-spec-plan-fix.md` (fix 1) remains unchanged. `docs/README.md`, controller, reviews, upstream `docs/12`-`docs/17`, A0-A3 contracts, prototypes, and screenshots were read-only.

## 5. Validation performed and not run

**Performed (read-only/local text checks):**

- Full read of the revised plan for internal consistency, formula attribution, slice ownership, and N-01..N-05 landing points.
- Formula-ID read-only review: every referenced `F-IVR-*`, `F-API-*`, `F-NODE-*`, `F-PB-*`, `F-REC-*`, `F-UP-*`, `F-DB-*`, `F-QUEUE-*`, `F-AI-*`, `F-SPOOL-*`, `F-BACKUP-*`, and `F-CAP-01` remains defined; no stale `F-REC-08` or S1 `groups=9` expectation was found.
- Manual dimensional hand checks remain synthetic only: recording `64000/8×60=480000 B`, daily `48,000,000 B`, retained `30×1.2`; upload average `48,000,000×8/86400`; spool `used=disk−free`, `peak=used+peakRate×outage`, `backlog=existing+avgRate×outage`, `drain=backlog/(recovery−continuing)`; node `ceil(demand/(perNodeRate×(1−headroom)))`; AI `loadRatio` separate from `arrivals×meanService`.
- `sha256sum` on the plan: `671192F522153C982EB1CFAAABFBC076381D09FCE665D341446BDA703FA68D49`.
- `wc -l -c`: `489` lines / `76533` bytes.
- `file`: UTF-8 text, no CRLF line terminators.
- Read-only Grep scans on the plan for trailing whitespace, conflict markers, CR, and U+FFFD: no matches.
- Read-only Git observations: `git diff --ignore-space-at-eol --stat -- docs/README.md` is empty and `git diff --numstat -- docs/README.md` reports `150 150` in this session; fix 2 did not write README.
- `node --version`: `v22.23.4`. This only confirms the runtime; no calculator or validator exists to execute.

**Not run (not claimed):**

- No `deployment/capacity/**` calculator, validator, template, or fixtures exist at this plan gate, so `node --check`, `validate.mjs --template`, `--stage s1`, `--stage s2`, fixture execution, and nonfinite probes were not run. No test result is claimed.
- A programmatic Node text cross-check was requested but the sandbox required approval, so it was not run; formula-ID consistency was checked by read-only Grep review instead.
- No real line/SIP/CDR, IVR media, PostgreSQL/Redis/RabbitMQ, COS/KMS/Secret Manager/CAM, CDN/LB/media gateway, cloud audit, load test, recovery drill, benchmark, or procurement action was performed.
- No stage, commit, push, PR, issue, review gate, or cloud mutation occurred. No other gate was entered.

## 6. Remaining risks and ownership

All production values and evidence remain deferred to the original owners/destinations in plan §8. Fix 2 added no new residual risk class:

- Spool payload rates, outage production, recovery throughput, and continuing production remain Telephony/IVR and Operations measurements; the plan only defines their typed contract and arithmetic.
- Upload wire/payload measurements and overhead assumptions remain Telephony/IVR and Infrastructure/Security inputs; catch-up wire remains an estimate when derived from overhead.
- AI arrivals, aggregate service rate, mean service time, and bytes/task remain Backend/Operations measurements; load ratio is not a capacity certification.
- Region/AZ/SKU, KMS/Secret Manager/CAM/COS capability, WORM/object-lock, CDN/LB topology, DB HA/IOPS, and recovery drills remain Infrastructure/Security, Platform/Database, and Operations evidence; no A3 residual is closed and no cloud capability is verified.
- `productionReady=false`, `procurementReady=false`, `cloudVerified=false`, and `a3RiskClosed=false` are invariants; capability assertions can never change them.

## 7. Completion status

**Completed — author-complete, pending controller validation and an independent re-review.** N-01..N-05 are addressed in the plan, F-01..F-09 show no regression, and the two behavioral slices and writable path boundaries are preserved. No blocker remains. If implementation discovers an upstream contract conflict or requires a new public contract, it must stop and report rather than silently changing an accepted contract.

No commit, push, PR, issue, review gate, or cloud action occurred.
