# PA17 Sharing Public Admission And Ticket — Plan Fix 2

## Identity

- **Work unit:** `PA17-sharing-public-admission-ticket`.
- **Gate:** `fix`, round 2, after `plan review`.
- **Task label:** `pa17-public-admission-ticket-plan-fix2-gpt-20261106`.
- **Target:** `docs/gateflow/pa17-sharing-public-admission-ticket-plan.md`.
- **Scope:** only the target plan was revised and this artifact was added. No implementation, migration, test, OpenAPI, POM, CI, README, Control, review, private-path, Git, or remote mutation was performed.

## Accepted findings closed

- **CF-01:** Section 2.2 and §2.3 now define a bounded read-only/advisory-only preliminary admission/witness read, with transaction-scoped organization-member advisory locks only at ranks 7/8/9. A hit closes that read before the replay write transaction. A miss closes every transaction and lock before approved policy lookup and fresh proof. The final write transaction alone is authoritative: it takes the lifecycle locks, re-reads current facts, locks the local grant, rechecks the key and predicates, discards proof if a concurrent request committed the key, and inserts only a still-missing active key at the last slot. No provider, Redis, or media I/O occurs with a transaction or lock open.
- **CF-02:** Section 2.3 explicitly supplies `admitted_at = T` rather than relying on V009 `DEFAULT now()`. `T` is the single post-grant-lock `clock_timestamp()` sample; `admissionExpiresAt = grant.expires_at` and `sessionExpiresAt = min(T + approved Q22 TTL, admissionExpiresAt)` are frozen into the admission witness, with positive-window expiry/TTL calculations using post-lock clock samples.

## Preserved contract

- Confirmed goal, one-slice decision, and non-goals remain unchanged.
- V001–V011 definitions, lifecycles, hashes, V009 behavior, privilege-only V012, and PA16 read-only role remain unchanged.
- The exact 35-path allowlist, named-interface boundary, provider seams, event identities, error precedence, persistence gating, and race-to-method mapping remain unchanged.
- Test arithmetic remains `20/103` Surefire plus `9/162` Failsafe equals `265`, with the exact named method inventory and counts. Existing replay, preliminary-miss/concurrent-commit, fresh-success, proof-unavailable/no-row, and no-I/O-under-transaction behavior are mapped to the existing methods without adding methods.

## Static validation

- Confirmed the target contains no fresh-proof step while the grant row lock or PostgreSQL transaction is held; fresh proof is explicitly outside all transaction and lock scopes.
- Confirmed explicit `admitted_at = T` and same-`T` witness expiry values.
- Confirmed target baseline V001–V011 SHA-256 manifest, V012 least-privilege statements, exact allowlist count, and `20/103 + 9/162 = 265` arithmetic remain present.
- Validation is document-only. No Maven, PostgreSQL, Redis, Flyway, build, CI, or runtime command is claimed.
