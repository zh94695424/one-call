# PA19A sharing evidence media read ACL final closeout

## Status

- Work unit: `PA19A-sharing-evidence-media-read-acl`.
- Completion: `final closeout pass`.
- Draft PR: `https://github.com/zh94695424/one-call/pull/29`.
- PR state at closeout: `OPEN`, `isDraft=true`, `MERGEABLE`, `CLEAN`.
- Final accepted PR-review head: `4b3dce9518ce2532e2dfd8e09644914dc7593941`.
- The work unit is not merged, approved, marked ready, or externally reviewed on GitHub.

## What changed

- Added privilege-only `V013__evidence_media_reader_role.sql`. It creates
  `evidence_media_reader_role`, grants exactly schema `USAGE` plus 15 evidence column `SELECT`
  privileges, and grants no table/function/sequence/database privilege or bucket/key/version
  column.
- Added the evidence-owned `evidence::media-read` named interface, a safe immutable
  `ResolvedMediaObject` DTO, and sealed `Found`/`Missing`/`Unavailable` outcomes.
- Added one parameterized read-only LEFT-JOIN resolver with exact evidence/artifact/media/recording
  linkage, state, metadata and cardinality validation. No row API or storage-location fact escapes.
- Added an explicitly named, profile-and-enablement-gated PostgreSQL datasource, read-only
  transaction resources and jOOQ context. Missing or invalid configuration fails closed; credentials
  are supplied externally.
- Preserved V001-V012 byte identity and the existing `sharing_admission_role` privileges. PA19
  playback, transport, signing/TTL, COS/KMS and production provisioning are not implemented here.

## Verification

- Controller full clean verification passed in the Linux Maven/Testcontainers environment against
  PostgreSQL 16.15: Surefire 22 classes / 128 methods, Failsafe 11 classes / 192 methods, combined
  33 classes / 320 methods, with zero failures, errors, or skips.
- The exact CI `source=CI=XML` inventory passes `22/128 + 11/192 = 33/320`, baseline
  `112+178=290 -> 128+192=320`, line scan `291 -> 321`, 11 exact Failsafe includes, and zero
  skips/errors/failures.
- Code review A (DeepSeek) and B (MiMo V2.6 Flash) both PASS with no material finding. Aggregate
  review A/B and PR review A/B likewise PASS with no material finding. Reviewers used identical,
  peer-free frozen inputs and submitted before Controller intake.
- Independent review probes replayed the PostgreSQL 16.15 role, 15+1 ACL, 16 `pg_shdepend`, replay,
  negative-access, sharing-preservation and query-semantics contracts.
- GitHub Actions workflow run `42445021159` passed both `contract-regressions` and `java-runtime`
  on the accepted PR-review head.

## Findings and documentation

- No accepted material correctness, stability, security, scope, goal-drift, test-strength or CI
  claim remains open.
- The two Controller SHA-transcription errors were corrected against `git rev-parse`: accepted slice
  `29b39b134fd334bf6d5c21a38e1968a3141d7ff5` and accepted deepreview
  `270f7aa802fb7623528460c8abffe1b5c3aa5d06`. Immutable reviewer artifacts retain the inputs they
  received; the live PR body uses the authoritative values.
- The only persistent whitespace diagnostics are documented Markdown hard breaks in three frozen
  plan-fix artifacts. No source, test, migration, POM or CI path has trailing whitespace.
- Documentation includes the accepted plan/fix history, implementation report, four code/aggregate
  review artifacts, two PR-review artifacts, Control and this closeout.

## Residual risks and owners

- Database/Platform/Security: provisioning the external reader credential and retaining or
  revisiting the PostgreSQL PUBLIC `CONNECT` baseline; a hardened deployment that revokes PUBLIC
  `CONNECT` needs an explicit future contract/migration decision.
- Deployment/DB Ops: post-provisioning manual V013 replay intentionally fails closed on the
  non-null password; ordinary Flyway execution does not require manual replay.
- Deployment/Platform/Security: supply a bare PostgreSQL URL plus separate credentials and enforce
  TLS at server/network level; the application URL rejects userinfo/query options.
- Evidence/Platform operational hardening: `Unavailable` is intentionally opaque with no reader
  logs/metrics, and no statement timeout is added.
- Database/Platform: current single-database `pg_shdepend` assumption remains; multi-database
  deployment needs explicit scoping/review.
- Migration authors: V013's broad PUBLIC-ACL rejection is a safe fail-closed guard that future
  migrations must account for.
- Evidence authorization and integrity: column grants are not row isolation; PA19 must authorize
  before reading, and recording/subject/hash integrity remains a later Evidence work unit.
- Later PA19: owns `Missing`/`Unavailable` playback mapping, media gateway/COS/KMS, URL/signing,
  TTL/skew/revocation and streaming/range behavior.
- Backend/QA: run-relative V001-V012 test guard is backed by Controller/reviewer raw-byte pins;
  temporary denied-role cleanup and mock/IT maintenance remain test hardening.

No residual risk is unclassified, and no blocking open question remains.

## Issue status

This work unit is not associated with a GitHub issue. No closing keyword, issue comment, issue
mutation or external PR comment was created or is required.

## Next entry after merge

After the user merges PR #29, fetch the updated `main`, create a fresh managed worktree and `codex/`
branch from that merge commit, update the frozen PA19 work unit's stale ACL-success signal to the
now-available `evidence::media-read` contract, and resume PA19 from its blocked `plan` gate. Do not
continue on the PA19A branch or inherit PA19A implementation/review authorization.
