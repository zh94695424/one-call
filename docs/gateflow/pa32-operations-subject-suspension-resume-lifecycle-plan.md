# PA32 Operations subject suspension/resumption lifecycle — implementation plan

- Task: `pa32-operations-lifecycle-plan-gpt-20261113-01`.
- Work unit: `PA32-operations-subject-suspension-resume-lifecycle`.
- Date: 2026-11-13, Asia/Shanghai. Gate: **plan only**; Controller adjudication remains required.
- Binding authority: the entire Goal Confirmation in `project-execution-control.md`, “Active resume checkpoint - PA32”, read before implementation evidence. Nothing in this plan supersedes it.
- Inspected base: `8206f2a8f8a40cd58793efdda55aa41843af5d5f`.

## 1. Goal, motivation, success and scope

Implement exactly `suspendOperationsSubject` and `resumeOperationsSubject`, respectively POST `/api/v1/operations/subjects/{subjectId}/suspensions` and `/resumptions`. These are one coherent Operations lifecycle behavior, not two delivery slices. An authenticated employee action must have one atomic, replayable, attributable effect on an existing activation, without confusing customer authentication, employee authorization, activation ownership or trial issuance.

Binding success: exact two-field input and ten-field output; independent opaque employee audience; current ACTIVE OPS checked under the prescribed owner locks; one revision increment, one immutable matching transition, one registered activation-status Outbox fact and one safe audit fact on first success; zero duplicate effects on replay; stable frozen errors; atomic rollback and role restoration; no trial reissue; frozen contracts/migrations/policy; exact source/CI/POM/XML inventory, PostgreSQL16 suite and packaged firewall evidence. The Outbox fact is not a new workflow: it is the existing §10.17 transition producer obligation and existing PA31 owner pattern.

Non-goals: employee login/issuance/refresh, production session adapter, IdP/Redis/MFA/Q1 delivery, new Operations queries, employee administration, customer/IVR suspend/resume, trial-policy/catalog/order work, UI, KMS/providers/provisioning, generic authorization/idempotency/event framework, new schema/grant/status/error, live acceptance or production-readiness claims. No modifications to OpenAPI, V001–V017, docs/12/14/15/17 or other existing policy. No V018. No later gate or remote action is authorized by this artifact.

## 2. First-principles judgment and direct evidence

1. Authentication is an attestation, not a client employee ID or a DB employee row used as login state. OpenAPI `employeeSession` at approximately 4000 and security spec §§11–12 require independent opaque audience `onecall:employee-session:v1`; the security audience table explicitly prohibits customer reuse. `CustomerSessionVerifier`, `CustomerBearerAuthenticator`, `OpaqueCustomerBearer` and `VerifiedCustomerSession` demonstrate a small, synthetic-testable, absent-provider-fails-closed pattern, not a production provider to copy or share.
2. Current authority cannot be inferred from a previously issued role. V002 `employee_account` has fixed `ADMIN|OPS`, `ACTIVE|DISABLED` and owner SELECT/column UPDATE grants. docs/14:3355–3356, §9.0.1, §10.9 collaborator table and T-S2-29 explicitly require the operations-iam-owned lock-only collaborator. V007 has the employee FK, but an FK is not permission for activation-consent to query or lock an employee.
3. Existing persistence is sufficient. V007 supplies the single subject activation head, conditional update columns and three transition uniqueness keys. Its broad matrix is deliberately insufficient as service authorization; the owner must narrow it. V001 supplies `audit.append_fact` and existing `operations_service_role` EXECUTE. No foreign table grant is needed: switch to the prescribed owner for employee locks and to the existing service audit capability for the append function.
4. docs/14 §§4.37–4.38, 9.19, 10.17 and T-S2-29 prescribe the activation transition ledger, revision, action fingerprint and no-trial-reissue semantics. `JooqSubjectActivationRepository` already uses `subject_number.subject:<uuid>`, activation `FOR UPDATE`, owner-role assertions, conditional updates and the exact activation-status Outbox payload. `ActivationSemantics` already supplies canonical UTF-16-key-sorted JSON, lowercase SHA-256 and `activation-status:<activationId>:<toRevision>`. Reuse these same-module utilities, not customer command/consent/trial behavior.
5. `DefaultActivationTrialService.scope` is concrete same-connection MANDATORY role-handoff evidence: resource/transaction assertion, entry-role assertion, literal SET LOCAL ROLE, finally restoration and redacted failures. PA32 uses that pattern for a different owner and never invokes that service.
6. OpenAPI:1210/1284 freezes OPS, request/header, 200, and the eight operation error codes; :4879/:4892 freezes input/result. docs/15:49–50,104–107,180–188,278–281,304 and docs/17:336–355,405–422,596 agree on audience, server-derived authority and deferred production authentication. Security spec:558–559,578–579 freezes `SUBJECT_SUSPEND`/`SUBJECT_RESUME`, actor EMPLOYEE, object SUBJECT, request reason and status/revision/reason snapshots.
7. Do not create an Operations-IAM → activation-consent → Operations-IAM module cycle. Operations owns its web/employee boundary and publishes a narrow lifecycle **inbound port**; the activation-consent owner implements that port and calls the separate Operations-owned employee lock interface. This is dependency inversion, not transferring activation table ownership.
8. `CustomerWebConfiguration` registers `CustomerBoundaryFilter` on `/*`, order HIGHEST_PRECEDENCE+1, and currently conceals every Operations route. A narrowly specified pass-through plus an earlier independent Operations filter is required; simply adding a controller would remain unreachable. No constructor/import changes or broader delegation are needed in customer code.

Reconciliation: the abbreviated S1 list places employee locks at 11 and audit at 14; the explicitly expanded S2 §9.0.1 total order places employee/administrator at **13**, Outbox 14, audit 15. Operations lifecycle uses the expanded S2 order required by §9.19 and the Goal Confirmation. The administrator advisory precedes the employee row within rank 13. No lower rank is acquired afterward.

## 3. Ownership and exact dependency edges

Keep the existing 14-module set. Preserve all existing edges. Change only `activation_consent/package-info.java` to append:

```
operations_iam::subject-lifecycle-command
operations_iam::employee-administrator-lock
```

Existing activation edges remain `subject_number`, `entitlement_metering::activation-trial`; the latter remains solely for existing customer activation, never the new lifecycle classes. `operations_iam` retains exactly `operations_audit`; it gains no Java dependency on activation-consent. Update the activation entry in `ModuleBoundaryTests.EXPECTED_ALLOWED`, and in the already-allowlisted `discoveredProductionEdgesStayInsideTheConfiguredMap` assertion add `"operations_iam"` exactly once to `containsExactlyInAnyOrder`, change its description from exactly seven to exactly eight production edges, and retain the method plus all nine test methods and the negative cycle/internal/forbidden fixtures. The resulting target multiset is exactly `sharing, evidence, subject_number, activation_consent, evidence, catalog_order, entitlement_metering, operations_iam`; all other exact dependency assertions remain unchanged and precise.

- Operations IAM: opaque session verification seam, trusted caller derivation, filter/controller/binder/problem output, inbound lifecycle port and employee lock owner implementation.
- Activation-consent: authoritative command transaction, activation reads/locks/update/ledger, semantic comparison, existing Outbox producer and scoped audit function call.
- operations_audit DB owner: existing security-definer append function; no new Java API or cross-owner base-table write.
- No use of customer session/verifier/principal/workspace, operations-iam internals from activation, activation internals from Operations, or trial service from lifecycle code.

## 4. Exact implementation allowlist (future implementation only)

Paths in the next two lists are relative to `backend/src/main/java/com/onecall/core/`. Twenty-two new production files:

| New file | Exact responsibility / types |
| --- | --- |
| `operations_iam/subjectlifecycle/package-info.java` | `@NamedInterface("subject-lifecycle-command")` |
| `operations_iam/subjectlifecycle/OperationsSubjectLifecycleService.java` | Inbound port; `suspend(Command, Caller)` and `resume(Command, Caller)`; nested records and redacted typed failure below |
| `operations_iam/employeelock/package-info.java` | `@NamedInterface("employee-administrator-lock")` |
| `operations_iam/employeelock/EmployeeAdministratorLock.java` | `Optional<EmployeeFacts> lockEmployeeAdministrator(UUID employeeId)`; nested facts and redacted `Failure` |
| `operations_iam/internal/security/EmployeeSessionVerifier.java` | Single pluggable verification interface |
| `operations_iam/internal/security/EmployeeSessionVerificationException.java` | Only UNAUTHENTICATED / SERVICE_UNAVAILABLE verification outcomes |
| `operations_iam/internal/security/OpaqueEmployeeBearer.java` | Opaque wrapper; explicit secret accessor for verifier only; redacted `toString` |
| `operations_iam/internal/security/VerifiedEmployeeSession.java` | Immutable employeeId/audience attestation; redacted `toString` |
| `operations_iam/internal/security/EmployeeBearerAuthenticator.java` | Strict Authorization extraction and unique verifier selection; creates trusted Caller |
| `operations_iam/internal/web/OperationsWebConfiguration.java` | Web-only authenticator/problem/controller-boundary beans and filter registration |
| `operations_iam/internal/web/OperationsBoundaryFilter.java` | Two-route firewall, auth, transport checks, principal, buffered/redacted problem handling |
| `operations_iam/internal/web/OperationsRequestPrincipal.java` | Request-local Principal wrapping trusted Caller; no bearer retained |
| `operations_iam/internal/web/OperationsProblemWriter.java` | Frozen status/code/title/headers mapping, no exception serialization |
| `operations_iam/internal/web/OperationsSubjectsController.java` | Only the two frozen POST mappings; parse input and dispatch to unique qualified owner port |
| `operations_iam/internal/web/OperationsSubjectLifecycleJsonBinder.java` | Closed strict UTF-8 JSON binder; nested `TransitionRequest` record |
| `operations_iam/internal/employeelock/DefaultEmployeeAdministratorLock.java` | Operations-IAM-owned `@Component("employeeAdministratorLock")` lock implementation, with exact persistence/property conditions and qualified manager/DSL constructor; MANDATORY same-resource owner lock and facts reread; no writes |
| `activation_consent/internal/operations/OperationsLifecyclePersistenceProperties.java` | Redacted dedicated datasource url/username/password configuration record |
| `activation_consent/internal/operations/OperationsLifecyclePersistenceConfiguration.java` | Conditional dedicated pool, manager, DSL, template, repository, owner service wiring |
| `activation_consent/internal/operations/OperationsLifecycleRepository.java` | Owner persistence interface and Head/Transition records, fixed operations below |
| `activation_consent/internal/operations/JooqOperationsLifecycleRepository.java` | Prepared SQL, row/shape checks, Outbox and scoped audit append |
| `activation_consent/internal/operations/DefaultOperationsSubjectLifecycleService.java` | Inbound port implementation, fixed state pairs, transaction/replay/conditional write ordering |
| `activation_consent/internal/operations/OperationsLifecycleSemantics.java` | Exact frozen fingerprint projection, replay comparison and persisted-result validation using existing ActivationSemantics |

Five changed existing files, and only these:

| Path | Change |
| --- | --- |
| `backend/src/main/java/com/onecall/core/activation_consent/package-info.java` | Two named edges above |
| `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` | Early canonical two-POST Operations pass-through to the already registered independent earlier filter; no employee imports or customer authentication of Operations |
| `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` | Update the activation allowlist entry and the exact discovered-target assertion: preserve `containsExactlyInAnyOrder`, add `"operations_iam"` once, change the message to exactly eight, and preserve the method name, all nine test methods, and every other exact dependency assertion |
| `backend/pom.xml` | Append exactly two Failsafe includes specified below; no dependency/version/plugin change |
| `.github/workflows/ci.yml` | Exact PA32 inventories/arithmetic, packaged Operations negative probes and employee verifier implementation firewall; preserve historical and dependency firewall assertions |

Nine new test/fixture files, relative to `backend/src/test/java/`:

| Path | Count |
| --- | ---: |
| `com/onecall/core/operations_iam/internal/security/EmployeeSessionBoundaryTests.java` | 8 |
| `com/onecall/archtests/OperationsSubjectsWebTests.java` | 12 |
| `com/onecall/archtests/activation/OperationsSubjectLifecycleServiceTests.java` | 18 |
| `com/onecall/archtests/OperationsLifecycleConfigurationTests.java` | 8 |
| `com/onecall/archtests/architecture/OperationsLifecycleBoundaryTests.java` | 8 |
| `com/onecall/archtests/container/OperationsSubjectLifecycleIT.java` | 24 |
| `com/onecall/archtests/container/OperationsLifecycleRoleLeastPrivilegeIT.java` | 12 |
| `com/onecall/archtests/container/OperationsSubjectLifecycleFixture.java` | 0; fixture only |
| `com/onecall/archtests/support/SyntheticEmployeeSessions.java` | 0; synthetic verifier only |

Implementation arithmetic: **31 new + 5 changed = 36 paths** (22 production additions, 9 test/fixture additions, 5 existing edits). The `ModuleBoundaryTests` edit changes assertion literals only; it adds no test method, class, file, or inventory entry. This plan artifact is not an implementation path. No source/test file is created at this gate. Any necessary path outside this allowlist must return to the Controller, not be silently added.

## 5. Interfaces, authentication and HTTP boundary

### 5.1 Lifecycle inbound port

`OperationsSubjectLifecycleService` has exactly two public service methods, both returning its nested ten-component `ActivationTransition` record. Failures use the nested redacted unchecked throwable below; they are thrown internally and translated at the controller boundary, never returned as a record. Nested types:

```
Command(UUID subjectId, String actionEventId, String reasonCode,
        BigInteger expectedActivationRevision)
Caller(UUID employeeId, String audience)
ActivationTransition(UUID transitionId, String statusEventId,
        String fromStatus, String toStatus, int fromRevision, int toRevision,
        String sourceChannel, String sourceActionDomain, String reasonCode,
        Instant occurredAt)
final class Failure extends RuntimeException {
    Failure(Code code) // fixed generic message; no raw cause/message/input
    Code code()
    @Override String toString() // redacted, no code-specific secrets
}
Code = UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, VALIDATION_FAILED,
       ACTIVATION_STATE_CONFLICT, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL
```

The employee-lock collaborator uses its nested redacted unchecked throwable `Failure extends RuntimeException` with exactly `Kind { UNAVAILABLE, INTEGRITY }`, fixed generic message, no raw cause/message/input, and redacted `toString()`. This is the authoritative condition-to-failure mapping for that contract; §§5.2, 6 and 8 use it without redefining the kinds:

| Condition | Employee-lock outcome | Lifecycle code / HTTP |
| --- | --- | --- |
| Transaction absent/unusable, wrong/unrestorable database role, connection failure, timeout or dependency unavailability; missing/ambiguous/lazily uncreatable employee-lock bean | `Failure(UNAVAILABLE)` | `SERVICE_UNAVAILABLE` / 503 |
| Malformed/unusable employee row or facts, null/mismatching employee identity, unknown status/role, or invalid revision shape (including nonpositive revision) | `Failure(INTEGRITY)` | `SERVICE_UNAVAILABLE` / 503 |
| No current employee row | `Optional.empty()`; not a typed failure | `UNAUTHENTICATED` / 401 |
| Valid current row with `status=DISABLED`, or `status=ACTIVE, role=ADMIN` | Valid `EmployeeFacts`; owner authorizes independently | `UNAUTHENTICATED` / 401, or `FORBIDDEN` / 403, respectively |
| Unknown programming defect outside the typed collaborator contract | Not an employee-lock failure kind | `INTERNAL` / 500 |

Both typed collaborator failure kinds map to lifecycle `SERVICE_UNAVAILABLE`/503; neither can become `INTERNAL`/500. `INTERNAL`/500 is reserved for unknown programming defects outside the typed collaborator contract, not malformed/unusable employee context. The owner validates returned facts as well as translating collaborator throwables; a malformed return uses the same `INTEGRITY` mapping. All other lifecycle mapping is `Failure.Code` to the frozen problem code/status at the controller. The owner marks rollback-only before mapping any failure after a transaction/role handoff; unique/technical SQL failures are mapped only after rollback as specified in §7.3. `OperationsProblemWriter` maps only the lifecycle code, emits the frozen title/status/headers and fresh trace UUID, and never serializes the throwable. Existing tests named in §§8.1–8.5 cover code/kind mapping, rollback, controller translation, and sanitization without adding methods.

No public generic transition/mutation method and no user-supplied action/domain/employee parameter. Controller route chooses suspend versus resume; owner selects fixed from/to pair. `Command`, `Caller`, and both failure representations are redacted; failures carry no raw cause text. Return only the result record, not the internal ledger row. `BigInteger` avoids inventing a wire maximum absent from OpenAPI; a positive expected revision above PostgreSQL integer range is valid JSON input but cannot match the head, so conflicts, not 422. A head at integer MAX cannot advance and returns 409 without overflow/write.

### 5.2 Verification seam (independent, no bundled provider)

```
EmployeeSessionVerifier.verify(OpaqueEmployeeBearer bearer, String expectedAudience)
    -> VerifiedEmployeeSession(UUID employeeId, String audience)
```

The verifier must resolve the opaque token through trusted employee-only server state, attest its employee identity and exact audience, and reject expired/revoked/invalid credentials with typed UNAUTHENTICATED. Production source, issuance, TTL, session-revocation implementation and MFA stay Q1/deployment inputs. No JWT decoding, customer verifier fallback, fake default, property-based identity or client issuer/subject/header lookup.

Authenticator accepts exactly one Authorization header using the existing opaque bearer grammar `(?i:Bearer) +([A-Za-z0-9._~+/-]+=*)`, not comma-combined or multiple headers. Missing/malformed/duplicate credential is 401 before provider selection. Resolve `ObjectProvider<EmployeeSessionVerifier>.stream().limit(2)` per authentication: zero, two, lazy bean failure, runtime failure, typed unavailable, null result, missing employeeId or missing audience => 503. Pass the constant employee audience to the verifier and independently require equal returned audience. A present wrong audience, including customer audience, => 401. Never accept servlet/container Principal as authority or trust any body/header employeeId/role/source/issuer/subject. Construct Caller only after verification. No request-local ThreadLocal or shared mutable caller.

The verified session intentionally contains no role: the collaborator rereads the employee and the owner independently requires ACTIVE OPS. Missing current employee or DISABLED employee => 401 (authentication is no longer current); ACTIVE ADMIN => 403. Apply §5.1's table: malformed row/facts, wrong returned identity or unknown status/role/revision => `INTEGRITY`; transaction/role/connection/timeout/dependency unavailability or unusable collaborator => `UNAVAILABLE`; both => 503, never 500. These authority checks run before subject existence, replay and state/conflict decisions. Trusted Caller null/malformed or wrong internal audience is fail-closed 503 at the owner boundary, not a way to authenticate through a direct service call.

SyntheticEmployeeSessions is test-source-only, supports independent opaque token maps, expired/revoked/invalid cases, missing/malformed/wrong-audience responses, failure canaries and two-verifier ambiguity. Use an unrelated synthetic customer verifier/token when proving cross-audience denial. No class implementing EmployeeSessionVerifier is packaged in production. Synthetic mappings do not replace PostgreSQL current employee checks.

### 5.3 Filter, binder and problem precedence

OperationsWebConfiguration is `@Profile("web")` and conditional servlet web application; no DB requirement. Register OperationsBoundaryFilter for `/api/v1/operations/*`, REQUEST dispatcher only, order HIGHEST_PRECEDENCE. No ERROR dispatch, second error controller, servlet security chain or catch-all Operations endpoint. Customer filter keeps its current registration/order, but before it sets headers/traces or authenticates, passes through only POST paths matching `/api/v1/operations/subjects/[^/;%]+/(suspensions|resumptions)` whose raw URI equals contextPath+servletPath. This admits invalid UUID text to the Operations 422 binder but not encoded, suffixed, semicolon, double-slash or wrong-method routes. Earlier Operations filter independently rejects all noncanonical/non-two-POST paths with 404. No generic `/operations/**` bypass and no widened customer/public admission.

Order for an admitted request: server trace UUID + safe headers; employee authentication; exactly-one nonblank 1–128 Idempotency-Key; JSON Content-Type/Accept transport checks; controller verifies OperationsRequestPrincipal; canonical UUID path + closed body binding; exactly-one qualified `operationsSubjectLifecycleService` owner candidate; authoritative transaction and authority/replay/state checks. Wrong/malformed routes/methods remain 404 before authentication. Missing/invalid credentials precede malformed JSON; valid credentials with absent/unusable verifier produce 503 before input parsing. DB current authority precedes revealing resource/replay. Input/schema failure precedes DB transaction. Current authority is not inferred by filter from a stale role.

Binder reads at most 8193 bytes, rejects bodies above 8192; malformed UTF-8, duplicate keys, trailing tokens, scalar/array root, null/missing/extra fields => 422. Keys are exactly `reasonCode`, `expectedActivationRevision`. Reason is a JSON string with 1–64 Unicode code points, nonblank, NUL-free and encodable; do not trim/normalize/truncate it. Revision must be integral JSON number >=1; no string/boolean/null/coercion/fraction, including floating lexical representation. Header is preserved byte-for-logical-text exactly, with the same encodability/NUL/nonblank and 1–128 code-point bounds. Reject duplicate headers even if identical. Preserve nonblank leading/trailing characters as identity; do not merge, trim or hash the stored action ID. Canonical UUID text is lowercase standard 36-character format; invalid admitted UUID => 422. No query/header may supply an alternate authority or domain; unknown JSON fields fail 422.

Use JSON request media type, optional UTF-8 parameters and existing JSON Accept negotiation conventions. Unsupported Content-Type => 422; an admitted valid-auth request whose Accept excludes JSON maps to existing INTERNAL 500 (do not introduce 406). Buffer downstream output like CustomerBoundaryFilter so Spring binder/servlet/container exceptions cannot leak HTML, SQL or stack traces. Do not turn a committed command into an automatic retry if output delivery fails; same-key retry retrieves the stored result.

Every error is application/problem+json with exactly `type="about:blank"`, fixed `title`, `status`, frozen `code`, server UUID `traceId`; no raw body/exception/header/input echoes. Map 401 Authentication required / 403 Access denied / 404 Not found / 409 Activation state conflict / 422 Request validation failed / 429 Too many requests / 503 Service unavailable / 500 Internal error. Add `WWW-Authenticate: Bearer` only for 401. Success and errors have Cache-Control no-store, Pragma no-cache, X-Content-Type-Options nosniff; no Set-Cookie. No new throttler: RATE_LIMITED remains an allowed mapping if a typed dependency supplies it, not a new policy.

## 6. Configuration and exact owner collaborator

Dedicated opt-in configuration: `@Profile("persistence")`, `@ConditionalOnProperty(prefix="onecall.operations-lifecycle", name="enabled", havingValue="true")`; credentials under `onecall.operations-lifecycle.datasource.{url,username,password}` with mandatory nonblank values and redacted representation/errors. No default credentials, inherited customer pool or auto-provisioning. Default/web-only/disabled mode creates zero lifecycle pool/manager/DSL/owner/employee-lock beans; authenticated calls fail 503 rather than starting a provider. `DefaultEmployeeAdministratorLock` itself is annotated with exactly `@Component("employeeAdministratorLock")`, `@Profile("persistence")`, and the same `@ConditionalOnProperty`; it is therefore an Operations-IAM-owned persistence bean, not a web bean and not declared/instantiated by activation-consent. Its constructor takes exactly `@Qualifier("operationsLifecycleTransactionManager") DataSourceTransactionManager` and `@Qualifier("operationsLifecycleDsl") DSLContext`; no unqualified manager/DSL, pool, or second transaction manager is allowed. Persistence-only enabled mode must create it under that exact bean name, not the default `defaultEmployeeAdministratorLock`; web-only, disabled, and default modes must not. Activation-consent imports only the two published Operations interfaces and never the internal implementation.

Exact qualified beans: `operationsLifecycleDataSource` (UTC-init Hikari, close on shutdown), `operationsLifecycleTransactionManager` (DataSourceTransactionManager, rollbackOnCommitFailure), `operationsLifecycleTransactionTemplate` (REQUIRES_NEW, READ_COMMITTED, writable), `operationsLifecycleDsl` (POSTGRES, prepared/indexed, execute logging false, TransactionAwareDataSourceProxy over that exact pool), `operationsLifecycleRepository`, `operationsSubjectLifecycleService`. Employee lock bean is `employeeAdministratorLock`, under the same persistence/property conditions, constructed with **those same** qualified manager/DSL; no extra pool or transaction manager in operations-iam.

Missing/ambiguous employee-lock collaborators do **not** fail application startup: owner construction stores an unqualified-by-name `ObjectProvider<EmployeeAdministratorLock>` without resolving it. At each suspend/resume invocation, before opening the lifecycle transaction or acquiring locks, resolve by type using `stream().limit(2)` and require exactly one usable bean. Zero/two candidates or lazy resolution/creation failure => redacted employee-lock `Failure(UNAVAILABLE)` => lifecycle `SERVICE_UNAVAILABLE`/503 per §5.1, with no writes. Do not use `getIfAvailable`, `@Primary`, a bean-name qualifier or default-name fallback to hide ambiguity. The normal sole bean is explicitly named `employeeAdministratorLock`; naming is pinned by the component/configuration assertions, while runtime cardinality covers **all** beans of the published type. No extra configuration file is needed. With otherwise valid configuration, the context and owner must start with zero or two candidates, and each authenticated call must return the sanitized 503, not a startup exception. This does not make invalid credentials or unrelated configuration failures lazily recoverable. Operations controller independently resolves exactly one port bean lazily so web-only mode boots safely.

Minimal collaborator signature is §4's EmployeeAdministratorLock. EmployeeFacts has exactly UUID employeeId, String status, String role, int revision. It returns current employee/administrator facts, not session/issuer/secret or a preapproved boolean; no enabled-admin counting or mutation is needed because this is not an employee administration command.

On each call:

1. Require actual Spring transaction and a bound resource for the qualified manager's pool, then enter a MANDATORY TransactionTemplate, same manager. Assert entry `current_user='activation_consent_role'`. Missing/unusable transaction or unexpected entry role => redacted `Failure(UNAVAILABLE)`; null employee identity => redacted `Failure(INTEGRITY)`, per §5.1. Both map to 503.
2. `SET LOCAL ROLE operations_iam_role`; assert current_user. On this same transaction-aware DSL/connection execute administrator advisory and employee `FOR UPDATE` as §7 states. Zero rows => Optional.empty; one valid matching row => EmployeeFacts; malformed/unusable row/facts/identity or unknown status/role/revision shape => `Failure(INTEGRITY)`. Role/connection/timeout/dependency failures => `Failure(UNAVAILABLE)`. Both map to 503 per §5.1. No network I/O, employee writes, append, enqueue, foreign lock, new transaction or manual commit/rollback.
3. In finally, attempt `SET LOCAL ROLE activation_consent_role`; assert restoration. A restoration failure marks transaction rollback-only and returns redacted `Failure(UNAVAILABLE)`/503 (preserve any primary redacted typed failure with safe suppression; both kinds still map to 503). In an aborted PostgreSQL transaction restoration may itself fail: never continue writes or claim successful restoration; outer rollback must clean SET LOCAL and pool reuse must observe session login. A normal domain refusal after a successful read must also restore the role.
4. Owner separately asserts activation role immediately after handoff and before every owner write. Observe same backend PID, session_user and txid across handoff in IT.

The audit append uses a bounded repository-local role scope, not a second employee-lock invocation: assert activation role, SET LOCAL ROLE **operations_service_role**, assert, invoke existing audit.append_fact, finally restore/assert activation role with the same rollback-only rule. This uses V001's already granted function EXECUTE, not a new table write or grant. It occurs at audit rank 15 after Outbox rank 14. No new Java audit abstraction/framework is required.

Runtime connection identity must already be able to SET ROLE activation_consent_role, operations_iam_role and operations_service_role, with no superuser/BYPASSRLS reliance. This plan does not edit provisioning, roles or memberships. Test-only NOINHERIT login membership is fixture setup, never a migration or production grant. If production cannot supply an already authorized identity, remain unavailable and return to the Controller; do not repair privileges at runtime or expand PA25 provisioning.

## 7. SQL, identities, replay and transaction algorithm

### 7.1 Repository operations / query intent

All values bind parameters; roles/schema identifiers are fixed literals. Methods: `enter`, `assertRole`, `lockSubject`, `lockHead`, `subjectExists`, `actionTransition`, `latestTransition`, `decisionTime`, `advance`, `appendTransition`, `appendOutbox`, `appendAudit`. All checks execute on the dedicated transaction-aware context.

```sql
SET LOCAL ROLE activation_consent_role;
SELECT current_user;
SELECT pg_catalog.pg_advisory_xact_lock(
  pg_catalog.hashtextextended(?,0)); -- subject_number.subject:<canonical UUID>
SELECT id,subject_id,status,revision,created_at,updated_at
FROM activation_consent.service_activation WHERE subject_id=? FOR UPDATE;
SELECT id FROM subject_number.business_subject WHERE id=?;
```

`created_at`/`updated_at` are activation-owner columns; subject read uses V007's granted id column and takes no foreign row lock. Head is zero or one row. Missing head is not created. Require known status, positive revision, matching subject/id, ordered timestamps. At rank 13, only collaborator SQL under operations_iam_role:

```sql
SELECT pg_catalog.pg_advisory_xact_lock(
  pg_catalog.hashtextextended('operations_iam.admin_mutation',0));
SELECT id,status,role,revision FROM operations_iam.employee_account
WHERE id=? FOR UPDATE;
```

No undeclared/new foreign read or row lock executes under `activation_consent_role`: the mandated V007-granted `subject_number.business_subject.id` read above remains allowed and takes no row lock; reading or locking `operations_iam` under the activation role remains prohibited. Read-only here means **no domain writes**; the shared transaction remains writable because FOR UPDATE and later owner writes require it. Do not set transaction_read_only for the collaborator.

Select ledger explicit columns (no SELECT *): `id,activation_id,subject_id,from_status,to_status,from_revision,to_revision,source_channel,source_action_domain,source_action_event_id,status_event_domain,status_event_id,request_fingerprint,actor_account_id,actor_employee_id,reason_code,consent_record_id,occurred_at`.

```sql
-- Global action identity; deliberately NOT restricted to requested subject/actor.
SELECT <ledger columns> FROM activation_consent.service_activation_transition
WHERE source_action_domain='OPERATIONS' AND source_action_event_id=?;
-- Current head witness, if needed after action lookup.
SELECT <ledger columns> FROM activation_consent.service_activation_transition
WHERE activation_id=? AND to_revision=?;
SELECT clock_timestamp();
UPDATE activation_consent.service_activation
SET status=?, revision=revision+1, updated_at=?
WHERE id=? AND subject_id=? AND status=? AND revision=?;
```

Bind only fixed permitted from/to statuses, head identity, expected revision and DB decision time. Require update count **one**. Zero count => one bounded head reread and 409, with no insert; >1 => unusable dependency 503/rollback. Capture decisionTime once after locks and predicates (not request time), use it identically for head updated_at, transition occurred_at, audit occurred_at and Outbox payload. Reject unusable/regressing DB timestamp instead of inventing one.

Append explicit INSERT into service_activation_transition all 18 ledger columns. New random UUID `id`; existing activationId/subject; fixed statuses; fromRevision=head.revision, toRevision=from+1; OPERATIONS channel/domain; **exact complete header** source_action_event_id; ACTIVATION_STATUS domain; derived statusEventId; fingerprint; actor_account_id=NULL, actor_employee_id=current verified employee; reason exact; consent_record_id=NULL; decisionTime. Require insert count one. No ON CONFLICT-as-success shortcut, transition update/delete, head INSERT or generic idempotency row.

### 7.2 Canonical identities and comparisons

- Global action identity is `(OPERATIONS, exact Idempotency-Key)`, per `uq_activation_transition_action`. A 128-code-point key remains 128 in the ledger; do not prefix it, shorten it, use a truncated UUID or replace it with a hash. Same text in another frozen action domain is not this action.
- Status fact identity reuses `ActivationSemantics.statusEvent(activationId,toRevision)`: `activation-status:<canonical activation UUID>:<decimal nextRevision>`, under `ACTIVATION_STATUS`. It fits 128 even at integer MAX; no raw header concatenation. It is not an additional action identity and not a generated response-time ID.
- Transition request_fingerprint is lowercase SHA-256 of §2.5 canonical JSON with **exactly the frozen eleven §10.17 keys**: `accountId:null`, `actorEmployeeId:<trusted UUID>`, `channel:"OPERATIONS"`, `consentRequestFingerprint:null`, `reasonCode:<exact input>`, `sourceActionDomain:"OPERATIONS"`, `sourceActionEventId:<exact header>`, `statusEventDomain:"ACTIVATION_STATUS"`, `statusEventId:<derived or stored immutable identity>`, `subjectId:<canonical UUID>`, `toStatus:<fixed route target>`.
- Reuse existing canonical/sha functions; no normalization, timestamp, traceId, generated transitionId/consentId, current status/revision, fromRevision/fromStatus or extra expectedRevision key in the frozen fingerprint. **expectedActivationRevision still participates in replay semantics through explicit equality to the stored from_revision**, not by changing §10.17's frozen field set.
- Replay reconstructs fingerprint using the candidate's stored immutable statusEventId, never a new ID from the now-advanced head. Independently verify stored statusEventId equals activationId/toRevision derivation, source/actor/null shape, requested activation/subject, fixed from/to pair, exact reason/action, expectedRevision=stored.fromRevision, toRevision=from+1 and hash equality. Validate persisted field ranges and hash encoding. A semantic mismatch => 409; structurally unusable/malformed stored dependency => 503, not a forged success. Do not disclose another subject's result on global key collision.
- Same action may replay after subsequent legitimate transitions; compare the historical row's immutable facts and require current employee authority, but do not require the current head revision/status still equal the original post-state. The replay returns its original ten values, no new current-state projection. Changed key is a fresh action, never a replay based only on status/reason.

### 7.3 One authoritative transaction and precedence

1. Before transaction, validate trusted caller shape/audience and input schema, select fixed state pair from called method, and resolve the employee-lock provider exactly as §6 specifies. Missing/ambiguous/unusable collaborator => §5.1 `UNAVAILABLE`/503 before transaction/locks/writes; never a startup substitute. Authentication provider I/O must already be complete. No verifier is called under locks or from the persistence service.
2. Execute REQUIRES_NEW writable READ_COMMITTED on the dedicated pool. Verify actual bound transaction/resource, enter/assert activation role. Acquire rank-3 subject advisory; rank-4 activation row if present; rank-5 nonlocking subject existence read. Never acquire employee locks first.
3. Invoke MANDATORY collaborator at rank 13, restore/assert activation role, validate current matching employee facts and independently require ACTIVE OPS. Apply §5.1's table: both typed failure kinds =>503 after rollback; valid missing/disabled/ADMIN authority retains401/403. Do this even on replay and missing-subject paths so missing subject never bypasses current authority. No task/reservation/idempotency locks are needed.
4. After authority, missing subject or missing current activation => 404. Do not create an INACTIVE activation. Invalid head facts => 503. Lookup global Operations action.
5. Existing structurally valid matching action/fingerprint/expected-revision semantics => return original transition **before** current-state/revision predicate. Changed subject/actor/route/reason/expected revision under same action =>409, never a second effect. Corrupt candidate =>503. A disabled/revoked employee never receives a stored success.
6. New action: require from state and expectedRevision=head.revision. Read latest ledger witness by head revision; if present, validate it agrees with head; reject inconsistent/unusable facts with 503. Missing transition witness does not authorize fixing history; require an existing ACTIVE/SUSPENDED head to have a matching valid latest transition and fail closed 503 if absent. A valid but nonmatching current status/revision =>409, not an idempotent success. Never treat a previous opposite action as success for a new key.
7. Check integer increment bound. Capture DB decisionTime; conditional update exactly one; append one matching transition; append existing activation-status Outbox at rank14; append audit at rank15; assert restored activation role. Commit before returning the ten-field record. All failures, including collaborator restoration, Outbox, audit and commit failure, roll back all writes.
8. No application-level generic retry loop. Map unique constraint races for `uq_activation_transition_action`, `uq_activation_transition_revision`, `uq_activation_transition_status_event` to 409 only **after rollback**; do not query an aborted transaction or reinterpret a raw uniqueness failure as success. Under the subject lock identical concurrent calls to the same subject naturally serialize and the second sees replay. Cross-subject same-key calls yield one200/one409: the loser may detect the committed global action during lookup; the global unique constraint remains a backstop, not a required executing mechanism. SQLSTATE 40001/40P01 and dependency connection/role/timeout failure =>503 after rollback; 42501 =>503, not a new grant. Unknown implementation defects outside the typed collaborator contract =>500; neither employee-lock failure kind maps to500. No leaked SQLSTATE/message in HTTP.
9. Concurrent same-subject different actions/keys at the same expected revision have one winner, one 409; a legitimately ordered later opposite transition requires the next expected revision and its own key. A losing transition cannot append audit/Outbox. Locks remain held until commit/rollback. No savepoint-partial-success path, no network/Redis/IdP I/O, no sleep/backoff under locks. SET LOCAL roles end with the transaction; post-transaction pool checks must observe the login role and no transaction residue.

Frozen single ACTIVE/append-only constraints are preserved; PA32 changes status/revision only, not the head identity, subject, history or entitlement facts. The narrower source/state matrix resides in the owner even if V007's broad shape would allow an Operations INACTIVE→ACTIVE.

### 7.4 Outbox and audit exact mapping

Use existing `platform.outbox_append` grant under activation_consent_role. Domain ACTIVATION_STATUS; dedupe `activation_transition:<transitionId>`; aggregate SERVICE_ACTIVATION/activationId; event ACTIVATION_TRANSITION_APPLIED. Exact 13-key redacted payload: `activationId,transitionId,subjectId,fromStatus,toStatus,fromRevision,toRevision,sourceChannel,sourceActionDomain,sourceActionEventId,statusEventDomain,statusEventId,occurredAt`. Match PA31 canonical payload/time formatting and SHA-256, including deterministic frozen occurredAt. No reason/employee/session/credentials added to this frozen payload. Exactly one Outbox fact per new transition, none on replay/conflict.

Under scoped operations_service_role invoke:

```sql
SELECT audit.append_fact(?, 'EMPLOYEE', ?, ?, 'SUBJECT', ?, ?,
                         ?::jsonb, ?::jsonb, ?, ?);
```

Arguments in order: decisionTime, trusted employeeId, SUBJECT_SUSPEND or SUBJECT_RESUME, subjectId, exact reasonCode; before snapshot exactly `{status:fromStatus,revision:fromRevision,reasonCode:reasonCode}`; after snapshot exactly `{status:toStatus,revision:toRevision,reasonCode:reasonCode}`; request_id=`statusEventId` (bounded non-secret server fact identity, not raw arbitrary HTTP headers); trace_id=server UUID string generated by owner for this fact. Trace is excluded from fingerprint and not used for replay. Require one nonnull returned audit UUID. The schema does not dedupe audit: **transaction control and the replay branch** guarantee exactly one append, and rollback handles a race after tentative writes. Audit is not an independent success or retry path. No failure-audit transaction added outside the authoritative command.

Naming evidence: docs/17 §17 and `security/validate.mjs`'s `audit-access` success projection explicitly use the authoritative projection keys `status`, `revision`, `reasonCode`. The high-risk rows for these two operations in docs/17 and validator metadata use the descriptive `status/revision/reason`, not a literal JSON snapshot-key schema; the validator's `high-risk` branch returns that `beforeAfter` metadata and does not validate literal snapshot keys. Keep the existing exact three-key snapshots above and the separate SQL argument `p_reason_code`; do not rename `reasonCode` to `reason` or claim the validator freezes a literal snapshot schema. Backend/Security must recheck this mapping at implementation time without changing frozen policy/validator bytes.

The client reason is the frozen bounded reason field, not an arbitrary snapshot document; do not invent a reason enum or sanitize away contract text. Sensitive canary tests target tokens, provider failures, SQL and foreign data, not a fictitious promise to hide the required echoed reasonCode. Internal actor IDs/action keys appear only where frozen persistence requires them, not in extra public fields.

## 8. One implementation slice and exact test names

Deliver one slice covering both commands end-to-end: ports/seam and web boundary; owner transaction/collaborator; synthetic fixture and negative/positive/concurrent tests; inventory and packaged checks. There is no behaviorally independent second slice. No intermediate “provider ready”, employee administration or generic workflow deliverable.

All new tests use ordinary **one `@Test` per method**, no parameterized/repeated/dynamic tests, nested test containers, disabled tests, duplicate method names or new annotation text in comments. Lists below are the exact CI method inventories; loops inside a test exercise both routes/values without increasing test arithmetic.

### 8.1 EmployeeSessionBoundaryTests — 8

1. `missingMalformedAndDuplicateBearerAreUnauthenticated` — no provider call on absent/bad/comma/multiple credentials.
2. `employeeAudienceIsPassedAndIndependentlyValidated` — exact expected audience, wrong returned audience 401.
3. `customerTokensAndClientIdentityNeverAuthenticateEmployee` — independent verifier/domain and spoofed identity ignored/rejected.
4. `absentAndAmbiguousVerifierFailClosed` — zero/two providers =>503.
5. `unusableVerifierAndMalformedAttestationFailClosed` — null facts/missing ID/audience/lazy failure =>503.
6. `invalidExpiredAndRevokedSessionAreUnauthenticated` — typed401 vs typed503 distinction.
7. `opaqueValuesAndProviderFailuresHaveRedactedRepresentations` — exception/toString/message canaries.
8. `concurrentSessionsNeverShareTrustedCallerState` — distinct caller identities, no shared mutable token state.

### 8.2 OperationsSubjectsWebTests — 12

1. `exactTwoPostsReturnOnlyTenFrozenFields` — route dispatch and exact field names/types, never actor/fingerprint/internal ids.
2. `wrongMethodAndNoncanonicalRoutesAreConcealed` — 404, suffix/encoded separator/semicolon/double slash/other Operations endpoints.
3. `authenticationPrecedesMalformedInputAndMediaNegotiation` —401 first; verifier outage503 first.
4. `closedBodyRejectsUnknownDuplicateMissingAndCoercedFields` — strict UTF-8/trailing/root/null/extra identity/domain fields.
5. `reasonAndRevisionBoundsPreserveFrozenInputSemantics` — whitespace/Unicode/64/65; BigInteger and integral lexical checks.
6. `idempotencyHeaderBoundsAndExactOpaqueTextArePreserved` — absent/blank/duplicate/1/128/129/NUL, no truncation.
7. `uuidAndContentNegotiationUseFrozenProblemSet` — canonical UUID422, JSON media422 and incompatible Accept500.
8. `wrongRoleAudienceAndUnavailableDependenciesMapExactly` —403/401/503 including ambiguous owner port.
9. `successReplayAndConflictHaveIdenticalFrozenProjectionRules` — historical result shape and only ACTIVATION_STATE_CONFLICT409.
10. `problemsAndResponsesNeverLeakSensitiveCanaries` — provider bearer/SQL/employee credentials/HTML/stacktrace/header injection absent; safe headers/fresh server trace.
11. `operationsAndCustomerFiltersNeverCrossAuthenticate` — real registration order, narrow customer pass-through, existing customer/public routes unchanged.
12. `concurrentRequestsKeepCallerActionAndReasonIsolated` — synthetic callers and port capture under overlap.

### 8.3 OperationsSubjectLifecycleServiceTests — 18

1. `suspendAdvancesOnlyActiveHeadAndAppendsOneFactSet`.
2. `resumeAdvancesOnlySuspendedHeadAndAppendsOneFactSet`.
3. `currentActiveOpsIsRequiredBeforeExistenceOrReplay` —401 disabled/missing,403 ADMIN; malformed facts/identity and unknown status/role/revision assert `INTEGRITY` => `SERVICE_UNAVAILABLE`/503, never 500, before existence/replay.
4. `missingSubjectOrActivationReturnsNotFoundWithoutCreation`.
5. `wrongStateAndStaleRevisionConflictWithoutWrites` — includes INACTIVE and max/range.
6. `exactActionReplayReturnsOriginalBeforeCurrentPredicate`.
7. `changedActorSubjectRouteReasonOrExpectedRevisionConflicts`.
8. `frozenFingerprintHasExactKeysNullsAndCanonicalUtf8` — eleven keys, expected revision excluded from hash but explicitly compared.
9. `maximumActionIdentityIsStoredWithoutTruncation`.
10. `statusIdentityUsesActivationAndNextRevisionWithoutOverflow`.
11. `lockOrderAndOwnerAssertionsPrecedeEveryLocalWrite`.
12. `unusableHeadTransitionOrEmployeeContextFailsClosed` — preserve head/transition assertions; cover every §5.1 employee-context condition, both typed kinds =>503 with no writes, and unknown programming defects outside the typed contract =>500.
13. `conditionalUpdateMissRereadsOnceAndConflicts`.
14. `auditAndOutboxMappingsAreExactAndContainNoSecrets`.
15. `auditOutboxAndTransitionFailuresRollBackWholeDecision` — transaction mock plus authoritative DB tests below.
16. `resumeHasNoTrialConsentOrForeignMutationCollaborator` — no trial dependency available to construct service.
17. `uniqueAndTechnicalFailuresMapOnlyAfterTransactionCleanup` — retain unique/technical cleanup assertions; both employee-lock kinds roll back before translation to503 after handoff, with no raw cause/message leakage.
18. `replayAfterLaterTransitionReturnsFirstImmutableResult`.

### 8.4 OperationsLifecycleConfigurationTests — 8

1. `defaultWebAndDisabledProfilesCreateNoLifecyclePool`.
2. `explicitEnablementRequiresDedicatedNonblankCredentials`.
3. `ownerBeansUseExactQualifiersAndOneDatasource`.
4. `writeTemplateUsesRequiresNewReadCommittedAndRollbackOnCommitFailure`.
5. `employeeCollaboratorIsOperationsOwnedAndUsesExactQualifiedManagerAndDsl` — persistence-only enabled creates exactly the `@Component("employeeAdministratorLock")` bean, with no `defaultEmployeeAdministratorLock` bean; constructor qualifiers are exact; it uses the same manager/DSL and no pool or transaction manager of its own; web-only creates none. Assert the owner's unqualified-by-name provider does not resolve candidates at construction or hide multiple candidates.
6. `missingAndAmbiguousOwnerCollaboratorsFailClosed` — with otherwise valid configuration and zero/two employee-lock candidates, assert successful context refresh and owner creation, then per-call redacted `UNAVAILABLE` => `SERVICE_UNAVAILABLE`/503 before transaction/locks/writes, never startup failure. Also assert per-call503 for lazy resolution failure. Use only the published collaborator type, no internal implementation import.
7. `webBoundaryUsesIndependentVerifierAndExactFilterOrder`.
8. `configurationAndStartupFailuresAreRedacted` — actual credential/configuration startup failures remain redacted; missing/ambiguous employee-lock candidates must instead start successfully and produce only the per-call503 asserted in test6.

### 8.5 OperationsLifecycleBoundaryTests — 8

1. `operationsPortsAndNamedInterfacesAreExactAndAcyclic` — reflection record fields, two port methods, one lock method, exact file/type classification, redacted throwable channels and exactly `UNAVAILABLE|INTEGRITY`; assert §5.1's condition table, both typed kinds =>503 (never500), unknown programming defect outside that contract =>500.
2. `activationImportsOnlyPublishedOperationsInterfaces` — two allowlisted packages; no internal import or reverse edge.
3. `employeeSessionIsIndependentAndHasNoProductionVerifierImplementation` — production implementation firewall plus test-only synthetic class.
4. `employeeOwnerSqlIsLockOnlyAndUsesMandatoryLiteralRoles` — only admin advisory/own employee SELECT FOR UPDATE, finally restoration; no writes/I/O.
5. `lifecycleSqlHasNoForeignBaseWritesOrTrialCalls` — exact SQL allowlist, no entitlement/consent/reservation/evidence/order operations, no issue_first_trial.
6. `routesBinderProblemsAndCustomerPassThroughStayExact` — no Operations queries, arbitrary bypass, new codes or shared auth; controller translates only redacted lifecycle failures, both employee-lock kinds => frozen `SERVICE_UNAVAILABLE`/503, and emits no cause/message/input/SQL.
7. `frozenOpenApiMigrationsAndPolicyBytesAreUnchanged` — retain existing raw/LF hash strategy; add docs/12 and docs/17 checks against table below, do not weaken PA31 frozen test.
8. `auditOutboxFingerprintAndResponseFieldSetsAreExact` — 3/3 snapshot,13 payload,11 fingerprint,10 result; no sensitive additions.

### 8.6 OperationsSubjectLifecycleIT — 24 (real PostgreSQL16 + HTTP)

1. `suspendActiveSubjectCommitsOneRevisionTransitionOutboxAndAudit`.
2. `resumeSuspendedSubjectCommitsOneRevisionTransitionOutboxAndAudit`.
3. `sameSuspendActionReplaysOriginalWithoutAnyWrite`.
4. `sameResumeActionReplaysOriginalWithoutAnyWrite`.
5. `historicalReplayAfterOppositeTransitionReturnsOriginalResult`.
6. `changedReasonAndExpectedRevisionConflictWithoutDuplicateFacts`.
7. `changedActorSubjectAndRouteUnderSameKeyConflict` — changed actor uses a second distinct, currently ACTIVE OPS employee and its own verified token, so409 proves fingerprint conflict rather than401/403 authority denial.
8. `missingSubjectAndMissingActivationAreNotFound`.
9. `inactiveAndWrongCurrentStatesConflict`.
10. `staleAndUnrepresentableExpectedRevisionsConflict`.
11. `missingDisabledAndNonOpsEmployeesCannotMutateOrReplay`.
12. `wrongAudienceAndCustomerCredentialNeverReachOwnerWrites`.
13. `absentAmbiguousAndUnusableVerifierFailClosed`.
14. `maximumActionAndReasonRoundTripWithoutTruncation`.
15. `concurrentSameSuspendActionConvergesOnOneFactSet`.
16. `concurrentSameResumeActionConvergesOnOneFactSet`.
17. `concurrentDistinctActionsAtOneRevisionHaveOneWinner`.
18. `concurrentCrossSubjectSameKeyHasOneWinnerAndOneConflict` — assert exactly one200/one409 and one committed fact set, not that a particular constraint exception must execute.
19. `employeeRevocationSerializesWithLockedCurrentAuthority`.
20. `activationAndCustomerCommandRacePreservesOneAuthoritativeHead`.
21. `resumeAndSuspendNeverChangeTrialConsentOrOtherBusinessFacts`.
22. `auditFailureRollsBackHeadTransitionAndOutbox`.
23. `transitionOutboxAndCommitFailuresCannotPartiallySucceed`.
24. `responsesProblemsAndEventsRespectSensitiveOutputBoundary`.

Assertions common to success tests: pre/post full rows, revision exactly +1, matching immutable from/to, exact actor_employee_id and NULL account/consent, exact header, recomputed fingerprint/status identity, exact one new transition/Outbox/audit, DB timestamp consistency, ten keys and no extra serialization. Replay asserts byte-equivalent result values including original id/time, head unchanged, no UPDATE or append calls and all business rows unchanged. Audit uniqueness is counted by actor/object/action and before/after status/revision, not just aggregate total.

Concurrency uses two independently borrowed connections/pools and barriers/latches plus observed pg_locks/backend state, bounded timeout, no sleeps as correctness evidence. Same subject/key has two 200 results with identical ten fields and one fact set; distinct keys at one revision have one200/one409. Cross-subject same-key collision asserts one200/one409 and only one committed transition/Outbox/audit set; global action lookup or the unique backstop may detect the conflict, never a subject-filtered replay lookup. Do not require a particular internal constraint mechanism. Employee disable/demotion setup uses admin advisory then employee row: test both serial orders; a waiting disable cannot interleave with the held employee lock, and a committed disable before acquisition denies first/replay. Customer activation race uses existing customer service as an opponent only: an INACTIVE first activation may win then make stale Operations expected revision conflict; Operations must never become an alternate first-activation path. No new IVR implementation is needed.

### 8.7 OperationsLifecycleRoleLeastPrivilegeIT — 12

1. `activationRoleCannotReadOrLockEmployeeTable` —42501/schema denial; no foreign privilege added.
2. `employeeOwnerLocksOnlyItsOwnRowWithoutWrites`.
3. `handoffUsesOneBackendPidSessionAndTransactionId`.
4. `mandatoryCallOutsideTransactionOrWrongEntryRoleIsRejected`.
5. `activationRoleIsRestoredBeforeEveryOwnerWrite`.
6. `auditUsesOnlyExistingFunctionCapabilityAndRestoresRole`.
7. `failedRestorationRollsBackAndPoolReuseCannotLeakRole`.
8. `ownerAndAuditRolesCannotWriteForeignBaseTables` —SELECT/read grants remain exact; no FOR UPDATE on foreign tables.
9. `locksFollowSubjectActivationAdministratorEmployeeOutboxAuditOrder` — real contention and statement/PID capture.
10. `runtimeLoginHasOnlyThreeFixtureMembershipsAndNoPrivilegeRepair`.
11. `frozenSeventeenMigrationsAndIndependent288AclOracleRemainExact`.
12. `failureCleanupLeavesNoTransactionRoleLockOrPartialFact`.

### 8.8 Fixture construction and failure injection

OperationsSubjectLifecycleFixture owns a PostgreSQL `postgres:16` GenericContainer using current fixture conventions, migrates exactly V001–V017 through test Flyway, then provisions only a test-local NOINHERIT login with three existing role memberships. No role change to the migration source or production PA25 identities. Use the independent 288 table/schema/function ACL tuple oracle from CustomerEvidenceReadPrivilegeIT unchanged; runtime login memberships are separate from that oracle. Include two distinct ACTIVE OPS employees, each with its own verified opaque token, plus an ADMIN employee with synthetic HMAC/issuer test data. The second ACTIVE OPS employee is the changed-actor fixture for existing §8.6 test7, not a new test or fixture file. Missing/disabled employees and bad attestations are explicit cases.

Seed valid subject, head and matching ledger history under fixture administrator authority. For no-trial-change assertions exercise both an activation with original trial/grant/balance (reuse existing customer activation fixture setup when appropriate) and a valid head with no trial. Snapshot **all** rows of trial_grant, entitlement_grant, entitlement_balance (including revision/expiry/units), consent_record, usage_reservation, usage_fact, evidence/media and customer_order plus unrelated policy/subject/employee rows before/after suspend/resume/replay/denial. Compare canonical full row values, not counts alone; exception is explicitly the activation head, its new transition, registered Outbox and audit. Statement capture asserts no issue_first_trial or forbidden SQL is attempted. No production provider/service implementation is used.

Use test-source repository decorators/execute listeners and JDBC failure wrappers to inject failure after head update, transition insert, Outbox append, audit invocation, role restoration and before actual commit. A deliberately aborted SQL transaction exercises restoration failure and rollback. Do not alter frozen function definitions/grants in production or use trigger hacks as implementation. Commit-acknowledgment loss is a separate outcome: database may already have committed; simulate lost HTTP response and prove exact replay retrieves that committed result once. Never claim an uncertain network commit was rolled back merely because the client saw an error.

Fixture reset/close owns its test rows/contexts/pools/container, restores listeners/wrappers and asserts no locks/transaction/role residue. Existing fixtures and canonical six-login/seven-effective-role/four-edge provisioning tests remain unchanged, isolated in their existing test containers. Test synthetic verifier beans are explicitly supplied, not a runtime `test` profile adapter.

## 9. Measured source = CI = POM = XML arithmetic

Read-only measurement parsed the actual embedded CI Python with AST and evaluated its literal inventories/update loops, then compared every method set with source regex `@Test\s+(?:public\s+)?void\s+(\w+)\s*\(`. It also compared all test-bearing classes and Failsafe POM includes. Result: **59 Surefire classes / 520 methods + 25 Failsafe classes / 404 methods = 84 classes / 924 methods**. Exactly 925 source lines contain the annotation token: 924 real annotations and one existing PostgresContainerIT comment. No XML run was available/claimed at plan gate; “source=CI” and POM equality are measured, XML equality is an implementation acceptance requirement.

Baseline Surefire class/count inventory (package qualification is the current CI dictionary; no existing class moves or method changes):

```
CustomerBusinessNumbersWebTests 12; CustomerEvidenceConfigurationTests 6;
CustomerEvidenceWebTests 12; CustomerMeWebTests 8; CustomerSharesCommandWebTests 4;
CustomerSharesWebTests 10; CustomerSubjectActivationConfigurationTests 8;
CustomerSubjectActivationsWebTests 12; CustomerSubjectClaimsWebTests 12;
CustomerSubjectDetailConfigurationTests 6; CustomerSubjectsWebTests 12;
CustomerWebConfigurationTests 6; EvidenceMediaReadPersistenceConfigurationTests 4;
MediaGatewayConfigurationTests 4; MediaGatewayWebTests 18;
MediaPlaybackConfigurationTests 19; OneCallApplicationTests 1;
PublicSharePersistenceConfigurationTests 2; PublicShareWebTests 12;
SharingCommandPersistenceConfigurationTests 2; SharingPersistenceConfigurationTests 2;
SubjectClaimPersistenceConfigurationTests 8; CustomerSubjectActivationServiceTests 18;
BootstrapDependencyBoundaryTests 1; CustomerApiBoundaryTests 3;
CustomerBusinessNumberBoundaryTests 4; CustomerEvidenceBoundaryTests 4;
CustomerSubjectActivationBoundaryTests 6; CustomerSubjectClaimBoundaryTests 6;
CustomerSubjectDetailBoundaryTests 4; ModuleBoundaryTests 9; SharingQueryBoundaryTests 8;
ActivationTrialServiceTests 8; ControlledMediaByteSourceTests 4;
EvidenceMediaObjectReaderTests 4; CustomerShareCommandServiceTests 6;
MediaPlaybackCredentialAuthorityTests 24; MediaPlaybackServiceTests 21;
PublicShareQueryServiceTests 4; ShareAdmissionServiceTests 26;
SharingPlaybackStateReaderTests 8; SingleByteRangeTests 6;
CustomerSubjectClaimServiceTests 17; SubjectClaimVerificationReferenceTests 8;
CustomerBusinessNumberQueryServiceTests 8; CustomerContextServiceTests 6;
CustomerEvidenceQueryServiceTests 12; CustomerSubjectDetailQueryServiceTests 12;
CustomerSessionBoundaryTests 7; CustomerBusinessNumberCursorCodecTests 8;
CustomerEvidenceCursorCodecTests 8; CustomerShareCursorCodecTests 6;
CustomerShareQueryServiceTests 8; FileMediaPlaybackSigningKeyProviderTests 10;
MediaPlaybackFileKeySourceIntegrationTests 8; MediaPlaybackKeyBundleParserTests 12;
MediaPlaybackTencentKeySourceIntegrationTests 10;
TencentSsmMediaPlaybackSigningKeyProviderTests 18; TencentSsmSecretStringClientTests 8.
```

Baseline Failsafe exact includes/counts, all under `com.onecall.archtests.container` and POM `**/<simpleName>.java`:

```
ActivationCommandRoleLeastPrivilegeIT 10; CustomerBusinessNumberReadPrivilegeIT 8;
CustomerBusinessNumbersIT 18; CustomerEvidenceIT 24; CustomerEvidenceReadPrivilegeIT 10;
CustomerMeIT 10; CustomerShareCommandsIT 18; CustomerSharesIT 8;
CustomerSubjectActivationsIT 24; CustomerSubjectClaimsIT 24;
CustomerSubjectDetailReadPrivilegeIT 10; CustomerSubjectsIT 20;
DatabaseRuntimeAclRbacHardeningIT 12; EvidenceMediaReadIT 8;
EvidenceMediaReadRoleLeastPrivilegeIT 8; MediaGatewayTransportIT 8; MediaPlaybackIT 9;
PostgresContainerIT 64; ProductionDatabaseIdentityProvisioningIT 20;
PublicShareAdmissionIT 42; PublicSharesIT 8; SharingAdmissionRoleLeastPrivilegeIT 9;
SharingQueryServiceIT 10; SharingRoleLeastPrivilegeIT 12; SubjectClaimRoleLeastPrivilegeIT 10.
```

PA32 delta and final hard assertions:

| Inventory | Existing | Added | Final |
| --- | ---: | ---: | ---: |
| Surefire classes/methods | 59 / 520 | 5 / 54 | **64 / 574** |
| Failsafe classes/methods | 25 / 404 | 2 / 36 | **27 / 440** |
| Total classes/methods | 84 / 924 | 7 / 90 | **91 / 1014** |
| Annotation-token lines | 925 | 90 | **1015** |
| Failsafe includes | 25 | 2 | **27** |

CI adds literal `pa32_unit_additions` sets for exactly the five fully qualified §4 classes and §8 names, in ordered cardinalities `[8,12,18,8,8]`; `pa32_container_additions` cardinalities `[24,12]`. Assert no name overlaps and update existing inventories. Add **only** `**/OperationsSubjectLifecycleIT.java` and `**/OperationsLifecycleRoleLeastPrivilegeIT.java` to Failsafe includes. Helpers have no annotations and are never includes.

Update CI's early aggregate Surefire 520 check to574, final method totals to `[574,440]`, class counts64/27, include27, exact_inventory totals574/440 and summary. Preserve PostgresContainerIT's64 exact names and every existing class/method set. Extend all historical start-count/line-scan subtraction sets to remove PA32 names as well as existing later additions; otherwise historical assertions would accidentally absorb90 new methods. Add `pa32_start_totals == [520,404]`, sum924 and `pa32_start_line_scan_total == 925`; preserve PA31 start `[468,370]` and839, PA30 `[417,336]` and754, and every older baseline value. Final source annotation line scan becomes1015; the single preexisting comment-hit tuple remains unchanged. Never add a new comment containing the annotation token in test source.

Run exact_inventory against fresh full-suite XML: exact filenames, FQCN/method pairs, source method sets, report testcase counts, zero failures/errors/skips, no duplicate testcase, no extra/missing report. Clean target before the full run so stale focused XML cannot satisfy acceptance. Keep current no dynamic/parameterized/repeated annotations and JUnit test-scope rules. If actual inventory differs, stop/reconcile the plan with Controller; do not loosen the inventory.

## 10. Validation commands (future authorized implementation gate)

None of the build/container/npm commands below were executed at plan gate. They may create build output, containers and test DBs and require the later implementation authorization. Run with repository-pinned Temurin21.0.13+9, Maven3.9.9, Node22.23.4, PostgreSQL16 Docker availability and no production credentials. Do not use a live database. This WSL session's java launcher currently fails with `UtilBindVsockAnyPort`; use the Controller's functioning pinned runner later, not an environment mutation here.

### 10.1 Focused PostgreSQL16 and unit validation

From repository root:

```bash
cd backend
./mvnw -B -ntp -Pcontainer-smoke \
  -Dtest=EmployeeSessionBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectLifecycleServiceTests,OperationsLifecycleConfigurationTests,OperationsLifecycleBoundaryTests,ModuleBoundaryTests,CustomerSubjectActivationBoundaryTests,CustomerApiBoundaryTests,CustomerWebConfigurationTests \
  -Dit.test=OperationsSubjectLifecycleIT,OperationsLifecycleRoleLeastPrivilegeIT,CustomerSubjectActivationsIT,ActivationCommandRoleLeastPrivilegeIT \
  verify
```

This focused command is not the final inventory acceptance run. No skipped-no-Docker switch. Check server version >=160000 and <170000 in both new IT fixtures; both use postgres:16.

### 10.2 Full PostgreSQL16 build, source/CI/XML, JAR smokes and firewall

```bash
cd backend
./mvnw -B -ntp clean -Pcontainer-smoke verify
cd ..
python3 - <<'PY'
from pathlib import Path
import re
import subprocess
import textwrap
workflow = Path('.github/workflows/ci.yml').read_text()
names = ['assert container smoke actually ran',
         'default and opt-in web jar smokes',
         'production jar dependency firewall']
for name in names:
    pattern = r'^      - name: ' + re.escape(name) + r'\n        run: \|\n((?:^          .*\n|^\n)*)'
    matches = list(re.finditer(pattern, workflow, re.M))
    assert len(matches) == 1, name
    subprocess.run(['bash', '-euo', 'pipefail', '-c',
                    textwrap.dedent(matches[0].group(1))], cwd='backend', check=True)
PY
```

This executes the exact checked-in CI steps, including existing runtime dependency parsing/adversarial probes, instead of inventing a weaker local firewall. Existing firewall baseline37/34, ssm72/65, counterparts64, dependency-only8 and sole packager exception remain unchanged; add no library. Extend its classfile direct-interface scan to reject implementations of `com/onecall/core/operations_iam/internal/security/EmployeeSessionVerifier` in BOOT-INF/classes in addition to the customer verifier ban. Preserve fixture/TestConfiguration/classname bans and Spring Security bans. No test verifier may be bundled and no provider dependency may appear.

Extend the existing web-only JAR probe list for **both** endpoints: missing Authorization=>401; well-formed opaque bearer with no employee verifier=>503; wrong method/other Operations path=>404. No persistence flags/credentials are supplied; assert no Operations Hikari pool, same five problem keys, headers, trace and canary rules. Default nonweb JAR still exits successfully without web server/pool. Preserve all existing customer/public/media probes and dependency firewall tests. Hosted CI on the final authorized PR head is a later acceptance input owned by Controller, not a claim made here.

### 10.3 OpenAPI and frozen-byte validation

```bash
(cd api/openapi && npm ci && npm run check:s3)
git diff --exit-code 8206f2a8f8a40cd58793efdda55aa41843af5d5f -- \
  api/openapi/onecall-v1.openapi.yaml backend/src/main/resources/db/migration \
  docs/12-technology-stack-and-system-architecture.md \
  docs/14-production-data-model-and-flyway-schema-spec.md \
  docs/15-production-openapi-surface-spec.md docs/17-production-security-design-spec.md
```

Existing CI contract-regressions A1–A5 steps remain mandatory, including security/IVR/capacity/acceptance validators; run the full workflow in the authorized environment, not only OpenAPI lint. No generated client or tmp output belongs in the implementation diff.

For raw-byte hashes in this exact worktree use `sha256sum` against §11. The existing PA31 boundary test records both raw and LF-normalized alternatives; retain its values and add the same discipline for docs/12/17 if crossing checkout line endings. Do not “fix” frozen files by normalizing their bytes. Confirm exactly17 migration files and17 successful Flyway versions, no18, unchanged function definitions/ACL oracle, and the independent no-foreign-lock negative tests.

### 10.4 Repository hygiene and plan-only verification

Native git discovery here reads a Windows gitdir (`D:/codex-call/.git/worktrees/codex-call16`), which Linux git does not resolve automatically. The following explicit read-only path was verified to reference **this** worktree HEAD, not an older worktree:

```bash
export GIT_OPTIONAL_LOCKS=0
git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call16 --work-tree="$PWD" rev-parse HEAD
git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call16 --work-tree="$PWD" -c core.fsmonitor=false status --short
git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call16 --work-tree="$PWD" diff --check
sha256sum docs/gateflow/pa32-operations-subject-suspension-resume-lifecycle-plan.md
```

Do not rewrite `.git`. In a functioning native checkout plain git equivalents are sufficient. At implementation completion use `git diff --name-only`, `git ls-files --others --exclude-standard` and `git diff --check` to enforce the36-path allowlist plus Controller-authorized gate artifacts. Preserve initial changes not owned by this task: Control already modified by Controller, and preexisting untracked `.codex` entry. Do not stage/remove/follow/copy it. No commit, fetch, branch creation, remote mutation or database/Docker-state modification is part of this plan gate.

## 11. Frozen evidence hashes

SHA256 of inspected **raw bytes** (lowercase hex). The Control value below, `79b305fe...`, is the **historical plan-authoring snapshot**. The review-freeze provenance for the mutable Controller bookkeeping is `5ee4dfd6e3b17c5b6400045fdb397d8baf050aab3ce1fc29a8a2b75f146c9039`; it is recorded for provenance only, not as a claim that the mutable Control remains byte-identical. The PA32 Goal Confirmation text in the Control, not a moving file hash, is the binding authority. POM/CI hashes establish measured inventory input, not a prohibition on the narrowly planned later implementation edits.

| Path | SHA256 |
| --- | --- |
| `docs/gateflow/project-execution-control.md` | historical plan-authoring snapshot: `79b305fe7ca474a40823eef309f5ef855abfddb950fbbc1164f09630c2ae8f9b` |
| `.github/workflows/ci.yml` | `7d2ea25830654744776f47db92417a610f0a44fd190473e682d4e16865a653e6` |
| `backend/pom.xml` | `6cc1c149e83fba720cbc793c995a7406c36eaa5fbefb22cff91912c510edb38a` |
| `api/openapi/onecall-v1.openapi.yaml` | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` |
| `docs/12-technology-stack-and-system-architecture.md` | `9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31` |
| `docs/15-production-openapi-surface-spec.md` | `db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0` |
| `docs/17-production-security-design-spec.md` | `7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e` |

Migration paths below are under `backend/src/main/resources/db/migration/`:

| File | SHA256 |
| --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| `V002__identity_organization_and_employee.sql` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| `V003__subject_number_and_claim.sql` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| `V004__catalog_order_and_payment.sql` | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| `V005__ivr_task_session_call.sql` | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| `V006__trial_entitlement_issuance_reservation_usage.sql` | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| `V007__activation_and_consent.sql` | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |
| `V008__evidence_media.sql` | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` |
| `V009__sharing.sql` | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` |
| `V010__sharing_role_least_privilege.sql` | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` |
| `V011__sharing_public_read_least_privilege.sql` | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` |
| `V012__sharing_admission_least_privilege.sql` | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` |
| `V013__evidence_media_reader_role.sql` | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` |
| `V014__database_runtime_acl_rbac_hardening.sql` | `bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8` |
| `V015__customer_business_number_read_privileges.sql` | `c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc` |
| `V016__customer_subject_detail_read_privileges.sql` | `a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c` |
| `V017__customer_evidence_read_privileges.sql` | `74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3` |

## 12. No-drift proofs, docs decision and residual owners

| Required proof | Implementation evidence / invariant |
| --- | --- |
| No goal drift | Exactly two mappings/two port methods; no queries/providers/issuance/admin endpoints; 36-path implementation allowlist |
| No overdesign | One slice, one verifier seam, one lock collaborator, one inbound port; reuse canonical/Outbox utilities; no framework/new store |
| No duplicate transition/audit | Global action lookup before fresh effects, immutable fingerprint plus expected-revision comparison, three ledger unique keys, conditional head update, one transaction including audit; concurrency/replay/rollback tests |
| No trial reissue | New service has no entitlement collaborator; no SQL/call to issue_first_trial; full-row snapshots for trial/grant/balance/consent/other business facts |
| No foreign grant/write | Existing roles only; employee SQL under owner role, audit only existing function, unchanged 288 ACL tuples and17 migrations; no production provisioning edit |
| No lock inversion | Subject3 → activation4 → subject read5 → admin advisory/employee13 → Outbox14 → audit15; no earlier reacquisition, network I/O or task/reservation paths |
| No OpenAPI/migration/policy change | Raw hashes, retained PA31 byte tests, added docs12/17 checks, base diff and full contract regressions |
| No partial success/role leak | MANDATORY same-resource handoff, explicit entry/exit assertions and finally restoration, rollback-only on restoration failure, commit before output, real pool-reuse/failure tests |

Docs decision: this bounded fix modifies only the cumulative plan and adds the fix-2 gate artifact. Existing fix-1, reviews, design/API/security/data docs and Control stay untouched. During implementation, tests and code embody the already frozen behavior; no README or existing policy update is necessary. Later completion/review records, if Controller requests them, are gate artifacts, not permission to rewrite policy or expand implementation scope.

Residual risks and owners:

- Security/Operations IAM deployment: production verifier attestation, independent store/issuer, expiry/revocation/MFA and Q1 readiness remain external. No synthetic verifier in JAR; absent/ambiguous/unusable provider503. No production-readiness claim.
- Database/deployment owner: actual same-login SET ROLE authority for the three preexisting roles must be supplied externally. Missing capability503; no migration/grant repair. Actual PostgreSQL16 lock/rollback/ACL behavior is proved in later ITs, not by this plan.
- Backend owner: historical replay must use stored status identity and separately compare expectedRevision; otherwise a current-revision fingerprint would break replay or broaden semantics. Frozen eleven-key fingerprint is not negotiable.
- Backend/CI owner: expanded suite runtime (27 IT classes) and pinned Java/Docker runner availability. Do not weaken timeout coverage, skip containers or change test arithmetic without Controller reconciliation. Current WSL launcher failure is an environment limitation, not a code defect fixed here.
- Operations owner: global admin advisory intentionally serializes employee authority with administration; throughput tuning is not PA32. UI secondary confirmation and production live acceptance remain runtime-unverified external scope.
- Controller: adjudicate this plan and any discovered privilege/contract/inventory blocker. No later gate begins automatically.

## 13. Plan-gate completion report

- Files changed by bounded task `pa32-operations-lifecycle-plan-fix2-gpt-20261113-01`: **one modified cumulative plan** and **one new fix artifact**, `docs/gateflow/pa32-operations-subject-suspension-resume-lifecycle-plan-fix-2.md`; no source, tests, migrations, OpenAPI, CI, POM, other existing docs, fix-1, reviews or Control changed by this task. The implementation allowlist remains 36 paths and its test/class/method arithmetic is unchanged.
- Evidence: binding checkpoint read first; frozen sources and current authentication/activation/owner-handoff/architecture/fixtures inspected; current source/CI method sets and POM includes reconciled exactly at84/924 and25 includes; raw frozen input hashes recorded above; current HEAD verified explicitly without changing git metadata.
- Preexisting workspace differences: Controller's Control modification and untracked `.codex` entry; preserved, not attributed to this task. The worktree was therefore not claimed clean despite the historical start-state sentence in Control.
- Validation actually performed: original planning measured the AST/source/POM arithmetic above; fix-2 performed read-only repository/validator inspection, prior/frozen-file hashes, unchanged-section and exact test-name/count comparisons, full regular-file hash-manifest comparison and whitespace checks (including explicit read-only `git diff --check`). Four PA32 plan-review artifacts are present and were inspected; the requested six are not available in this checkout (the six earlier same-date reviews concern PA31). No tests/build/container/database/network/remote mutations executed. Later commands are instructions, not claimed results.
- Artifact SHA256 is reported alongside this document after final write; an ordinary document cannot contain its own final cryptographic digest without changing it. §11 contains the source/frozen evidence hashes; the Controller can reproduce the artifact hash with §10.4.
- Unresolved **goal/contract/privilege/inventory blockers: none found**. External production verifier/identity readiness and the local Java-launcher limitation remain explicit risks, not assumed delivered features.
- Status: **bounded plan fix-2 complete; stop for Controller adjudication**. REV-03 completion and REV-06..08 are corrected in plan text, pending Controller acceptance; REV-01/02/04/05 and every frozen boundary/count remain preserved. No re-review, implementation, hosted CI, merge or production readiness is entered or claimed.
