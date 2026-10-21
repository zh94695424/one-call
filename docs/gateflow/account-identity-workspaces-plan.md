# Account identity and workspaces - implementation plan

## Gate / scope

- Work unit: `account-identity-workspaces`; gate: `plan`.
- Scope authority: `account-identity-workspaces-control.md` and the user's confirmed account rules.
- Goal: make the customer prototypes truthfully demonstrate one natural-person account, separate login identities, membership-derived organization workspaces, and business numbers that do not grant login.
- Non-goals and residual risks are exactly those recorded in the control artifact; this plan does not add production authentication or change employee accounts.

## First-principles decision

Authentication answers “which natural person is signed in”; membership answers “which organization and role may that person enter”; a business subject answers “which calling number owns recordings/trial/order facts”. These are separate trust decisions and must not be represented by one phone field or a mutable role selector.

The shared module is already inlined into all prototypes and is the narrow common contract. Add immutable account fixtures and pure account lookup functions there, but keep them outside `createDemoState`, `snapshot`, and `loadReviewSnapshot`. This preserves the current cross-channel business snapshot contract and its explicit rejection of auth/session state.

## Shared account contract

Keep one private, deep-frozen `ACCOUNT_DIRECTORY` with exact collections:

- `accounts`: `id, displayName, status(ACTIVE/DISABLED)`.
- `authIdentities`: `id, accountId, provider(WECHAT/PHONE), providerSubject, maskedLabel, status(ACTIVE/DISABLED)`; provider + providerSubject is unique.
- `identityProfiles`: `accountId, realNameStatus(UNVERIFIED/VERIFIED), maskedCredential`.
- `organizations`: `id, name, shortName, status(ACTIVE/DISABLED)`.
- `memberships`: `id, accountId, organizationId, role(ADMIN/MEMBER/EVIDENCE_MANAGER/AUDITOR), department, status(ACTIVE/DISABLED)`; active account + organization has at most one active membership.

Demo people are 李晓 (`ACCOUNT-DEMO-001`, phone `13800006208`, WeChat `WX-DEMO-LI`), 陈雨 (`...002`, `13900007812`), 周宁 (`...003`, `18600003360`) and 王璐 (`...004`, `13700004102`). 李晓 is 华启管理员 and 云拓普通成员；the other three are respectively 华启普通成员、证据管理员、审计员. Include disabled account/membership fixtures solely to prove fail-closed filtering. `18600004471` and all landlines remain business numbers only and must not occur as login identities.

Raw `providerSubject` values stay in the private directory and are used only for equality matching. Export `demoAccountDirectory`, a deep-frozen redacted projection in which auth identities expose `id, accountId, provider, maskedLabel, status` but not `providerSubject`. Export `createAccountDirectoryFixture()` only as a clearly named test/prototype fixture factory returning a mutable full clone; it is not a session or business snapshot API.

Export pure functions:

- `validateAccountDirectory(candidate) -> {ok, code}` rejects malformed references, duplicate provider subjects, duplicate profiles, duplicate active memberships and unsupported enums. “Business-only number” is not inferred by this validator; the invariant is proved by the default private directory excluding those numbers and by business mutations never changing the directory.
- `resolveDemoLogin({method, phone, code, providerSubject}) -> {ok, code, value:{account, profile, identities, workspaces}}`; phone requires exact 11 digits, fixed demo OTP `482916`, and an active PHONE identity; WeChat requires a known active provider subject. User-visible failures use one generic `INVALID_CREDENTIALS` code.
- `workspacesForAccount(accountId) -> {ok, code, value}` returns one personal workspace plus only active memberships on active organizations. Organization workspace role is immutable fixture data.
- `accountById(accountId)` and `loginIdentitiesForAccount(accountId)` return cloned fixture data.

No login/query result or exported redacted directory exposes an unmasked credential. The full fixture factory is explicitly test/prototype data and is not used by UI rendering. No login function mutates business state.

## Business-subject alignment

- Seed Web member mobile subjects with their actual natural-person account IDs: 陈雨 `...002`, 周宁 `...003`. 李晓 personal mobile and organization fixed lines remain `...001` in the current simplified business snapshot; organization ownership remains documented residual risk `R2`.
- Authorization visibility continues to use organization role and record subject ID. Revoke/reactivate must target the record's canonical `subjectId`, never search by the currently signed-in account ID.
- Mini-created numbers and IVR claims continue to use the authenticated李晓 account ID. Claiming a number changes only the business association already defined by the existing domain function and never creates `authIdentity` data.

## Slice S1 - shared identity contract

- Objective: reject arbitrary logins and derive workspaces from validated immutable fixture data.
- Allowed files: `shared/onecall-business.js`, `shared/qa/business.test.js`, `shared/README.md`, generated prototype HTML sections/previews, S1 implementation/review artifacts.
- Changes: add the exact contract above; keep `BUSINESS_FIELDS`, snapshot/import allowlist and business transitions unchanged; regenerate inline copies.
- Validation: `node --test shared/qa/business.test.js`; `node shared/build-prototypes.js --check`.
- Required assertions: known phone/OTP and WeChat resolve exact accounts; wrong OTP/unknown/business-only phone fail; active workspaces only; disabled account/membership excluded; duplicate provider subject and duplicate active membership rejected; fixtures do not appear in business snapshots.
- Completion: all shared tests pass and generated hashes match.

## Slice S2 - Web and Mini adoption

- Objective: expose the same signed-in natural person and membership-derived workspaces across Web/Mini without mutable role escalation.
- Allowed files: `web-prototype/evidence-hub.html`, `web-prototype/qa/qa.js`, `web-prototype/README.md`, `prototype/onecall-miniapp-pages.html`, `prototype/qa/qa.js`, `prototype/README.md`, `shared/qa/cross-channel.js`, `docs/09-business-activation-and-lean-admin.md`, `docs/10-cross-channel-rules.md`, `docs/11-account-identity-and-workspaces.md`, `docs/README.md`, generated previews/screenshots, S2 implementation/review artifacts.
- Web changes:
  - resolve QR/phone login through shared functions; unknown phone cannot enter;
  - retain signed-in `accountId`, build chooser/sidebar options from `workspacesForAccount`, and reject workspace IDs not returned for that account;
  - every successful login, including session-expiry recovery, clears the prior account's workspace/role context before rebinding the authenticated account. A return route is restored only when the newly authenticated account still has the same workspace membership; otherwise the user returns to the membership-derived chooser;
  - replace `#role-select` with a read-only current membership summary; current profile/name/role/department comes from the authenticated account and selected membership;
  - settings shows account ID, verified login identities, current workspace/member role, and business numbers in a separate block labelled “not login identities”;
  - preserve existing personal/enterprise authorization behavior while migrating 陈雨/周宁 subject owners.
- Mini changes:
  - resolve the demo WeChat identity to `ACCOUNT-DEMO-001` during initialization;
  - add a reachable account/security page showing the natural-person account, login identities, available workspaces and separate business numbers; update the visible page count intentionally;
  - expose a read-only QA account-context hook; number binding/claim paths never mutate login identities.
- Cross-channel changes: assert the same 李晓 account ID on Mini/Web, account fixtures are not transferable business snapshot state, and existing IVR claim, fixed-line activation and four authorization revocations still pass with explicit per-record owners. Add a Web regression in which an expired admin session is reauthenticated as a normal member and cannot inherit the prior admin workspace role.
- Validation: `powershell -ExecutionPolicy Bypass -File prototype/qa/run.ps1`; `powershell -ExecutionPolicy Bypass -File web-prototype/qa/run.ps1`; `node shared/qa/cross-channel.js`; existing operations QA as regression; responsive screenshots at existing target widths.
- Completion: all suites pass with no browser/page errors; Web cannot arbitrarily change role; business-only numbers cannot log in; account/security views render without overflow.

## Goal alignment and sequencing

- S1 establishes success signals 1, 2 and 5 before any UI depends on them.
- S2 establishes success signals 3 and 4, and revalidates signal 5 across existing flows.
- Two behavioral slices are the minimum useful split: S2 cannot safely precede the shared contract, while splitting Web and Mini would leave a temporarily inconsistent cross-channel behavior and add review cost without reducing implementation risk.

## Documentation and completion report

- Add `docs/11-account-identity-and-workspaces.md` with recommended production table responsibilities, login flow, phone rules and explicit employee-domain separation. Update existing docs only where a current statement would conflict.
- Final report must name changed behavior, validation commands, review artifacts, residual risks/owners, local commits, and the missing-remote limitation. Static prototypes are delivered as local preview file links; no dev server is needed.

## Open questions

- None blocking. Real credential storage, organization-owned subject schema and server-side authorization remain the classified follow-up work units `R1-R3`, not hidden implementation scope.

