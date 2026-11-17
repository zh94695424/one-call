# PA35 bounded plan fix 3 — author evidence and Controller intake

Task label: `pa35-operations-ivr-session-read-plan-fix3-gpt-20261116-01`  
Work unit: `PA35-operations-ivr-session-read-foundation`  
Date: 2026-11-16 (Asia/Shanghai)  
Gate: bounded `plan fix3` only; not implementation or independent acceptance.

## 1. Scope and exact plan transition

Only the cumulative plan is updated and this fix-3 artifact is added:
- `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md`
- `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan-fix-3.md`

The confirmed goal remains exactly the two frozen Operations IVR session GETs,
current opaque employee authority, safe list/detail projections and one
read-only REPEATABLE_READ snapshot. No goal drift, additional slice, endpoint,
field, V018, migration, grant, role, credential or media capability is added.
The plan is non-conditional and directly implementable; accepted findings and
review open questions have explicit dispositions below. External delivery
residuals are owner-routed obligations, not unresolved mapping branches.

| Cumulative plan identity | Before fix3 (reviewed fix2) | After fix3 |
|---|---|---|
| Raw SHA-256 | `79561fd08d1fd4ee99d8340381ae52fc14479526eadd02a8a472ee482a1dfea3` | `431c56117832b121dd3f9706231587640c5765c09475ee36d1ec6d5288301b68` |
| LF SHA-256 | `79561fd08d1fd4ee99d8340381ae52fc14479526eadd02a8a472ee482a1dfea3` | `431c56117832b121dd3f9706231587640c5765c09475ee36d1ec6d5288301b68` |
| Raw bytes | 61,496 | 69,073 |
| Lines | 853 | 944 |

Both versions use LF with final newline; delta +7,577 bytes/+91 lines. LF
normalization means CRLF replacement only. This artifact's final byte/line
counts and raw/LF hashes are reported at handoff, outside itself to avoid
self-referential measurements.

## 2. Effective inputs and authority

Read the active PA35 Control and cumulative plan/fix1/fix2, both effective
reviews, frozen OpenAPI/docs14/15/17, V002/V005/V008, PA25 production identity
runbook, docs18 capacity destination and current Operations authority, read
context, qualified persistence/pool, requests, cursor, owner readers, module
descriptors, directly cited baseline tests, CI and POM. Source/CI/POM inventory
is measured from current bytes, not inferred from a historical passing report.

Effective reviews (isolated snapshot artifacts, not copied or edited here):
- A3: `/mnt/d/codex-temp/pa35-plan-review-20261115-01-A3/docs/reviews/plan-review-20261116-004459.md`,
  41,968 bytes/130 lines, raw=LF SHA-256
  `1d419d9ca328b0b5d4eda39c65f6b4c4d0a13e93f82fe0ac9c05887b488c223d`.
- B: `/mnt/d/codex-temp/pa35-plan-review-20261115-01-B/docs/reviews/plan-review-20261116-000416.md`,
  36,498 bytes/356 lines, raw=LF SHA-256
  `62242e4fb4c151a485e2170be4e8ca78974b490adb8b519a4ed9de9f635239e5`.

Failed A/A2 provider runs are not effective review acceptance. Current Control
raw SHA-256 is `363581a58299787ba303065e62e39802306d9edc543bb50661a171a6c9be0446`
(2,467,336 bytes/5,845 lines); LF SHA-256 is
`d09e310859cdbb782265882e134ec882d81f4527d1293e88fdd130fdced240b1`.
Its drift from fix2's `e502d899...` and the review snapshot's `f559f341...`
is Controller-owned post-freeze bookkeeping, including accepted fix3 intake;
it does not alter the frozen plan inputs. Control is preserved, not repaired.

## 3. Accepted finding dispositions — author-corrected

**A3-01 / B-03 — authority contradiction closed.** Plan §3/§4 validates
persisted id/status/role/revision before the DISABLED check, matching V002 and
PA33/PA34: missing employee or wrong audience401; valid DISABLED403;
impossible role/status, null required fields, revision<1 or mismatched id503,
including corrupt rows that also say DISABLED. ACTIVE ADMIN/OPS alone proceeds.
There is no contradictory non-ADMIN/OPS403 branch. C3 changes exactly from
`disabledOrDisallowedAuthorityIsForbidden()` to
`disabledAuthorityIsForbiddenAndCorruptEmployeeFactsAreUnavailable()`.
The same C3 covers valid DISABLED403 and every corrupt-fact503 variant. C15
still covers whole-request data/lookahead503; C17 still covers unknown500.
G4/D6 mirror precedence. Only the plan-only C3 listing and future CI dictionary
entry change identity; no implemented identity exists to rename. The future
`pa35_unit_additions` C-class entry uses the new exact name from plan §8;
its length remains18, and the grand total remains112, not113.

**A3-02 — call presence closed.** Plan §5.2 explicitly requires only status
in IvrCallSummary. startedAt/endedAt/authoritativeSeconds are optional and
omitted on SQL NULL. V005 terminal shape is applied: ACTIVE/UNKNOWN iff
ended_at IS NULL; ENDED/FAILED/REJECTED require ended_at. Compare start/end
only if both exist; present authoritative_seconds must be>=0 with status
ENDED/FAILED. Optional NULLs are not corruption. G18/D12 exercise an ACTIVE
call with NULL ended_at and omitted endedAt, optional NULL start/seconds,
valid terminal variants and invalid shape503 without adding methods.

**B-01 — production identity prerequisite closed.** Plan §3/§12 names the
configured shared Operations read datasource's non-superuser login and its
required preexisting SET capabilities for operations_iam_role, ivr_role and
evidence_role. The current PA25 production identity runbook does not cover
this login/IVR edge. Platform/Database/Security owns its runbook update,
membership delivery and production SET/restoration verification. Missing
membership remains sanitized503, never self-grant or fallback owner/superuser.
No migration, role, grant script or credential is added in this slice; fixture
memberships are disposable test configuration. Production SET capability has
not been verified here and must not be represented as a local test result.

**B-02 — artifact provenance closed.** The frozen required artifactRoles
response schema is authoritative. Plan §5.2/§12 explicitly records that the
frozen detail x-onecall-a0-models/docs15 metadata omits evidence.evidence_artifact:
an A1/A0 documentation residual owned by Contract/Data, destined for their
metadata reconciliation backlog. The evidence owner reads only
evidence_record(id,status,session_id,call_id) and
evidence_artifact(evidence_id,artifact_role), using existing V008 facts/grants.
F4 explicitly permits that exact safe evidence table/projection and still
forbids media, recording, hashes, snapshots and protected columns. No public
field/contract or frozen documentation changes; no inferred snapshot source.

**B-04 — encrypted digest closed.** Plan §6 moves the32-byte canonical
filter digest into the encrypted plaintext after the28-byte anchor, matching
PA33 subject placement. Header is only version/key-id length/key-id (2+k).
Payload60, ciphertext60, GCM tag16, nonce12 => frame90+k=91–122 bytes,
canonical unpadded base64url122–163 characters for k1–32. AAD uses four-byte
big-endian length-prefixes for purpose/endpoint/sort/header, never the current
request digest. Tampered header/envelope or malformed authenticated payload
is400 CURSOR_INVALID; after successful decryption and payload validation,
constant-time digest mismatch is400 CURSOR_FILTER_MISMATCH. B2/B3/B4/B5/B8/
B9/B12 and D10/G16 cover lengths, no clear anchor/digest metadata, each AAD
component, encrypted-digest tamper and authenticated mismatch separately.
All existing method names remain; no parallel codec/ring/error vocabulary.

**B-05 — capacity residual closed.** Plan §12 adds a dedicated
Backend/Database/Capacity row for no global (created_at DESC,id DESC) index,
session-side OR scans on ivr_decision/evidence_record, and contention in the
existing shared maximum4 Operations read pool with connection3s/template5s/
statement2s budgets. Destination is docs18's volume model plus implementation
EXPLAIN evidence (unfiltered/status/subject/task pages and session-side OR
reads) and deployment sizing. Deadline exhaustion remains sanitized503;
no index, V018 or pool change is authorized.

**B-06 — exact timeout labels closed.** Plan §3 and capacity row state:
- Pool maximum4, connectionTimeout3000ms.
- TransactionTemplate REQUIRES_NEW/REPEATABLE_READ/readOnly timeout5s.
- statement_timeout2000ms.
- lock_timeout250ms.
- idle_in_transaction_session_timeout5000ms.

E4/H12 assert those exact settings without confusing template5s with a pool
property. The unchanged persistence datasource configuration is not added to
the29-path implementation manifest.

**Batch artifact-role lookup — closed.** Plan §5.2 uses one grouped query
for all collected evidence IDs, selecting only evidence_id,artifact_role;
UUID bind placeholders, GROUP BY both, ORDER BY evidence_id then ORIGINAL/
DERIVED. Empty evidence skips lookup; initialize each row to [] and merge by
ID, rejecting unexpected IDs/invalid roles; duplicate memberships collapse.
C13/G19/H3/F4 prove multiple IDs, one batch/no N+1, deterministic native UUID
and role order and empty arrays. No new test identity or implementation path.

## 4. Additional resolutions — no open question

- Frozen list A0 metadata is an allowed superset; twelve inbound summary
  fields do not require a decision read. Only present taskId adds the exact
  V005 inbound_session→call_record→outbound_call_task filtering semijoin.
- Missing contract-required call is a fail-closed503 detail assumption;
  the same session remains listable when no task filter is present.
- `[0-9]{1,3}` limit grammar already exactly matches PA33/PA34. Preserve
  leading-zero acceptance within parsed1–100; no parser-helper change.
- Reuse `OperationsSubjectReadService.Failure/Code` everywhere; do not add
  a parallel error vocabulary in service/controller/ports/codec.
- Control hash drift is Controller bookkeeping after the review freeze,
  not a plan-input mismatch requiring another edit or an unresolved question.

## 5. Recomputed inventory, hashes and arithmetic

Plan §7 remains29 contiguous rows,20 new/9 existing. Every existing path is
present and every new path absent. Row-order existence pattern (E existing,
N absent/new) is `EEENNNNNNNNNNNNNNNNNNNEENEEEE`. Rows01–03 and23–24 and
26–29 exist; rows04–22 and25 do not. The20 new paths remain9 production+
8 test classes+2 zero-test helpers+1 implementation report. No manifest row is
added or removed.

Plan §8 has112 unique class/method pairs and112 distinct method names:
A12+B12+C18+D16+E10+F8=76 Surefire, G24+H12=36 Failsafe. C3 is the only
planned identity rename; all other111 are unchanged. CI dictionary entries
derive exactly from §8 with C3's new name; no count change or extra method.

| Verification set | Surefire classes/methods | Failsafe classes/methods | Combined |
|---|---|---|---|
| Current measured source=CI; POM31 container-smoke includes | 76/704 | 31/504 | 107/1208 |
| PA35 new delta | 6/76 | 2/36 | 8/112 |
| Existing focused locksteps, zero delta | 2/25 | 1/12 | 3/37 |
| Required future focused source=CI=XML set | 8/101 | 3/48 | 11/149 |
| Required future full source=CI=POM=XML set | 82/780 | 33/540 | 115/1320 |

Static checks used CI's exact `@Test\s+(?:public\s+)?void\s+(\w+)\s*\(`
regex, evaluated only CI's in-memory named-map construction, and compared
every current per-class source method set to both CI maps. POM container-smoke
XML's31 includes exactly equal the current source/CI IT class set. The three
existing locksteps remain16+9 unit and12 IT methods; no new token/identity.
Token-lines1209+112=1321, including the one preserved PostgresContainerIT
comment at line2502. PostgresContainerIT remains64 ordinary methods.
POM container-smoke31+2=33; its exact additions remain the two PA35 ITs.
POM has two other include entries outside that profile (all-profile33→35);
the established POM33 gate refers only to container-smoke Failsafe includes.

Plan §9 preserves historical method totals
290→320→360→400→445→480→523→543→555→613→677→753→838→924→1014→1120→1208
and token totals291→321→361→401→446→481→524→544→556→614→678→754→839→925→1015→1121→1209.
Future CI subtracts only PA35 names from every pre-PA35 historical calculation;
pa35_start_totals=[704,504]/token1209, final[780,540]/token1321. Early Surefire
aggregate704→780, exact XML780/540, class82/33 and POM33 checks remain required.
No reports are executed or claimed; future focused/full XML must prove exact
identities with zero failures/errors/skips, not merely counts.

All21 frozen raw/LF pairs in plan §10 were recomputed and match exactly:
OpenAPI+docs14/15/17+17 migrations. Exactly V001–V017 exist; no V018. Pair-set
digest is `1c0e5701d8c6a3cb5b04ae93946adc143e6ad98ef8bcaa4002911d42c013f285`,
SHA-256 over path-sorted UTF-8 path, NUL, raw digest bytes, LF digest bytes
for each of the21 rows. The exact42 digests stay listed unchanged in plan §10.

| Preserved input | Raw bytes / lines | Raw SHA-256 |
|---|---|---|
| fix1 | 5,558 /92 | `c5be3390fd5c541c49ed6aad6362d140689c8d2d984fe173c48e40234c3f0016` |
| fix2 | 12,711 /197 | `979e4fc9973e3b741a1d72639f8eea430ad08fe35fd8e7ab163f02267eb3e22a` |
| .github/workflows/ci.yml | 178,411 /2555 | `2c4addc3a09c2418869c6252e06be9257011d0fc34751289db6c15292887faf0` |
| backend/pom.xml | 9,861 /230 | `6a0e01aa11959149a55cd731118809d9efc9231ee11fd645292d490a2d9ea19a` |

## 6. Scope proof and mandatory stop

Git status is unavailable because the existing .git pointer resolves to the
unavailable `D:/codex-call/.git/worktrees/codex-call20`. It is not modified.
Before editing, a filesystem snapshot hashed every path except exactly the
two authorized outputs:2,120 files. SHA-256 over sorted UTF-8 path, NUL and
raw-file SHA-256 digest bytes is
`b223421a23154050918f752a40fd3a24e53641d95a2ac0d9d530c57d6d867b73`.
Final verification must reproduce the exact path set, individual file hashes,
count and aggregate. This covers Control, fix1/fix2, all sources/tests,
CI/POM, frozen files and private helpers. No other repository path changes.

Validation is read-only static inspection/hash/inventory arithmetic only.
Exploratory corrections were local tooling issues: use python3 rather than
unavailable python, evaluate all historical CI map merges, and scope POM31
to container-smoke rather than the two unrelated includes. No project build,
Maven/container/runtime test, installation or production verification ran.

Stop for Controller intake and fresh independent A/B re-review. Author
completion is not re-review acceptance or authorization to implement. No
source, test, Control, existing artifact, frozen document, migration, CI/POM
or helper edit; no stage, commit, push, PR mutation, merge, approval,
mark-ready, reviewer request or external comment. No hosted-CI, live/cloud/
media/deployment or production-readiness claim. External identity, key,
Contract/Data documentation and capacity delivery remain explicitly owned;
a genuine future new-privilege/frozen-contract need requires new authorization,
not an implementation workaround or expansion of this one slice.
