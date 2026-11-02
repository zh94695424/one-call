# PA11 aggregate deepreview fix 1

- Task label: `pa11-aggregate-fix1-gpt-20261102-01`
- Gate: `aggregate deepreview -> fix`
- Work unit: `PA11-customer-auth-api-foundation`
- Status: `aggregate-fix-1-complete`

## Scope

This bounded fix addresses only the eight controller-accepted findings. The opt-in web profile, provider-neutral opaque customer-session verifier port, fixed customer audience, and `GET /api/v1/customer/me` frozen CustomerContext contract remain unchanged. No login, OTP, Redis, provider adapter, TTL, additional API or mutation, schema/migration, OpenAPI, V001-V009, architecture/design/security document, commit, stage, push, PR or review-gate action was performed.

Bounded documentation correction `pa11-aggregate-fix1b-gpt-20261102-01` changes only `backend/README.md`, `docs/gateflow/pa11-customer-auth-api-foundation-plan.md`, and this artifact. The changed paths and runtime validation below describe the prior fix run; no full runtime validation was rerun for this documentation correction. Controller will independently rerun full validation.

## Changed paths

- `.gitignore`
- `.github/workflows/ci.yml`
- `backend/README.md`
- `backend/src/main/java/com/onecall/core/customer_iam/internal/security/CustomerBearerAuthenticator.java`
- `backend/src/test/java/com/onecall/core/customer_iam/internal/security/CustomerSessionBoundaryTests.java`
- `docs/gateflow/pa11-customer-auth-api-foundation-plan.md`
- `docs/gateflow/pa11-customer-auth-api-foundation-plan-fix-2.md`
- `docs/gateflow/pa11-customer-auth-api-foundation-s1-implementation.md`
- `docs/gateflow/pa11-customer-auth-api-foundation-s1-review-fix-1.md`
- `docs/gateflow/pa11-customer-auth-api-foundation-aggregate-fix-1.md`

## Accepted findings

1. README current state now labels PA10-S1 historical, separates the current PA11/shared runtime dependency summary, and describes current PA11 MVC/Tomcat/Jackson additions. The approved Jackson dependencies are `com.fasterxml.jackson.core:jackson-annotations` version `2.21` and `tools.jackson.core:jackson-databind` / `tools.jackson.core:jackson-core` version `3.1.7`. README states the exact 49 Surefire / 11 classes plus 84 Failsafe / 3 classes (64+10+10) / 133 combined inventory with zero failures, errors or skips; that statement is not a claim of a successful full rerun in this fix (see Validation).
2. Accepted plan section 2 and plan-fix-2 review-input tables now use committed `docs/reviews/plan-review-*` paths only; no other provenance was changed.
3. S1 implementation report now explicitly supersedes its historical pre-review-fix "final" identities with accepted diff `0f70cf59d40f9aee6f7234dfe57b36c2f25ebf6f2f9ac97cdf420bebbccf4d64` and JAR `f70f36d5c9aeeccf01139eafa627f881bd46ad3f41828238894d27abf79ea1ea`.
4. S1 review-fix-1 records that `0f70...4d64` is the SHA256 of `/tmp/pa11-web-probe/fix1-product-test-ci-readme.diff`, using the exact `sha256sum` command; product code was not changed for this item.
5. `CustomerBearerAuthenticator` now translates verifier `ObjectProvider.stream()` acquisition/creation runtime failures to sanitized `SERVICE_UNAVAILABLE` (503), while preserving 401 malformed/missing bearer, 503 zero/ambiguous verifier, 401 wrong audience, and existing verifier-failure mapping.
6. Existing `missingAmbiguousAndUnavailableVerifiersFailClosed` remains the sole test method changed; it now exercises an `ObjectProvider` whose `stream()` throws. No new test method was added.
7. CI dependency parsing now accepts five-field empty-classifier input and literal six-field Maven order `group:artifact:type:classifier:version:scope`, normalizing to `group:artifact:type:version:classifier:scope`. Self-tests cover both forms, malformed rejection, and assert exactly 82 adversarial probes.
8. Root `.gitignore` now ignores `build/` output.

## Validation

- Working-tree manifest: default Git was redirected through an invalid Windows-form `.git` path, but the mounted metadata at `/mnt/d/codex-call/.git/worktrees/codex-call9` was used for status and diff checks. The allowed-path manifest above is the explicit scope manifest; no other paths were intentionally edited.
- Focused command `JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -f backend/pom.xml -B -ntp -Dtest=CustomerSessionBoundaryTests test`: passed; exactly 5 tests, 0 failures, 0 errors, 0 skipped.
- CI YAML parser/self-test: extracted and executed from `.github/workflows/ci.yml`; passed both five-field empty-classifier and six-field Maven-classifier normalization, malformed-input rejection, full firewall checks, and exact `82` adversarial probes.
- Full `JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -f backend/pom.xml clean verify -Pcontainer-smoke`: attempted and failed before a valid inventory because the sandbox denied embedded Tomcat socket binding (`java.net.SocketException: Operation not permitted`) and Mockito could not self-attach its Byte Buddy agent. The run reported 49 Surefire methods with 25 errors, 0 failures, 0 skips; no 84 Failsafe result was produced. The required 49+84=133 green result is not claimed.
- `git diff --check`: passed using explicit mounted Git metadata; Git emitted only line-ending normalization warnings for the mixed WSL/Windows worktree.

## Hashes and evidence

- Prior accepted post-fix product diff: `0f70cf59d40f9aee6f7234dfe57b36c2f25ebf6f2f9ac97cdf420bebbccf4d64`.
- Prior accepted product JAR: `f70f36d5c9aeeccf01139eafa627f881bd46ad3f41828238894d27abf79ea1ea`.
- New final product-diff hash: not calculated; the worktree contains pre-existing untracked review/source material and the mounted Git index does not provide a clean single-base product diff for this bounded edit.
- This artifact hash: calculated externally after all edits; no self-referential hash is embedded.

## Residual risks and owners

- Maven/container execution and post-fix 133 inventory remain controller/Backend QA validation because this sandbox denies the required Tomcat socket/Mockito attachment operations.
- CI firewall runtime execution passed locally; hosted CI remains Backend-CI/controller validation.
- No new residual product behavior was introduced. No stop condition was reached beyond the tooling limitation: work stopped after the allowed files and artifact were edited; no out-of-scope fix was required.
