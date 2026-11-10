# PA23 sharing media Secret Manager key source — final closeout

## Work unit

- ID: `PA23-sharing-media-secret-manager-key-source`
- Type: backend/security feature and production key-source integration
- Base: merged `main` at `8a8c2455ad96b9a6adf0daa4bac7549f371da64a` (PA22 / PR #33)
- Branch: `codex/pa23-sharing-media-secret-manager-key-source`
- Accepted PR-review checkpoint: `6654014e9d0e6f161dbefc1bcf3ced4450f9d086`
- Draft PR: `https://github.com/zh94695424/one-call/pull/34`
- Status: `final closeout pass`; work unit completed. PR remains OPEN/Draft for the user to merge.

## What changed

- Added one read-only Tencent Cloud Secret Manager adapter for K20 `MEDIA_PLAYBACK_SIGNING`.
- Pinned `com.tencentcloudapi:tencentcloud-sdk-java-ssm:3.2.46`.
- Used direct official `CvmRoleCredential`; environment/profile/default/OIDC credential fallback is forbidden.
- Reads one exactly configured secret name/version through `GetSecretValue`, requires exact response identity, and accepts only a bounded nonblank `SecretString`.
- Sends the validated UTF-8 bytes to the unchanged PA22 schema-v1 parser.
- Preserves the existing `MediaPlaybackSigningKeyProvider`, PA21 credential wire, sole PA22 authority, ACTIVE/VERIFY_ONLY overlap, exact replay and current-state checks.
- Added explicit `tencent-ssm` selection while preserving explicit `file` selection.
- `provider=file` ignores unselected/stale Tencent metadata.
- `provider=tencent-ssm` with any pre-value-resolution `bundle-path` presence creates no built-in provider and cannot fall back to file.
- Invalid/missing/conflicting selectors fail closed; zero/one/many provider ambiguity remains checked before any key read.
- Added SDK/client/provider architecture guards, production-JAR dependency firewall entries and exact test/CI inventories.

## What was verified

| Evidence | Result |
| --- | --- |
| Focused suite | 9 classes / 117 tests; zero failures/errors/skips |
| Surefire | 36 classes / 309 tests; zero failures/errors/skips |
| Failsafe / PostgreSQL 16.15 | 13 classes / 214 tests; zero failures/errors/skips |
| Total source=CI=XML | 49 classes / 523 tests; annotation scan 524 |
| Exact inventory validator | passed with current/historical counts and zero skips/errors/failures |
| Production JAR firewall | 72 runtime coordinates, 65 packaged JARs, 8 dependency-only, 64 counterparts, historical deltas 35/31, seven unchanged positive-first collisions and 116 probes |
| Default/web JAR smokes | passed in WSL/JDK 21.0.13 |
| GitHub Actions at accepted PR-review head `6654014e` | run `42758692475`: `java-runtime` SUCCESS (6m44s), `contract-regressions` SUCCESS (22s) |

No V001-V008, database/ACL/migration, public OpenAPI/DTO/controller, resource/YAML or deployment path changed. `.codex` remained untracked and excluded throughout.

Windows/JDK 21.0.12 exposed an AF_UNIX/WEPoll Tomcat loopback defect, and Docker Desktop's WSL random port proxy intermittently refused individual PostgreSQL containers. Verification therefore used WSL/JDK 21.0.13 and completed the final same-candidate XML set through reruns of only affected IT classes against the fixed `postgres:16.15-alpine` image. The exact-head hosted CI independently passed the full runtime and contract jobs.

## Review lifecycle and finding status

- GPT completed the code-generation-ready plan from official SDK/API, credential, transitive graph and firewall evidence.
- Independent A/B plan reviews found one Medium selector defect. It was accepted and fixed by PA23 FIX-1.
- Fresh A/B plan re-reviews independently closed the finding and found no new material issue.
- Independent A/B code reviews found no material issue.
- Independent A/B aggregate reviews of the complete 20-path work unit found no material issue.
- Independent A/B PR reviews of the complete 22-path PR diff found no material issue.
- No accepted finding remains open and therefore no final fix/re-review cycle is required.

## Residual risks and owners

| Residual | Owner / destination |
| --- | --- |
| `CvmRoleCredential` is CVM-only; non-CVM/TKE workload-identity deployment requires a new goal and design | Platform/Security |
| CVM metadata acquisition exposes no connect/read/call timeout; SSM exposes no total `callTimeout` or DNS deadline | Platform/Security runtime validation; Operations/SLO |
| Underlying OkHttp connection retry remains possible within one application invocation | Platform/Security/Operations |
| CAM least privilege, region/endpoint/capability, Secret Manager resource/version provisioning and live response-echo validation are not performed here | Platform/Security deployment acceptance |
| Every issuance/verification can perform one SSM read with no cache; quota, throttling, cost and latency require production analysis | Platform/Security capacity and Operations |
| Java/SDK `String` secret material remains in process memory; buffer clearing is not secure heap erasure or HSM isolation | Security |
| New transitive SDK artifacts require dependency/advisory and logging-drift monitoring | Build/Security |
| Several architecture checks are source-string based; future refactors may require focused test maintenance | Backend QA |
| Production selector values are case-sensitive exact literals; deployment configuration must use `file` or `tencent-ssm` exactly | Deployment configuration owner |

No unclassified residual risk, blocking open question or required product decision remains for PA23.

## Issue status

PA23 is not a GitHub issue. No issue link, closing keyword, issue comment or issue mutation is applicable.

## Next entry point

After the user merges PR #34, update `origin/main`, create a fresh managed worktree and `codex/` branch from the merge commit, and start a new Phaseflow goal confirmation for the next productionization unit. The tentative next work unit is `PA24-sharing-media-secret-manager-live-rotation-and-platform-acceptance`, owned first by Platform/Security, covering real CAM/CVM identity, explicit-version rollout/rotation, Secret Manager capability validation and measurable runtime limits. Do not inherit PA23 authorization, start from this unmerged branch, invent Q22 values, add production cloud fixtures, or treat this closeout as live-cloud acceptance.
