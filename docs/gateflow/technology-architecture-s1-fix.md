# Technology stack and system architecture — S1 fix

- Gate: fix / re-review for S1; work unit: `technology-stack-system-architecture`.
- Scope: architecture document only; review record: `docs/reviews/code-review-20261021-153413.md`.
- S1-01 accepted and fixed: authorize recording through an atomic `UsageReservation` per globally unique `callId`; active reservations reduce available balance. End events settle authoritative usage and release only unused seconds; unknown duration remains reserved pending recovery. Terminal records prevent duplicate settlement/release.
- S1-02 accepted and fixed: remove direct API-to-RabbitMQ publication; API/Scheduler commit state plus Outbox, Java Worker relays events.
- S1-03 accepted and fixed: exhausted admission count rejects new sessions while preserving the last admitted session; revocation/expiration/rotation invalidate future access. Failed verification and link prefetch do not consume admission. Already buffered/issued media has a bounded revocation window.
- Documentation clarification: PostgreSQL owns business metadata, object storage owns locked media bytes.
- Validation: static tracing of concurrent/duplicate IVR calls, missing/late completion, Outbox outage recovery, last share admission and revoked/expired sessions; diagram/prose consistency. Markdown structure, links and whitespace are checked before accepted slice commit.
- Docs decision: all fixes remain within the approved production architecture deliverable; no runtime/prototype modification.
- Residual risks: R1 line/IVR behavior assigned to telephony/backend; R2 supplier/security products assigned to infrastructure/security; R3 capacity and SLO assigned to production-readiness. No unclassified risk.
- Completion: all three accepted findings fixed and re-reviewed; next gate `accepted slice commit`.
