# PA25 PostgreSQL 16 runtime identity provisioning

## Authority and limits

This is the bounded operator handoff for the accepted PA25 S1 contract. The executable is `backend/ops/postgresql/pa25-runtime-identities.sql`, not a Flyway migration or an application initializer. It is outside the runtime JAR. V001–V013 remain frozen; there is no V014, business DDL, object ACL repair, new datasource or credential manager.

Backend owns this nonsecret contract and disposable acceptance tests. Platform/Database owns the selected cluster/database, actual superuser migrator/provisioner, protected service profile and exclusive cluster-wide administration window. Security owns credential generation, distribution, authentication policy, TLS, `pg_hba`, expiry approval and administrative audit. Operations owns deployment evidence, pool drain/restart and recovery coordination. No production connection, identity, password or endpoint is supplied here.

Role comments are trusted administrative assertions, not signatures. Anyone authorized to write the relevant shared-catalog comments can forge provenance; PostgreSQL administrative/catalog-writing authority must therefore be explicitly protected and audited. An advisory lock only coordinates cooperative executions in the same database. Platform must exclude concurrent role/catalog writers across the entire cluster, including other databases. A provider that cannot provide the frozen PostgreSQL 16 superuser authority blocks deployment; CREATEROLE alone is insufficient.

## Exact current identity map

The symbols LQ, LC and LW are private, externally approved names, not literal deployed names. There are six logins, seven effective roles, ten managed nodes and four membership edges.

| Boundary | Credential inputs | Effective identity |
| --- | --- | --- |
| Sharing query (`persistence`) | `ONECALL_DB_URL`, `ONECALL_DB_USERNAME`, `ONECALL_DB_PASSWORD` | LQ → `sharing_role` |
| Customer context (`web`) | `ONECALL_CUSTOMER_DB_URL`, `ONECALL_CUSTOMER_DB_USERNAME`, `ONECALL_CUSTOMER_DB_PASSWORD` | LC → `customer_iam_role` |
| Sharing commands (`persistence`, `ONECALL_SHARING_COMMAND_ENABLED=true`) | `ONECALL_SHARING_COMMAND_DB_URL`, `ONECALL_SHARING_COMMAND_DB_USERNAME`, `ONECALL_SHARING_COMMAND_DB_PASSWORD` | LW → `sharing_command_role` and separately `evidence_role` |
| Public share GET (command-enabled persistence) | `ONECALL_SHARING_PUBLIC_READ_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | Direct `sharing_public_read_role` |
| Admission (`persistence`, `ONECALL_SHARING_ADMISSION_DATASOURCE_ENABLED=true`) | `ONECALL_SHARING_ADMISSION_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | Direct `sharing_admission_role` |
| Evidence media read (`persistence`, `ONECALL_EVIDENCE_MEDIA_READ_DATASOURCE_ENABLED=true`) | `ONECALL_EVIDENCE_MEDIA_READ_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | Direct `evidence_media_reader_role` |

The `_USERNAME` and `_PASSWORD` suffixes in the last three rows extend that row's full `_URL` prefix. Direct usernames are fixed. Use actual Spring environment binding; do not invent dashed-name aliases. No pool may fall back to another boundary's credentials. The evidence projection reader uses the command's same transaction/connection, not a seventh datasource. Default application startup remains database-free.

All four approved edges require `ADMIN FALSE, INHERIT FALSE, SET TRUE`. No nested/reversed/duplicate-grantor/additional/inbound-login edge is accepted. In particular, do not grant `evidence_role` to `sharing_command_role`: the membership belongs to LW. Direct logins have no memberships in either direction. Session-wide SET ROLE to the same approved target is a PostgreSQL capability; the application deliberately uses SET LOCAL ROLE. Own-role/RESET/NONE restoration is allowed.

The frozen customer and evidence roles have existing write/routine capabilities. A read-only application transaction is not proof those credentials are globally SELECT-only. The command login can assume the existing evidence role's broader approved capabilities. Backend/Security retains this risk; PA25 does not narrow or widen object ACLs. The evidence media reader has no runtime current-user assertion; its fixed username, qualified pool and ACL boundary are distinct proofs.

## Preconditions and execution order

1. Platform/Database records the intended cluster/database and authorized administrator privately; verifies PostgreSQL major 16 and actual current/session superuser authority; approves protected, sanitized client/server/audit logging and an exclusive cluster-wide change window. A structurally identical wrong cluster cannot be identified by this script: independently verify service resolution.
2. The migration owner runs/validates V001 → V013 using the approved external Flyway mechanism. Require exactly thirteen successful SQL history entries, exact scripts/checksums and named module ownership. Preserve migration-era provenance for the three direct logins: V011/V012/V013 may encounter existing names during migration, so custody of the original migration process/evidence remains necessary. PA25 does not retrospectively attest their creation and does not rewrite or replay those migrations after password handoff.
3. Platform/Security approves absent LQ/LC/LW names matching `[a-z_][a-z0-9_]{0,62}`. They must be distinct and cannot be managed/system/administrator names. Provisioner runs initial `apply`, or `check` for an existing provisioned deployment. An existing compatible but unmarked name is a collision, never an adoption candidate.
4. Security generates six independent credentials and distributes them using the existing private mechanism. Use an approved non-logged process such as controlled interactive `psql \password`, with server/audit/history handling separately approved. Never put passwords/verifiers in `-v`, a URI, command-line ALTER PASSWORD, shell trace, repository `.pgpass`, report or CI artifact. The script accepts only the four nonsecret variables below; it neither reads nor changes login password verifiers.
5. Provisioner reruns `check`. Platform/Database/Security then independently prove each actual runtime login's TCP authentication, TLS/peer verification, `pg_hba` policy, current/session identities, role-switch boundary and actual application operation. Container-local fixture trust is not production authentication evidence. Production checks must exclude trust/other password-bypass paths as required by approved policy.
6. Operations records sanitized evidence and obtains the external deployment/security decisions. Synthetic success alone does not authorize deployment or establish production readiness.

Six logins are LOGIN/NOINHERIT, non-owner, NOSUPERUSER/NOCREATEDB/NOCREATEROLE/NOREPLICATION/NOBYPASSRLS, connection limit -1, no role/database-role settings or direct database grants. Validity is NULL except the frozen evidence reader's infinity. The four effective service roles remain NOLOGIN; sharing/customer/evidence retain INHERIT while command retains NOINHERIT, with NULL passwords. There is no automatic attribute demotion, expiry normalization, settings reset, ownership transfer or object ACL repair.

## Nonsecret invocation

Run from repository root with an externally approved service/authentication profile. All shell values here are externally supplied nonsecret profile/identifier names. No real values or secret-bearing example file belongs in Git.

```sh
PGSERVICE="$PA25_ADMIN_SERVICE" psql -X -w -v ON_ERROR_STOP=1 \
  -v mode=apply -v sharing_login="$PA25_SHARING_LOGIN" \
  -v customer_login="$PA25_CUSTOMER_LOGIN" -v command_login="$PA25_COMMAND_LOGIN" \
  -f backend/ops/postgresql/pa25-runtime-identities.sql

PGSERVICE="$PA25_ADMIN_SERVICE" psql -X -w -v ON_ERROR_STOP=1 \
  -v mode=check -v sharing_login="$PA25_SHARING_LOGIN" \
  -v customer_login="$PA25_CUSTOMER_LOGIN" -v command_login="$PA25_COMMAND_LOGIN" \
  -f backend/ops/postgresql/pa25-runtime-identities.sql
```

`-X` suppresses local startup commands; `-w` prevents unexpected password prompting. Retain only the exit status and fixed sanitized marker. Success is exactly `PA25_APPLY_OK logins=6 effective_roles=7 memberships=4` or `PA25_CHECK_OK logins=6 effective_roles=7 memberships=4`, after COMMIT. Failure exits nonzero with a fixed `PA25_*_REJECTED` category; provenance failures use `PA25_PROVENANCE_REJECTED`. Do not retain raw connection/authentication diagnostics or catalog rows. Client connection failures can occur before the script takes control: the external launcher/operator must sanitize them too.

## Creation-only provenance and replay

Each newly created external login receives exactly one shared role comment, in the same explicit transaction as creation and membership grants:

```text
onecall:pa25:v1:db=<database_oid>:role=<role_oid>:slot=<slot>:name=<login_name>
```

There is no whitespace/newline. Decimal OIDs have no leading zero. Slots are `sharing_query`, `customer_context`, `sharing_command`, respectively. The script constructs the marker from the current database/role catalogs and chosen validated name. All existing external markers are checked byte-for-byte using UTF-8 bytea equality, independently of collation, before any DDL, and checked again after reconciliation.

`apply` creates only absent external roles, immediately comments only those newly created roles, reaffirms already-compatible marked login flags and repairs only absent approved edges. It preserves existing marker bytes, role OIDs, compatible edge grantors, password verifiers, validity, settings and object ACLs. `check` never changes persistent state. Both modes validate history, safe attributes, ownership, all touching memberships, shared dependencies, explicit/effective ACLs, PUBLIC/current-database/default ACLs and the exact 300-tuple frozen seven-role privilege set. Locking and transaction-local GUCs stay inside the same explicit transaction. All guarded changes roll back together on rejection.

Unmarked, missing, empty, malformed, wrong-version, copied, renamed or OID-mismatched markers reject. Copying a marker between slots, roles or databases is invalid. Restore/drop-recreate changes to OIDs require external disposition. PA25 never adopts an existing name, regenerates/restamps a lost comment, accepts an operator-provided marker, or adds comments to direct-login/NOLOGIN migration roles. Protect comment-writing authority accordingly.

## Stop, recovery and rotation handoff

On any incompatibility stop rollout, retain only sanitized category/version/count evidence, and escalate to Platform/Database/Security. Do not retry with disabled guards, shared application superuser credentials or a guessed old name. A provenance rejection is not permission to manually stamp a colliding role. The script has no adoption/override/force/revoke/drop/rename mode and no destructive rollback.

Investigation and any authorized remediation occur through a separate protected external change process. Do not transfer ownership, alter object grants, delete settings, replay old migrations, drop old logins or terminate sessions as an implicit PA25 recovery action. Current-database PUBLIC CONNECT is preserved, while CREATE/TEMP and unapproved settings are rejected. Other-database/shared dependencies and strict cluster policy conflicts need explicit disposition. The script does not certify all cluster PUBLIC reachability or client connection options.

Security owns external password/credential rotation and expiry decisions; Operations owns distribution, rollback window and rolling pool drain/restart. Existing authenticated pooled sessions survive password changes. Prove new-password success, old-password denial on new connections, required TLS/authentication, pool recycling and the unchanged graph using separately approved evidence. Script rollback does not roll back external credential rotation. No rotation interval, secret manager, certificate, `pg_hba` rule or topology is invented here.

## Acceptance evidence checklist

| Evidence | Owner | Live status |
| --- | --- | --- |
| Approved intended cluster/database, exact PG16 patch/image/provider and actual superuser principal | Platform/Database | NOT EXECUTED |
| Exclusive cluster-wide administration and protected comment/catalog writers/audit | Platform/Database/Security | NOT EXECUTED |
| V001–V013 validate/checksums/history and frozen direct-login migration custody | Database | NOT EXECUTED |
| Private LQ/LC/LW mapping and six independent credential-version/distribution records | Platform/Security | NOT EXECUTED |
| Sanitized initial apply, post-handoff check and replay/no-drift evidence | Database/Operations | NOT EXECUTED |
| Six real TCP/TLS/password-authentication and wrong-password-denial proofs | Platform/Database/Security | NOT EXECUTED |
| Role/ACL/read-only/writable application probes and pool restoration | Backend/Operations | NOT EXECUTED |
| Rotation, old-password denial, session drain, recovery/rollback window | Security/Operations | NOT EXECUTED |
| Production deployment/security acceptance and residual-risk ownership | Platform/Database/Security/Operations | NOT EXECUTED |

The checked-in test class has twenty ordinary methods, each with a fresh migrated `postgres:16` fixture and synthetic UUID credentials; the image tag is inherited and unpinned. Actual image digest/patch and XML results belong in execution evidence, not inferred from the tag. The full expected inventory is 36/309 Surefire plus 14/234 Failsafe = 50/543, annotation scan 544 and fourteen Failsafe includes. These are acceptance requirements, not claims that tests or live checks have passed.

**Live acceptance NOT PERFORMED / production readiness NOT CLAIMED.**
