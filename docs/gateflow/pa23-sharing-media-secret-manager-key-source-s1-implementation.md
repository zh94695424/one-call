# PA23 S1 implementation checkpoint

## Status

- Work unit: `PA23-sharing-media-secret-manager-key-source`
- Gate: implementation S1, accepted for independent code review only
- Accepted plan: `docs/gateflow/pa23-sharing-media-secret-manager-key-source-plan.md`, SHA256 `6264888343795715026FE4F6DA07834C1BC7C49A69DE2ABDAAF09B3CE4D1E779`
- Accepted plan checkpoint: `8ccba5bca804e1d0a63fcc813cd5f1b5ed7cbc17`
- Implementation start checkpoint: `055eb9bc`, `gateflow: start PA23 implementation`
- This artifact does not claim merge readiness, production readiness or live Tencent acceptance.

## Author execution

- Task: `pa23-secret-manager-key-source-implementation-gpt-20261110-01`
- Runtime/provider: Codex / GPT `gpt-6.1-sol` / high
- Author exit: `0`
- Event stream: 157 non-empty JSONL lines; every line parsed with Python `json.loads`; exactly one `turn.completed`; zero error/failed events.
- Stderr: only known managed-worktree Git-pointer diagnostics.
- The author changed exactly the ten paths allowed by the accepted plan and committed/pushed nothing.

## Delivered behavior

- Added the official `com.tencentcloudapi:tencentcloud-sdk-java-ssm:3.2.46` compile dependency.
- Added a lazy, read-only `TencentSsmSecretStringClient` using direct `CvmRoleCredential`, explicit HTTPS endpoint `ssm.tencentcloudapi.com`, POST, TC3 signing, debug off and 2/3/2-second connect/read/write defaults derived from the accepted validation.
- Added one provider that converts only a validated SecretString into the unchanged PA22 schema-v1 parser and maps every checked/runtime failure to the existing empty `Unavailable`, with no fallback to another source or version.
- Enforced exact response name/version identity, SecretString-only payload, null/empty binary absence, 65,536-byte bounds, reporting UTF-8 and owned-buffer clearing.
- Preserved the sole PA21/PA22 authority, credential codec/provider interface, replay/current-state, parser and file provider.
- Implemented the FIX-1 asymmetric selector:
  - exact `provider=file` ignores all unselected `tencent.*` metadata and retains existing file/path behavior;
  - exact `provider=tencent-ssm` plus any `bundle-path` presence creates zero built-in providers and no file fallback;
  - invalid selectors create no built-in provider;
  - zero/one/many provider ambiguity-before-read remains unchanged.
- No production logging, static credential, environment/profile credentials, custom metadata client, cache, scheduler, retry framework or public diagnostic carrier was added.

## Exact changed paths

| Path | SHA256 |
| --- | --- |
| `backend/src/main/java/com/onecall/core/sharing/internal/keysource/TencentSsmMediaPlaybackSigningKeyProvider.java` | `B481CDA71EF7943EED41A26479E722D8FE5D5A0F09D2B0F719E056DA5E08A12C` |
| `backend/src/main/java/com/onecall/core/sharing/internal/keysource/TencentSsmSecretStringClient.java` | `639DC952F6BFC587EF2BBD6E61EB7040C9126FA106EF065E6D1A2A7E66589F7C` |
| `backend/src/main/java/com/onecall/core/sharing/internal/keysource/MediaPlaybackKeySourceConfiguration.java` | `05B66111092C5E5EB44A40F765B27EADB31D3CCBBA7591A23DB3117373FB3A68` |
| `backend/src/test/java/com/onecall/core/sharing/internal/keysource/TencentSsmMediaPlaybackSigningKeyProviderTests.java` | `2C0D5CAC0348A2B6EDEBCA6793ADED0D6A2BAEB842E9744B4FB068F91793AB83` |
| `backend/src/test/java/com/onecall/core/sharing/internal/keysource/TencentSsmSecretStringClientTests.java` | `066B787DA3D304F3D9608C252776CB3CFFC8C1683B0D982CC8168807EB409968` |
| `backend/src/test/java/com/onecall/core/sharing/internal/keysource/MediaPlaybackTencentKeySourceIntegrationTests.java` | `0114053757B85A8C79AB46E252FC8D1F3084A82AFBA0FA4B0DF924D89272B428` |
| `backend/src/test/java/com/onecall/archtests/MediaPlaybackConfigurationTests.java` | `94489511C5AA240C773D24C1C7718736DA5C161534A37FE50C898FD851EC3578` |
| `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` | `C9F94EC0669996967AA28F655F52A46852D55C4104DF2760313F2FA3274A3746` |
| `backend/pom.xml` | `A266587B71653429EFB65EBD9985BD4A65E457330E990B2E85C582454A5A4E46` |
| `.github/workflows/ci.yml` | `7602ECE901CE2B5893A494BABC104F8FC4BBB71D12D954743FFAE2625FC37087` |

`git diff --check` and the exact changed/untracked audit pass; `.codex` remains untracked and excluded.

## Validation

| Check | Result |
| --- | --- |
| Focused suite | **117/117**, zero failures/errors/skips |
| WSL/JDK 21.0.13 Surefire | **36 classes / 309 tests**, zero failures/errors/skips |
| Final combined Failsafe XML | **13 classes / 214 tests**, zero failures/errors/skips |
| Checked-in source=CI=XML inventory | **49/523**, annotation scan 524, configuration 19, boundary 8, authority 24, POM 13 includes |
| Actual packaged runtime graph | **72** coordinates, **8** dependency-only, **64** counterparts, **65** packaged jars including the sole Boot jarmode |
| Production JAR firewall | **116 = 82 + 34** probes pass; historical deltas 35/31; seven positive-first collisions unchanged |
| Default/web JAR smoke | passes in WSL with Java 21.0.13 |
| Contract A1 s3 | passes, including locked `npm ci`, lint, bundle, generate, typecheck, doc map and media-gateway lint |
| Contract A3 s3 | passes, 23/23 fixtures and 10 negative cases |
| Contract A4 s2 | passes, 113/113 fixture cases, 3 repeat checks and determinism check |
| Contract A2 s3 | fails on pre-existing A0 CHECK/column-dictionary/document drift outside the PA23 change set |
| Contract A5 s1/s2 | fails on the pre-existing `state table header changed in section 7.1` source-tree documentation check |

The Windows host JDK 21.0.12 exposes an AF_UNIX/WEPoll loopback defect when starting Tomcat; WSL/JDK 21.0.13 ran the full Surefire suite and JAR smokes. Docker Desktop's WSL random port proxy intermittently refused a subset of PostgreSQL containers. The final report set was completed by rerunning only the affected IT classes against the same fixed `postgres:16.15` image and candidate; the aggregate XML then passed the exact CI inventory with zero failures/errors/skips. No source or test change was made while rerunning.

## Scope preservation and residuals

- V001-V008 migrations, resources, OpenAPI, deployment, PA21/PA22 authority/wire/current-state and all paths outside the ten-path allowlist are unchanged.
- No live Tencent SSM/CAM/CVM metadata/STS/KMS/COS call, credential acquisition, cloud provisioning or production secret occurred.
- Platform/Security/Operations retain CVM-only identity suitability, absent metadata timeout, absent total SSM call deadline/DNS bound, underlying connection retries, CAM/region/version provisioning, resource/version rollout and per-call quota/cost/latency validation.
- Security retains Java/SDK String memory and future dependency/logging advisory monitoring.
- The A2/A5 existing source-tree contract failures are outside PA23 and must not be represented as PA23 regressions or fixed in this slice.

## Next gate

Proceed to independent peer-free A/B `code review` using the `deepreview` skill against the identical ten-path candidate, accepted plan, this implementation artifact and unchanged V001-V008/public/deployment boundaries. Reviewers must submit their own timestamped artifacts before the Controller reads either. No source edit, commit, push, PR, approval, ready action, reviewer request or external comment is part of this checkpoint.
