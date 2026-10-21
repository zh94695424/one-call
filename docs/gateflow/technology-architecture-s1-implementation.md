# Technology stack and system architecture — S1 implementation

- Gate: implementation S1.
- Work unit: `technology-stack-system-architecture`.
- Scope: production-target stack/architecture document and documentation navigation only.
- Changed files: `docs/12-technology-stack-and-system-architecture.md`, `docs/README.md`, `docs/03-technical-solution.md`.
- Delivered: concrete frontend/backend/IVR/media/data/infra stack; logical and deployment diagrams; deployable units; module ownership; account/auth boundaries; IVR, claim, purchase, share and evidence flows; consistency, security, observability, recovery, test, delivery, evolution and open-decision guidance.
- Correctness decision: PostgreSQL plus Outbox owns durable state and publication intent; API/Scheduler do not publish directly to RabbitMQ. IVR audio allowance is atomically reserved per `callId`, then settled/released idempotently; Python media/AI results return through versioned messages and never write core transaction state directly.
- Business alignment: carrier ownership is limited to communication carriage and machine-room conditions; ONECALL owns activation, trial, order and entitlement. IVR payment, online support/refund/invoice workflows, PBX and custom operations roles remain excluded.
- Validation passed: all local Markdown links in the three changed docs resolve; exactly two balanced Mermaid flowchart blocks exist; all required architecture sections are present; terminology scan confirms current design uses internal `ServiceActivation`; `git diff --check` passes. Review fixes add explicit concurrent IVR reservation/finalization and remove direct API/Scheduler MQ publishing.
- Residual risks: R1 IVR/line verification, R2 infrastructure/vendor selection and R3 SLO/capacity remain assigned in control to later work units. No unclassified risk.
- Completion status: S1 complete; accepted findings fixed and re-reviewed in `docs/reviews/code-review-20261021-153413.md`.
