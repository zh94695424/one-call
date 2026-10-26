# A4 生产容量与部署规格 — Plan Fix Report

> Gate: `plan fix` ONLY
> Task: `a4-capacity-planfix-ds-20261026-01` / provider `ds` / model `deepseek-flash`
> Base: `f3bcbd99a6f49cb64977358cea08e028bc634a54`；branch `codex/a4-production-capacity-deployment-spec`
> Writable scope: revised `docs/gateflow/a4-production-capacity-deployment-spec-plan.md` + this report only.
> Read-only inputs: complete old plan, `docs/reviews/plan-review-20261026-062330.md`, `docs/gateflow/project-execution-control.md` A4 contract/gate record, `docs/12` §4/§5/§13/§17/§18, `docs/13`, `docs/14`, `docs/15`, `docs/16` §6-§7, `docs/17` §9/§16/§20.

## 1. Scope, non-actions, and goal alignment

The plan remains a parameterized Tencent Cloud + private COS + carrier-room IVR deployment/capacity specification with a small deterministic dependency-free Node 22 calculator. Two behavioral slices are preserved: **S1 capacity arithmetic/unknown propagation** and **S2 deployment/recovery/capability evidence/discoverability**. No third slice was added.

- No production numbers, provisioning, cloud action, public contract change, runtime implementation, protected registry replica, or generic policy engine was introduced.
- S1 template remains 7 groups; S2 is explicitly authorized to add 2 groups by additive template edit. S1 now owns correctness tests for every formula it implements, including API and node formulas.
- Slice success is defined by behavior assertions, not exact fixture counts; fixtures files/cases are grouped compactly.
- No calculator, validator, or fixtures exist at this gate. No Node command, `node --check`, `validate.mjs` run, or test result is claimed.
- No stage/commit/push/PR/issue/review/cloud mutation occurred. `docs/README.md` was not written by this fix.

## 2. Finding closure (F-01..F-09)

| Finding | Final plan fix | Status |
| --- | --- | --- |
| F-01 S1/S2 template count contradiction | §3/§6.2/§7: S1 template and exit assert exactly 7 groups; S2 explicitly authorized additive template edit to 9 groups; S1 fixture `s1-06` now tests API/node formulas; behavior assertions replace fixture-count success. | fixed |
| F-02 spool catch-up semantics/double subtraction | §4.2 uses `SPOOL_RECOVERY_TOTAL_BYTES_PS` (gross recovery-window throughput) and `SPOOL_CONTINUING_BYTES_PS`; F-SPOOL-03 computes `netRecovery = total − continuing`, subtracts continuing once, returns zero drain for zero backlog, `DRAIN_IMPOSSIBLE` for positive backlog with net<=0. F-SPOOL-01/02 distinguish `SPOOL_DISK_BYTES`, `SPOOL_FREE_BYTES`, `SPOOL_EXISTING_BYTES`; used=disk−free; existing is never subtracted from free again; peak/avg independent with `PEAK_AVG_WINDOW_UNQUALIFIED` when not same window/source. | fixed |
| F-03 headroom/N-1 and node topology | F-NODE-01 applies per-role headroom; F-NODE-02 applies the same headroom to N-1 and checks `deployedNodes>=requiredNodes`; F-NODE-04 stateful normal/degraded topology capacity (no active-active node math); F-NODE-05 shared media-gateway capacity (no separate node sum); F-NODE-03 requires CPU/RAM/connection/IO evidence; F-NODE-06 `roleVerdict` cannot be `FIT_EVIDENCE_COMPLETE` with missing resource evidence; `ESTIMATE` yields `ARITHMETIC_ONLY`; no SKU recommendations. | fixed |
| F-04 backup truthiness/zero RPO/missing vs excluded | F-BACKUP-01/02 use typed `included:boolean`, ScalarInput seconds/date fields, explicit top-level deterministic `asOf`, zero RPO valid, non-future dates, distinct `DEPENDENCY_EXCLUDED` vs `DEPENDENCY_MISSING`, absent drill => `INCOMPLETE`, observed>target => `NOT_MET` + `TARGET_NOT_MET`; `evidenceRef` coverage is evidence completeness only with `EVIDENCE_COMPLETENESS_NOT_RECOVERABILITY`; no wall-clock reads. | fixed |
| F-05 missing upload bandwidth surface | F-UP-01 daily average payload lower bound (`daily bytes×8/86400`, not peak); F-UP-02 measured concurrent peak×per-stream wire or explicit measured peak wire; F-UP-03 labeled overhead payload estimate; F-UP-04 separates ongoing, net backlog catch-up, combined only when explicitly measured; codec bitrate is never treated as upload speed; missing peak inputs => `PEAK_INPUTS_MISSING`. | fixed |
| F-06 derived partial/global unknown conflict | Output is metric-level with `partial`, group/root worst status, no zero-fill. `REC_DERIVED_BY_TYPE` has independent ratio/retention/version per type; `DISABLED` is explicit zero with provenance; `REC_BITRATE_BPS` is total all-channel; PCM fallback is `sampleRate×bitDepth×channels`; legal hold is extra bytes outside ordinary totals; backup copy factor independent of COS internal replicas; metadata five types and DB aux kinds are independent. | fixed |
| F-07 capability self-attestation | F-CAP-01 treats entries as caller assertions; `UNVERIFIED` => `NOT_VERIFIED`; `CLAIMED_VERIFIED` requires claimedBy/owner/source/evidenceRef/verifiedAt and still remains `NOT_VERIFIED` (never COMPLETE/VERIFIED); always emits `NO_CLOUD_VERIFICATION`, `A3_RISK_NOT_CLOSED`, `cloudVerified=false`, `DEPLOYMENT_EVIDENCE_INCOMPLETE`; regional/AZ/SKU validation stays manual with Infrastructure/Security. | fixed |
| F-08 JSON cannot represent NaN/Infinity | §6.1: JSON fixtures contain only JSON-representable invalid values; `validate.mjs` programmatically injects real JS `NaN`/`+Infinity`/`-Infinity` into numeric leaves and asserts `NUMBER_NONFINITE`; string `"NaN"`/`"Infinity"` is `TYPE_INVALID` (no coercion); every arithmetic step checks finite/safe-integer bounds. | fixed |
| F-09 zero/failure wording | §4.4: explicit zero with provenance/unit/source/owner is valid; division is checked only at actual division; defined `0/0` idle cases use `IDLE_ZERO_DENOMINATOR`; explicit zero-factor multiplication/division short-circuits with `ZERO_FACTOR_SHORT_CIRCUIT` (except peak-evidence formulas, which require peak inputs); addition/evidence/divisors are never zero-filled. | fixed |

## 3. Controller clarifications incorporated

- **Scalar leaf contract**: every numeric/enum leaf is `{value,unit,provenance,source,owner,approval}` with fixed Provenance/Approval/Unit enums; missing source/approval is never reported APPROVED; root `meta.status` cannot approve unrelated leaves; synthetic arithmetic is allowed only with `SYNTHETIC_TEST_ONLY`; unapproved real inputs stay UNAPPROVED. Collections, output DTO, status severity, `partial`, and stable error/reason codes are fixed in §4.1.
- **Little's law**: §2.3, F-IVR-05a, F-IVR-06, and §4.5 state `rate×meanDuration` is a window-qualified steady-state average only; independent observed A/B/shared/bridge peaks require window/scope and never guarantee peak/trunk admission; line recommendations remain unverified without adequate peak evidence.
- **CDN/upload boundary**: F-PB-03 is conditional cache arithmetic only, not delivery-topology approval and not permission to bypass bearer/generation checks; origin/client egress and shared media-gateway capacity are separately labeled; A3 CV-4 upload semantics remain unchanged.
- **AI/queue/DB**: F-AI-01 uses measured task arrivals/service rate/bytes (no recording-seconds×bitrate shortcut); F-QUEUE-01 defines backlog growth/drain; F-DB-01 reports index/WAL/replication/backup as independent inputs and never claims complete database sizing from row payload.
- **Stale/inconsistent content resolved**: all referenced formula IDs are defined (`F-IVR-01..07`, `F-IVR-05a`, `F-API-01/02`, `F-NODE-01..06`, `F-PB-01..03`, `F-REC-01..07`, `F-UP-01..04`, `F-DB-01`, `F-QUEUE-01`, `F-AI-01`, `F-SPOOL-01..03`, `F-BACKUP-01/02`, `F-CAP-01`); old `F-REC-08`/fixture-count/`groups=9` S1 expectations are removed; S2's impossible "do not enter implementation" stop is replaced with bounded stop conditions; S1/S2 docs/18 section boundaries and template ownership are explicit.
- **README statement**: `docs/README.md` has no A4 content change. The working tree has a pre-existing CRLF line-ending-only diff (`git diff --ignore-space-at-eol --stat` empty; `git diff --numstat` 150/150); this fix did not write it and does not repeat any "premodified" claim.

## 4. Changed paths and hashes

| Path | Change | SHA-256 | Lines/bytes |
| --- | --- | --- | --- |
| `docs/gateflow/a4-production-capacity-deployment-spec-plan.md` | revised in place | `D81B6608CBC7582A86FA2FA1AB91ACEE73F4122EFBA6C6C69020BE950B37C9D4` | 456 / 62247 |
| `docs/gateflow/a4-production-capacity-deployment-spec-plan-fix.md` | new fix report | computed by controller after this write (self-referential hash cannot be embedded stably) | see controller record |

No other path was written by this fix. `docs/README.md`, `docs/gateflow/project-execution-control.md`, `docs/reviews/plan-review-20261026-062330.md`, upstream docs, prototypes, screenshots, and contracts retain their pre-existing state.

## 5. Validation performed vs not run

**Performed (read-only/local text checks):**

- Full read of the revised 456-line plan for internal consistency, section/file boundaries, formula references, and slice ownership.
- Dimensional hand checks (synthetic arithmetic only):
  1. Recording: `64000 bits/s ÷ 8 × 60 s = 480000 bytes/recording`; `×100/day = 48000000 bytes/day`; `×30×1.2 = 1728000000 bytes`; `/2^30 ≈ 1.6093 GiB`.
  2. Upload average: `48000000 bytes/day × 8 ÷ 86400 ≈ 4444.44 bits/s` (daily payload lower bound, not peak).
  3. Spool: disk 100000000000 B, free 20000000000 B => used 80000000000 B; headroom 0.2 => usable 80000000000 B; new 1000000 B/s ×3600 s = 3600000000 B => peak occupancy 83600000000 B > usable, overflow 3600000000 B; existing 2000000000 B <= disk−free. Drain with recovery 2000000 B/s and continuing 500000 B/s => net 1500000 B/s => `2000000000/1500000 = 1333.33 s`.
  4. Nodes: demand 100 req/s, per-node 40 req/s, headroom 0.25 => usable 30 req/s; required `ceil(100/30)=4`; deployed 4 => N-1 capacity `30×3=90 <100` => NOT_MET; deployed 5 => `30×4=120 >=100` and deployed>=required.
  5. Little's law: `10 calls/s ×120 s = 1200` average channels; independent observed peak 1500 with usable 1400 => average fits but observed peak fails; no line fit from the average.
  6. Backup: approved RPO 0 and observed RPO 0 => MET; observed 5 s > 0 => NOT_MET.
  7. Queue: arrival 10/s, service 12/s, backlog 600 => net 2/s => 300 s drain; byte change `(10−12)×1000 = −2000 B/s`.
  8. Playback: `100×128000 = 12800000 bits/s`; `1000×60×128000/8 = 960000000 bytes/day`; hit ratio 0.8 => origin 192000000 bytes/day.
- Formula ID cross-check: every referenced `F-*` ID is defined in §4.3; no stale IDs found.
- Scoped hygiene: Grep scans for trailing whitespace, conflict markers, CR, and U+FFFD returned no matches; `file` reports UTF-8 with no CRLF in the plan.
- README check: `git diff --ignore-space-at-eol --stat -- docs/README.md` empty; `git diff --numstat` 150/150; `file` shows CRLF line endings — pre-existing line-ending-only difference, not an A4 edit.

**Commands actually run and results:** `wc -l`/`wc -c`/`sha256sum` on the revised plan (`456` lines, `62247` bytes, `D81B6608CBC7582A86FA2FA1AB91ACEE73F4122EFBA6C6C69020BE950B37C9D4`); `file` (UTF-8, no CRLF on the plan); Grep scans for `[ \t]+$`, conflict markers, `\r`, and U+FFFD (no matches); `grep -oE 'F-[A-Z]+-[0-9]+[a-z]?'` for formula-ID cross-check; `git diff --ignore-space-at-eol --stat -- docs/README.md` (empty) and `git diff --numstat -- docs/README.md` (`150 150`); `git status --short` (only the two A4 fix paths are new/untracked; README/controller/review retain pre-existing state). No index/stage operation occurred.

**Not run (not claimed):**

- No calculator or validator exists; `node --check`, `validate.mjs --template`, `--stage s1`, `--stage s2`, and fixture execution were not run. No test result is claimed.
- No real line/SIP/CDR, IVR media, PostgreSQL/Redis/RabbitMQ, COS/KMS/Secret Manager/CAM, CDN/LB/media gateway, cloud audit, load test, recovery drill, benchmark, region/SKU, or procurement action was performed.

## 6. Remaining risks and ownership

All production values and evidence remain deferred to their original owners/destinations in plan §8: Telephony/IVR (line/recording/upload measurements), IVR/Backend/QA (SLO/peak windows), Infrastructure/Security (CV-1..5, region/AZ/SKU, CDN/LB/media topology, WORM/COS capability), Operations/Backend (audit/Outbox/queue/AI volume), Platform/Database/Security (DB HA/IOPS, roles), Security/Legal (KMS, retention/legal hold). No A3 residual is closed; capability assertions are never cloud verification; recovery evidence completeness is never proof of recoverability.

## 7. Completion statement

- F-01..F-09 are fixed in the revised plan; additional controller clarifications are incorporated without adding a slice.
- Two behavioral slices preserved: S1 arithmetic/unknown/zero/partial with API and node tests; S2 deployment/recovery/capability evidence/discoverability.
- The revised plan is code-generation-ready but contains no production values, no implementation, and no procurement claim.
- No blocking question remains. If implementation finds an upstream contract conflict or needs a new public contract, it must stop and report rather than silently changing accepted contracts.
- This gate stops here: no implementation, review, stage, commit, push, PR, issue, or cloud action.
