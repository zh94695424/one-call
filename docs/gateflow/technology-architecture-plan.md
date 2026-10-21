# Technology stack and system architecture — plan

## Goal and alignment

Create one production-target architecture document that translates the approved product boundaries into concrete implementation choices. It must cover every client and trust boundary, keep operator-room IVR separate from customer/operations access, and distinguish current prototypes from proposed production components. This maps directly to the confirmed success signal; no runtime behavior changes.

## Design decisions

- Use native WeChat Mini Program with TypeScript; use Vue 3 + TypeScript + Vite for customer Web, public share page and operations admin, with separate applications sharing only design tokens, API types and pure utilities.
- Use Java 21 LTS, Spring Boot 3.x and Spring Modulith for the business core. Keep transactional ownership in one PostgreSQL database while enforcing module/package boundaries. Run API, asynchronous Worker and Scheduler as separate deployable processes from the same versioned codebase.
- Use PostgreSQL as the sole transactional source of truth; Redis only for sessions, OTP/rate limits and short-lived playback/session state; RabbitMQ plus transactional Outbox for asynchronous delivery; encrypted China-region object storage with versioning/WORM for evidence media.
- Use FreeSWITCH in the operator data center for SIP/RTP, prompt playback, DTMF, recording and hangup control. A narrow IVR gateway authenticates to the business API with mTLS and request signatures. The carrier supplies line/network/room hosting only and never owns ONECALL activation, trial, order or entitlement state.
- Use a Python media/AI Worker with FFmpeg only where audio/AI libraries require it. It consumes versioned jobs and cannot write core order, entitlement or evidence lifecycle state directly.
- Deploy initially on hardened VMs or a managed container service with Docker; Kubernetes is an evolution option, not a launch dependency.

## S1 — architecture document and navigation

- Allowed files: `docs/12-technology-stack-and-system-architecture.md`, `docs/README.md`, `docs/03-technical-solution.md`, and this work unit's gate/review artifacts.
- Document status taxonomy: `已确定` for approved product facts, `推荐` for the proposed implementation stack, `待确认` for vendor/legal/capacity choices.
- Include: stack matrix, logical architecture, deployment/network topology, deployable units, module responsibilities and forbidden dependencies, key data domains, API/auth boundaries, four critical flows (IVR trial/evidence, claim/login, purchase/entitlement, public recording share), consistency/idempotency rules, security, observability, testing, delivery stages, scaling triggers and explicit exclusions.
- Correct the architecture vocabulary: use `ServiceActivation` / internal activation instead of carrier provisioning. Any legacy `CarrierProvisioning` text remains historical only and must point readers to the new document.
- Keep support tickets, refunds and invoices offline this phase. Do not invent workflow services for them.
- Validation: Markdown links resolve; Mermaid blocks are syntactically balanced by inspection; required sections and business terms exist; forbidden claims such as carrier-owned business activation or mandatory Kubernetes do not appear as current design; `git diff --check` passes.
- Completion: a reviewer can identify the selected technology for each layer, each system's source of truth, every external trust boundary, and the first implementation/deployment shape without referring to prototype code.

## Risks and documentation decision

One slice is sufficient because the deliverable is a coherent architecture artifact; splitting by frontend/backend/IVR would create contradictory partial documents and add review cost without an independently shippable behavior. Vendor versions are expressed as supported major/LTS families and pinned later in dependency manifests. R1—R3 remain assigned in control and will be repeated as unresolved production inputs.

Final report will link the architecture document, summarize the selected launch shape and list validation plus unresolved decisions.
