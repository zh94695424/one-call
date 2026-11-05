# PA15 Public Share Read — Plan Fix 4

- Task label: `pa15-public-share-read-plan-fix4-gpt-20261106`.
- Work unit: `PA15-sharing-public-share-read`; date: 2026-11-06.
- Author role: GPT plan-fix author under active Control dispatch; no independent runtime model-identity claim.
- Gate/status: `fix` / `fix-complete` for Controller intake only; no implementation authorization, review verdict or gate advancement.
- Authority: active PA15 adjudication in `docs/gateflow/project-execution-control.md`, accepted plan and fixes 1–3, all six PA15 reviews (`035347`, `043640`, `051000`, `051649`, `054300`, `055017` on 2026-11-06), and the historical blocked implementation artifact.

## Accepted Blocker And Exact Correction

The first implementation retry truthfully stopped before changing any candidate source: Tomcat 11.0.27's connector defaults to `EncodedSolidusHandling.REJECT`, and `%2F` receives HTML 400 during URI decoding before the REQUEST filter. The blocked report and Control record root/context/header diagnostics, not a successful public-read implementation. Boot 4.1.2 does not bind `server.tomcat.encoded-solidus-handling=PASS/ALLOW`; those property attempts cannot satisfy the closed 404 contract. This fix accepts the container-route diagnosis without weakening that contract or claiming its runtime resolution.

Plan §4 freezes exactly one additional bean, `customerEncodedSolidusHandlingCustomizer`, inside the existing web-profile/servlet-conditional `CustomerWebConfiguration`: return `WebServerFactoryCustomizer<TomcatServletWebServerFactory>` and call `factory.addConnectorCustomizers(connector -> connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue()))`. Exact imports are `org.springframework.boot.web.server.WebServerFactoryCustomizer`, `org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory`, and `org.apache.tomcat.util.buf.EncodedSolidusHandling`. No new filter, valve, dependency, profile, endpoint, pool, credential, decoding/property workaround or fallback is authorized.

Security contract: PASS_THROUGH preserves the encoded slash instead of preempting the filter with HTML 400; it does not decode it into another operation, authorize traversal or admit a route. The sole application admission gate remains the existing REQUEST-only highest-precedence `CustomerBoundaryFilter`. Retain canonical `(contextPath + servletPath).equals(requestURI)` and every exact method/route matcher, including their percent exclusion. Encoded/noncanonical presentations must be rejected by equality and/or the exact matcher with closed 404 `NOT_FOUND` before auth, shape/hash, service lookup or database access. Existing customer routes and all public token-domain error distinctions remain frozen. Real-container regression proof is still required after implementation; this plan does not assert it has run.

## Future Allowlist And Test Changes

The future source allowlist grows from 23 to exactly **25 distinct paths: 11 existing + 14 new**. Original items 1–23 remain unchanged and ordered; only these existing paths are added:

- Item 24: `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java` — the single connector bean; filter registration unchanged.
- Item 25: `backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java` — extend the existing method, not a new class or test.

Extend only `webProfileIsExplicitAndMissingAdapterHasNoDatabaseEffects` for the connector regression. Inspect the actual embedded connector through `ServletWebServerApplicationContext.getWebServer()` → `org.springframework.boot.tomcat.TomcatWebServer.getTomcat().getConnector()` and require `getEncodedSolidusHandling()` equals `PASS_THROUGH.getValue()` (or its public enum getter equivalent), not an environment property or detached factory mock. Use method-local root and `/pa15` context starts, preserving the current missing-adapter/web-only/no-verifier/no-database assertions and lazy singleton/pool/manager checks. Existing helpers may include the real context prefix.

GET an otherwise non-operation customer path, `/api/v1/customer/not-an-operation/a%2Fb`, anonymously and with malformed supplied bearer, including HTML Accept/untrusted trace controls. Require exact closed five-key 404 `NOT_FOUND` Problem, `application/problem+json`, no-store/no-cache/nosniff, one fresh server UUID trace per response, no input-trace reuse, HTML, URI/header/token echo, cookie or WWW-Authenticate. Prefixing the same request with the configured real servlet context must not change that pre-auth route result. No new `@Test`, parameterized/repeated/dynamic/conditional test or adapter; the class stays six methods and this method stays one.

Extend the existing `CustomerApiBoundaryTests.profileAndVerifierArtifactsCannotEscapeIntoProduction` source assertions to freeze the exact bean/type/import/connector call, single `/*` REQUEST-only highest-precedence filter, no competing filter/valve/decoding fallback, and existing ERROR-dispatch, controller-construction, `@Import` and production verifier/test/dev bypass bans. Existing exact restrictions are not weakened. The customer internal 25→26 and web/servlet four→five inventories still change only for the planned new controller, not for a bean in an existing file.

Extend the existing packaged-JAR CI smoke block with explicit GET `/api/v1/public/shares/a%2Fb` anonymously and with malformed bearer/HTML Accept/untrusted trace controls. Reuse its exact closed Problem/header/fresh-trace assertions and require 404 `NOT_FOUND` before service lookup, never HTML400/auth401/missing-service503. Lowercase `%2f` and double-encoded `%252F` controls are optional only as needed for equivalent rejection proof. Retain every current default/web/public canonical/non-GET smoke, no-Hikari check, bounded JVM cleanup and production-JAR firewall check. These HTTP probes add no JUnit method and do not change the **82** adversarial firewall probes.

## Exact Write Scope And Identities

Only two repository paths receive task writes: existing `docs/gateflow/pa15-sharing-public-share-read-plan.md` and this new `docs/gateflow/pa15-sharing-public-share-read-plan-fix-4.md`. The fix artifact was absent at intake. Control, fixes 1–3, six reviews and the blocked implementation report remain untouched.

| Artifact identity | Before | After |
| --- | --- | --- |
| Plan SHA-256 (LF repository bytes) | `07605141492b281337fc37046c01cd5995c071867970d5fa7bb17ec7b38d26ae` | `eed82e23743c07b71afae09c5183757dbafdfc44e3b71fea55cf67c2518a8012` |
| Plan line count (`wc -l`) | 372 | 399 |
| Fix-4 SHA-256 / line count | Absent, not an empty baseline | Final byte hash/count are measured after the EOF reread and recorded in the completion report; a final self-referential hash is deliberately not embedded in its own input. |

`docs/gateflow/pa15-sharing-public-share-read-implementation.md` is historical blocked-attempt evidence, not an allowlisted source path. It must later be replaced by a complete implementation artifact under separate reporting authority. This task neither changes it nor treats its partial baseline diagnostics as acceptance evidence.

## Preserved Inventories And Decisions

- Exactly one observation-free public GET slice. Disjoint sharing partitions remain eight public-read / 13 customer-query / 19 command paths; all named APIs, file lists, SQL/write/lock/logging bans and sole service-DSL/repository-column exceptions remain unchanged.
- Fixes 1–3 remain intact: canonical-route `NOT_FOUND` versus unresolved-token `SHARE_TOKEN_INVALID`; grant-tenant current membership independent of workspace selection; equal CLEAR/OPEN shape; before-validFrom concealment; constructor-wired no-DSL repository interface; exact controller signature/lazy providers; PostgreSQL-enforced read-only transaction, same-connection evidence role switch/restore, no writes and safe projection.
- Current source=CI: 16 Surefire classes/79 methods + six Failsafe classes/118 methods =197. Future: 19/91 + 7/126 =217; exactly 12 new Surefire methods (6+4+2) and eight Failsafe methods; net +20 methods/+4 classes. Existing architecture/configuration methods extend in place; no method/class increment from fix 4 or the JAR probes.
- All named new test rows and plan §§5–8 remain byte-identical to intake. Failsafe breakdown remains `64+10+10+8+8+18+8=126`. V001–V010, ACLs, schema, OpenAPI, source/tests, CI, POM, README, settings, dependencies and deployment remain unmodified in this fix.

## Validation Actually Performed

- Read active Control, accepted plan, fixes 1–3, six PA15 review artifacts and the stopped implementation report; inspected CustomerWebConfiguration, CustomerBoundaryFilter, CustomerWebConfigurationTests, existing web/servlet/profile architecture assertions, CI inventory/JAR/firewall blocks and README current/historical clauses.
- Used `/opt/jdk21/bin/javap` on resolved `spring-boot-tomcat-4.1.2.jar`, `spring-boot-web-server-4.1.2.jar` and `tomcat-embed-core-11.0.27.jar`. Confirmed inherited `TomcatWebServerFactory.addConnectorCustomizers(TomcatConnectorCustomizer...)`, its connector callback, generic customizer signature, exact servlet factory package, actual-server accessor, connector String setter/getter and public enum getter, REJECT initialization and `PASS_THROUGH.getValue()` = `passthrough`. Boot Tomcat configuration metadata contains no solidus property.
- Isolated `/tmp` Java compile-shape check with resolved jars succeeds for the exact bean and actual embedded-server connector accessor. This does not compile or execute the application, start a server, run JUnit or prove runtime routing. No dependency download/change or Maven build was needed; exact local jar evidence controls over an inconclusive documentation lookup.
- Mechanically checked 25 numbered/distinct paths, original ordered items retained, exactly two existing additions, 11 existing/14 absent future files; compared all 8/13/19 partition lists byte-for-byte with intake and actual current architecture/source sets. Rechecked unchanged §§5–8 and all 12/8 new test rows.
- Re-extracted exact current source method identities and CI expected sets, including both `unit_expected.update` blocks and named container sets: 16/79 and 6/118 match exactly; existing configuration class has six methods and the named regression method occurs once. An initial audit parser could not resolve CI set references; corrected AST-based literal extraction passed. Arithmetic remains 79+12=91, 118+8=126, 91+126=217.
- Exhaustive CI count-site search still finds current checks at lines 162/163/366/367/368; plan's atomic future 91/126/217 instructions remain current. README current clauses remain lines 40/291/300/322, preserving co-located PA9/PA12/PA14 history and historical lines 305/313. Firewall assertion remains `probes == 82` at line 766; smoke extensions are not JUnit methods.
- Plain Git exposed the known Windows worktree pointer; process-local `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call15`, `GIT_WORK_TREE` and `GIT_OPTIONAL_LOCKS=0` were confirmed with live `git rev-parse --git-dir` before read-only status/diff checks. No `.git` or index rewrite. Intake hashes of all 1,366 tracked files and the pre-existing untracked blocked report support protected-path comparison; private `.codex` contents are not inspected.
- Final validation covers explicit new-file diff/whitespace checks, hashes/line counts, protected-path hashes and rereading both outputs through EOF, not an inference from tracked diff alone. No application build, Maven, Docker/PostgreSQL/Testcontainers, runtime/JUnit tests, packaged-JAR smokes or remote CI are claimed.

## Residual Risks And Stop State

- Backend/Platform/Security must still prove the connector setting and closed encoded-slash rejection in the real embedded and packaged Tomcat, root/context/header variants, and unchanged customer/public route precedence. Static APIs and scratch compilation are not runtime proof; a contradictory implementation result requires another bounded stop, not a new filter/valve or relaxed assertion.
- Security/Identity retains the absent production verifier; Backend/Database/Platform retain PostgreSQL first-SQL/read-only/PID/role/SQLSTATE25006/pool-reset/no-write and full 217-test regression evidence, dual-role provisioning and shared-pool capacity. Dedicated public-read ACL remains later Security/Schema work; Security/Product owns production token representability; later admission/media owners must recheck because a READ COMMITTED observation is not an authorization lease.
- Documentation correction only; gate remains `fix` for Controller intake. No source/runtime/configuration implementation, stage, commit, push, branch/worktree/PR creation, merge, approval, ready action, reviewer request, external comment, issue mutation, remote mutation, re-review dispatch or later-gate entry occurred.
