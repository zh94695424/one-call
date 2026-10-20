# Cross-channel alignment — plan review fix

## Gate record

- Gate: `fix` in the plan review loop; work unit: `cross-channel-alignment`.
- Review inputs: `docs/reviews/plan-review-20261020-110536.md` and second review `docs/reviews/plan-review-20261020-113049.md`.
- Scope: repair accepted findings PR-001, PR-002 and PR-003 and apply the controller's open-question decisions. No implementation, re-review, commit, push or PR action was performed.
- Changed files: `docs/gateflow/cross-channel-alignment-plan.md`; this artifact `docs/gateflow/cross-channel-alignment-plan-fix.md`.
- Artifact path: `docs/gateflow/cross-channel-alignment-plan-fix.md`.

## Accepted finding status

| Finding | Final fix status | Plan correction |
| --- | --- | --- |
| PR-001 — trial consumption can mix IVR-session and quota subjects | 已修复 | Removed caller-supplied `subjectId` from `consumeTrial`; the confirmed IVR session freezes `subjectId === confirmedSubjectId`, and consumption derives the sole subject only after all references validate. Rejection occurs before grant/evidence/sequence/processedEvents mutation. Loader now validates evidence→confirmed IVR session→subject equality. Claim is explicitly forbidden from changing any IVR-session subject link or evidence record. S1 and S3 require cross-subject, unresolved and removed/replaced/duplicated subject tests with exact before/after equality of all four affected state areas. |
| PR-002 — Web S1 runner can report a false pass | 已修复 | Moved the Web runner exit-code fix, allowed file and completion gate into S1. Added a planned controlled subprocess test using a temporary failing Node script and an optional QA-script path on the QA-only runner. S3 now retains this only as regression coverage. |
| PR-003 — snapshot both requires IVR sessions and ambiguously excludes “session” | 已修复 | Defined the exact allowed top-level business snapshot fields and explicitly included `sessions: IVRSession[]`, which must round-trip with linked evidence. Replaced the ambiguous exclusion with exact employee login/credential/role, customer authentication-session and UI/widget-state exclusions. Any forbidden or arbitrary top-level key rejects the entire payload before state replacement; tests cover a dynamic IVR-session/evidence round-trip and atomic no-change rejection for every forbidden category. |

## Open-question decisions applied

- `loadReviewSnapshot` no longer says or implies a fixture-ID allowlist. Dynamic IDs are allowed when unique, structurally valid and internally referentially complete; invalid imports are rejected atomically.
- `businessSnapshot`, `loadReviewSnapshot` and scenario hooks are explicitly local demo/review test infrastructure, not production interfaces. Because the repository has no production build, their removal/disablement is assigned to a future build/release work unit before any real publication.
- Business `sessions` unambiguously means `IVRSession[]` and is allowed/required. Employee login-session state, credentials, roles, customer authentication-session state and UI/widget state are forbidden top-level fields; they are rejected rather than ignored so imports cannot silently become partial. Tests use the exact representative keys recorded in the plan and compare the complete state before/after rejection.

## Validation

- Read back the modified plan sections for contract, loader, S1 and S3 consistency.
- `rg` check confirms the old `consumeTrial({subjectId,sessionId,...})`, ambiguous “checks known IDs”, and S3-only runner-fix wording are absent.
- `rg`/readback confirms the ambiguous bare loader exclusion is absent, `sessions: IVRSession[]` is in the exact allowlist, and S1/S3 include dynamic IVR-session/evidence round-trip plus forbidden-field atomic-rejection assertions.
- `git diff --check -- docs/gateflow/cross-channel-alignment-plan.md docs/gateflow/cross-channel-alignment-plan-fix.md` passed with no output.
- No QA, generator or browser run was performed because this gate changes planning artifacts only.

## Residual risks

| Classification | Risk / destination |
| --- | --- |
| covered by later approved slice | The revised contracts and failure cases are plans until implemented: S1 owns the shared state/loader and Web runner; S3 repeats the reference checks through local hooks. |
| assigned to later work unit | Real trusted-caller binding, durable subject identity, atomic trial reservations and immutable evidence storage remain IVR/backend responsibilities. |
| assigned to later work unit | Removing/disabling local prototype QA hooks from any future production artifact belongs to build/release and is a release prerequisite. |
| existing external-state blocker | The repository still has no Git remote; this does not block local re-review or implementation, but blocks later push/PR gates. |

## Completion

Fix gate: complete. All three accepted findings are `已修复`; no accepted finding remains `未修复`, `部分修复` or `证据失效`. Next entry point is plan `re-review`, owned by the root controller.
