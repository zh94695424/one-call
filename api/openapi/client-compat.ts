// Compile-time compatibility fixture for the generated ONECALL v1 types.
//
// This file imports only the generated declaration file. It asserts the exact
// S1 operation union, key path entries, Problem code registry and sensitive
// field exclusions. It does not create, publish or exercise a runtime client.

import type { components, operations, paths } from '../../tmp/openapi-client/onecall-v1';

type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;

type ExpectedOperationId =
  | 'getCustomerMe'
  | 'listCustomerBusinessNumbers'
  | 'getCustomerSubject'
  | 'createCustomerSubjectClaim'
  | 'createCustomerSubjectActivation'
  | 'listCustomerEvidence'
  | 'getCustomerEvidence'
  | 'listOperationsSubjects'
  | 'getOperationsSubject'
  | 'suspendOperationsSubject'
  | 'resumeOperationsSubject'
  | 'listOperationsActivations'
  | 'listOperationsIvrSessions'
  | 'getOperationsIvrSession';

type ActualOperationId = keyof operations;
type AssertOperationUnionExact = Expect<Equal<ExpectedOperationId, ActualOperationId>>;

type AssertCustomerPaths = [
  paths['/api/v1/customer/me']['get'],
  paths['/api/v1/customer/business-numbers']['get'],
  paths['/api/v1/customer/subjects/{subjectId}']['get'],
  paths['/api/v1/customer/subject-claims']['post'],
  paths['/api/v1/customer/subjects/{subjectId}/activations']['post'],
  paths['/api/v1/customer/evidence']['get'],
  paths['/api/v1/customer/evidence/{evidenceId}']['get'],
];

type AssertOperationsPaths = [
  paths['/api/v1/operations/subjects']['get'],
  paths['/api/v1/operations/subjects/{subjectId}']['get'],
  paths['/api/v1/operations/subjects/{subjectId}/suspensions']['post'],
  paths['/api/v1/operations/subjects/{subjectId}/resumptions']['post'],
  paths['/api/v1/operations/activations']['get'],
  paths['/api/v1/operations/ivr-sessions']['get'],
  paths['/api/v1/operations/ivr-sessions/{sessionId}']['get'],
];

type ExpectedProblemCode =
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'VALIDATION_FAILED'
  | 'CURSOR_INVALID'
  | 'CURSOR_FILTER_MISMATCH'
  | 'CLAIM_RISK_BLOCKED'
  | 'CLAIM_REPLAY_CONFLICT'
  | 'CLAIM_CONTEXT_CONFLICT'
  | 'ACTIVATION_STATE_CONFLICT'
  | 'CONSENT_REQUIRED'
  | 'ACTIVATION_CONSENT_DECLINED'
  | 'RATE_LIMITED'
  | 'SERVICE_UNAVAILABLE'
  | 'INTERNAL';

type AssertProblemCodeExact = Expect<Equal<components['schemas']['ProblemCode'], ExpectedProblemCode>>;

type ForbiddenIvrProblemCode = Extract<
  components['schemas']['ProblemCode'],
  'TASK_CONFLICT' | 'CALL_IN_PROGRESS' | 'TASK_CLAIMED' | 'TASK_UNAVAILABLE' | 'CONFIRMATION_NOT_ALLOWED'
>;
type AssertIvrReasonsAreNotProblemCodes = Expect<Equal<ForbiddenIvrProblemCode, never>>;

type ForbiddenEvidenceKey = Extract<
  keyof components['schemas']['EvidenceDetail'],
  'objectKey' | 'objectVersionId' | 'bucket' | 'cryptoKeyRef' | 'capabilityToken' | 'codeHash' | 'codeSalt'
>;
type AssertEvidenceSensitiveFieldsAbsent = Expect<Equal<ForbiddenEvidenceKey, never>>;

type ForbiddenClaimRequestKey = Extract<
  keyof components['schemas']['ClaimRequest'],
  'accountId' | 'ownerAccountId' | 'ownerOrganizationId' | 'role' | 'workspaceRole' | 'priceCents' | 'paymentStatus'
>;
type AssertClaimRequestTrustedFactsAbsent = Expect<Equal<ForbiddenClaimRequestKey, never>>;

type ForbiddenActivationRequestKey = Extract<
  keyof components['schemas']['ActivationRequest'],
  'accountId' | 'ownerAccountId' | 'ownerOrganizationId' | 'role' | 'trialOutcome' | 'activationStatus'
>;
type AssertActivationRequestTrustedFactsAbsent = Expect<Equal<ForbiddenActivationRequestKey, never>>;

type AssertClaimVerificationMethod =
  Expect<Equal<components['schemas']['ClaimRequest']['verificationMethod'], 'SMS' | 'WECHAT_PHONE' | 'CALLER_CHALLENGE' | 'MANUAL_REVIEW'>>;

type AssertActivationDecision =
  Expect<Equal<components['schemas']['ActivationRequest']['decision'], 'ACCEPT' | 'DECLINE'>>;

type AssertActivationStatus =
  Expect<Equal<components['schemas']['ActivationResult']['activationStatus'], 'INACTIVE' | 'ACTIVE' | 'SUSPENDED'>>;

// C2 projection exactness: removing, making optional or changing the enum of
// BusinessNumber.activationStatus must fail typecheck.
type AssertBusinessNumberActivationStatus =
  Expect<Equal<components['schemas']['BusinessNumber']['activationStatus'], 'INACTIVE' | 'ACTIVE' | 'SUSPENDED'>>;

type AssertTrialOutcome =
  Expect<Equal<components['schemas']['ActivationResult']['trialOutcome'], 'GRANTED' | 'REPLAY' | 'ALREADY_GRANTED'>>;

type AssertEvidenceStatus =
  Expect<Equal<components['schemas']['EvidenceDetail']['status'], 'PENDING' | 'SOLIDIFIED' | 'QUARANTINED' | 'FAILED'>>;

type AssertSubjectRiskState =
  Expect<Equal<components['schemas']['SubjectSummary']['riskState'], 'OPEN' | 'CLEARED'>>;

type AssertTransitionDomain =
  Expect<Equal<components['schemas']['ActivationTransition']['sourceActionDomain'], 'ACTIVATION' | 'IVR_CONFIRM' | 'OPERATIONS'>>;

type AssertIvrDecisionReasonCode =
  Expect<Equal<
    components['schemas']['IvrDecisionSummary']['reasonCode'],
    | 'ACTIVATION_REQUIRED'
    | 'CONSENT_REQUIRED'
    | 'CALLER_UNTRUSTED'
    | 'SESSION_IDENTITY_MISMATCH'
    | 'CALL_IN_PROGRESS'
    | 'CONFIRMATION_NOT_ALLOWED'
    | 'ACTIVATION_SUSPENDED'
    | 'CALLER_BINDING_REVOKED'
    | 'WORKSPACE_ACCESS_REVOKED'
    | 'TASK_ACCESS_REVOKED'
    | 'ORGANIZATION_INACTIVE'
    | 'ACCOUNT_DISABLED'
    | 'CALL_STATE_UNRESOLVED'
    | 'TASK_CONFLICT'
    | 'TASK_CLAIMED'
    | 'NO_AVAILABLE_ENTITLEMENT'
    | 'NO_VALID_TASK'
    | 'TASK_EXPIRED'
    | 'TASK_UNAVAILABLE'
    | 'DIRECT_AUTHORIZED'
    | 'DATA_INTEGRITY'
  >>;

const problem: components['schemas']['Problem'] = {
  type: 'about:blank',
  title: 'Claim risk blocked',
  status: 409,
  code: 'CLAIM_RISK_BLOCKED',
  traceId: 'trace-example',
};

const claimRequest: components['schemas']['ClaimRequest'] = {
  subjectId: '00000000-0000-4000-8000-000000000001',
  bindingId: '00000000-0000-4000-8000-000000000002',
  verificationMethod: 'SMS',
  verificationProofId: 'proof-example',
};

const activationRequest: components['schemas']['ActivationRequest'] = {
  activationEventId: 'activation-event-1',
  decision: 'ACCEPT',
  policyCode: 'SERVICE_ACTIVATION',
  policyVersion: 'v1',
  promptVersion: 'v1',
  contentSha256: '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef',
};

const transitionRequest: components['schemas']['ActivationTransitionRequest'] = {
  reasonCode: 'OPERATIONS_SUSPEND',
  expectedActivationRevision: 1,
};

// Keep the fixtures type-checked without emitting or using them at runtime.
export type ClientCompatibilityFixtures = [
  typeof problem,
  typeof claimRequest,
  typeof activationRequest,
  typeof transitionRequest,
];

export type {
  AssertOperationUnionExact,
  AssertCustomerPaths,
  AssertOperationsPaths,
  AssertProblemCodeExact,
  AssertIvrReasonsAreNotProblemCodes,
  AssertEvidenceSensitiveFieldsAbsent,
  AssertClaimRequestTrustedFactsAbsent,
  AssertActivationRequestTrustedFactsAbsent,
  AssertClaimVerificationMethod,
  AssertActivationDecision,
  AssertActivationStatus,
  AssertBusinessNumberActivationStatus,
  AssertTrialOutcome,
  AssertEvidenceStatus,
  AssertSubjectRiskState,
  AssertTransitionDomain,
  AssertIvrDecisionReasonCode,
};
