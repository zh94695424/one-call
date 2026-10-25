# A3 Production Security Design Spec — bounded plan-fix 1

> Work unit: `A3-production-security-design-spec`
>
> Gate: `fix` (bounded plan-only correction; no implementation, no later gate)
>
> Task label: `a3-security-planfix1-ds-20261025-01`
>
> Reviewed target: `docs/gateflow/a3-production-security-design-spec-plan.md`, 1034 lines / 109030 bytes, SHA-256 `09B27DA90E29864117D6B2CE32B7918229A30BE0C9393124BC67E6AED4733A12` (matches the controller `plan` record)
>
> Independent review: `docs/reviews/plan-review-20261025-205516.md`, SHA-256 `BC4CAD5CA2CBF77AD6EBBA3967AD2FDBE241065CE1AB2E3DAD3DE74D499665EA` (`fail`; F-01 High, F-02/F-03/F-04/F-05 Medium, F-06/F-07/F-08/F-09 Low)
>
> Controller adjudication: `docs/gateflow/project-execution-control.md`, subsection `A3 plan-review adjudication and bounded-fix entry` (binding for F-01..F-09)
>
> Revised target: same plan path, now 1052 lines / 127861 bytes, SHA-256 `12E4136EE557D604FF680F1B162265C68171C5C724EF2888FB69D429007405F1`
>
> Result: revised plan plus this new fix artifact. No other repository path was edited by this task.

## 1. Gate, reviewed target and scope

- The reviewed target is the A3 plan only. The binding scope remains the A3 goal contract in `docs/gateflow/project-execution-control.md` L37–59: a code-generation-ready, statically verifiable security design with a machine model, deterministic fixtures and a zero-dependency validator; no runtime/cloud/UI implementation, no invented region/SKU/price/capacity/WORM/legal-retention/MFA/provider/numeric value, and no A0/A1/A2 contract change.
- This fix implements exactly the nine adjudicated findings. It preserves the three behavior slices, the A0 envelope/HMAC shapes, AES-256-GCM/12/16/AAD, KMS→purpose-DEK hierarchy, historical decrypt/HMAC lookup, five trust surfaces, six service identities, IVR canonical signing, transport/domain replay separation, Tencent fixed-versus-deferred boundary, private COS bucket, 20 key classes, 28 data classes, 10 credential classes, 2 signature profiles, 2 media policies, 39 audit/access event codes, 10 high-risk operations and 10 call classes.
- No endpoint, Problem code, A0 table/column/state/transaction, employee role, service decomposition, cloud resource, UI change, production numeric value or provider capability claim was added. The only registry expansion is the already-accepted five A3 `CV-*` rows, now stated explicitly as A3-origin rather than falsely denied.
- Only `docs/gateflow/a3-production-security-design-spec-plan.md` and this new artifact were written. The controller, review, A0/A1/A2 sources, OpenAPI, prototypes, screenshots, shared business logic and pre-existing dirty/untracked paths were not modified. No stage, commit, push, PR, merge, plan re-review or implementation occurred.

## 2. Accepted findings and disposition

| Finding | Verdict | Change and section references |
| --- | --- | --- |
| F-01 (High) — playback credential had no client→gateway transport | Resolved | §1.5, §1.6, §4.9 Playback, §5.4 C10, §5.7 P1, §6.5, §7.4 `sec-s3-02`, §7.6, §8.4, §9.2. C10 is presented as `Authorization: Bearer <credential>` to the authenticated media-gateway data plane; the bearer alone resolves the authorized object/scope server-side; the client supplies no COS key/bucket/version or public A1 resource identifier. This is not a new A1 business operation. Gateway origin/path, CDN/LB wiring, range/streaming protocol and delivery topology are the typed deployment residual `R-A3-MEDIA-GATEWAY-TRANSPORT` owned by Infrastructure/Security with destination `media gateway deployment / storage capacity`. Fixtures assert bearer presentation, redaction, audience/object/purpose/generation/expiry scope and denial, never a URL/route. |
| F-02 (Medium) — ACTION_DERIVED reason and UI confirmation | Resolved | §2.4, §4.10, §5.9, §6.6, §7.4 `sec-s3-05`, §7.5, §9.2. `ACTION_DERIVED` is frozen only as the technical A0 `reason_code` projection required by the current A1 request schema; it is explicitly not the employee's human-entered motive and does not close the architecture's semantic operation-reason requirement, which remains `R-A3-A1-EMPLOYEE-REASON-PROJECTION` (Product/Operations + API governance; future A1/UI refinement). The `二次确认` column was removed from the §5.9 registry; UI second confirmation remains a required runtime high-risk control but is marked runtime-unverified and is not a validator-assertable success criterion. A0 same-transaction audit/reason fail-closed behavior is preserved. |
| F-03 (Medium) — upload credential dual shape | Resolved | §1.5, §2.4, §4.9 Upload, §4.13, §5.4 C09, §5.7 U1, §5.10 CV-4, §6.5, §7.4 `sec-s3-01`, §8.4, §9.2. The ambiguous presigned-or-custom-HMAC dual shape is removed. The one primary logical authorization is a short-lived, single-object Tencent COS presigned PUT credential, issued only if CV-4 verifies required method/object/expiry and supported size restrictions; otherwise issuance fails closed and implementation stops for architecture resolution, with no fallback to custom HMAC/signing gateway. `onecall:evidence-upload:v1` is an issuance-side policy/audience classification, not a COS-validated embedded audience. Issuance validates `callId`/`recordingId`/`purpose` and derives the hidden target object; completion validates server-observed object identity, hash, size, MIME/duration and declaration binding. The A1 field remains one opaque 1-2048 redaction-required credential, may be URL-shaped, and is never logged or decomposed into public object fields. |
| F-04 (Medium) — 401/403 IVR outbox behavior | Resolved | §2.4, §4.7, §4.12 rule 3, §7.4 `sec-s2-09`, §7.5, §8.3, §9.2. Observed 401 or 403 leaves the same outbox entry in the existing A2 `PENDING` state (logically security-blocked via local operational metadata if available); no automatic hot retry, new event identity, payload mutation, redial, re-resolve, reservation or settlement occurs; an alert/audit signal is emitted without credentials; retry occurs only after an explicit credential/key/certificate or service-authorization recovery signal and reuses the same domain identity and immutable payload with a fresh timestamp/nonce. This is an A3 security-layer refinement of the A2 deferred authentication boundary using an existing A2 state, not a new A2 event/state/digest or server classification. Numeric retry/backoff/alert values remain Q8/Q13/Q25. |
| F-05 (Medium) — deferred/capability registry contradiction | Resolved | §0, §3.4, §3.5, §5.10, §7.2, §7.3, §8.2, §8.4, §9.3, §11. The 23 rows are stated exactly as 18 unchanged A0 Q-subset rows plus five A3-originated `CV-1..CV-5` cloud-capability verification rows. CV rows do not consume Q IDs or modify A0 §14. The false "A3 不新增 open-input row" and "all owner/destination unchanged from A0 for all 23" statements were removed. The validator now must compare the 18 Q rows exactly to A0 and separately require the five A3 CV rows with non-empty owners/destinations. |
| F-06 (Low) — premature S1 invariant freeze | Resolved | §3.4, §6.8, §7.3, §7.4 (`sec-s1-06`, `sec-s2-09`, `sec-s3-08`), §7.5, §8.2/§8.3/§8.4, §10. Invariant text is staged by behavior: S1 INV-A3-01..06 (6), S2 adds INV-A3-07..11 (cumulative 11, +5), S3 adds INV-A3-12..21 (cumulative 21, +10). Future-slice invariant text is rejected as premature. Final count remains 21 and there is no fourth slice. |
| F-07 (Low) — header citation overclaim | Resolved | §1.5, §4.7, §7.2, §7.4 `sec-s2-04`. The plan now states that A1 froze all three header names, TIMESTAMP length 1-64 and NONCE length 1-128, but did not freeze SIGNATURE length; A3 freezes the signature value format. The drift check asserts TIMESTAMP/NONCE name+length and SIGNATURE name+format only, never a SIGNATURE length. |
| F-08 (Low) — audit registry lacked persistence discriminator | Resolved | §3.4, §3.5, §4.10, §5.8, §6.6, §7.3, §7.4 `sec-s3-04`, §7.5, §8.4, §10. Every one of the 39 rows now carries `persistence` limited to `append_fact` or `access_event`: `AUDIT_READ` and `AUDIT_READ_DENIED` are `access_event` and must not recursively call `audit.append_fact`; the other 37 are `append_fact`. The document marker, model, stage validator and fixture require the route, not only the 39-row count. |
| F-09 (Low) — five-domain citation imprecision | Resolved | §1.2, §4.1, §5.1, §11. The five trust surfaces/audience domains are now cited from `docs/12` §8's five API faces plus A1 `x-onecall-audience`; `docs/12` §11.1's four authentication domains are explicitly retained as a distinct authentication-mechanism categorization, not the five-face enumeration. The five-domain design is unchanged. |

## 3. Revised per-stage invariant and fixture contract

| Item | S1 | S2 | S3 |
| --- | --- | --- | --- |
| Invariant IDs present | INV-A3-01..06 | INV-A3-01..11 | INV-A3-01..21 |
| New invariants in slice | 6 | +5 (07..11) | +10 (12..21) |
| Cumulative invariants | 6 | 11 | 21 |
| New fixtures in slice | 6 | 9 | 8 |
| Cumulative fixtures | 6 | 15 | 23 |
| Slice fixture composition | `6` | `6+9` | `6+9+8` |
| Cumulative marked negatives | 2 | 7 | 10 |
| Deferred rows | 23 = 18 A0 Q + 5 A3 CV | same | same |
| Audit persistence | absent | absent | 39 rows: 2 `access_event` + 37 `append_fact` |

- The added S2 fixture is `sec-s2-09-ivr-outbox-auth-blocked` (F-04); it proves both observed 401 and observed 403, the `PENDING`/security-blocked state, no hot retry/identity mutation/redial/re-resolve/reservation/settlement, credential-free alerting, explicit-recovery retry with the same domain identity and immutable payload plus a fresh timestamp/nonce, and zero A2 business side effects.
- The revised fixture total is 23 (`6+9+8`); the previous 22 was `6+8+8`. All stdout examples, slice acceptance counts, completion-report requirements and the §11 mapping were updated together, so no stale `fixtures=22`, `S2=14`, `negative=9` or `6+8+8` text remains.

## 4. Validation performed

| Check | Method | Outcome |
| --- | --- | --- |
| Before hash | `sha256sum` before edit | `09b27da90e29864117d6b2ce32b7918229a30be0c9393124bc67e6aed4733a12` — matches the reviewed/controller target |
| After hash and size | `sha256sum`, `wc -l -c` | `12e4136ee557d604ff680f1b162265c68171c5c724ef2888fb69d429007405f1`; 1052 lines / 127861 bytes |
| Encoding | `file -bi` | `text/plain; charset=utf-8` |
| Conflict markers | Grep `^(<<<<<<<|=======|>>>>>>>)` | No matches |
| Trailing whitespace | Grep `[[:blank:]]$` | No matches |
| Git whitespace | `git diff --check -- docs/gateflow/a3-production-security-design-spec-plan.md` | No output |
| Audit registry rows | Grep count of numbered audit rows; persistence partitions | 39 rows; 2 `access_event` (`AUDIT_READ`, `AUDIT_READ_DENIED`); 37 `append_fact` |
| Invariant rows/stages | Grep count of `INV-A3-*` rows and `S1/S2/S3` ownership cells | 21 total; 6 S1 + 5 S2 + 10 S3; cumulative 6/11/21 |
| Deferred registry | Grep count of `Q*` and `CV-*` rows | 18 Q + 5 CV = 23 |
| Fixture rows | Grep count of §7.4 fixture rows | 23 |
| Stale-text scan | Grep for old counts/phrases (`fixtures=22`, `S2=14`, `negative=9`, `6+8+8`, `21 invariants` in S1 contexts, `A1 已冻结名称/长度`, dual-shape upload, no-new-input claim, UI confirmation as an asserted column) | No stale matches |
| Source spot-check — A1 headers | Read OpenAPI `IvrSignatureParam`/`X-ONECALL-SIGNATURE` (L4011–4012), IVR TIMESTAMP (L4066–4074), IVR NONCE (L4075–4083), provider TIMESTAMP/NONCE (L4084–4101) | SIGNATURE has no length; TIMESTAMP 1–64 and NONCE 1–128 confirmed |
| Source spot-check — A2 outbox | Read `docs/16` §6.6 L317–326 | Four states (`PENDING`/`SENT`/`ACKED`/`PERMANENT_FAILURE`); 401/403 are in neither permanent nor retryable set; retransmission keeps identity and never redials |
| Source spot-check — five domains | Read `docs/12` §8 L269–277 and §11.1 L383–389 | Five API faces in §8; §11.1 lists four authentication domains/mechanism categories |
| Source spot-check — A0 Q rows and audit reason | Read `docs/14` §14 L4772–4805, §9.3 L3045–3051, §10.6 L3498–3507 | 28 Q rows; every employee-actor action requires a reason; `audit.append_fact` is same-transaction/append-only |
| Write scope | `git status --short` for the two allowed paths | Only the revised plan (pre-existing untracked target) and this new fix artifact; no A0/A1/A2, controller, review, OpenAPI, prototype or screenshot edit |

No `node`/`npm` validator run occurred: the planned `docs/17`, `security/v1` model, fixtures and `security/validate.mjs` do not exist yet, and the environment's command permission gate denies Node/npm. All checks above are document-level/static. The validator and fixture contracts remain a specification for the S1/S2/S3 implementation to encode and execute.

## 5. Residual risks and honest limitations

- **Static-only proof.** This fix proves plan-text consistency, not runtime security. No KMS/Secret Manager/CAM/COS/Redis/DB/IVR/logging/analytics execution occurred. The static-validator co-authorship risk remains: a self-consistent `SecurityMachine` can still be semantically wrong about a real backend.
- **Audit persistence is a design determination.** The 2/37 split follows accepted A0/A1 semantics (`AUDIT_READ`/`AUDIT_READ_DENIED` as access events, all other audit rows as `append_fact`); implementation must actually route them and the validator must check the route, not only row count.
- **UI second confirmation is runtime-unverified.** It remains an architecture-required high-risk control but is outside A3's static proof surface and must be implemented and verified by the operations UI work.
- **Upload credential depends on external CV-4 verification.** Until Infrastructure/Security verifies COS presign method/object/expiry/size support, issuance must fail closed and implementation must stop for architecture resolution; no fallback shape exists.
- **Media-gateway transport is deployment configuration.** Bearer presentation and token semantics are frozen, but the exact origin/path, CDN/LB wiring, range/streaming protocol and delivery topology remain `R-A3-MEDIA-GATEWAY-TRANSPORT` (Infrastructure/Security → media gateway deployment / storage capacity). No fixture proves a concrete URL/route.
- **Invariant staging must be enforced by the future validator.** The plan now specifies S1=S6/S2=11/S3=21 and rejects premature text; a permissive validator could still accept early text if implemented incorrectly.
- **Fixture count changed mechanically.** The new `sec-s2-09` makes the cumulative fixture plan 23; implementation must create all 23 fixture files and keep the S2/S3 cumulative outputs consistent.
- **Deferred values remain deferred.** Q8/Q13/Q25 numeric retry/backoff/alert values, Q11 COS/WORM/region, Q3 retention, Q22 TTL/skew, Q6/Q7/Q15 provider values and all other typed inputs are not guessed.
- **Evidence limits.** The A0/A1/A2 source checks were targeted spot checks for the findings (headers, outbox, §8/§11.1, Q rows, audit reason), not a full re-derivation of every upstream count; the plan retains the accepted drift counters (A0 44/28, A1 49/6, A2 12/11/22/15 + digest).

## 6. Completion status and changed paths

- Status: **complete for this bounded plan-fix Gate**. The revised plan is ready for a fresh independent MiMo plan re-review; implementation remains unauthorized until the controller accepts that re-review.
- Changed paths:
  - `docs/gateflow/a3-production-security-design-spec-plan.md` — revised (1052 lines / 127861 bytes; SHA-256 `12E4136EE557D604FF680F1B162265C68171C5C724EF2888FB69D429007405F1`).
  - `docs/gateflow/a3-production-security-design-spec-plan-fix-1.md` — this new artifact.
- No other path was written. No stage, commit, push, PR, merge, plan re-review or implementation was performed. The fix stops here as required.
