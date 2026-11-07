# PA18 Sharing Media Authorization and Playback — Code-Generation-Ready Plan

- **Work unit:** `PA18-sharing-media-authorization-and-playback`
- **Task label:** `pa18-sharing-media-authorization-plan-fix2-gpt-20261107`
- **Gate:** `fix`
- **Runtime/provider:** Java/Spring MVC, PostgreSQL 16, existing PA17 transaction/repository seams, deterministic test media-provider seam; production Q22/Redis/signing/media-provider values remain unavailable and fail closed.
- **Goal contract:** implement only frozen `POST /api/v1/public/shares/{capabilityToken}/admissions/{admissionId}/media-authorizations` for an existing PA17 admission.
- **Plan shape:** one implementation slice. No new goal, endpoint, schema, migration, ACL, OpenAPI, frontend, production Q22/provider value, media delivery, future-slice behavior, commit, PR, approval, or external action is authorized.

## 1. Binding contract and non-goals

The Goal Confirmation in `docs/gateflow/project-execution-control.md:15-30` is binding. The operation accepts exactly `authorizationRequestId` (1–128 UTF-8 code points) and `purpose` (`PLAYBACK`, `SEEK`, `RENEW`), validates the capability token, grant/admission association, durable admission witness, current grant status/time/generation, subject risk, captured/current binding, and organization-member re-authentication, then returns exactly `{credential, expiresAt}`. It permits an `ACTIVE` grant or an already-admitted `LOCKED` session, replays the first credential for the same request and purpose, conflicts on changed semantic input, closes PostgreSQL transactions/locks before provider I/O, appends the frozen deterministic redacted event where required, and never changes admission count, admission/session expiry, status, generation, or media delivery state.

Use only V009 `sharing.evidence_share_grant`, `sharing.share_admission`, and `sharing.share_access_event`, existing V012 privileges, and existing PA17 facts/seams. V001–V012 remain byte-identical. Any required table, column, lifecycle transition, ACL, OpenAPI code/field, production value, or delivery behavior is a blocking scope change; stop for renewed confirmation.

## 2. Exact HTTP and boundary behavior

### 2.1 Filter reachability

Modify `CustomerBoundaryFilter.java` with a dedicated boolean named `publicMediaAuthorization`:

```java
boolean publicMediaAuthorization = "POST".equals(request.getMethod())
        && servletPath.matches("/api/v1/public/shares/[^/;%]+/admissions/[^/;%]+/media-authorizations");
```

The allow decision must include `publicMediaAuthorization`. It must use the same optional-bearer behavior as `publicRead` and `publicAdmission`: absent `Authorization` yields a null principal; present bearer authentication is attempted. It must use the same-origin/path guard exactly as the other public routes: `(request.getContextPath() + servletPath).equals(request.getRequestURI())`; encoded-solidus, semicolon, extra path segments, and a different request URI reach `NOT_FOUND`. It must include `publicMediaAuthorization` in the `application/json` Content-Type validation branch. Add an MVC assertion that the exact path reaches the controller and never returns `NOT_FOUND` before testing the operation’s own responses.

### 2.2 Request/response and caller handoff

Bind a closed JSON object with exactly `authorizationRequestId` and `purpose`; reject unknown/duplicate/missing fields, trailing tokens, invalid UTF-8, NUL, blank/overlong values, and unknown enum values as `VALIDATION_FAILED` (422). Return exactly the existing `ShareAdmissionView.MediaAuthorization` record (`credential`, `expiresAt`); do not add a duplicate response DTO. Its existing redacted `toString` remains the response redaction contract.

The future `PublicSharesController` constructor generic parameter types and qualifiers are exactly, in order:

```text
ObjectProvider<PublicShareQueryService>                  @Qualifier("publicShareQueryService")
ObjectProvider<CustomerContextService>                  @Qualifier("customerContextService")
CustomerProblemWriter                                    no qualifier
ObjectProvider<ShareAdmissionService>                   @Qualifier("shareAdmissionService")
ObjectProvider<ShareMediaAuthorizationService>           @Qualifier("shareMediaAuthorizationService")
```

`CustomerApiBoundaryTests.java` must extend its existing assertions (no new test method) with these exact generic type names and qualifier values. Its exact sharing imports in `PublicSharesController.java` must be exactly:

```text
import com.onecall.core.sharing.internal.publicquery.OpaqueCapabilityToken;
import com.onecall.core.sharing.internal.publicquery.PublicShareQueryException;
import com.onecall.core.sharing.internal.publicquery.PublicShareQueryService;
import com.onecall.core.sharing.internal.publicquery.PublicShareView;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionCapabilityToken;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionException;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionJsonBinder;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionView;
import com.onecall.core.sharing.internal.publicadmission.TrustedShareAdmissionCaller;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest;
import com.onecall.core.sharing.internal.publicadmission.ShareMediaAuthorizationService;
```

The controller may continue reusing `ShareAdmissionException` for the frozen error codes and `ShareAdmissionView.MediaAuthorization` for the response; it must not import persistence/provider types. Existing GET and PA17 admission signatures/behavior remain unchanged.

`ShareMediaAuthorizationService` is code-generation-ready with exactly one service method and one nested resolver contract:

```java
ShareAdmissionView.MediaAuthorization authorize(
        ShareAdmissionCapabilityToken capabilityToken,
        UUID admissionId,
        MediaAuthorizationRequest request,
        CallerResolver callerResolver)
        throws ShareAdmissionException;

@FunctionalInterface
interface CallerResolver {
    Optional<TrustedShareAdmissionCaller> resolve() throws ShareAdmissionException;
}
```

The public signature uses only public-admission types plus `UUID`, `Optional`, and the existing typed sharing exception. `CallerResolver` is the only caller handoff; it is lazy and may throw only `ShareAdmissionException` after wrapping controller context failures into the existing typed 401/403/503 sharing codes. The controller constructs it after the non-locking mode discovery input is available: absent bearer returns `Optional.empty()` without any context lookup; a present valid bearer returns a principal-backed resolver that performs the context lookup at most once; a syntactically/authenticated invalid bearer remains an existing filter-boundary failure. The controller must not eagerly call customer context services.

The service first performs the non-locking token/mode read. It invokes `callerResolver.resolve()` exactly once only for `ORGANIZATION_MEMBER`, after a valid token is found and before authoritative sharing reads or locks. Invalid/missing/retired token therefore remains 404 with no resolver/context I/O; non-member shares work with an absent bearer and zero resolver/context calls; member with an absent caller is 401; member context unavailable is 503; member wrong tenant/membership remains the existing 403 mapping. This precedence is asserted at web, service, and integration layers.

## 3. Ordered authorization algorithm

Earlier applicable outcomes win. The implementation must use the following exact order:

1. The filter performs same-origin, method/path, optional-bearer, and JSON Content-Type checks. The binder validates the closed body and the controller parses the admission UUID; malformed input returns 422.
2. Perform a non-locking token-digest read only to discover grant mode/owner facts. Digest exact capability-token UTF-8 bytes with lowercase SHA-256, without normalization/case folding. Missing, invalid, or retired token is uniformly `SHARE_TOKEN_INVALID` (404).
3. For `NONE`, `EXTRACTION_CODE`, and `DESIGNATED_MOBILE`, do not invoke the lazy resolver. For `ORGANIZATION_MEMBER`, after the valid-token mode read and before any authoritative grant/admission/witness read or lock, invoke the resolver exactly once. A missing/invalid caller is `UNAUTHENTICATED` (401); unavailable context is 503; a tenant/account/member refusal is `SHARE_WRONG_TENANT` or `FORBIDDEN` (403) per PA17 mapping. Then acquire the trusted organization, membership, and account lifecycle locks in PA17 rank order **7, 8, 9**. Add ordering and call-count assertions proving the non-locking mode read precedes resolver invocation, resolver invocation precedes ranks 7/8/9, and ranks 7/8/9 precede the authoritative sharing read/lock.
4. Resolve the admission only under the discovered grant; require UUID equality, grant ownership, and a durable `ADMISSION_GRANTED` witness. A missing/mismatched admission/witness is a denied share-access result without token existence disclosure; malformed/unreadable durable projection is `DEPENDENCY_UNAVAILABLE` (503), otherwise use the frozen 403 mapping.
5. Validate witness identity exactly: admission-granted event ID, event type/result, allowed payload keys, canonical payload hash, verification mode, `admittedAt < sessionExpiresAt <= admissionExpiresAt <= grantExpiresAt`, original recipient account/organization facts, and current grant owner.
6. In the authoritative read/lock transaction, evaluate current status/time/generation, captured/current binding, and risk. Precedence is pinned: invalid token 404; missing/invalid organization caller 401 before member/grant facts; wrong tenant/member/risk 403; stale generation 409; revoked/expired/session-expired 410 according to the frozen mapping; unavailable/malformed projection 503. `LOCKED` is eligible only when the admission is already witnessed; `SHARE_LOCKED` is never returned for this operation.
7. For `ORGANIZATION_MEMBER`, require current account equal to the witnessed recipient, current active organization/membership/account, current membership pair equal to witnessed organization/account, and grant owner organization equal to the witness. A changed or inactive member cannot use the historical admission.
8. Compute the frozen event identity and semantic fingerprints. Before provider I/O, perform an admission-scoped lookup across `share_access_event` rows for the same `requestKeyDigest`, not only the event ID. A prior success or failure row with a different purpose/event type, or the same purpose/event type with a different `preProviderFingerprint`, is a stable semantic conflict: return 409 with `SHARE_GENERATION_STALE`, append no event, and call no provider. A historical `FAILURE` with the same purpose and fingerprint is not a request-key poison: it permits a fresh retry. A same-purpose success is a replay candidate and its durable credential/expiry is authoritative.
9. Commit and close the read/lock transaction. Assert no PostgreSQL transaction or lock is active before policy/provider I/O. Do not perform a new media-policy lookup at P3. Derive `credentialExpiresAtBound = min(witness.sessionExpiresAt, witness.admissionExpiresAt, witness.grantExpiresAt)` from the durable admission witness, which already contains the approved Q22 session window persisted by PA17. If any witness bound is absent, malformed, non-positive, or not strictly after the decision time, return `SHARE_SESSION_POLICY_UNAVAILABLE`; do not call the media provider and do not append a failure event. A missing injected service maps to `SERVICE_UNAVAILABLE` only where PA17’s existing provider/service-absence precedent requires it.
10. Call the additive deterministic media provider outside PostgreSQL. Its idempotency key is `eventId`; `preProviderFingerprint` is a consistency input, and the deterministic expiry bound is an upper bound only, never part of the replay key. Concurrent first calls must receive the same provider credential and expiry. On success, re-enter a new short transaction and append/reuse the success event. PostgreSQL `insertAccessEvent` is the sole conflict authority; the provider is not an event store and cannot override a database conflict. An identical append is `REUSED_IDENTICAL`; return the first durable credential and provider-returned expiry. A same-event-ID append with different payload or provider mismatch is fail-closed `DEPENDENCY_UNAVAILABLE`, with no success claim. On provider unavailable or malformed output, map to `DEPENDENCY_UNAVAILABLE`; if a durable valid decision reached provider I/O, append exactly one deterministic redacted `FAILURE` event with `result=FAILED` and one of exactly `MEDIA_CREDENTIAL_UNAVAILABLE` or `MEDIA_CREDENTIAL_MALFORMED`, with no success claim. A historical same-purpose same-fingerprint `FAILURE` may be retried; a retry may append the purpose-specific success event. Once a success exists, replay returns its first credential/expiry and provider outage returns `DEPENDENCY_UNAVAILABLE` without appending another `FAILURE`. An identical repeated failure event is reused rather than duplicated.

## 4. Replay, identity, event payload, and Q22 rules

### 4.1 Frozen identity

`requestKeyDigest` is lowercase SHA-256 of exact UTF-8 bytes of `authorizationRequestId`; the raw ID is never stored, logged, returned, or sent to the provider. Purpose maps to event type: `PLAYBACK→PLAYBACK_AUTHORIZED`, `SEEK→SEEK_AUTHORIZED`, `RENEW→RENEWED`. The unchanged event ID formula is:

```text
share-access:<lowercase SHA-256(canonical JSON {shareGrantId, shareAdmissionId, eventType, requestKeyDigest})>
```

Canonical JSON uses existing PA17 ordering, lowercase UUID text, existing UTC timestamp formatting, sorted object keys, preserved array order, and lowercase hex. `event_domain=SHARE_ACCESS` remains unchanged.

### 4.2 Pre-provider semantic fingerprint

The pre-provider semantic subset is deterministic and contains no provider-produced credential field:

```json
{
  "shareGrantId":"uuid",
  "shareAdmissionId":"uuid",
  "eventType":"PLAYBACK_AUTHORIZED|SEEK_AUTHORIZED|RENEWED",
  "purpose":"PLAYBACK|SEEK|RENEW",
  "requestKeyDigest":"64-hex",
  "codeGeneration":0,
  "revocationGeneration":0,
  "grantExpiresAt":"RFC-3339",
  "admissionExpiresAt":"RFC-3339",
  "sessionExpiresAt":"RFC-3339"
}
```

`preProviderFingerprint` is SHA-256 of that canonical object. It is the semantic comparison used for same-purpose replay/conflict and is computed before provider I/O. A same request digest with a different purpose/event type conflicts even though event identity includes event type.

### 4.3 Credential expiry and post-provider payload

After the authoritative lock transaction, obtain the decision time used for validation. Compute the deterministic witness bound; do not sample a fresh `databaseNow + TTL` expiry and do not perform a new policy lookup:

```text
credentialExpiresAtBound = min(witness.sessionExpiresAt,
                               witness.admissionExpiresAt,
                               witness.grantExpiresAt)
```

The implementation must reject the decision as `SHARE_SESSION_POLICY_UNAVAILABLE` if any witness bound is absent, malformed, non-positive, or not strictly after the decision time. This is the Q22 policy already applied and persisted by PA17; it introduces no production TTL and no P3 media-policy lookup. The provider must return an expiry strictly after the decision time and at or before this bound. The post-provider durable success payload contains only deterministic non-secret result/expiry facts: the exact pre-provider fields above plus the provider-returned `credentialExpiresAt`, `result=SUCCESS`, and `preProviderFingerprint`. It contains no credential, credential digest, token, URL, object identity, bytes, Redis value, or policy secret.

On same request/purpose replay, read the first durable event and return its stored `credentialExpiresAt`; do not recompute it from a fresh wall clock and do not alter the replay payload. Provider output must use event ID as its idempotency key and use the pre-provider fingerprint as a consistency input. `postProviderFingerprint` is SHA-256 of the exact canonical durable payload and is compared by PostgreSQL append/reuse. An identical payload is `REUSED_IDENTICAL`; a payload conflict or provider-output mismatch is `DEPENDENCY_UNAVAILABLE` and cannot claim success.

### 4.4 Additive provider seam

Do not change PA17’s abstract method or record:

```text
ShareMediaCredentialProvider.issue(MediaCredentialRequest)
MediaCredentialRequest(UUID shareGrantId, UUID admissionId, int revocationGeneration,
                       int codeGeneration, Instant expiresAt)
```

`DefaultShareAdmissionService.java` remains unchanged and PA17 behavior remains functional. Add, in the already allowlisted provider file, separate PA18 types and method:

```text
MediaAuthorizationCredentialRequest(
  UUID shareGrantId, UUID shareAdmissionId, String purpose, String eventType,
  String requestKeyDigest, int codeGeneration, int revocationGeneration,
  Instant grantExpiresAt, Instant admissionExpiresAt, Instant sessionExpiresAt,
  Instant credentialExpiresAt, String preProviderFingerprint, String eventId)
MediaAuthorizationCredentialResult
issueOrReplay(MediaAuthorizationCredentialRequest)
```

`ShareMediaCredentialProvider` remains a valid `@FunctionalInterface`. `issueOrReplay` and its PA18 request/result types are additive as a `default` method whose default implementation fails closed with `Unavailable`; existing PA17 lambdas and `DefaultShareAdmissionService.java` compile unchanged. The result is a sealed redacted result with `Issued(String credential, Instant expiresAt)`, `Unavailable`, and `Malformed`; PA18 must use distinct nested names if Java nesting would collide with PA17 names. It may not persist a credential-derived digest or become a second conflict authority. The request/result contains no raw token, object identity, URL, bytes, Redis structure, or policy secret. The singleton wiring in `ShareAdmissionPersistenceConfiguration.java` and deterministic test fixture override in `ShareAdmissionServiceTests.java` are the only required overrides; `DefaultShareAdmissionService.java` remains unchanged.

### 4.5 Failure payload

The only PA18 provider-failure reason codes are `MEDIA_CREDENTIAL_UNAVAILABLE` and `MEDIA_CREDENTIAL_MALFORMED`. A durable failure payload contains exactly:

```json
{
  "shareGrantId":"uuid",
  "shareAdmissionId":"uuid",
  "eventType":"FAILURE",
  "purpose":"PLAYBACK|SEEK|RENEW",
  "requestKeyDigest":"64-hex",
  "result":"FAILED",
  "reasonCode":"MEDIA_CREDENTIAL_UNAVAILABLE|MEDIA_CREDENTIAL_MALFORMED",
  "preProviderFingerprint":"64-hex",
  "grantExpiresAt":"RFC-3339",
  "admissionExpiresAt":"RFC-3339",
  "sessionExpiresAt":"RFC-3339",
  "credentialExpiresAt":"RFC-3339"
}
```

The failure event uses the unchanged `share-access:` formula with `eventType=FAILURE`; it contains no credential and makes no success claim.

Validate provider output exactly: credential is nonnull/nonblank, 1–2048 Unicode code points, contains no NUL, and is UTF-8 encodable; expiry is nonnull, strictly after the decision time, and at or before `credentialExpiresAtBound`. Malformed credential length, NUL, encoding, null/expired/equal-window expiry, or expiry above the bound maps to `DEPENDENCY_UNAVAILABLE`. Same purpose/event type with changed pre-provider fingerprint maps to 409 `SHARE_GENERATION_STALE`, with no provider call and no event. Provider unavailable or malformed output maps to `DEPENDENCY_UNAVAILABLE`; malformed witness/policy facts map to `SHARE_SESSION_POLICY_UNAVAILABLE`; injected service absence maps to `SERVICE_UNAVAILABLE` only under the PA17 precedent.

## 5. Exact implementation allowlist

The implementation must touch exactly **17 repository files** in one slice: **10 production, 6 existing test, and 1 CI workflow**. No other file may change.

### Production — 10 exact paths

1. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` — add `publicMediaAuthorization`, optional bearer, same-origin path, JSON Content-Type, and route reachability behavior.
2. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/PublicSharesController.java` — add the route, closed binder call, trusted member caller handling, exact constructor parameter, service invocation, and frozen response/error projection.
3. `backend/src/main/java/com/onecall/core/sharing/internal/publicadmission/ShareAdmissionJsonBinder.java` — bind the exact PA18 request without changing PA17 binding.
4. `backend/src/main/java/com/onecall/core/sharing/internal/publicadmission/MediaAuthorizationRequest.java` — new exact two-component request record and enum.
5. `backend/src/main/java/com/onecall/core/sharing/internal/publicadmission/ShareMediaAuthorizationService.java` — new named public-admission interface returning the existing nested response record/outcomes; no repository/provider types.
6. `backend/src/main/java/com/onecall/core/sharing/internal/admission/DefaultShareMediaAuthorizationService.java` — new ordered algorithm, transaction closure, fingerprints, replay/conflict, provider call, failure event, and response projection.
7. `backend/src/main/java/com/onecall/core/sharing/internal/admission/ShareAdmissionRepository.java` — additive grant-scoped admission/event lookup only; retain existing PA17 methods and event insert.
8. `backend/src/main/java/com/onecall/core/sharing/internal/admission/JooqShareAdmissionRepository.java` — implement only the frozen V009 reads/event append/reuse with V012 columns.
9. `backend/src/main/java/com/onecall/core/sharing/internal/admission/ShareMediaCredentialProvider.java` — retain PA17 `issue`/`MediaCredentialRequest` unchanged; add only PA18 request/result and `issueOrReplay`.
10. `backend/src/main/java/com/onecall/core/sharing/internal/persistence/ShareAdmissionPersistenceConfiguration.java` — add the PA18 service/provider singleton wiring through existing seams; no production values or data-source/role changes.

### Tests — 6 exact existing paths

11. `backend/src/test/java/com/onecall/archtests/sharing/ShareAdmissionServiceTests.java`
12. `backend/src/test/java/com/onecall/archtests/PublicShareWebTests.java`
13. `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`
14. `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java`
15. `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java`
16. `backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java`

`CustomerProblemWriter.java` is not allowlisted because it already maps every needed code. Neither package-info file is allowlisted because existing named-interface/package dependency annotations require no edit. The existing `ShareAdmissionView.MediaAuthorization` is reused, so no duplicate response file is added. No new test class/file is added.

### CI — 1 exact existing path

17. `.github/workflows/ci.yml` — update the authoritative source=CI=XML method inventories and only their expected totals; no workflow behavior or test selection changes.

## 6. Exact tests and inventory additions

### 6.1 Surefire unit/web/architecture additions

`ShareAdmissionServiceTests` adds exactly:

- `mediaAuthorizationBinderAcceptsOnlyFrozenFieldsAndPurpose`
- `mediaAuthorizationIdentityUsesExactUtf8DigestAndPurposeEventMapping`
- `activeAndAdmittedLockedAuthorizeWithoutAdmissionMutation`
- `sameAuthorizationRequestAndPurposeReplaysCredentialAndEvent`
- `sameAuthorizationRequestAcrossPurposesConflictsWithoutSecondCredentialOrEvent`
- `organizationMemberReauthenticatesWitnessBeforeGrantRead`
- `riskBindingGenerationAndTimeDenialsFailClosedWithoutMutation`
- `concurrentSameKeyCallsConvergeAfterTransactionClosureAndCredentialNeverEntersEvent`
- `providerFailureAppendsOnlyDeterministicFailureAndRetrySucceeds`

`PublicShareWebTests` adds exactly:

- `mediaAuthorizationUsesExactRouteAndReturnsOnlyCredentialAndExpiresAt`
- `mediaAuthorizationRejectsClosedBodyAndInvalidPurposeWith422`
- `mediaAuthorizationMapsFrozenDeniedStatusesAndPrecedence`
- `organizationMemberMediaAuthorizationRequiresTrustedCurrentSession`
- `mediaAuthorizationReplayAndConflictUseFrozenProblemSurface`
- `mediaAuthorizationResponseAndProblemNeverLeakInternalMediaFacts`

The service/web assertions also cover invalid token plus unavailable context → 404 with zero context lookup, non-member plus absent bearer → success-path authorization with zero context lookup, member plus absent caller → 401, malformed witness/policy facts → `SHARE_SESSION_POLICY_UNAVAILABLE`, provider unavailable/malformed output → `DEPENDENCY_UNAVAILABLE`, injected service absence → `SERVICE_UNAVAILABLE` where PA17 requires it, all credential/expiry validation boundaries, and same-purpose changed fingerprint → 409 with zero provider/event. The named service retry test proves same-key `FAILURE` → fresh retry success and success replay during provider outage without a second `FAILURE`.

`SharingQueryBoundaryTests` adds exactly `mediaAuthorizationSurfacePreservesNamedInterfaceAndNoForeignWriteBoundary` and transitions from **1 to 2** test methods. Its complete unions must preserve the existing exact sets and add only PA18 names. `PUBLIC_ADMISSION_FILES` is the existing 8 `publicadmission/package-info.java`, `publicadmission/ShareAdmissionCapabilityToken.java`, `publicadmission/ShareAdmissionRequest.java`, `publicadmission/ShareAdmissionView.java`, `publicadmission/ShareAdmissionException.java`, `publicadmission/TrustedShareAdmissionCaller.java`, `publicadmission/ShareAdmissionService.java`, `publicadmission/ShareAdmissionJsonBinder.java`, plus `publicadmission/MediaAuthorizationRequest.java` and `publicadmission/ShareMediaAuthorizationService.java` (10 total). `ADMISSION_IMPLEMENTATION_FILES` is the existing 10 `admission/ShareAdmissionRepository.java`, `admission/JooqShareAdmissionRepository.java`, `admission/DefaultShareAdmissionService.java`, `admission/ShareAdmissionTicketProvider.java`, `admission/ShareMediaCredentialProvider.java`, `admission/ShareAdmissionPolicyProvider.java`, `admission/ShareAdmissionCodeVerifier.java`, `admission/DesignatedMobileProofVerifier.java`, `admission/ShareAdmissionSha256Provider.java`, `persistence/ShareAdmissionPersistenceConfiguration.java`, plus `admission/DefaultShareMediaAuthorizationService.java` (11 total). The non-`$` named-interface simple names preserve the existing 7 `ShareAdmissionCapabilityToken`, `ShareAdmissionRequest`, `ShareAdmissionView`, `ShareAdmissionException`, `TrustedShareAdmissionCaller`, `ShareAdmissionService`, `ShareAdmissionJsonBinder`, plus `MediaAuthorizationRequest` and `ShareMediaAuthorizationService` (9 total). The nested named-interface names preserve the existing 11 `ShareAdmissionRequest$Verification`, `ShareAdmissionRequest$Verification$None`, `ShareAdmissionRequest$Verification$ExtractionCode`, `ShareAdmissionRequest$Verification$DesignatedMobile`, `ShareAdmissionRequest$Verification$OrganizationMember`, `ShareAdmissionView$MediaAuthorization`, `ShareAdmissionException$Code`, `ShareAdmissionService$AdmissionOutcome`, `ShareAdmissionService$AdmissionCommitted`, `ShareAdmissionService$ReplayCommitted`, `ShareAdmissionService$DeniedCommitted`, plus `MediaAuthorizationRequest$Purpose` and `ShareMediaAuthorizationService$CallerResolver`. Provider/result nested types live in `admission` and are asserted separately on `ShareMediaCredentialProvider`, not appended to the public named-interface set: `com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider$MediaAuthorizationCredentialRequest`, `com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider$MediaAuthorizationCredentialResult`, `com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider$MediaAuthorizationCredentialResult$Issued`, `com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider$MediaAuthorizationCredentialResult$Unavailable`, and `com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider$MediaAuthorizationCredentialResult$Malformed`. The command path `internal/command/**` is explicitly excluded from this query boundary and remains classified only by its existing command tests; `CustomerShareCommand`, `CustomerShareCommandService`, and command imports are prohibited from the PA18 public surface. The test must prohibit repository/JOOQ/provider/transaction imports from public DTO/controller surfaces, prohibit object-storage/Redis/COS/generic-media imports, retain only named-interface exposure, and assert the exact new CI method name. Classifiers must not be weakened; adding these files to the two exact sets removes them from the command classifier without weakening any exact inventory.

`CustomerApiBoundaryTests` puts the exact five-constructor-parameter generic list/qualifiers in its existing `customerApiSignaturesAndProjectionAreExact` method. It puts the public-controller sharing-import `containsExactly` update in its existing `customerDependenciesAndReadOnlySqlStayInsideBoundary` method. It adds no annotation and does not weaken either assertion.

### 6.2 Failsafe additions

`PublicShareAdmissionIT` adds exactly:

- `mediaAuthorizationUsesExactRouteAndFrozenResponse`
- `invalidTokenOrUnassociatedAdmissionDoesNotDiscloseState`
- `activeGrantAuthorizesPlaybackSeekAndRenewWithoutAdmissionMutation`
- `alreadyAdmittedLockedSessionAuthorizesAndShareLockedIsNotReturned`
- `sameRequestAndPurposeReplaysOneCredentialAndOneEvent`
- `sameKeyPlaybackThenSeekConflictsWithoutSecondCredentialOrEvent`
- `organizationMemberReauthenticatesOriginalAccountTenantAndMembership`
- `capturedAndCurrentBindingAndOpenOrMissingRiskDenyWithoutEvent`
- `revokedExpiredGenerationStaleAndSessionExpiredMapExactly`
- `twoConnectionConcurrentFirstCallsConvergeOnOneCredentialAndOneEvent`
- `providerUnavailableOrMalformedAppendsFailureWithoutSuccessAndRetrySucceeds`
- `responseAndAccessPayloadRedactAllForbiddenMediaFacts`
- `mediaAuthorizationDoesNotChangeAdmissionCountRevisionOrSession`

The direct same-key `PLAYBACK` then `SEEK` test must prove admission-scoped pre-provider conflict, zero second provider call, zero second event, and `SHARE_GENERATION_STALE` 409. `twoConnectionConcurrentFirstCallsConvergeOnOneCredentialAndOneEvent` uses two connections with independently sampled/forced database-time observations and proves both callers return the same provider credential and provider expiry, exactly one event exists, exactly one semantic provider credential is issued, the first durable credential expiry is returned, and no transaction spans provider I/O. The failure test proves exact same-key `FAILURE` → retry-success and success replay during provider outage: no duplicate identical failure, no later failure after success, and outage maps to `DEPENDENCY_UNAVAILABLE` while preserving the first success fact. The two-connection test also proves policy/witness derivation is outside the lock.

`SharingAdmissionRoleLeastPrivilegeIT` adds exactly `v012AclSupportsMediaAuthorizationWithoutNewPrivilege` and proves no V012/schema/column/function/sequence privilege changed.

### 6.3 Authoritative source=CI=XML method inventory

The CI workflow’s exact expected totals are **103 Surefire → 119**, **162 Failsafe → 176**, and **265 combined → 295**. The current CI/source baseline is **265 actual `@Test` methods**. A separate authoritative word-boundary line scan reports **266 → 296** because it includes one comment at `PostgresContainerIT.java:2466`; it is not an actual method total. If both recipes are recorded, CI must label the 265 actual-method recipe and the 266 line-scan recipe with these purposes. The 16 Surefire additions are the 9 service methods, 6 web methods, and 1 `SharingQueryBoundaryTests` method. The 14 Failsafe additions are the 13 `PublicShareAdmissionIT` methods and 1 ACL method. The CI file must list every exact method name above in the matching existing class inventories and compare the XML names to source names; no new class/file or selected-IT shortcut is permitted.

The measured baseline is **56 test Java files** and **75 test class declarations**; neither total changes because no test class/file is added. The full `container-smoke` profile remains exactly nine existing Failsafe classes; no POM edit is authorized. The planned file count is **17**, with **3 new production Java files**, **7 modified production Java files**, **6 modified test files**, and **1 modified workflow**.

## 7. Validation and completion contract

The implementation gate must run all of:

```bash
cd backend
./mvnw -q -DskipTests compile
./mvnw -q -Dtest=ShareAdmissionServiceTests,PublicShareWebTests,SharingQueryBoundaryTests,CustomerApiBoundaryTests test
./mvnw -q -Pcontainer-smoke verify
./mvnw -q test
```

The workflow inventory check must run against source and XML and assert 119 Surefire, 176 Failsafe, 295 combined, the 265 actual-method baseline/total recipe and the separately labeled 266→296 word-boundary line-scan recipe, zero skips/failures/errors, exact method sets, and all nine `container-smoke` classes. The implementation must verify exact route reachability, 200 response field set, 401/403/404/409/410/422/503 precedence, invalid token + unavailable context → 404 with no context lookup, non-member + absent bearer → no context call, member + absent caller → 401, no secret/internal fact leakage, ACTIVE/admitted-LOCKED authorization, same-purpose replay, cross-purpose and changed-fingerprint conflict, one success/failure event, fresh retry after transient failure, success replay during outage, no admission/count/session mutation, two-connection transaction closure and convergence, witness-bound provider output validation (length/NUL/UTF-8/expiry), post-provider conflict/mismatch fail-closed behavior, V012 compatibility, and unchanged PA15/PA16/PA17 behavior.

Read-only plan-fix validation only:

```bash
git status --short
rg --files docs api backend .github/workflows
rg -n 'PA18|Goal Confirmation|Controller plan intake|Plan-review adjudication' docs/gateflow/project-execution-control.md
rg -n 'A01|A02|A03|A04|A05|A06|A07|A08|A09|A10|A11|A12|B1|B2|B3|B4' docs/reviews/plan-review-20261107-110904.md docs/reviews/plan-review-20261107-111633.md
rg -n 'publicMediaAuthorization|MediaAuthorization|issue\(MediaCredentialRequest|Surefire|Failsafe|103|119|162|176|265|295|266|296' docs/gateflow/pa18-sharing-media-authorization-and-playback-plan.md
find backend/src/main/resources/db/migration -maxdepth 1 -type f -name 'V*.sql' -print0 | sort -z | xargs -0 sha256sum
```

The implementation completion report must state task label, runtime/provider, exit status, changed files, exact line/byte counts and SHA-256, one slice, exact 17-path allowlist count, 3-new/7-modified production arithmetic, 6 test files, 1 workflow, `103+162=265 → 119+176=295`, the separately labeled `266→296` line-scan recipe, `SharingQueryBoundaryTests 1→2`, exact 401/404/503 precedence and `SHARE_SESSION_POLICY_UNAVAILABLE`/`DEPENDENCY_UNAVAILABLE`/`SERVICE_UNAVAILABLE` mappings, commands and actual outputs, PostgreSQL/provider evidence only if run, residual classifications, and whether any blocking question remains. It must say gate result only when actually supported; this plan-fix report is **bounded fix complete; Controller intake and fresh A/B re-review next**, not implementation or re-review.

## 8. Residual classifications and scope closure

| Item | Classification | Treatment |
| --- | --- | --- |
| `SHARE_GENERATION_STALE` is used for changed request identity because the operation lists no separate 409 business code. | Residual contract semantic conflation. | Keep the exact listed code; explicitly do not claim current generation staleness for this conflict; do not use `SHARE_TRANSITION_CONFLICT`. |
| Production Q22 TTL/skew/revocation-window/signing-key/Redis/media-provider values are unavailable. | Residual deferred production configuration. | Use explicit unavailable seams and deterministic test settings; fail closed; invent no value. |
| Provider availability/malformed output after a valid decision requires a durable failure fact. | Resolved bounded event behavior. | Append exactly one redacted `FAILURE`/`FAILED` event with the two-code reason allowlist; no success claim; test it. |
| Same-purpose concurrent requests can both reach provider I/O. | Residual provider concurrency risk. | DB append is sole conflict authority; deterministic provider request/event identity and two-connection test are required; no lock spans I/O. |
| Actual media gateway/COS/object resolution/streaming/range delivery is absent. | Deferred non-goal. | Return only the opaque credential contract. |
| Reusing `ShareAdmissionView.MediaAuthorization` avoids duplicate DTO drift. | Resolved minimal design. | No new response record/file or architecture surface. |

There is no blocking question affecting the confirmed goal. The provider must prove deterministic replay for the additive PA18 request/result seam; if doing so would require schema, ACL, OpenAPI, production value, or an edit outside the exact allowlist, stop rather than broaden scope.

This plan has no over-design or goal drift: it implements one frozen public POST behavior, one sharing service path, three additive production Java types/files, existing V009/V012 persistence, one transaction-before-I/O boundary, deterministic event/provider replay, and the minimum exact tests. It adds no generic media framework, playback lease platform, object-key protocol, production Redis/signing implementation, schema object, ACL, OpenAPI change, frontend, streaming behavior, or future-slice scaffolding.
