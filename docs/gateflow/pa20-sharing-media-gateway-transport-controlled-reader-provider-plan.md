# PA20 media-gateway transport and controlled byte-source plan

## 1. Authority, goal and completion signal

- Task: `pa20-sharing-media-gateway-transport-plan-gpt-20261108`; work unit: `PA20-sharing-media-gateway-transport-controlled-reader-provider`; date: 2026-11-08; gate: **plan**.
- Binding scope: the user-confirmed Goal Confirmation in `docs/gateflow/project-execution-control.md:15` (in particular lines 20–30), not a continuation of PA19's narrower transport exclusion. Requested dispatch model/reasoning are task metadata, not an independently attested runtime identity.
- Verified start HEAD: `f3179d771962ee31793f3c6d2396217f67a274fa`, goal-confirmation bookkeeping on top of PR #30 merge `9da37930651d2d0f4ba76a1e8c8d1c15a6ba435e`. Worktree/branch are those in the task. Windows `.git` points at `D:/codex-call/.git/worktrees/codex-call22`; on this Linux runner use read-only Git commands with `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD"`. Do not repair the pointer or change Git configuration.
- Authoritative implementation frozen-source comparison base: **the Controller's accepted-plan commit**, whose source tree descends from merge `9da37930651d2d0f4ba76a1e8c8d1c15a6ba435e`. Record its exact SHA at S1 dispatch as `PA20_IMPLEMENTATION_BASE` and compare all frozen source/migration/public-contract bytes to that commit throughout §§10–12. The earlier `f3179d7` was correct at plan authoring; later Control intake `7079da74dd713cced53a3c24263c33a0028bbcef` is bookkeeping, not plan drift or an alternate implementation base. No accepted-plan SHA exists at this fix gate; do not invent one or implement from either earlier bookkeeping ref.
- Goal/motivation: turn PA19's verified metadata handle into **one deterministic HTTP delivery path**: opaque Bearer → unchanged `MediaPlaybackService` decision → one evidence-owned bounded physical byte session → exact full-object or single-range bytes. The current handle is not itself a stream; treating it as one would silently skip the missing transport/storage boundary.
- Success: real servlet HTTP and PostgreSQL 16 tests prove exact `200`/`206`/`416`, rejection precedence, no physical open/read on rejection, at-most-one physical open, bounded reads, exactly-once cleanup attempts, no PostgreSQL transaction/lock during transfer, unchanged durable business facts, and no secret leakage. Existing 360 methods remain; 40 new methods give **29 Surefire classes / 196 methods + 13 Failsafe classes / 204 methods = 42 classes / 400 methods**, with source=CI method identity, exact POM IT includes and fresh XML equality, zero failures/errors/skips.
- Default production verifier, PA19 controlled-reader authority and new physical byte provider remain unavailable. Deterministic implementations exist **only in test source**. Successful local tests do not mean production acceptance, public deployment readiness or hosted CI success.

The original plan gate created only this plan. The authorized fix task `pa20-sharing-media-gateway-transport-plan-fix1-gpt-20261108` (gate **fix**, requested Codex/GPT `gpt-6.1-sol` / `high`) updates only this plan and creates `docs/gateflow/pa20-sharing-media-gateway-transport-controlled-reader-provider-plan-fix-1.md`, using `apply_patch`. Independent A/B re-review remains mandatory. This does not authorize implementation dispatch, Control/review edits, staging, commit, push, PR creation/mutation, review requests, approval, ready, merge or external comments.

## 2. Scope and alignment

### Included

One internal, logical core-api/edge operation; a separate data-plane contract; strict HTTP envelope validation; one range parser; a small evidence-owned byte-source interface; synchronous fixed-buffer transfer/cleanup; unavailable production fallback; deterministic tests and minimum inventory/docs changes. Sharing owns authorization, evidence owns object/byte authority, the gateway owns HTTP and resource lifecycle. There is no new Modulith module or deployment unit.

### Explicitly excluded/frozen

- No client bucket, key, version, object ID, evidence/share/admission ID, object URL, permanent URL or presigned URL. No such query/path/body parameter exists. The Bearer is the sole object selector.
- No new `SELECT *`, schema/ACL change, migration, runtime business-state write, evidence-record/content projection, admission recount, replay/event write or issuance during delivery. Existing V008 trigger text is frozen, including its historical SQL; this is not permission to copy its `SELECT *` into delivery code.
- V001–V013 remain byte-identical; no V014. Existing PA15–PA19 authorization, issuance, replay, event, count and transaction behavior remain unchanged. PA19's production classes are not modified.
- Existing `api/openapi/onecall-v1.openapi.yaml`, the customer/public operation `authorizePublicShareMedia`, its five-surface inventory, existing DTOs/error vocabulary and public controllers remain unchanged. New gateway errors belong only to the separate gateway contract.
- No multiple/multipart ranges, HLS, transcoding, time-to-byte conversion, resumable protocol/session, cache/revalidation, ETag, CDN, player UI/protocol, download permission expansion, upload, Redis redesign or provider registry. Standard single byte ranges here are not a resume feature; there are no validators, checkpoints or resume guarantees.
- No production COS/KMS/Secret Manager calls, secret/provider value, signing format/key, Q22 TTL/skew/revocation number, public origin/path commitment, TLS/CDN/LB/WAF provisioning or deployment change. Test constants/timeouts/buffer size are implementation/test limits, not production policy.

### Design-document alignment and first-principles evidence

References below are start-line anchors in the current tree; adjacent lines complete the cited behavior.

| Repository fact / direct evidence | Judgment and PA20 consequence |
| --- | --- |
| Control `docs/gateflow/project-execution-control.md:20` requires exact bytes, one source, no transfer transaction and fail-closed production adapters. | Implement one composed behavior, not independent module scaffolding. Internal route choice is authorized; public origin remains deferred. |
| `docs/12-technology-stack-and-system-architecture.md:331` (§9.5) requires current generation/time checks, admitted LOCKED behavior and no permanent raw URL; `:520` (§18) separates API, security and deployment specifications. | Reuse PA19 decisions without recount/reissue; distinguish a local HTTP contract from deployment/player policy. §18 is not an already-defined gateway route. |
| `docs/17-production-security-design-spec.md:297` makes media-gateway a logical core-api/edge point; `:419` (§16), `:427` (P1) require opaque Authorization Bearer, bearer-derived object identity, no storage fields and redaction. | No new deployment/module, object path parameter or customer-session substitution. Do not parse/sign opaque credential claims in the gateway. |
| `api/openapi/onecall-v1.openapi.yaml:3804` is POST credential issuance/rebuild; `:7205` accepts only request ID/purpose; `:7218` returns only credential/expiry. `api/openapi/check-doc-map.mjs:29` freezes five path-derived public/control surfaces. | A binary GET cannot be grafted onto that POST or its public namespace. Add a dedicated gateway OpenAPI document/config, not a sixth surface in the existing checker. |
| `backend/src/main/java/com/onecall/core/sharing/internal/playback/MediaPlaybackService.java:30` rejects ambient transactions; `:39` verifies; `:48` checks state; `:53` resolves evidence; `:57` checks object/duration; `:66` calls the metadata controlled reader and only checks scope equality afterward. | Invoke this service exactly once. Do not duplicate its verifier/state checks or introduce downstream authorization SQL. Preserve its existing no-post-open-dependency-I/O test invariant. |
| `backend/src/main/java/com/onecall/core/sharing/internal/playback/ControlledMediaReader.java:9` returns a `ControlledReadHandle` containing only `ReadScope`; `VerifiedMediaClaim.java:9` defines a **Duration** window. | Keep PA19 open as a metadata authorization gate. Add one physical byte-source API; never pretend Duration is a byte offset. Count metadata opens and physical opens separately. |
| `backend/src/main/java/com/onecall/core/sharing/internal/playback/MediaPlaybackConfiguration.java:18` uses zero→Unavailable, one→explicit, multiple→fixed-safe failure; `:35` keeps reader/verifier unavailable. | Reuse that selection rule for the new byte authority, including rejecting two beans even with `@Primary`. Do not enable a test provider by property or add a production verifier. |
| `backend/src/main/java/com/onecall/core/sharing/internal/playback/SharingPlaybackStateReader.java:26` copies the existing template, READ ONLY / REPEATABLE_READ / REQUIRES_NEW; `:34` completes its snapshot before returning. | Preserve this transaction boundary and grant/frozen-window rules. No enclosing gateway transaction, NOT_SUPPORTED suspension or lock-across-transfer workaround. |
| `backend/src/main/java/com/onecall/core/evidence/internal/mediaread/EvidenceMediaReadPersistenceConfiguration.java:55` uses READ ONLY / READ_COMMITTED / REQUIRES_NEW; `JooqEvidenceMediaObjectReader.java:35` uses an explicit 15-column, parameterized evidence-owned query. | Existing resolver transactions finish before either metadata open or physical open. Byte source uses resolved safe facts, not a second evidence projection or database storage locator. |
| `backend/src/main/java/com/onecall/core/evidence/mediaread/ResolvedMediaObject.java:7` exposes UUID/hash/size/MIME/optional duration, not location. `backend/src/main/resources/db/migration/V008__evidence_media.sql:19` keeps storage/crypto identity in private media rows; V013 `:7`/`:127` grants only 15 safe SELECT columns plus evidence schema USAGE. | Add byte-source types inside existing `evidence::media-read`; no storage ACL workaround, bucket lookup or migration. Test provider owns an in-memory mapping independent of client identity. |
| `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java:28` registers `CustomerBoundaryFilter` on `/*` at HIGHEST_PRECEDENCE. `CustomerBoundaryFilter.java:44` allowlists existing routes; its `BufferedResponse` buffers the whole control-plane body. | A new controller alone is unreachable, and sharing this wrapper would defeat bounded streaming. Register a terminal gateway filter first; move only the customer filter order to HIGHEST_PRECEDENCE+1. Never run gateway delivery through the customer authenticator/buffer. |
| `backend/src/main/java/com/onecall/core/sharing/package-info.java:1`, `backend/src/main/java/com/onecall/core/evidence/mediaread/package-info.java:1`, `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java:163` allow the existing named seam; `SharingQueryBoundaryTests.java:154` classifies every non-query/admission/playback file as command code. | Keep the exact module/dependency map; extend its named-interface assertion for the new evidence type, and add a closed exact gateway file classifier rather than weakening command/playback tests. |
| `backend/src/main/resources/application.yml:5` defaults to non-web; `application-web.yml:5` disables request details and access log, suppresses web/SQL/pool logs. `backend/pom.xml:173` explicitly includes 12 IT classes; `.github/workflows/ci.yml:438` compares exact method sets. | Activate gateway only with existing `web` servlet profile; retain logging/default startup and toolchain constraints. No new profile/property. Update CI inventories before source/XML comparisons, not afterward. |

### History reconciliation

The complete current Control was scanned (4,266 lines; SHA256 `02ffbd68ee91643730d235cd16569a6d815fec36695c5e820318c77399ec3fd7`), including the current goal and retained PA19/PA19A/PA18 adjudications. The PA18 plan/fixes/implementation/final closeout establish unchanged issuance/replay and post-transaction provider I/O. PA19's plan/fix, implementation report and final closeout, and the merged PR #30 history supply these binding lessons:

- Plan reviews `docs/reviews/plan-review-20261108-131036.md`, `plan-review-20261108-132655.md`, `plan-review-20261108-135532.md`, `plan-review-20261108-140514.md`: A-01 monotonic grant extension must retain frozen caps; A-02 internal scope retention is not permission to serialize it; A-03 inconsistent `Allowed` is Unavailable; B-01 future cross-module provider placement requires approved adapter/named-interface review. PA20 neither reopens these nor uses B-01 to introduce a new edge.
- Code reviews `docs/reviews/code-review-20261108-162007.md`, `code-review-20261108-164928.md`, aggregate `code-review-20261108-170530.md`, `code-review-20261108-173049.md`, and PR reviews `docs/reviews/pr-30-review-20261108-175806.md`, `pr-30-review-20261108-185236.md` close current-scope findings; production signing, storage, transport/deployment and operational redaction remain classified residuals. PA20 closes only the now-authorized deterministic local transport portion.
- `docs/gateflow/pa19-sharing-media-gateway-playback-foundation-final-closeout.md:39` records PostgreSQL 16.15 and 25/162 + 12/198 = 37/360. This plan independently measured source/CI/POM below; it does **not** reuse PA19's runtime/hosted green as PA20 evidence.
- Existing `MediaPlaybackServiceTests` (20), `SharingPlaybackStateReaderTests` (8), `MediaPlaybackConfigurationTests` (4), `MediaPlaybackIT` (6), evidence reader unit/configuration tests (4+4), evidence IT/ACL suites (8+8), and their sources remain the regression authority. The existing reader tests pin Missing vs Unavailable, ambiguity, exact columns, sanitized failures and role separation. No replacement with mocks-only authorization proof is allowed.

## 3. Exact HTTP/data-plane contract

### 3.1 Route, method, request and ordering

**Pin `GET /internal/media-gateway/v1/playback`**, operationId `readAuthorizedMedia`, no parameters identifying a resource. This is the internal logical route, not a production public gateway URL. It is served synchronously by `MediaGatewayFilter`, not an MVC controller/ResponseEntity or asynchronous `StreamingResponseBody`.

`MediaGatewayConfiguration` is `@Profile("web")` + servlet-conditional; its `FilterRegistrationBean` is REQUEST-dispatch only, `/*`, order HIGHEST_PRECEDENCE. `CustomerWebConfiguration` changes only its existing registration order to HIGHEST_PRECEDENCE+1. Pin one raw, case-sensitive terminal namespace predicate: let `uri = request.getRequestURI()` and `context = request.getContextPath()`; remove `context` from the beginning of `uri` only when context is nonempty and uri equals context or starts with `context + "/"`; otherwise retain uri unchanged as `rawPath`. Then **terminal iff `rawPath != null && rawPath.startsWith("/internal/media-gateway")`**. This deliberately captures `media-gatewayx` but not a changed/encoded namespace prefix. No percent decoding, case folding, slash collapse, semicolon stripping, redirect or forward. Outside this predicate call `chain.doFilter` exactly once without touching request/response headers, attributes, authentication or body. Inside it never invoke the chain; canonical route requires `rawPath` and `servletPath` both exactly `/internal/media-gateway/v1/playback`, non-null context, `uri.equals(context + servletPath)` and null `pathInfo`; otherwise terminal 404 before method/authentication. Thus inconsistent path fields on a terminal request cannot authorize bytes.

The following is the shared normative implementation/test table (GET, empty context, matching servletPath and null pathInfo unless stated). Tests enumerate these literal cases independently, not by calling the implementation predicate. “Pass” means gateway chain-once/untouched, **not** a promised final application status; the customer filter may later return its own 404.

| Raw context-relative path / path-field case | Gateway behavior |
| --- | --- |
| `/internal/media-gateway/v1/playback` | Canonical; proceed to envelope/auth checks |
| `/internal/media-gateway`, `/internal/media-gateway/`, `/internal/media-gatewayx`, `/internal/media-gatewayx/v1/playback` | Terminal 404 |
| `/internal/media-gateway/v1/playback/`, `/internal/media-gateway/v1/playback/extra`, `/internal/media-gateway//v1/playback` | Terminal 404 |
| `/internal/media-gateway/V1/playback`, `/internal/media-gateway/v1/Playback` | Terminal 404 |
| `/internal/media-gateway/v1/%70layback`, `/internal/media-gateway%2Fv1/playback`, `/internal/media-gateway/v1/%2570layback` | Terminal 404; no decode, including double-encoded suffix |
| `/internal/media-gateway;v=1/v1/playback`, `/internal/media-gateway/v1/playback;v=1` | Terminal 404; no semicolon stripping |
| `/internal//media-gateway/v1/playback`, `//internal/media-gateway/v1/playback`, `/Internal/media-gateway/v1/playback`, `/internal/Media-gateway/v1/playback` | Pass; namespace prefix does not match |
| `/internal/%6dedia-gateway/v1/playback`, `/internal%2Fmedia-gateway/v1/playback`, `/internal/medi%61-gateway/v1/playback`, `/internal;v=1/media-gateway/v1/playback` | Pass; encoded/semicolon namespace prefix does not match |
| `/internal/other`, `/api/v1/customer/me`, `/api/v1/public/shares/probe` | Pass |
| Raw URI `/app/internal/media-gateway/v1/playback`, context `/app`, servletPath `/internal/media-gateway/v1/playback`, null pathInfo | Canonical after exact context removal; proceed |
| Raw URI `/internal/media-gateway/v1/playback`, context `/other` or servletPath `/other` or non-null pathInfo | Terminal 404; inconsistent path fields |

Raw path aliases that Tomcat rejects or normalizes before filter entry are container limitations; the isolated direct-filter test pins the table without customer-filter masking. Real HTTP proves the canonical route and actual registration order, not that the container preserves every malformed raw target.

Processing precedence is fixed:

1. Namespace/route: noncanonical gateway routes → 404. Exact route with method other than GET → 405, `Allow: GET`. HEAD has 405 headers but **no body**, and does not trigger Servlet/MVC implicit HEAD support. OPTIONS/CORS preflight is not enabled.
2. Request shape: any non-null query string (including empty `?` where preserved by the container), nonzero/unknown body length with body framing, any Transfer-Encoding, conflicting/repeated Content-Length, or conditional headers (`If-Range`, `If-Match`, `If-None-Match`, `If-Modified-Since`, `If-Unmodified-Since`) → 400. No body or Content-Length absent/one `0` is allowed. Content-Type alone grants nothing and is ignored. No request body is read. Container-rejected illegal framing is a pre-servlet limitation, not an application envelope guarantee.
3. Authorization: exactly one header value, case-insensitive `Bearer` scheme followed by **one ASCII space**, then 1–2048 ASCII characters matching `[A-Za-z0-9._~+/-]+={0,}`. No trimming, splitting, decoding, normalization, query/cookie fallback, customer principal or forwarded identity. Missing/duplicate/comma-folded/wrong-scheme/blank/whitespace/control/non-ASCII/overlong value → 401 before verifier. `Bearer` with different scheme casing is allowed; credential bytes/case are preserved exactly into `new OpaquePlaybackCredential(value)`.
4. Range grammar parsing, without size/object access, as §4. Malformed or unsupported range → 400 before authorization service. `Accept` is deliberately not negotiated; authorized media or the fixed problem format is returned. Accept-Encoding never changes bytes; no Content-Encoding/compression is applied.
5. Ambient transaction/resource guard: if `TransactionSynchronizationManager.isActualTransactionActive()`, synchronization is active, or its resource map is nonempty → 503 without service/source access. Do not suspend an upstream transaction; that could leave its locks held during delivery.
6. Call `MediaPlaybackService.authorizeAndOpen(credential, expectedPurpose)` exactly once; do not invoke verifier/state/evidence independently. Absent Range selects `Purpose.PLAYBACK`; any syntactically valid Range selects `Purpose.SEEK`, **not** whichever purpose the verifier returns. A PLAYBACK credential cannot be used with Range, nor SEEK without Range. RENEW remains PA18's control-plane reissuance purpose and is not consumable by this byte operation. No purpose header/query/body override.
7. On `Opened`, inspect only the returned immutable scope; enforce the full-temporal-window guard below and safe MIME. Normalize against its `media.sizeBytes()`. Denial/Unavailable/invalid interval → no physical open or read.
8. Reassert no active transaction/synchronization/bound resources; then exactly one `ControlledMediaByteSource.open(OpenRequest)`; validate the returned session request equals the requested media/interval before a read. Copy as §5. No authorization refresh, clock/dependency call, SQL, replay/event write or evidence projection after PA19's metadata open.

**Temporal-to-byte safety decision:** PA19 windows are time intervals; the repository has no authenticated codec/sample index or time→byte map. PA20 serves original object bytes only when `window.startInclusive == Duration.ZERO` and `window.endExclusive == Duration.ofSeconds(scope.media.durationSeconds)` (duration must be positive/non-null, already checked by PA19). A narrower window returns the same 403 as any authorization denial, before physical source open, whether a syntactically valid Range would be satisfiable or unsatisfiable. Malformed Range/query/body/bearer never reaches PA19 and retains the earlier 400/401 from steps 2–4; do not inspect a claim to override an envelope error. With valid envelope/bearer and syntactically valid Range, a valid partial-window SEEK claim reaches successful PA19 metadata authorization/open, then the gateway returns the single non-disclosing 403 **before range resolution/416 or physical open**. Do not proportionally scale duration into bytes, clip a time window, assume CBR, or treat all bytes as authorized for a partial window. This is a bounded data-plane capability, not a change to PA19's acceptance of partial windows. Positive byte-delivery test claims use full windows; partial-window denial probes are explicit exceptions. Supporting partial temporal windows needs a separately authorized format-aware contract, not a PA20 TODO or blocking question.

HEAD=405 with empty body (would-be problem Content-Length retained) and conditional headers=400 are confirmed **plan-gate-owned envelope decisions** under Control's Goal Confirmation, not unresolved protocol-policy questions. The existing safe audio MIME/full-window restriction remains this foundation's intentional fail-closed scope; no arbitrary-media/format support is added.

### 3.2 Status, headers, safe error envelope

Common application-controlled headers on every gateway response: `Cache-Control: no-store`, `Pragma: no-cache`, `X-Content-Type-Options: nosniff`. Never set `Set-Cookie`, `Location`, `Content-Disposition`, ETag, Last-Modified, Vary, Authorization, storage/hash/ID headers or CORS headers. Container-managed Date/connection framing is not part of exact application header equality.

| Outcome | Status / response contract | Physical source |
| --- | --- | --- |
| Authorized full GET, no Range | 200; `Content-Type` exactly safe evidence MIME; `Content-Length: N`; `Accept-Ranges: bytes`; **no Content-Range**; body exactly object `[0,N)` | One open; bounded reads; close once |
| Authorized satisfiable single Range | 206 even if normalized range covers entire object; same MIME; `Content-Length: endExclusive-start`; `Accept-Ranges: bytes`; `Content-Range: bytes start-(endExclusive-1)/N`; exact interval bytes only | One open; bounded reads; close once |
| Authorized full-temporal-window, syntactically valid but unsatisfiable range | 416 `MEDIA_RANGE_UNSATISFIABLE`; `Accept-Ranges: bytes`; `Content-Range: bytes */N`; problem body length (not object size) | Zero physical opens/reads |
| Invalid request shape, malformed/overflow/reversed/multiple/unsupported range | 400 `MEDIA_REQUEST_INVALID`; no Content-Range or Accept-Ranges; precedes PA19 even for an otherwise valid partial-window claim (steps 2–4 still order bearer presentation before Range) | Zero service/physical calls |
| Invalid/missing HTTP bearer presentation | 401 `UNAUTHENTICATED`; `WWW-Authenticate: Bearer`, no parameters | Zero; verifier not called |
| Any PA19 `Denied` (invalid, expired, wrong audience/purpose, cross-object/admission, stale, revoked, missing evidence, current state, etc.), or unsupported partial time window after PA19 `Opened` | 403 `MEDIA_FORBIDDEN`; indistinguishable title/body shape; no Content-Range, size/MIME facts or PA19 reason; partial window wins over satisfiable/unsatisfiable **syntactically valid** Range, never over malformed envelope/bearer | Zero physical opens/reads; retain existing PA19 early-stop behavior; valid partial-window claim completes one PA19 metadata open before gateway denial |
| PA19 Unavailable, ambient-resource guard, missing physical provider, physical `Unavailable`, or null open result | 503 `MEDIA_UNAVAILABLE`; no dependency detail or Retry-After guess | Zero reads; open may have been attempted once only for physical provider failure |
| Invalid MIME, session-request mismatch, impossible provider counts, premature EOF/zero-progress, provider open/read/close exception, servlet write/flush failure before commitment, unexpected gateway failure | 500 `INTERNAL`; reset uncommitted buffer and stale media/range headers, then safe problem | Abort/close owned session as §5; no retry |
| Wrong gateway route / wrong method | 404 `NOT_FOUND` / 405 `METHOD_NOT_ALLOWED`; Allow only on 405 | Zero service/provider calls |
| Failure after response commitment | Keep already-committed status/headers; terminate servlet processing/connection rather than send more application data. A partially sent body is truncated; if all bytes were already sent, a cleanup failure cannot retroactively invalidate them. **No second status, JSON, multipart boundary, error text or remaining media** is appended. | Abort once, close once (attempts); throw fresh `IOException("Media transfer failed")` without original cause to terminate servlet processing |

`MediaGatewayProblemWriter` emits UTF-8 JSON with `Content-Type: application/problem+json` and exact UTF-8 `Content-Length`. Fixed fields, no extras: `{ "type": "about:blank", "title": <fixed>, "status": <integer>, "code": <fixed>, "traceId": <server UUID> }`. Titles in table order: `Range not satisfiable`, `Invalid media request`, `Authentication required`, `Media access denied`, `Media unavailable`, `Internal error`, `Not found`, `Method not allowed`. Generate trace UUID locally once per handled request; never trust or echo inbound trace/request IDs. Do not include `detail`, instance/path/query, message, exception, stack, timestamp, PA19 reason, credential, identifiers, size (except authorized 416 header) or dependency names. `WWW-Authenticate` exists only for 401. HEAD suppresses serialized error body while preserving its would-be Content-Length. Status/code/title mapping is an internal gateway vocabulary, not an edit to existing public errors.

Safe MIME is an exact ASCII lower-case `audio/` type followed by one or more token characters, excluding wildcard `*`; no parameters, whitespace, CR/LF or non-ASCII. Pin the complete regex (backtick is a literal token character):

```text
^audio/[a-z0-9!#$%&'+.^_`|~-]+$
```

Do not normalize/reflect a malformed value, sniff content or silently substitute a type; unsafe metadata is 500 before physical open. Positive fixtures use `audio/wav`. This restricted foundation is an audio transport, not arbitrary active-content hosting.

## 4. Interval and parser contract

`SingleByteRange` lives under `sharing.internal.gateway`; it does not access storage, credentials or time. Implement a sealed parsed specification (`Full`, `Closed(first,last)`, `OpenEnded(first)`, `Suffix(length)`), `parse(List<String> rangeHeaderValues)` and `resolve(Spec,long objectSize)` with sealed `ParseResult` (`Accepted(spec)`, `Malformed`) and `Resolution` (`Selected(ByteInterval interval, boolean partial)`, `Unsatisfiable`). Only these parser-owned specification/result types remain nested in this file. **Exactly one interval owner exists:** `com.onecall.core.evidence.mediaread.ControlledMediaByteSource.ByteInterval` (§5.1). `SingleByteRange` imports that exact nested evidence type (`import com.onecall.core.evidence.mediaread.ControlledMediaByteSource.ByteInterval;`); `Resolution.Selected.interval()` and `OpenRequest.interval()` use the identical type, passed directly with no conversion/duplicate `SingleByteRange.ByteInterval`. Dependency is sharing → evidence::media-read only; evidence must not import sharing/parser types. No exception message embeds input; no regex converts to floating point/int.

| Input | Parse / resolution |
| --- | --- |
| No Range header | Full → `[0,N)`, partial=false |
| Exactly `bytes=a-b` | ASCII decimal longs, `a<=b`; if `a>=N`, 416; otherwise `[a,min(b,N-1)+1)`, partial=true |
| Exactly `bytes=a-` | if `a>=N`, 416; otherwise `[a,N)`, partial=true |
| Exactly `bytes=-s` | `s=0` → 416; otherwise `[N-min(s,N),N)`, partial=true |
| Repeated Range headers or comma anywhere | 400; no multiple/multipart negotiation or fallback full read |
| Other unit/case (`items`, `Bytes`), whitespace, empty, signs, Unicode digits, missing both bounds, internal CR/LF, extra hyphen/equal, reversed bounds, decimal > Long.MAX_VALUE | 400; no overflow, wrapping, clipping malformed input or provider call |

Range value length is capped at **128 ASCII characters** before parsing (400 above it); leading decimal zeroes are accepted within that cap. Use `Long.parseLong` on already-validated ASCII digits, catching overflow as Malformed. N is a trusted positive long from authorized metadata; invalid N is internal failure, not 416. Closed end normalization performs `min(last,N-1)` **before** adding 1, so N=Long.MAX_VALUE is safe. Suffix uses subtraction after `min`, never `N+suffix`. Resolve never performs `last+1` on an unbounded unnormalized endpoint. `ByteInterval` is half-open: `0 <= startInclusive < endExclusive <= N`; length is `endExclusive-startInclusive`. Byte intervals and Duration windows are distinct types.

Pin deterministic examples for N=16 and payload ASCII `0123456789ABCDEF`: no header → 200/full 16; `bytes=2-5` → 206/`2345`/4/`bytes 2-5/16`; `bytes=14-99` → `EF`; `bytes=12-` → `CDEF`; `bytes=-4` → `CDEF`; `bytes=-99` → all 16 but 206; `bytes=16-`, `bytes=99-100`, `bytes=-0` → 416/`bytes */16`; `bytes=5-2`, overflow or `bytes=0-1,4-5` → 400. Test N=1, N=Long.MAX_VALUE and zero-leading decimals without allocating an object-sized array.

## 5. Evidence-owned byte API and lifecycle

### 5.1 Exact API (one new production interface file)

Add `backend/src/main/java/com/onecall/core/evidence/mediaread/ControlledMediaByteSource.java` inside existing `evidence::media-read`. Its nested `ByteInterval` is the sole production interval definition and is shared unchanged by parser selection and provider requests; neither a gateway-owned copy nor an evidence→sharing dependency is permitted:

```java
public interface ControlledMediaByteSource {
    Result open(OpenRequest request) throws IOException;

    record ByteInterval(long startInclusive, long endExclusive) {
        public long length();
    }
    record OpenRequest(ResolvedMediaObject media, ByteInterval interval) { }

    sealed interface Result permits Result.Opened, Result.Unavailable {
        record Opened(ReadSession session) implements Result { }
        record Unavailable() implements Result { }
    }
    interface ReadSession extends AutoCloseable {
        OpenRequest request();
        int read(byte[] destination, int offset, int length) throws IOException;
        void abort() throws IOException;
        @Override void close() throws IOException;
        @JsonValue default String redacted() { return "MediaReadSession[REDACTED]"; }
    }
}
```

The displayed method bodies/record bodies are signature specifications, not literal compilable implementation stubs. Constructors reject null media/interval/session, negative/empty/reversed interval and interval end > media size, with fixed safe IllegalArgumentException messages and no cause/input. `length()` uses safe subtraction under the constructor invariant. `Result` is sealed; all owned request/interval/opened values have constant redacted `toString` and `@JsonValue` forms; the session interface defines an annotated default redacted JSON value and implementations must provide a constant redacted `toString`. Never serialize a session to HTTP. No public/provider field contains location, URL, crypto reference, credential or a client selector.

- `open` does not read bytes. It owns partial acquisition and must release it internally on Unavailable/exception before returning; a throwing open cannot transfer hidden cleanup responsibility to a caller with no handle. Non-null Opened transfers exclusive ownership of exactly one session to the gateway. No retry/fallback/reopen, registry or second object. Null result is 503; invalid constructed/returned session/request is internal failure, never success.
- `OpenRequest` contains **the service-returned ResolvedMediaObject**, unchanged, and one validated interval. Provider must independently bind opaque UUID + SHA256 + total size to its immutable local fixture object; wrong UUID/hash/size returns Unavailable without bytes. No using consumer-time lookup to manufacture the supposedly verified claim's identity.
- The session reads from interval start, sequentially, only within that interval. `read` returns 1..requested count or -1 at its interval EOF; valid buffer offset/count rules apply. It never skips/reads other object bytes to reach start; local provider slices/seeks before exposing reads. No `InputStream`/channel escapes to bypass bounds. EOF after interval completion is a provider contract tested directly, not probed by the HTTP copier.
- `abort` cancels pending source work, drops/zeros owned transient buffers where applicable and disables further reads. `close` releases all acquired resources even if it reports an exception. Both are idempotent, abort after close is a no-op, read after terminal state throws a fixed-safe IOException. Abort does not replace the obligation to attempt close. Gateway calls each at most once as below. Java byte-array zeroing cannot promise removal of network/client/JVM copies.
- Default provider is a local fallback lambda returning Unavailable, selected with `ObjectProvider.stream().limit(2)`: zero → fallback, one → that exact bean, two → `IllegalStateException("Ambiguous media byte authority")` with no provider detail, even with `@Primary`. Do not register an in-memory provider in production. No conditional bean-order/override trick. Existing PA19 unavailable verifier/metadata-reader fallbacks remain untouched.

### 5.2 State machine and ownership

`UNTOUCHED → REQUEST_VALIDATED → AUTHORIZING → AUTHORIZED_METADATA → INTERVAL_SELECTED → PHYSICAL_OPEN → TRANSFERRING → CLOSED_SUCCESS`; rejection is terminal before PHYSICAL_OPEN. No physical source is owned before an Opened result. PA19's controlled `reader.open(scope)` happens at most once **inside AUTHORIZING** and remains metadata-only; new physical `source.open(request)` happens at most once **after authorization returns**. There are not two physical/storage opens.

1. After successful service response, use in-memory scope/window/MIME/interval checks only. Reject partial time windows and unsatisfiable ranges before physical acquisition. Do not modify PA19's handle or use it as a public DTO.
2. Guard transaction/synchronization/resources again. Call physical open once. Immediately record ownership of returned session **before** invoking `session.request()` or comparing its immutable request; null/throwing/mismatched descriptor must not leak an acquired session.
3. Validate descriptor equality, then set success status/headers and copy with one **8192-byte** buffer and a `long remaining`. Each call requests `(int) min(8192,remaining)`; validate result >0 and <= requested before writing exactly that count. Decrement remaining by actual count. A zero return, early -1 or impossible count is a failure, not a spin/retry. Never call `available`, `readAllBytes`, `transferTo`, whole-body buffering, unbounded skip, an extra EOF probe or a second session. Long lengths must not be narrowed to int for Content-Length/accounting.
4. When remaining reaches zero, attempt session.close **once**, then flush the response. Do not close the container-owned ServletOutputStream. Clear/zero the copy buffer in finally. No explicit flush per chunk is necessary; container commitment may occur naturally. If close fails, its invocation is already counted; abort once and do not call close again.
5. Any failure after ownership: abort once, then close once if not already attempted, preserving fixed-safe error precedence even if abort/close also throw. Cleanup flags prevent a cleanup exception from reentering cleanup. Never log/propagate original or suppressed dependency exceptions. Null result/Unavailable/open exception have no transferred session; provider handles partial acquisition itself.
6. If response is not committed, reset response/buffer, reapply safe common headers and write the mapped problem; stale media Content-Type/Length, Content-Range, Accept-Ranges, Allow and WWW-Authenticate must not survive except where the selected error explicitly requires them. If committed, never reset/rewrite/append; throw only a new cause-free fixed IOException after cleanup. A disconnect or post-commit failure cannot truthfully become a new 500 response. The client must treat a short Content-Length/terminated connection before completion as failure; already-received bytes cannot be recalled, and a late close failure after the complete body was transmitted cannot be made observable as a new HTTP error. Ordinary IOException/RuntimeException paths are sanitized; a finally block owns cleanup even during exceptional unwinding, without claiming recovery from JVM/process termination.
7. If final response flush fails after a successful close, invoke abort once (specified no-op after close), do not close again, and follow commitment handling. On normal completion: exactly one physical open, finite bounded reads, zero abort calls and one close. On read/write/misbind failure: one open, one abort attempt and one close attempt. On 416/denial/malformed input: zero physical opens/reads/abort/close.

No asynchronous executor, thread-local credential, HTTP session, retry, object buffering cache or post-open clock check is introduced. PA19's snapshot/expiry decision is not a promise of continuous authorization throughout a long transfer; expiry/revocation races and already-buffered bytes remain the existing Q22 residual.

## 6. Transactions, non-disclosure and production fail-closed wiring

- No `@Transactional`, transaction template, JDBC/DataSource/jOOQ, SQL string or locking primitive in gateway/source-transfer production code. Do not use NOT_SUPPORTED/REQUIRES_NEW around the transfer to hide a still-held ambient lock. Reject ambient bindings before authorization and physical open.
- Unchanged PA19 state transaction: read-only REPEATABLE_READ REQUIRES_NEW on the qualified admission resources. Unchanged evidence transaction: enforced read-only READ_COMMITTED REQUIRES_NEW on its dedicated evidence reader resources. Each completes before the service's metadata reader opens; all authorization/evidence connections are returned before physical source open/read/write/abort/close.
- New deterministic provider reads only test-owned local memory; it does no SQL, even during open. No storage-location read is added to V013. A future COS adapter requires separate private location/identity approval, not use of evidence/admin/command credentials here.
- No extra audit/access event, count/revision increment, admission, issuance or replay mutation during gateway success/failure. Integration snapshots cover grant, admission and access-event facts before/after every gateway case. Fixture setup/lifecycle mutations occur outside measured delivery and use existing test-admin mechanisms only.
- No credential/provider data in logs, problem JSON, redirects, exception text/cause/suppressed list, metrics labels or trace IDs. Legitimate successful media **must** be present in the successful response body; “no bytes in response/log” means no media bytes in an error body, metadata, log or diagnostic, not removal of the authorized success payload. Partial bytes already committed on failure are not described as redacted/recalled.
- Retain application-web request-details=false, accesslog=false and existing logging levels. Gateway catches provider/runtime failures without original logging; original exception objects never cross the filter boundary. Test captured output and raw response headers/problems against canaries for credential, UUID/hash, `cos://bucket/key?version=...`, URL, crypto reference and distinctive media bytes. Positive body-byte assertions remain separate from redaction assertions.
- Boot without `web` stays database-free/non-servlet; `web` without adapters returns 401 for missing bearer and 503 for syntactically valid bearer, no database checkout/physical open. Supplying only a physical provider cannot bypass the unavailable verifier/PA19 reader. A test verifier with no physical provider still returns 503 after authorization, zero bytes. Duplicate physical provider beans fail closed at configuration.

## 7. One composed implementation slice

**Slice S1 — independently behaviorally verifiable:** accepted opaque bearer → unchanged real PA19 authorization → one bounded byte session → exact HTTP bytes, with negative/lifecycle/transaction proof. This is the only slice; its success signal does not depend on a later slice. One is minimal because parser/API/filter-only deliveries cannot prove the stated trust/lifecycle chain and a split by module/file would add no independently safe end-user outcome.

Ordered work *within S1* (not separate slices):

1. Add the standalone gateway contract, closed evidence byte API and pure interval parser; implement deterministic test source with operation counters/failure injection.
2. Add terminal gateway filter/problem writer/configuration; change only customer filter ordering; preserve PA19 production code, profiles, module map and public contract. Implement full-window guard and exact transfer/cleanup state machine.
3. Add the exact tests in §8; reuse PA19 container fixture through test-only nested-type visibility changes specified in §9, not reflection or production fixture beans. Extend closed architecture classifiers/interfaces without weakening assertions.
4. Add one IT include and synchronized exact CI inventories **before** every class/method/XML comparison. Add separate gateway OpenAPI lint to existing npm check; update authoritative backend README inventory and write implementation evidence report.
5. Run focused checks, fresh full Docker/PG16 verification, embedded CI inventory and package/web/firewall/contract checks. Stop for Controller intake with only allowlisted candidate changes; do not advance gates or claim hosted checks ran.

## 8. Exact new test inventory and expected assertions

All additions are ordinary unconditional `@Test void methodName()` methods. No parameterized/repeated/dynamic/disabled/conditional tests, assumptions, Docker skips, replaced existing methods or renamed baseline methods. Loops/tables live inside the named methods; do not introduce extra annotated methods. Test support files have zero annotated tests. Unit web tests directly exercise the filter with Spring mock servlet objects and the real `MediaPlaybackService` plus deterministic collaborators. Container IT uses actual PG16/repository/evidence implementations throughout: real HTTP scenarios use embedded Tomcat/Java HttpClient; §8.6 method 5 deliberately invokes the real filter directly with a deterministic throwing servlet response and that same real PG-backed service. It is not a mock-service substitute or a claim of real-socket write-failure injection. §8.2 method 2 also uses embedded Tomcat/HTTP despite being a Surefire method.

### 8.1 `com.onecall.archtests.MediaGatewayWebTests` — 18 new Surefire methods

| Exact method | Required assertions |
| --- | --- |
| `fullGetReturnsExactAuthorizedBytesAndHeaders` | Grammar-valid positive bearer as §8.6, N=16 full-window PLAYBACK claim; exact 200/16-byte payload/MIME/Accept-Ranges, no Content-Range; verifier→state→evidence→metadata-open→physical-open→reads→close; each open once; no abort. |
| `singleRangeGetReturnsExactIntervalAnd206Headers` | Grammar-valid positive bearer as §8.6; full-window SEEK claims, closed/open/suffix/clipped/full-covering range examples from §4; exact body/length/Content-Range and no out-of-interval read. |
| `bearerIsPropagatedOpaqueWithoutCustomerIdentity` | Scheme casing allowed, exact credential bytes passed unchanged, token not decoded/trimmed; customer principal/cookies/forwarded headers grant nothing; no customer authenticator use. |
| `gatewayRouteMethodQueryAndBodyAreClosed` | Enumerate every §3.1 literal path/path-field row against an isolated chain spy: terminal aliases→404/chain zero, outside lookalikes→chain once/request+response untouched; no customer filter to mask routing. Canonical POST/PUT/HEAD/OPTIONS→405 Allow GET (HEAD empty), query/body/framing/conditional headers→400; zero service/verifier/physical calls. |
| `malformedAndDuplicateBearerStopBeforeVerification` | Absent, blank, wrong scheme, duplicate/comma, tabs/spaces/control/non-ASCII, >2048 and invalid token character → exact 401 + bare Bearer challenge; zero service collaborators. |
| `malformedMultipleAndOverflowRangesStopBeforeAuthorization` | Entire §4 grammar/128-character/reversed/overflow rejection table → 400; zero verifier, state, evidence or either open. |
| `unsatisfiableRangeDisclosesSizeOnlyAfterAuthorization` | Valid SEEK + start>=N/-0 →416/problem/bytes */N, zero physical open; same ranges with denied/unavailable credential yield403/503 without size/Accept-Ranges/Content-Range. |
| `rangePresenceSelectsExpectedPurposeWithoutClaimFallback` | No range→PLAYBACK, range→SEEK; wrong-purpose and all RENEW claims denied, no physical open; purpose header/query cannot change decision. |
| `allPlaybackDenialsHaveOneNonDisclosingEnvelope` | Real service receives invalid/expired/not-yet-valid/audience/purpose/cross-object/wrong-admission/stale/revoked/missing-evidence fixtures; one 403 code/title/schema and zero physical reads; exact early-stop counters. |
| `unavailableDependenciesAndDefaultByteProviderFailClosed` | Null/Unavailable/throwing verifier/state/evidence/metadata reader handled by unchanged service→503; missing/Unavailable/null physical result→503; no read or second attempt, no dependency text. |
| `partialTemporalWindowAndUnsafeMimeNeverOpenBytes` | With otherwise valid partial-window claims: malformed Range/query/body→400 and malformed bearer→401 before PA19 (combined faults follow §3.1); syntactically valid SEEK Range (satisfiable and unsatisfiable)→one successful PA19 metadata open then identical403/no size/zero physical open. Unsafe MIME/header injection/parameters/active content→500; safe full window/audio MIME success; never derive proportional byte offsets. |
| `misboundSessionAbortsAndClosesBeforeAnyRead` | Provider echoes wrong object/hash/size/interval or throws from descriptor; 500, zero session reads, exactly one abort/close, no resource leak. |
| `shortZeroAndImpossibleReadsFailWithoutExtraRead` | Premature EOF, zero progress, count>requested, open/read exceptions before commit; reset buffered prefix, exact500 safe body, abort/close once; no EOF probe or read beyond remaining on success. |
| `disconnectAndCommittedFailuresNeverAppendProblems` | Servlet output throws after deterministic commitment; original success headers retained, no JSON/error suffix, abort/close once, fresh cause-free sanitized IOException; no retry. |
| `abortCloseAndFlushFailuresPreserveCleanupCounts` | Read+abort+close all throw; success close throws; final flush throws after close; prescribed calls once each, no suppressed canaries, no recursive cleanup or close retry. |
| `ambientTransactionsAndBoundResourcesPreventTransfer` | Active transaction, active synchronization alone, and bound-resource-only permutations→503; no auth/provider call; guard recheck catches collaborator leak before physical open; finally restores test thread state. |
| `responsesAndCapturedLogsNeverLeakPrivateCanaries` | Exact problem field set/titles/types/UUIDs, fresh local trace IDs, inbound trace ignored, common headers/no cookies/URLs/IDs/bytes on errors; safe value JSON/toString and captured logs exclude canaries; successful bytes tested only as permitted payload. |
| `largeObjectTransferUsesFixedBufferAndLongAccounting` | Synthetic size>Integer.MAX_VALUE; range above int boundary serves small exact bytes with long Content-Range; full transfer disconnected on first buffer asserts Content-Length long, every read<=8192, one buffer/no object allocation, abort/close, no timeout/spin. |

### 8.2 `com.onecall.archtests.MediaGatewayConfigurationTests` — 4 new Surefire methods

1. `nonWebProfilesDoNotRegisterGatewayOrActivateProviders` — default/persistence non-web contexts register no gateway filter; no new DataSource/provider activation; existing unavailable verifier/reader behavior unchanged.
2. `webGatewayPrecedesCustomerBoundaryAndDefaultBearerIsUnavailable` — use the existing `CustomerWebConfigurationTests` pattern: `new SpringApplicationBuilder(OneCallApplication.class).profiles("web").properties("server.port=0", "spring.main.banner-mode=off")`, obtain the actual port from `ServletWebServerApplicationContext`, send loopback requests with Java `HttpClient`, close client/context in try/finally or try-with-resources. Inspect actual `FilterRegistrationBean`s for gateway HIGHEST, customer HIGHEST+1, REQUEST/`/*`; HTTP proves default missing bearer401 and grammar-valid `Bearer pa20-positive-playback-token`503 rather than the customer filter's catch-all404. Existing customer/public routes retain expected status/body; no DB/source activity. Include packaged-default configuration assertions; `ApplicationContextRunner`/mock-chain-only is not a substitute for this method's real embedded-Tomcat/HTTP proof.
3. `singleExplicitByteAuthorityIsUsedInEitherRegistrationOrder` — explicit test source used unchanged in both configuration orders; zero source beans fallback unavailable; no `ConditionalOnMissingBean` ordering dependence or default bean collision.
4. `duplicateByteAuthoritiesFailClosedEvenWithPrimary` — two sources, with/without Primary, fail with fixed safe ambiguity root cause; source toString not evaluated; verifier fallback is not overridden by presence of a source.

### 8.3 `com.onecall.archtests.sharing.SingleByteRangeTests` — 6 new Surefire methods

1. `absentRangeSelectsExactlyFullPositiveObject` — full interval, N=1 and Long.MAX_VALUE; no Range → partial=false; invalid N internal invariant failure; selected interval is exactly `ControlledMediaByteSource.ByteInterval`, identical to `OpenRequest.interval()` type, no parser interval copy.
2. `closedRangesClipOnlyTheirEndWithoutOverflow` — normal/clipped/one-byte/leading-zero cases; normalize end before addition; Long.MAX_VALUE endpoints safe; start>=N unsatisfiable.
3. `openEndedAndSuffixRangesResolveOneBoundedInterval` — open/suffix/suffix>=N/-0, correct partial=true/length, no negative/empty selected intervals.
4. `malformedUnitsWhitespaceAndMultipleRangesAreRejected` — exact malformed/duplicate/comma/non-ASCII/128-character tables from §4; no full fallback.
5. `reversedAndOverflowDecimalBoundsNeverWrapOrClamp` — reversed→Malformed before size; >long→Malformed, not 416/wrap; N-bound unsatisfiable only for syntactically valid long.
6. `normalizationMatchesDeterministicSmallObjectOracle` — bounded exhaustive loop over N=1..32, closed/open/suffix specs, compare with explicit selected-index oracle; interval always within object and length/body selection agree; no dependency or allocation proportional to large N.

### 8.4 `com.onecall.archtests.evidence.ControlledMediaByteSourceTests` — 4 new Surefire methods

1. `byteIntervalsAndOpenRequestsRejectUnboundedOrNullScope` — every constructor/null/negative/empty/reversed/out-of-object case; safe invariant messages, positive long accounting, sealed outcomes.
2. `deterministicSourceBindsObjectHashSizeAndExactInterval` — independently stored immutable fixture bytes/hash/UUID; mismatch→Unavailable/no read; correct full/slice opens once, arbitrary chunk sizes, exact bytes/EOF at interval end, no backing bytes outside interval read.
3. `sessionsCloseAbortAndRejectReadsAfterTerminalState` — close/abort idempotence, abort then close and close then abort; no reopening, fixed-safe terminal read failures; partial acquisition failure closes provider-owned resource; fault injection covers cleanup contract independently of HTTP.
4. `byteRequestsSessionsAndFailuresHaveRedactedRepresentations` — JSON/toString of request/interval/opened/session/exception fixtures contain no credentials, IDs/hash/location/media bytes; no new public/storage accessor; constant redaction.

### 8.5 Architecture changes — 2 new methods, existing methods retained

Add to `com.onecall.archtests.architecture.SharingQueryBoundaryTests`:

1. `gatewayUsesExactOwnedFilesAndEvidenceNamedInterface` — exact four gateway files; only named evidence byte API imports, including `ControlledMediaByteSource.ByteInterval` in `SingleByteRange`; assert exactly one production `ByteInterval` definition, nested in the evidence interface, and `Resolution.Selected.interval()`/`OpenRequest.interval()` have that identical evidence type with no conversion or parser duplicate. No evidence.internal/customer_iam/provider implementation imports, new module edge or sharing named export; byte API has exact signatures/nested types and contains no sharing dependency (sharing → evidence::media-read, never reverse). Extend existing command-path classifier with exact GATEWAY_FILES membership (not broad directory exclusion).
2. `gatewayTransfersWithoutTransactionsProjectionOrPublicContractChanges` — new gateway production source forbids JDBC/jOOQ/transaction creation or propagation/SQL/locks/issuance/events/whole-body/async/logging/credential reflection; `TransactionSynchronizationManager` read-only guards are the sole permitted Spring transaction API. Dedicated route/GET/purpose mapping/header and contract identities pinned; original public/control document SHA256 remains `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68`. Cross-check standalone gateway OpenAPI route/operation/error/header literals and no external `$ref` to public-control components.

Extend existing `ModuleBoundaryTests.evidenceMediaReadIsNamedAndInternalImplementationCannotBeImported` in place (zero new method) to require the new `ControlledMediaByteSource` and its nested public API under media-read, continue forbidding evidence.internal imports and verify modules. Keep exact 14-module/dependency map tests unchanged. Existing playback file-set/API/no-post-open-I/O tests stay intact.

### 8.6 `com.onecall.archtests.container.MediaGatewayTransportIT` — 6 new Failsafe methods

1. `realHttpFullAndRangeUseQualifiedAuthorizationThenExactBytes` — embedded Tomcat ephemeral loopback port + Java HttpClient, fresh test verifier accepts exactly the grammar-valid positive token `pa20-positive-playback-token` bound to independently issued fixture claims, real SharingPlaybackStateReader/Jooq repository and JooqEvidenceMediaObjectReader on actual dedicated logins; full PLAYBACK200 and all full-window range SEEK206 shapes exact (never existing `MediaPlaybackIT.CANARY` as positive bearer); state READ ONLY/REPEATABLE_READ, evidence READ ONLY/READ_COMMITTED; sole selected evidence UUID/physical object; both opens once.
2. `realHttpMalformedUnsatisfiableAndDeniedRequestsNeverReadBytes` — 400 grammar,401 presentation (including existing CANARY presented as bearer, verifier zero),403 invalid/expired/wrong purpose/cross-object/admission,416 authorized full-window unsatisfiable and503 unavailable; no physical opens for malformed/denied/416. With otherwise valid partial-window claims, malformed Range/query/body/bearer rejects before PA19 as400/401; syntactically valid satisfiable and unsatisfiable SEEK Range reaches successful PA19 metadata open then single non-disclosing403/no size/zero physical open. Exact §3.1 combined-fault/header/problem precedence; missing evidence/configuration unavailable behavior consistent with PA19.
3. `committedRevocationRotationAndRiskDenyNextByteRequest` — make legal existing owner fixture changes and commit before the next HTTP request; revoked/stale generation/open risk/current binding/member checks deny with zero physical open; ACTIVE and already-admitted LOCKED remain permitted; no recount. No sleeps, immutable-row edits or new ACL.
4. `byteOpenReadAbortAndCloseHoldNoDatabaseTransactionOrLock` — at every physical provider callback assert no actual transaction/synchronization/resources, both auth pools active=0; instrumented connection/SQL counters seal at metadata open, fail any later checkout/query. Block a read with latches while a separate admin connection checks those auth backend PIDs have no open xact/idle-in-transaction or transaction/row/advisory locks; committed lifecycle update on separate connection finishes while read blocked. Use lock_timeout=1000ms, statement_timeout=3000ms, latch/future<=5s, always release in finally; no sleep-based race. Capture PIDs before seal; observer SQL uses separate test-admin connection, never transfer/auth pools. Ordinary idle relation/catalog bookkeeping is distinguished from a held transaction lock.
5. `transferFaultsReleaseOneSessionAndPreserveDurableSnapshots` — before/after snapshots identical for success,416,denial,source unavailable,short read/read/write/abort/close faults; grant revisions/counts/generations, admissions/session witness and access-event rows unchanged. **Directly invoke the production `MediaGatewayFilter.doFilter` with the real `MediaPlaybackService`, real PG16 state/Jooq/evidence fixture and deterministic test byte source**, a valid servlet request and a test `HttpServletResponse`/`ServletOutputStream` double. The response captures headers/bytes/reset attempts and owns an explicit committed flag; configure a fixed failure offset (e.g. after a 4-byte prefix of the 16-byte payload), record that prefix, set committed=true and throw on the next attempted byte. Assert retained200/206 headers, exactly that prefix/no JSON or error suffix, no reset after commit, fresh cause-free/suppressed-free `IOException("Media transfer failed")`, physical open/abort/close attempts each exactly once, zero post-open SQL/checkouts and unchanged snapshots. Also inject a precommit write failure (flag false) to prove reset/stale-header removal/safe500 and exactly-once cleanup; cleanup/flush faults retain §5 counts. This PG-backed direct-filter proof is mandatory, not mock-only service/collaborators and not real-socket failure injection. Do not attempt a wrapper filter before HIGHEST_PRECEDENCE or depend on tied registration order; helpers stay nested in this IT or the already-allowlisted support file.
6. `realHttpAndLogsRedactSecretsAndLeaveExistingPublicFlowUnchanged` — **separate real embedded-Tomcat/Java-HttpClient truncation proof**: deterministic test source returns a nonempty prefix then throws/EOF before the advertised interval completes; choose a payload larger than the configured servlet response buffer and assert source progress past that buffer before failure, so actual container commitment precedes the source fault. Read through `BodyHandlers.ofInputStream()` to completion/failure with bounded timeout; client observes short Content-Length/IOException/terminated connection, never a fabricated500-after-commit or appended problem/error suffix. A client close/disconnect probe may supplement, never replace, the deterministic method5 cleanup assertions; do not assume a TCP disconnect throws on a chosen write. Raw headers/problems/captured logs exclude both positive HTTP token and existing private canary; exact authorized bytes only in success. Old customer/public GET/admission/media-authorization behavior remains regression covered; defaults no real provider; V001–V013/source/control OpenAPI hashes unchanged, role ACL snapshots remain exactly existing ones; close response streams/client/context/pools/container and all futures in finally.

Test fixture reuse is exact: widen `MediaPlaybackIT.Scenario`, `MediaPlaybackIT.Read`, `MediaPlaybackIT.Fixture` from private nested types to package-private nested types, retaining static/final/record modifiers. Existing fixture hardcodes size123/hash64-times-a (`MediaPlaybackIT.java:413`, `:625`); no practical real payload has that SHA. Add `scenario(String mode, String recordingStatus, boolean wrongRelationship, String mediaSha256, long mediaSize)` and `media(UUID id, String mediaSha256, long mediaSize)` fixture overloads. Move existing bodies to these overloads, replacing only media-object SHA/size, recording declared-size/declared-SHA/validated-SHA literals with parameters; both scenario media-creation calls (original and wrong-relationship alternate) explicitly call the new media overload with those parameters. Existing three-argument scenario and one-argument media delegate with their original hash/size constants, preserving all six old tests and fixtures exactly. The new IT computes SHA256/size from independently registered immutable bytes, passes them at scenario creation, and never updates append-only media/recording/artifact facts. Unrelated evidence snapshot/content hashes remain their existing fixture constants.

New IT accesses that fixture from the same `com.onecall.archtests.container` package and reuses `state`, `evidence`, `base`, pools, capture instrumentation and snapshot helpers. Build a fresh service with real state/evidence and a metadata-only test reader; do not reuse the fixture's `service(...)` verifier or hardcoded `play(...PLAYBACK)` convenience. Define test-only `POSITIVE_HTTP_BEARER = "pa20-positive-playback-token"` in the new IT/support and assert it matches §3.1 grammar; the fresh verifier compares credential bytes exactly to that constant, increments verification counters and returns the independently bound current fixture claim (other grammar-valid values are Invalid unless an explicit unavailable fault is configured). Existing private `MediaPlaybackIT.CANARY = "PA19_PRIVATE_cos://bucket/key?version=secret"` remains unchanged and private: it is reserved for existing database canaries and negative/redaction probes, not positive200/206 input. The new IT may repeat that literal locally for its negative probe without widening CANARY visibility; assert presenting it yields401 with zero verifier/physical calls, while the positive token yields200/206 and is itself absent from headers/problems/logs. Copy the independently issued fixture claim with exact PLAYBACK or SEEK purpose as needed, retaining every identity/time/generation field and its full Duration window except the explicit partial-window denial probes. Reset existing trace seal between separate requests and seal anew at metadata open. Serialize fixture setup/reset/request observation with test-side synchronization; never allow concurrent HTTP authorization requests to share mutable fixture claim/seal fields. The blocked-transfer observer uses a separate admin connection and does not take the request's fixture monitor. Use a local test HTTP configuration to inject those exact beans; never reflection, production annotations on providers or a second production persistence path. The N=16 web/parser example may also be used in the new overload; old PG fixtures remain N=123.

## 9. Exact implementation ownership / unconditional allowlist

**22 implementation paths, 14 new + 8 modified.** By purpose: 6 production Java (5 new,1 modified), 9 test Java (6 new,3 modified), 3 contract/tooling (2 new,1 modified), POM, CI, backend README and implementation report =22. This plan and its fix-1 artifact are documentation-only fix-gate outputs, not additional implementation-author paths. No wildcard permits another file.

| # | Exact proposed path | Ownership / permitted change |
| --- | --- | --- |
| 1 | `backend/src/main/java/com/onecall/core/evidence/mediaread/ControlledMediaByteSource.java` | Evidence; new interface/nested interval/request/session/result/redaction only, §5. |
| 2 | `backend/src/main/java/com/onecall/core/sharing/internal/gateway/MediaGatewayFilter.java` | Gateway; terminal HTTP auth/range/transfer/cleanup adapter; no business authorization reimplementation. |
| 3 | `backend/src/main/java/com/onecall/core/sharing/internal/gateway/MediaGatewayConfiguration.java` | Gateway; web servlet-only registration and strict unavailable physical-provider selection. |
| 4 | `backend/src/main/java/com/onecall/core/sharing/internal/gateway/SingleByteRange.java` | Gateway; pure closed parser/resolver types, §4. |
| 5 | `backend/src/main/java/com/onecall/core/sharing/internal/gateway/MediaGatewayProblemWriter.java` | Gateway; fixed UTF-8 safe problem/header writer, no customer/public dependencies. |
| 6 | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java` | Customer IAM; only filter order HIGHEST_PRECEDENCE→HIGHEST_PRECEDENCE+1. |
| 7 | `backend/src/test/java/com/onecall/archtests/MediaGatewayWebTests.java` | Test; 18 methods §8.1. |
| 8 | `backend/src/test/java/com/onecall/archtests/MediaGatewayConfigurationTests.java` | Test; 4 methods §8.2. |
| 9 | `backend/src/test/java/com/onecall/archtests/sharing/SingleByteRangeTests.java` | Test; 6 methods §8.3. |
| 10 | `backend/src/test/java/com/onecall/archtests/evidence/ControlledMediaByteSourceTests.java` | Evidence test; 4 methods §8.4. |
| 11 | `backend/src/test/java/com/onecall/archtests/support/MediaGatewayTestProviders.java` | Test-only deterministic verifier/metadata reader/immutable byte source/counters/faults; zero tests, no component scanning. |
| 12 | `backend/src/test/java/com/onecall/archtests/container/MediaGatewayTransportIT.java` | Test; 6 methods §8.6, nested local HTTP/fixture adapters, no extra fixture file. |
| 13 | `backend/src/test/java/com/onecall/archtests/container/MediaPlaybackIT.java` | Test maintenance; three nested-type visibility edits plus the two hash/size fixture overloads specified above; original delegates preserve all six existing methods/behavior. |
| 14 | `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` | Architecture; closed exact gateway classifier, two methods §8.5; all four current methods preserved. |
| 15 | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` | Architecture; extend existing named media-read assertion only; all nine methods and map preserved. |
| 16 | `api/openapi/media-gateway-v1.openapi.yaml` | API; dedicated standalone OpenAPI 3.1 gateway operation/header/problem contract; relative server `/`, Bearer security scheme, binary response schemas, no real server/provider value or public-control refs. |
| 17 | `api/openapi/media-gateway.redocly.yaml` | API; standalone `extends: [recommended]`, `info-license-strict: off`; no existing five-surface custom plugin. No unused credential example or secret. |
| 18 | `api/openapi/package.json` | API tooling; add `lint:media-gateway` = `redocly lint media-gateway-v1.openapi.yaml --config media-gateway.redocly.yaml`; append its invocation to existing `check`, retaining existing check sequence. No dependency/lockfile change. |
| 19 | `backend/pom.xml` | Build; exactly one extra container-smoke include `**/MediaGatewayTransportIT.java`; no dependency/version/plugin/security change. |
| 20 | `.github/workflows/ci.yml` | CI; exact additions/baselines/aggregate/source/POM/XML arithmetic §10; add default gateway packaged-web probes to existing smoke; retain toolchain/Docker/timeouts/firewall/contract jobs. Existing npm check now also lints separate contract. |
| 21 | `backend/README.md` | Docs; current PA20 inventory, internal GET contract/temporal restriction/fail-closed provider/lifecycle/testing pointer; mark older “current PA17” counters historical rather than leave contradictory current totals. No production readiness claim. |
| 22 | `docs/gateflow/pa20-sharing-media-gateway-transport-controlled-reader-provider-implementation-report.md` | Author report only; candidate paths, commands/fresh XML, snapshots/redaction/cleanup evidence and residuals; no acceptance or Controller mutation. |

State arithmetic: new rows1–5,7–12,16–17,22 (5+6+2+1=14); modified rows6,13–15,18–21 (1+3+4=8). There are four new Surefire classes, one new IT class and one support file, not six new test classes. All existing annotated methods keep their names and behavior.

There are **no conditional implementation paths**: reachability/order, named API, architecture classifier, POM and contract-lint ripples are already demonstrated repository facts, not deferred discovery. No application YAML, package-info, existing public OpenAPI/config/checker, migration, dependency lockfile, design doc, Control or review path is allowed. If implementation needs any other path or production value, stop for Controller plan amendment; do not silently add a conditional bucket.

## 10. Measured inventory and validation commands

### 10.1 Current source=CI=POM and XML evidence limits

At this gate an AST evaluation of literal CI inventories plus its PA19 additions, compared against every source `@Test void` name, measured **25/162 Surefire, 12/198 Failsafe**, no name differences. POM has exactly 12 includes equal to those IT class basenames. This worktree has **zero current Surefire/Failsafe XML reports**; no runtime suite or Docker run was performed for a documentation-only gate. The PA19 merged record reports green 37/360 XML; that is historical evidence, not newly measured PA20 XML.

| Suite/class (existing baseline) | Current methods | Planned methods |
| --- | ---: | ---: |
| ModuleBoundaryTests | 9 | 9 |
| OneCallApplicationTests | 1 | 1 |
| BootstrapDependencyBoundaryTests | 1 | 1 |
| CustomerShareQueryServiceTests | 8 | 8 |
| SharingPersistenceConfigurationTests | 2 | 2 |
| SharingQueryBoundaryTests | 4 | 6 |
| ShareAdmissionServiceTests | 21 | 21 |
| CustomerSessionBoundaryTests | 7 | 7 |
| CustomerContextServiceTests | 6 | 6 |
| CustomerWebConfigurationTests | 6 | 6 |
| CustomerMeWebTests | 8 | 8 |
| CustomerApiBoundaryTests | 3 | 3 |
| CustomerShareCursorCodecTests | 6 | 6 |
| CustomerSharesWebTests | 10 | 10 |
| CustomerShareCommandServiceTests | 6 | 6 |
| CustomerSharesCommandWebTests | 4 | 4 |
| SharingCommandPersistenceConfigurationTests | 2 | 2 |
| PublicShareWebTests | 12 | 12 |
| PublicShareQueryServiceTests | 4 | 4 |
| PublicSharePersistenceConfigurationTests | 2 | 2 |
| EvidenceMediaReadPersistenceConfigurationTests | 4 | 4 |
| EvidenceMediaObjectReaderTests | 4 | 4 |
| MediaPlaybackServiceTests | 20 | 20 |
| SharingPlaybackStateReaderTests | 8 | 8 |
| MediaPlaybackConfigurationTests | 4 | 4 |
| New MediaGatewayWebTests / MediaGatewayConfigurationTests / SingleByteRangeTests / ControlledMediaByteSourceTests | 0 | 18+4+6+4=32 |
| **Surefire** | **25 classes / 162** | **29 classes / 196 (162+32+2)** |
| PostgresContainerIT | 64 | 64 |
| SharingQueryServiceIT / CustomerMeIT / CustomerSharesIT | 10+10+8 | 10+10+8 |
| SharingRoleLeastPrivilegeIT / CustomerShareCommandsIT | 12+18 | 12+18 |
| PublicSharesIT / PublicShareAdmissionIT / SharingAdmissionRoleLeastPrivilegeIT | 8+37+9 | 8+37+9 |
| EvidenceMediaReadRoleLeastPrivilegeIT / EvidenceMediaReadIT / MediaPlaybackIT | 8+8+6 | 8+8+6 |
| New MediaGatewayTransportIT | 0 | 6 |
| **Failsafe** | **12 classes / 198** | **13 classes / 204** |
| **Combined** | **37 classes / 360** | **42 classes / 400** |

CI update is not just changing two totals. Declare `pa20_additions` using the six exact classes/sets from §8 (lengths `[18,4,6,4,2,6]`), merge into unit/container dictionaries **before** `source_classes` and all `exact_inventory` comparisons. Preserve PA18/PA19 addition sets. When calculating historic checkpoints, subtract PA20 names as well: pre-PA18 remains `[112,178]`/290, PA19-start remains `[128,192]`/320, PA20-start is `[162,198]`/360, current `[196,204]`/400. Assert every subtraction is a source subset, no duplicate method names and exact class coverage.

Preserve the lone historical comment hit `PostgresContainerIT.java` (“no new @Test ...”); actual-method totals are not grep line totals. Comment-inclusive word-boundary line scans: historical 291 → PA19-start321 → PA20-start361 → planned401, with no additional annotation-in-comment hits. Current/new class counts25→29 and12→13; POM include count12→13 with exact set equality. Existing special counts PublicShareAdmissionIT37, SharingAdmissionRoleLeastPrivilegeIT9 and PostgresContainerIT64 remain. Change both the early Surefire total check (currently162) and final method/XML assertions; never compare XML before merging new method sets. Fresh XML must exactly contain all 42 classes/400 `(classname,name)` pairs, no stale reports, failures/errors/skips zero.

### 10.2 Executable validation sequence (future implementation gate)

Run from repository root unless a subshell changes it. JDK21 only; Maven wrapper3.9.9; CI retains Temurin21.0.13+9 and existing30-minute ceiling. Docker daemon is required; existing Testcontainers uses `postgres:16`, fixtures assert PostgreSQL16 and exactly13 migrations, Flyway remains test-only. Record actual patch/minor/image identity from runtime, do not claim 16.15 merely because PA19 used it. No H2/SQLite/mock-DB substitution, host production database, disabled checks, test skipping or permission changes. Missing Docker/network/toolchain is an explicit failed prerequisite, not green validation.

```bash
# Read-only start/frozen-artifact evidence; save outputs under ignored backend/target later.
# Controller supplies the accepted-plan commit SHA, not the earlier plan/intake bookkeeping HEAD.
: "${PA20_IMPLEMENTATION_BASE:?Controller accepted-plan commit SHA required}"
git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD" rev-parse "${PA20_IMPLEMENTATION_BASE}^{commit}"
git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD" merge-base --is-ancestor 9da37930651d2d0f4ba76a1e8c8d1c15a6ba435e "$PA20_IMPLEMENTATION_BASE"
git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD" rev-parse HEAD
java -version
(cd backend && ./mvnw -B -ntp -v)
docker info
sha256sum api/openapi/onecall-v1.openapi.yaml backend/src/main/resources/db/migration/V0*.sql

# Focused tests: 34 new Surefire methods plus existing architecture/PA19 regressions.
(cd backend && ./mvnw -B -ntp -Dtest=MediaGatewayWebTests,MediaGatewayConfigurationTests,SingleByteRangeTests,ControlledMediaByteSourceTests,SharingQueryBoundaryTests,ModuleBoundaryTests,MediaPlaybackServiceTests,SharingPlaybackStateReaderTests,MediaPlaybackConfigurationTests test)

# Focused new real-HTTP/PG proof: exactly six IT methods, never silently absent.
(cd backend && ./mvnw -B -ntp -Pcontainer-smoke -Dit.test=MediaGatewayTransportIT -Dfailsafe.failIfNoSpecifiedTests=true verify)

# Fresh final reports; no focused-run report residue may satisfy the full gate.
(cd backend && ./mvnw -B -ntp -Pcontainer-smoke clean verify)

# Existing public contract/prototypes plus the new separate gateway lint.
(cd api/openapi && npm ci && npm run check:s3)
node --test shared/qa/business.test.js shared/qa/call-task.test.js shared/qa/runner-exit.test.js
```

Replay the workflow's exact embedded scripts locally after the full build (existing dependencies for firewall/JAR scripts must be available). This avoids inventing another looser XML checker. From repo root:

```bash
python3 - <<'PY'
from pathlib import Path
import re
import subprocess
import textwrap

workflow = Path('.github/workflows/ci.yml').read_text()
step_names = [
    'assert container smoke actually ran',
    'default and opt-in web jar smokes',
    'production jar dependency firewall',
]
for name in step_names:
    match = re.search(r'^      - name: ' + re.escape(name) + r'\n(.*?)(?=^      - name: |^  [A-Za-z0-9_-]+:|\Z)', workflow, re.M | re.S)
    if match is None:
        raise SystemExit('Missing exact CI validation step: ' + name)
    run = re.search(r'^        run: \|\n(.*)', match.group(1), re.M | re.S)
    if run is None:
        raise SystemExit('Missing CI run body: ' + name)
    subprocess.run(['bash', '-e', '-o', 'pipefail', '-c', textwrap.dedent(run.group(1))], cwd='backend', check=True)
PY
```

These are the exact existing labels at workflow lines60,651,732; no deferred command discovery or step renaming is needed. Their commands/probes remain authoritative. Extend the existing web-smoke probe list with GET gateway/no bearer→401 and opaque bearer→503, identical safe problem/common-header assertions, and assert no Hikari startup/real provider; retain all existing probes and82 adversarial dependency-firewall probes. No positive fixture is packaged into the production JAR. Node contract validation retains CI's22.23.4 pin and locked npm toolchain; script-only package.json changes do not require a lockfile/dependency edit.

Finish with explicit allowlist equality (new14/modified8), source/CI/XML equality and frozen byte comparisons against `PA20_IMPLEMENTATION_BASE`, the accepted-plan commit defined in §1. `git ... diff --check` and SHA256 hashes are read-only; use explicit Git dir/work-tree as above in this environment. Require zero changed V001–V013/public OpenAPI/PA19 production paths; compare migration and public contract hashes byte-for-byte to that accepted-plan base, not expected semantic equivalence or an earlier bookkeeping HEAD. Keep logs/XML under ignored target/private evidence destinations, not new author paths. Hosted CI is reported only if actually observed by the Controller later.

## 11. Documentation decision and residual ownership

Dedicated data-plane OpenAPI is **required**: standalone gateway document/config under `api/openapi`, not an operation/component/security-scheme change in `onecall-v1.openapi.yaml`. Pin `/` relative server; HTTP bearer scheme without declaring a JWT/signing format; request/response headers/statuses from §§3–4; binary 200/206 schemas; closed problem schema and exact codes; descriptions explain HEAD/commitment/full-time-window and source lifecycle limitations. No deployed origin, storage identifier, credential example, external public-control reference or fifth/sixth-surface map mutation. The existing npm check invokes its standalone recommended-rule lint; custom five-surface governance still runs unchanged for the existing contract.

Minimum prose changes at implementation are backend README plus one author implementation report. Do not edit docs/12 or docs/17: the chosen placement preserves module/deployment/opaque credential/private media policy; a protocol contract is captured separately instead of silently “fixing” design policy. No docs/15 public operation count or docs/README inventory change is needed. Architecture maps remain exact; only the existing media-read API/member assertion and closed gateway file classification expand. If a new cross-module provider edge is later desired, B-01's adapter/module-map/design review remains a prerequisite.

| Residual / risk | Owner | Destination / boundary |
| --- | --- | --- |
| Production verifier, authenticated issuance-time object/duration/window provenance, signature/key rotation and opaque wire format | Security + Sharing + Platform | Dedicated credential-provider work unit; PA19 B-01/PA0-R-09 adapter review before new module edge; all current defaults unavailable |
| Real private object location, COS reads, version/hash binding, KMS/Secret Manager/CAM and resource timeouts | Evidence + Platform + Security | Production controlled-byte-provider plan/runbook; no V013 ACL widening or production secret in PA20 |
| Q22 TTL, clock skew (provider/local vs PostgreSQL clock), expiry during transfer and snapshot→open revocation window | Security + Sharing + Platform | Q22 policy decision; no numeric value or continuous/zero-race authorization promise |
| Partial temporal windows and format-aware time↔byte mapping | Evidence + Media/Backend + Security | Separate media-format authorization contract if needed; PA20 returns403 before physical open rather than leaking whole object |
| Public gateway origin/path mapping, TLS, ingress/LB/CDN/WAF, CORS/player integration and deployment values | Infrastructure + Security + Backend | Remaining `R-A3-MEDIA-GATEWAY-TRANSPORT`; internal GET is not public rollout authorization |
| Deployed edge/proxy/collector/analytics redaction and already-buffered bytes | Platform + Security + Observability | `R-A3-REDACTION-RUNTIME`; app tests prove only application boundary, never recall committed/client buffers |
| Slow/disconnected clients, resource budgets, request/body/URL rejection by Tomcat before filter, connection termination after commitment | Backend + Platform + Observability | Operational hardening/deployment tests; synchronous bounded memory here, no fabricated portable replacement status after commit |
| PostgreSQL image patch drift, Docker/toolchain availability, CI timing ceiling and hosted check execution | Backend + Database + CI Controller | Actual PG16 runtime/clean XML evidence; no skips or recycled reports; keep pinned Java/Maven and30-minute workflow |
| Existing advisory/issuance/replay/current-state semantics and immutable fixture cleanup | Sharing + Evidence + Backend tests | Existing PA15–PA19 suites and V001–V013 hashes; only test fixture visibility/additive hash-size parameterization, no production behavioral refactor |

**Blocking open questions: none for this foundation.** Endpoint, purpose selection, temporal guard, parser, API, headers/error commitment behavior, cleanup, ownership, tests and path inventory are decisions above, not future discovery tasks. Real provider/format/deployment policy remains intentionally unavailable and owned; it is not silently selected by this plan.

## 12. Completion report format

Plan/fix-gate handoff: artifact paths, measured SHA256/line/byte counts, changed-path equality (original plan gate: plan only; fix1: plan plus fix-1 artifact only), current source/CI/POM measurements and explicit lack of fresh XML, planned class/method/include arithmetic, exact22 proposed implementation paths (§9), one independently verifiable composed slice and blocking questions (none). Fix1 records input plan/Control/review hashes, accepted finding dispositions and exact changed sections; mandatory independent A/B re-review remains pending. Do not report plan acceptance or implementation execution.

Future S1 author report: exact Controller-supplied accepted-plan `PA20_IMPLEMENTATION_BASE` and start/end candidate identity; actual-minus-allowlist and allowlist-minus-actual (with intentional unchanged baseline paths explained); source/CI/POM/fresh XML names and counts; commands/exit status/toolchain/Docker/actual PG16 image; exact full/range/status/header bytes, open/read/abort/close counters and committed-failure limitation; separate PG-backed direct-filter write-failure and real-HTTP truncation evidence; DB connection/transaction/lock proof and before/after durable snapshots; frozen migration/public contract hashes against that accepted-plan base; log/JSON positive-token/private-canary separation evidence; docs/contract lint and JAR/firewall results; residual owner table and explicit unrun external checks. Stop for Controller intake; no staging/commit/push/PR/approval/ready/merge/reviewer/external action.
