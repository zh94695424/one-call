# PA21 plan fix 1 — closure record

- Task: `pa21-plan-fix1-gpt-20261109`; work unit: `PA21-sharing-media-production-provider-integration`; gate: fix after independent plan review; date: 2026-11-09.
- Cumulative plan changed: `docs/gateflow/pa21-sharing-media-production-provider-integration-plan.md`.
- This artifact records closure of all eight accepted findings from both independent reviews; it is not a new plan and contains no implementation.

## Finding closure

1. **Replay first credential:** the first committed event records only normalized issued/not-before micros and active `kid`; replay passes that metadata into the authority and reconstructs byte-identically under the recorded key. Verify-only keys are replay-only, never fresh issuance. Missing/disabled/malformed/legacy metadata is unavailable/conflict, never a replacement token. Advancing-clock, fresh-instance, one-event, overlap, and disabled-key tests are required.
2. **Window/accessor:** use `decision.grant().evidenceId()` and `EvidenceMediaObjectReader.resolve(...)`; require `Found`, positive duration, object ID, and valid metadata. Issue exactly `[Duration.ZERO, Duration.ofSeconds(durationSeconds))`, the current PA20 full-content authorization. Sub-windows remain deferred and are not issued; no schema/witness change.
3. **Architecture:** move credential implementation to legal `internal/credential`; add the three production files and authority test to `SharingQueryBoundaryTests` exact sets and scans. The playback scan continues to forbid the credential seam. The cumulative plan pins exactly 17 allowed paths.
4. **Constructors/integration:** enumerate and update `ShareAdmissionServiceTests.java:697`, `PublicShareAdmissionIT.java:455`, and `ShareAdmissionPersistenceConfiguration.java:115`. Require a real-authority PA19/PA20-compatible end-to-end test and distinguish PA19 requested-window checks from PA20 full-object transport.
5. **CI/source/XML:** `.github/workflows/ci.yml` is allowed and must update literal dictionaries, totals, line scans, and identity text. Measured accepted baseline is source/CI `29/196 + 13/204 = 42/400`, with 13 POM includes; XML is unavailable in this planning workspace and must be measured after implementation. Planned target is `30/225 + 13/212 = 43/437`, subject to replacing it with measured source=CI=XML identities.
6. **Binary contract:** the plan now gives a complete ordered field table, fixed widths, UUID order, the only u16 fields, strict UTF-8/Base64URL rules, size/overflow/truncation/trailing-byte rejection, deterministic epoch-microsecond truncation, exact Invalid/Unavailable mapping, and fixed independent golden-vector inputs.
7. **P2 issue:** legacy `issue(MediaCredentialRequest)` is always `Unavailable`, including with a valid configured provider; no unscoped token is returned. P2 ticket integration is explicitly later scope.
8. **Spring ownership:** one authority bean in `MediaPlaybackConfiguration` implements both interfaces; fallback ordering, visibility, profile matrix, zero/ambiguous/invalid key behavior, and same-object assertions are pinned.

## Exact scope and residuals

- Allowed implementation/test/CI/POM paths: 17, exactly as listed in the cumulative plan §9. Only `backend/pom.xml` is conditional, and only if its existing 13 Failsafe includes require a measured update.
- No public OpenAPI, database schema/table/column/lifecycle/migration, COS, KMS/Secret Manager, production secret, Q22, Redis, deployment, frontend, or external review mutation is allowed.
- Residual risks are limited to later production key custody/rotation, future sub-window policy, unavailable hosted/external validation, and maintenance of exact architecture/CI pins. Any missing authorized fact, unavailable replay key, public/schema change, or out-of-allowlist file is a blocker—not a reason to weaken the design.

## Verification identity

The final response for this fix must report the changed-path identity, cumulative-plan line/byte/SHA256, this artifact line/byte/SHA256, 1 slice, exact allowlist count 17, measured/planned source=CI=POM=XML identities, all findings above resolved, residual risks, and blockers. Stop after documentation verification; independent re-review follows.
