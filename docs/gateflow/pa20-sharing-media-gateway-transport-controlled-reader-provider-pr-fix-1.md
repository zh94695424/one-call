# PA20 PR #31 review fix 1

## Identity and disposition

- Task: `pa20-pr31-review-fix1-gpt-20261109`.
- Work unit: `PA20-sharing-media-gateway-transport-controlled-reader-provider`.
- Gate: `fix`.
- Frozen reviewed PR #31 head: `afe9c725087e4b10caa8b576a330af5fd6bbb490`.
- Controller-adjudicated `PA20-PR-A1` and `PA20-PR-A3`: addressed below.
- Rejected `PA20-PR-A2`: not applied; accepted plan and plan-fix-1 remain immutable.

## Changes

- `backend/src/main/java/com/onecall/core/evidence/mediaread/ControlledMediaByteSource.java`: documented that reads fail with `IOException` after either terminal operation; abort and close are idempotent; close releases or attempts release exactly once even if it throws; abort after successful close is a no-op; and gateway owns exactly one session and closes on success or aborts then closes on failure. No signature or behavior change.
- `api/openapi/media-gateway-v1.openapi.yaml`: removed every YAML merge key. Responses 206, 401, 405 and 416 now explicitly list their required headers. Existing header-map/content anchors and aliases remain. HTTP semantics, schemas, codes and security scheme are unchanged.
- `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`: added no-merge-token and required status/header assertions inside the existing `gatewayTransfersWithoutTransactionsProjectionOrPublicContractChanges` method; no test method added.
- Updated the implementation report with this bounded fix and validation disposition.

## Validation

- `node --version`: `v22.23.4`.
- `npm --version`: `10.9.9`.
- `npm run check:s3` (working directory `api/openapi`): PASS. This ran contract lint, bundle/generation/typecheck, doc-map checks, standalone gateway Redocly validation, and the S3 doc-map check.
- Node YAML assertion command (working directory `api/openapi`): `node -e` using the installed `yaml` package, `YAML.parse(source,{version:"1.2",uniqueKeys:true})`, checking zero `<<:` syntax and required headers on 206/401/405/416: PASS.
- `rg -o '<<:' api/openapi/media-gateway-v1.openapi.yaml | wc -l`: `0`.
- Required Maven command, attempted from repository root: `mvn -q -o -f backend/pom.xml -Dmaven.repo.local=/tmp/pa19-m2 -DskipTests test-compile && mvn -q -o -f backend/pom.xml -Dmaven.repo.local=/tmp/pa19-m2 -DargLine=-javaagent:/tmp/pa19-m2/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar -Dtest=SharingQueryBoundaryTests#gatewayTransfersWithoutTransactionsProjectionOrPublicContractChanges test`. It did not start; launcher emitted `<3>WSL (3 - ) ERROR: UtilBindVsockAnyPort:309: socket failed 1`, so neither phase ran.
- `./mvnw -o -DskipTests test-compile` (working directory `backend`): also unavailable; `/bin/sh^M: bad interpreter`. These are environment blockers, not passes.

## Remaining ownership and limits

- Controller verification passed: `npm run check:s3` and strict YAML 1.2/header assertions passed; exact full `container-smoke clean verify` completed `BUILD SUCCESS` at `2026-11-08T19:50:02Z` in 10m10s with fresh Surefire `29/196`, Failsafe `13/204`, zero failures/errors/skips, gateway IT 6/6, exact inventory `42/400`, POM 13 and line scan 401. Log `controller-runtime/pa20-pr-review-fix1-full-verify.log`, 820,308 bytes / SHA256 `3AF64A0D1EC004BB82280F195B11E6691FCC6081285DE55DBD94D27F82F91D27`.
- Controller/CI owns committing/pushing this fix and obtaining a new exact-head hosted CI pass; no hosted result is claimed here.
- No staging, commit, push, PR mutation, approval, mark-ready, reviewer request, merge, external comment, or issue mutation was performed.
- No Control, production gateway behavior, migrations, public-control OpenAPI, POM/dependency files, plans, prior review artifacts, or unrelated tests were changed.
