# PA18 Plan Fix 1 — Accepted Findings and Dispositions

- **Task label:** `pa18-sharing-media-authorization-plan-fix1-gpt-20261107`
- **Work unit:** `PA18-sharing-media-authorization-and-playback`
- **Gate:** `fix`
- **Runtime/provider:** Java/Spring MVC, PostgreSQL 16, existing PA17 seams, deterministic test media-provider seam; production Q22/provider values remain unavailable and fail closed.
- **Allowed repository changes:** the PA18 plan in `docs/gateflow/pa18-sharing-media-authorization-and-playback-plan.md` and this fix artifact only.
- **Final disposition:** bounded plan fix complete; Controller intake and fresh independent A/B re-review are next. No implementation or re-review is claimed.

## Binding scope retained

The fix retains the PA18 Goal Confirmation and adds no goal, endpoint, schema, migration, ACL, OpenAPI, frontend, production Q22/provider value, media delivery, or future-slice behavior. V001–V012 remain byte-identical. The plan remains one implementation slice and stops for renewed confirmation if the exact frozen boundary is insufficient.

## Accepted findings

| Finding | Exact plan repair | Final disposition |
| --- | --- | --- |
| A01 — `CustomerBoundaryFilter` blocks the route. | Added the exact production path to the 17-path allowlist. Pinned `publicMediaAuthorization` for the exact POST path, matching optional bearer behavior, exact same-origin request-URI check, `application/json` validation, and an MVC assertion that the path reaches the controller rather than `NOT_FOUND`. | Accepted and repaired in plan §2.1 and §5. |
| A02 — `CustomerApiBoundaryTests` constructor/import assertions. | Added the exact existing test path. Pinned the five constructor generic parameters and qualifiers, and the exact current 11 public-controller sharing imports plus `MediaAuthorizationRequest` and `ShareMediaAuthorizationService`. The existing architecture method is extended without adding an annotation. | Accepted and repaired in plan §2.2, §5, and §6.1. |
| A03 — CI source=CI=XML inventories are hardcoded. | Added `.github/workflows/ci.yml` to the allowlist. Pinned exact method additions, `103→119` Surefire, `162→176` Failsafe, `265→295` combined, and `266→296` source annotation inventory. Required source/XML inventory validation covers the full authoritative set; no new class/file is allowed. | Accepted and repaired in plan §5 and §6.3. |
| A04 — changed purpose must conflict for the same request ID. | Added admission-scoped pre-provider lookup across `share_access_event` rows by `requestKeyDigest`. Prior success/failure with a different purpose/event type conflicts with no second provider call or event. Added direct `PLAYBACK` then `SEEK` test. Frozen event ID formula is unchanged. | Accepted and repaired in plan §3 step 8, §4.1, and §6.2. |
| A05 — mode discovery and lock order. | Pinned non-locking token-digest mode/owner discovery; `NONE`/`EXTRACTION_CODE`/`DESIGNATED_MOBILE` work without a customer session; `ORGANIZATION_MEMBER` requires the trusted caller and ranks 7/8/9 before authoritative grant/admission/witness read/lock. Pinned 401-versus-404 precedence and ordering assertions. | Accepted and repaired in plan §3 steps 2–3 and §6.1/§6.2. |
| A06/B2 — provider seam could break PA17. | Explicitly preserves `ShareMediaCredentialProvider.issue(MediaCredentialRequest)` and the record unchanged; `DefaultShareAdmissionService.java` remains unchanged. Adds separate PA18 request/result types and `issueOrReplay` only in the allowlisted provider file. | Accepted and repaired in plan §4.4 and §5. |
| A07/B1 — replay and Q22 timing under-specified. | Split pre-provider semantic fingerprint from post-provider durable payload; removed provider-field dependency from the pre-check; pinned post-lock database-time expiry formula and strictly positive window; replay reads first durable expiry; policy lookup is outside locks; pinned policy/provider/service-absence mappings; event witness facts are durable admission-time facts; PostgreSQL append is sole conflict authority and provider output must match stored facts. | Accepted and repaired in plan §3 steps 9–10 and §4.2–§4.4. |
| A08 — `credentialDigest` must be removed. | Removed `credentialDigest` from all durable payloads, provider contracts, tests, and the design. Deterministic provider replay uses frozen event identity, semantic fingerprint, and first durable expiry facts. | Accepted and repaired in plan §4.3 and §4.4. |
| A09 — exact 409 code and semantic conflation. | Kept `SHARE_GENERATION_STALE` because it is the operation-listed 409 business code. Explicitly states this is a contract-forced overload for request-identity conflict, not current generation staleness; classifies the conflation as residual risk; forbids `SHARE_TRANSITION_CONFLICT`. | Accepted as precision and repaired in plan §3 step 8 and §8. |
| A10/A11/B3 — file, test, and annotation arithmetic. | Removed no-op `CustomerProblemWriter.java` and both package-info files; reused the existing nested response record; added only required filter, customer boundary test, and CI paths. Final allowlist is 17 paths: 10 production, 6 test, 1 workflow; 3 new and 7 modified production Java files. Pinned measured 56 test files and 75 test class declarations unchanged, `266→296`, `SharingQueryBoundaryTests 1→2`, and all target arithmetic. | Accepted and repaired in plan §5 and §6.3. |
| A12 — `SharingQueryBoundaryTests` additions were not exact. | Enumerated the public-admission file set, admission implementation set, named-interface/simple names, nested classes, command-path exclusion/classification, import prohibitions, no object-storage/Redis/COS/generic-media leakage, and exact new CI method name. Classifiers remain strict. | Accepted and repaired in plan §6.1. |
| B4 — selected IT validation omitted seven Failsafe classes. | Replaced the selected-IT validation with full `./mvnw -q -Pcontainer-smoke verify`, retaining compile, targeted unit/architecture, and full regression commands. The plan requires all nine existing container-smoke classes and authoritative inventory checks. | Accepted and repaired in plan §7. |

## Accepted precision questions

| Question | Exact plan repair | Final disposition |
| --- | --- | --- |
| Provider-failure `FAILURE` event trigger and payload. | Chosen behavior is explicit: after a durable valid decision reaches provider I/O, unavailable/malformed provider output appends exactly one deterministic redacted `FAILURE` event with `result=FAILED` and only `MEDIA_CREDENTIAL_UNAVAILABLE` or `MEDIA_CREDENTIAL_MALFORMED`; it never claims success. Pre-provider denial and policy unavailability append no failure event. Tests pin this. | Resolved in plan §3 step 10 and §4.5. |
| Duplicate response DTO risk. | Reuses existing `ShareAdmissionView.MediaAuthorization`; no separate response record/file or architecture surface is introduced. | Resolved minimal design in plan §2.2 and §5. |
| OQ2 provider-side conflict authority. | Provider is deterministic/replay-capable but not an event store. PostgreSQL append/reuse/conflict is the sole conflict authority; provider output must match durable event facts. | Resolved in plan §3 steps 8–10 and §4.4. |
| OQ3 concurrent duplicate provider calls. | Kept as a residual provider-concurrency risk; deterministic identity/fingerprint and the two-connection test are mandatory, and no lock spans I/O. | Residual, not a scope blocker, in plan §8. |
| OQ4 citation/count drift. | Corrected all cited arithmetic and validation expectations to the measured workspace values, including full container-smoke and source/CI/XML totals. | Accepted and repaired in plan §6.3 and §7. |

## Boundary and validation confirmation

The final plan allowlist is exactly 17 paths and excludes source/test/CI/OpenAPI files other than the explicitly required 17. It excludes `CustomerProblemWriter.java`, both package-info files, migrations, OpenAPI, POM, and all unrelated files. The plan requires:

```text
Surefire: 103 → 119
Failsafe: 162 → 176
Combined: 265 → 295
Source annotations: 266 → 296
SharingQueryBoundaryTests: 1 → 2
container-smoke: all 9 existing Failsafe classes
```

The implementation validation commands are compile, targeted Surefire including `CustomerApiBoundaryTests`, full `container-smoke` verify, full Maven test, and authoritative source=CI=XML inventory validation. This fix artifact does not claim any of those implementation commands were run.

## Residuals and gate result

- `SHARE_GENERATION_STALE` remains a contract-forced overload for request-identity conflict; the semantic conflation is recorded residual risk.
- Production Q22 TTL/skew/revocation-window/signing-key/Redis/media-provider values remain unavailable and fail closed.
- A non-idempotent production provider could receive concurrent identical calls; the provider seam and two-connection test require deterministic convergence without a database lock across I/O.
- Media gateway/COS/object resolution/streaming/range delivery remains a non-goal.
- No blocking question remains for the confirmed goal. Any schema/ACL/OpenAPI/production-value/allowlist expansion is a new blocking scope question.

**Gate result:** bounded fix complete; Controller intake and fresh A/B re-review next. No implementation, re-review, approval, readiness, or external action is claimed.
