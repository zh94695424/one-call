# Technology stack and system architecture — aggregate fix

- Gate: aggregate fix / re-review; work unit: `technology-stack-system-architecture`.
- Scope: IVR authorization section of `docs/12-technology-stack-and-system-architecture.md`.
- Finding: A-01 from `docs/reviews/code-review-20261021-155440.md`; decision `accepted`; final status `已修复`.
- Fix: verify the current consent version and call/subject association; reject suspended activation without silently restoring it. Check current activation and entitlement validity again in the reservation transaction; expired remaining balance cannot be authorized. Terminal replay never grants a new reservation.
- Metering clarification: authoritative usage excludes ringing/menu time. Completion/recovery settles against the original reservation even after later expiration/suspension, so elapsed usage does not disappear.
- Validation: static trace of inactive/active/suspended subjects, expired trial with remaining balance, status change between confirmation and reservation, terminal replay and delayed settlement. Link/structure/whitespace checks are repeated for the final changed document.
- Docs decision: current business rules from documents 09/10 are made explicit; no runtime code, new product policy or additional workflow introduced.
- Residual risk: real line behavior (R1), supplier/security choices (R2) and capacity/recovery targets (R3) remain assigned to their later work units. No unclassified risk.
- Completion: A-01 fixed and re-reviewed; no unresolved aggregate finding.
