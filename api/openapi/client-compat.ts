// Compile-time compatibility fixture for the generated ONECALL v1 types.
//
// This file imports only the generated declaration file. It asserts the exact
// S3 cumulative 49-operation union, five-surface path entries, 59-code Problem
// registry and sensitive field/secret exclusions. It does not create, publish
// or exercise a runtime client.

import type { components, operations, paths } from '../../tmp/openapi-client/onecall-v1';

type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;
type Expect<T extends true> = T;

type ExpectedOperationId =
  // S1
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
  | 'getOperationsIvrSession'
  // S2 customer C9-C12
  | 'listCustomerCatalogOffers'
  | 'createCustomerOrder'
  | 'listCustomerOrders'
  | 'getCustomerOrder'
  // S2 operations O8-O21
  | 'listOperationsCatalogProducts'
  | 'getOperationsCatalogProduct'
  | 'publishOperationsCatalogVersion'
  | 'offsaleOperationsCatalogVersion'
  | 'listOperationsTrialPolicies'
  | 'publishOperationsTrialPolicy'
  | 'disableOperationsTrialPolicy'
  | 'listOperationsOrders'
  | 'getOperationsOrder'
  | 'listOperationsAuditFacts'
  | 'createOperationsCatalogVersionDraft'
  | 'replaceOperationsCatalogVersionDraft'
  | 'createOperationsTrialPolicyDraft'
  | 'replaceOperationsTrialPolicyDraft'
  // S3 customer C6-C8 / C15-C18
  | 'createCustomerOutboundCallTask'
  | 'getCustomerOutboundCallTask'
  | 'cancelCustomerOutboundCallTask'
  | 'createCustomerShare'
  | 'listCustomerShares'
  | 'revokeCustomerShare'
  | 'rotateCustomerShareCode'
  // S3 ivr I1-I6
  | 'resolveIvrInbound'
  | 'confirmIvrActivation'
  | 'authorizeIvrDirectOutbound'
  | 'ingestIvrCallEvent'
  | 'declareIvrRecording'
  | 'completeIvrRecording'
  // S3 public-share P1-P3
  | 'getPublicShare'
  | 'createPublicShareAdmission'
  | 'authorizePublicShareMedia'
  // S3 provider-callback K1
  | 'receivePaymentProviderCallback';

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
  paths['/api/v1/customer/catalog/offers']['get'],
  paths['/api/v1/customer/orders']['post'],
  paths['/api/v1/customer/orders']['get'],
  paths['/api/v1/customer/orders/{orderId}']['get'],
  paths['/api/v1/customer/outbound-call-tasks']['post'],
  paths['/api/v1/customer/outbound-call-tasks/{taskId}']['get'],
  paths['/api/v1/customer/outbound-call-tasks/{taskId}/cancellations']['post'],
  paths['/api/v1/customer/shares']['post'],
  paths['/api/v1/customer/shares']['get'],
  paths['/api/v1/customer/shares/{shareId}/revocations']['post'],
  paths['/api/v1/customer/shares/{shareId}/code-rotations']['post'],
];

type AssertOperationsPaths = [
  paths['/api/v1/operations/subjects']['get'],
  paths['/api/v1/operations/subjects/{subjectId}']['get'],
  paths['/api/v1/operations/subjects/{subjectId}/suspensions']['post'],
  paths['/api/v1/operations/subjects/{subjectId}/resumptions']['post'],
  paths['/api/v1/operations/activations']['get'],
  paths['/api/v1/operations/ivr-sessions']['get'],
  paths['/api/v1/operations/ivr-sessions/{sessionId}']['get'],
  paths['/api/v1/operations/catalog/products']['get'],
  paths['/api/v1/operations/catalog/products/{productId}']['get'],
  paths['/api/v1/operations/catalog/versions/{versionId}/publications']['post'],
  paths['/api/v1/operations/catalog/versions/{versionId}/offsales']['post'],
  paths['/api/v1/operations/trial-policies']['get'],
  paths['/api/v1/operations/trial-policies']['post'],
  paths['/api/v1/operations/trial-policies/{policyVersionId}/publications']['post'],
  paths['/api/v1/operations/trial-policies/{policyVersionId}/disables']['post'],
  paths['/api/v1/operations/orders']['get'],
  paths['/api/v1/operations/orders/{orderId}']['get'],
  paths['/api/v1/operations/audit']['get'],
  paths['/api/v1/operations/catalog/versions']['post'],
  paths['/api/v1/operations/catalog/versions/{versionId}/draft']['put'],
  paths['/api/v1/operations/trial-policies/{policyVersionId}/draft']['put'],
];

type AssertIvrPaths = [
  paths['/api/v1/ivr/inbound/resolve']['post'],
  paths['/api/v1/ivr/calls/{callId}/activation-confirmations']['post'],
  paths['/api/v1/ivr/calls/{callId}/outbound-authorizations']['post'],
  paths['/api/v1/ivr/calls/{callId}/events']['post'],
  paths['/api/v1/ivr/calls/{callId}/recordings']['post'],
  paths['/api/v1/ivr/calls/{callId}/recordings/{recordingId}/complete']['post'],
];

type AssertPublicSharePaths = [
  paths['/api/v1/public/shares/{capabilityToken}']['get'],
  paths['/api/v1/public/shares/{capabilityToken}/admissions']['post'],
  paths['/api/v1/public/shares/{capabilityToken}/admissions/{admissionId}/media-authorizations']['post'],
];

type AssertCallbackPaths = [paths['/api/v1/callbacks/payment-events']['post']];

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
  | 'PURCHASE_CONSENT_REQUIRED'
  | 'PURCHASE_CONSENT_STALE'
  | 'PURCHASE_CONSENT_DECLINED'
  | 'REAL_NAME_REQUIRED'
  | 'ACTIVATION_NOT_ACTIVE'
  | 'ORDER_UNKNOWN_PAYMENT_PENDING'
  | 'IDEMPOTENCY_CONFLICT'
  | 'CATALOG_DRAFT_CONFLICT'
  | 'CATALOG_DRAFT_STALE_VIEW'
  | 'TRIAL_POLICY_DRAFT_CONFLICT'
  | 'TRIAL_POLICY_DRAFT_STALE_VIEW'
  | 'CATALOG_PUBLICATION_CONFLICT'
  | 'PUBLICATION_STALE_VIEW'
  | 'TRIAL_POLICY_PUBLICATION_CONFLICT'
  | 'TRIAL_POLICY_STALE_VIEW'
  | 'RATE_LIMITED'
  | 'SERVICE_UNAVAILABLE'
  | 'INTERNAL'
  // S3 registry additions (29)
  | 'OUTBOUND_CALL_IN_PROGRESS'
  | 'OUTBOUND_TASK_CONFLICT'
  | 'OUTBOUND_TASK_NOT_CANCELLABLE'
  | 'SHARE_CREATION_CONFLICT'
  | 'SHARE_ACCESS_POLICY_UNAVAILABLE'
  | 'SHARE_RISK_BLOCKED'
  | 'SHARE_TRANSITION_CONFLICT'
  | 'SHARE_GENERATION_STALE'
  | 'SHARE_REVOKED'
  | 'IVR_ARGUMENT_INVALID'
  | 'SESSION_IDENTITY_MISMATCH'
  | 'CALL_UNKNOWN'
  | 'EVENT_ID_CONFLICT'
  | 'UPLOAD_OBSERVATION_REQUIRED'
  | 'RECORDING_IDENTITY_CONFLICT'
  | 'UPLOAD_EXPIRED'
  | 'STORAGE_UNKNOWN'
  | 'SHARE_TOKEN_INVALID'
  | 'SHARE_VERIFICATION_FAILED'
  | 'SHARE_WRONG_TENANT'
  | 'SHARE_LOCKED'
  | 'SHARE_ADMISSION_LIMIT_REACHED'
  | 'SHARE_SESSION_EXPIRED'
  | 'SHARE_EXPIRED'
  | 'SHARE_SESSION_POLICY_UNAVAILABLE'
  | 'DEPENDENCY_UNAVAILABLE'
  | 'CALLBACK_SIGNATURE_INVALID'
  | 'PAYMENT_EVENT_CONFLICT'
  | 'CALLBACK_RETRY_LATER';

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

type ForbiddenOrderCreateKey = Extract<
  keyof components['schemas']['OrderCreateRequest'],
  | 'accountId'
  | 'ownerAccountId'
  | 'ownerOrganizationId'
  | 'role'
  | 'price'
  | 'priceCents'
  | 'amount'
  | 'amountCents'
  | 'currency'
  | 'paidAt'
  | 'snapshotSha256'
  | 'versionToken'
  | 'paymentStatus'
  | 'paymentSuccess'
  | 'trialOutcome'
  | 'purchaseConsentId'
  | 'purchaseConsentEventId'
  | 'issuanceStatus'
  | 'activationStatus'
>;
type AssertOrderCreateTrustedFactsAbsent = Expect<Equal<ForbiddenOrderCreateKey, never>>;

type ForbiddenTrialDraftCreateKey = Extract<
  keyof components['schemas']['TrialPolicyDraftCreateRequest'],
  'enabled' | 'publishedAt' | 'publishedByEmployeeId' | 'campaignKey' | 'status'
>;
type AssertTrialDraftCreatePublisherAbsent = Expect<Equal<ForbiddenTrialDraftCreateKey, never>>;

type ForbiddenTrialDraftReplaceKey = Extract<
  keyof components['schemas']['TrialPolicyDraftReplaceRequest'],
  'enabled' | 'publishedAt' | 'publishedByEmployeeId' | 'campaignKey' | 'versionNo' | 'status'
>;
type AssertTrialDraftReplacePublisherAbsent = Expect<Equal<ForbiddenTrialDraftReplaceKey, never>>;

type ForbiddenCatalogDraftReplaceImmutableKey = Extract<
  keyof components['schemas']['CatalogVersionDraftReplaceRequest'],
  'productId' | 'versionNo' | 'benefitLines' | 'channels' | 'publishedAt' | 'saleStatus' | 'status'
>;
type AssertCatalogDraftReplaceImmutableAbsent = Expect<Equal<ForbiddenCatalogDraftReplaceImmutableKey, never>>;

type ForbiddenOrderKey = Extract<
  keyof components['schemas']['Order'],
  'objectKey' | 'bucket' | 'objectVersionId' | 'cryptoKeyRef' | 'providerPayload' | 'idempotencyKey' | 'requestFingerprint'
>;
type AssertOrderSensitiveFieldsAbsent = Expect<Equal<ForbiddenOrderKey, never>>;

type ForbiddenAuditFactKey = Extract<
  keyof components['schemas']['AuditFact'],
  'objectKey' | 'bucket' | 'objectVersionId' | 'cryptoKeyRef' | 'payloadCiphertext' | 'codeHash' | 'codeSalt'
>;
type AssertAuditFactSensitiveFieldsAbsent = Expect<Equal<ForbiddenAuditFactKey, never>>;

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

type AssertPaymentStatus =
  Expect<Equal<components['schemas']['Order']['paymentStatus'], 'UNPAID' | 'UNKNOWN' | 'PAID' | 'CLOSED'>>;

type AssertIssuanceStatus =
  Expect<Equal<components['schemas']['Order']['issuanceStatus'], 'NONE' | 'PENDING' | 'ISSUED' | 'FAILED' | 'MIXED'>>;

// S3: ShareCodeRotation must never carry capabilityToken (plan §5.6).
type ForbiddenShareRotationKey = Extract<
  keyof components['schemas']['ShareCodeRotation'],
  'capabilityToken'
>;
type AssertShareRotationNoCapabilityToken = Expect<Equal<ForbiddenShareRotationKey, never>>;

// S3 F-02/F-03/F-04/R-01/R-03: flattened ShareCreated and discriminated
// ShareCodeRotation with canonical wire literals on the branches.
type AssertShareCreatedCapabilityTokenRequired =
  Expect<Equal<undefined extends components['schemas']['ShareCreated']['capabilityToken'] ? false : true, true>>;
type AssertShareCreatedExtractionCodeOptional =
  Expect<Equal<undefined extends components['schemas']['ShareCreated']['extractionCode'] ? true : false, true>>;
type AssertShareCreatedNoCreatedAt =
  Expect<Equal<'createdAt' extends keyof components['schemas']['ShareCreated'] ? true : false, false>>;
type AssertShareSummaryNoCreatedAt =
  Expect<Equal<'createdAt' extends keyof components['schemas']['ShareSummary'] ? true : false, false>>;
type ShareCodeRotationRotated = components['schemas']['ShareCodeRotationRotated'];
type ShareCodeRotationReplayed = components['schemas']['ShareCodeRotationReplayed'];
type ShareCodeRotation = components['schemas']['ShareCodeRotation'];
type AssertShareRotationRotatedExtractionCodeRequired =
  Expect<Equal<undefined extends ShareCodeRotationRotated['extractionCode'] ? false : true, true>>;
type AssertShareRotationReplayedExtractionCodeForbidden =
  Expect<Equal<'extractionCode' extends keyof ShareCodeRotationReplayed ? true : false, false>>;
// R-01: canonical wire discriminator values are generated on the closed
// branches (not component-name literals) and the wrapper is inhabitable as
// exactly the branch union with no duplicate envelope intersection.
type AssertShareRotationRotatedResultLiteral =
  Expect<Equal<ShareCodeRotationRotated['result'], 'ROTATED'>>;
type AssertShareRotationReplayedResultLiteral =
  Expect<Equal<ShareCodeRotationReplayed['result'], 'REPLAYED'>>;
type AssertShareRotationWrapperIsBranchUnion =
  Expect<Equal<ShareCodeRotation, ShareCodeRotationRotated | ShareCodeRotationReplayed>>;
type AssertShareRotationWrapperNotNever =
  Expect<Equal<[ShareCodeRotation] extends [never] ? true : false, false>>;
// R-03: no unowned timestamp projection remains on the wrapper or branches
// (the F-03 shapes validator pins absence in the YAML source of truth).

// R-01 inhabitance: canonical wire values are accepted on the wrapper union;
// component-name discriminator literals and a replayed extractionCode are
// rejected at the branch types (where closure is owned).
const shareRotationRotatedFixture: ShareCodeRotation = {
  result: 'ROTATED',
  shareId: 'SHARE-20261024-001',
  verificationMode: 'EXTRACTION_CODE',
  status: 'ACTIVE',
  codeGeneration: 1,
  revocationGeneration: 0,
  extractionCode: 'CODE-ONCE-001',
};
const shareRotationReplayedFixture: ShareCodeRotation = {
  result: 'REPLAYED',
  shareId: 'SHARE-20261024-001',
  verificationMode: 'EXTRACTION_CODE',
  status: 'ACTIVE',
  codeGeneration: 1,
  revocationGeneration: 0,
};
const shareRotationReplayedWithExtractionCode: ShareCodeRotationReplayed = {
  result: 'REPLAYED',
  shareId: 'SHARE-20261024-001',
  verificationMode: 'EXTRACTION_CODE',
  status: 'ACTIVE',
  codeGeneration: 1,
  revocationGeneration: 0,
  // @ts-expect-error REPLAYED must never accept extractionCode.
  extractionCode: 'CODE-ONCE-001',
};
const shareRotationComponentNameLiteral: ShareCodeRotationRotated = {
  // @ts-expect-error component-name discriminator values are not wire values.
  result: 'ShareCodeRotationRotated',
  shareId: 'SHARE-20261024-001',
  verificationMode: 'EXTRACTION_CODE',
  status: 'ACTIVE',
  codeGeneration: 1,
  revocationGeneration: 0,
  extractionCode: 'CODE-ONCE-001',
};

// S3 F-06: OutboundCallTask.expiresAt stays non-nullable string.
type AssertOutboundTaskExpiresAtNonNullable =
  Expect<Equal<components['schemas']['OutboundCallTask']['expiresAt'], string>>;
// S3 F-06: I4 sequenceNo optional; payload present.
type AssertIvrCallEventSequenceNoOptional =
  Expect<Equal<undefined extends components['schemas']['IvrCallEventRequest']['sequenceNo'] ? true : false, true>>;
type AssertIvrCallEventPayloadRequired =
  Expect<Equal<undefined extends components['schemas']['IvrCallEventRequest']['payload'] ? false : true, true>>;

// S3 F-07: RecordingDeclarationRequest.sha256 required.
type AssertRecordingDeclarationSha256Required =
  Expect<Equal<undefined extends components['schemas']['RecordingDeclarationRequest']['sha256'] ? false : true, true>>;

// S3 F-08/R-01: IvrDecision five frozen domain fields required. The two
// routing snapshot fields are non-null per A0 §10.14; only the three
// reservation-conditional fields are nullable.
type AssertIvrDecisionRecordingSecondsRequired =
  Expect<Equal<undefined extends components['schemas']['IvrDecision']['availableRecordingSeconds'] ? false : true, true>>;
type AssertIvrDecisionBalanceAsOfRequired =
  Expect<Equal<undefined extends components['schemas']['IvrDecision']['balanceAsOf'] ? false : true, true>>;
type AssertIvrDecisionReservationIdRequired =
  Expect<Equal<undefined extends components['schemas']['IvrDecision']['reservationId'] ? false : true, true>>;
type AssertIvrDecisionAuthorizedSecondsRequired =
  Expect<Equal<undefined extends components['schemas']['IvrDecision']['authorizedRecordingSeconds'] ? false : true, true>>;
type AssertIvrDecisionAuthorizationExpiresAtRequired =
  Expect<Equal<undefined extends components['schemas']['IvrDecision']['authorizationExpiresAt'] ? false : true, true>>;
type AssertIvrDecisionRecordingSecondsNonNullable =
  Expect<Equal<components['schemas']['IvrDecision']['availableRecordingSeconds'], number>>;
type AssertIvrDecisionBalanceAsOfNonNullable =
  Expect<Equal<components['schemas']['IvrDecision']['balanceAsOf'], string>>;
type AssertIvrDecisionReservationIdNullable =
  Expect<Equal<components['schemas']['IvrDecision']['reservationId'], string | null>>;
type AssertIvrDecisionAuthorizedSecondsNullable =
  Expect<Equal<components['schemas']['IvrDecision']['authorizedRecordingSeconds'], number | null>>;
type AssertIvrDecisionAuthorizationExpiresAtNullable =
  Expect<Equal<components['schemas']['IvrDecision']['authorizationExpiresAt'], string | null>>;

// S3 F-09: I2 decision required; I3 three confirmation fields required.
type AssertIvrActivationConfirmDecisionRequired =
  Expect<Equal<undefined extends components['schemas']['IvrActivationConfirmRequest']['decision'] ? false : true, true>>;
type AssertIvrDirectAuthPromptVersionRequired =
  Expect<Equal<undefined extends components['schemas']['IvrDirectAuthorizationRequest']['promptVersion'] ? false : true, true>>;
type AssertIvrDirectAuthConfirmationKeyRequired =
  Expect<Equal<undefined extends components['schemas']['IvrDirectAuthorizationRequest']['confirmationKey'] ? false : true, true>>;
type AssertIvrDirectAuthConfirmationContentRequired =
  Expect<Equal<undefined extends components['schemas']['IvrDirectAuthorizationRequest']['confirmationContent'] ? false : true, true>>;

// S3: ShareCreateRequest / ShareCodeRotationRequest reject client secrets.
type ForbiddenShareCreateKey = Extract<
  keyof components['schemas']['ShareCreateRequest'],
  'capabilityToken' | 'extractionCode' | 'creatorAccountId' | 'workspaceId' | 'tokenHash'
>;
type AssertShareCreateSecretsAbsent = Expect<Equal<ForbiddenShareCreateKey, never>>;

type ForbiddenShareRotationRequestKey = Extract<
  keyof components['schemas']['ShareCodeRotationRequest'],
  'capabilityToken' | 'extractionCode'
>;
type AssertShareRotationRequestSecretsAbsent = Expect<Equal<ForbiddenShareRotationRequestKey, never>>;

// S3: extraction-code verification variant exposes only mode + writeOnly code.
type AssertShareVerificationExtractionCodeKeys =
  Expect<Equal<keyof components['schemas']['ShareVerificationExtractionCode'], 'mode' | 'code'>>;

// R-01: P2 verification branches carry canonical wire mode literals (not
// component-name literals), so the generated verification union is inhabitable.
type AssertShareVerificationNoneModeLiteral =
  Expect<Equal<components['schemas']['ShareVerificationNone']['mode'], 'NONE'>>;
type AssertShareVerificationExtractionCodeModeLiteral =
  Expect<Equal<components['schemas']['ShareVerificationExtractionCode']['mode'], 'EXTRACTION_CODE'>>;
type AssertShareVerificationDesignatedMobileModeLiteral =
  Expect<Equal<components['schemas']['ShareVerificationDesignatedMobile']['mode'], 'DESIGNATED_MOBILE'>>;
type AssertShareVerificationOrganizationMemberModeLiteral =
  Expect<Equal<components['schemas']['ShareVerificationOrganizationMember']['mode'], 'ORGANIZATION_MEMBER'>>;
type ShareAdmissionVerification = components['schemas']['ShareAdmissionRequest']['verification'];
const shareVerificationNoneFixture: ShareAdmissionVerification = { mode: 'NONE' };
const shareVerificationExtractionCodeFixture: ShareAdmissionVerification = {
  mode: 'EXTRACTION_CODE',
  code: 'CODE-001',
};
const shareVerificationComponentNameLiteral: ShareAdmissionVerification = {
  // @ts-expect-error component-name mode values are not wire values.
  mode: 'ShareVerificationNone',
};

// S3: PublicShareView.content / verificationChallenge stay optional.
type AssertPublicShareViewContentOptional =
  Expect<Equal<'content' extends keyof components['schemas']['PublicShareView'] ? true : false, true>>;
type AssertPublicShareViewContentNotRequired =
  Expect<
    Equal<
      'content' extends Required<components['schemas']['PublicShareView']> ? true : false,
      false
    >
  >;
type AssertPublicShareViewChallengeOptional =
  Expect<
    Equal<
      'verificationChallenge' extends Required<components['schemas']['PublicShareView']> ? true : false,
      false
    >
  >;

// S3: media/upload credentials are readable, never client-submitted.
type ForbiddenMediaAuthorizationRequestKey = Extract<
  keyof components['schemas']['MediaAuthorizationRequest'],
  'credential' | 'expiresAt'
>;
type AssertMediaAuthorizationRequestNoCredential =
  Expect<Equal<ForbiddenMediaAuthorizationRequestKey, never>>;

type ForbiddenShareAdmissionRequestKey = Extract<
  keyof components['schemas']['ShareAdmissionRequest'],
  'credential' | 'mediaAuthorization'
>;
type AssertShareAdmissionRequestNoCredential =
  Expect<Equal<ForbiddenShareAdmissionRequestKey, never>>;

type ForbiddenRecordingDeclarationRequestKey = Extract<
  keyof components['schemas']['RecordingDeclarationRequest'],
  'credential' | 'upload' | 'objectKey' | 'bucket'
>;
type AssertRecordingDeclarationRequestNoCredential =
  Expect<Equal<ForbiddenRecordingDeclarationRequestKey, never>>;

// S3: payment callback payload stays opaque.
type AssertPaymentCallbackPayloadOpaque =
  Expect<
    Equal<
      keyof components['schemas']['PaymentProviderCallbackRequest'],
      'payload'
    >
  >;

// R-02: the two approved opaque payloads are explicitly open records
// (unknown-valued), not Record<string, never>.
type AssertIvrEventPayloadOpenRecord =
  Expect<Equal<components['schemas']['IvrEventPayload'], Record<string, unknown>>>;
type AssertOpaqueProviderPayloadOpenRecord =
  Expect<Equal<components['schemas']['OpaqueProviderPayload'], Record<string, unknown>>>;
type AssertIvrEventPayloadNotNeverRecord =
  Expect<
    Equal<
      components['schemas']['IvrEventPayload'] extends Record<string, never> ? false : true,
      true
    >
  >;
type AssertOpaqueProviderPayloadNotNeverRecord =
  Expect<
    Equal<
      components['schemas']['OpaqueProviderPayload'] extends Record<string, never> ? false : true,
      true
    >
  >;
// R-02 inhabitance: non-empty string keys with unknown values are admitted on
// both opaque payloads and on the I4/K1 request payload property types.
const ivrEventPayloadInhabited: components['schemas']['IvrEventPayload'] = {
  vendorField: 'value',
  nested: { count: 1 },
};
const opaqueProviderPayloadInhabited: components['schemas']['OpaqueProviderPayload'] = {
  transactionId: 'TX-20261024-001',
  amountCents: 100,
};
const ivrCallEventPayloadInhabited: components['schemas']['IvrCallEventRequest']['payload'] = {
  vendorEventType: 'ring',
};
const paymentCallbackPayloadInhabited: components['schemas']['PaymentProviderCallbackRequest']['payload'] = {
  vendorSignature: 'sig',
};

type AssertOutboundTaskStatus =
  Expect<
    Equal<
      components['schemas']['OutboundCallTask']['status'],
      'PENDING' | 'CLAIMED' | 'REPLACED' | 'CANCELLED' | 'EXPIRED' | 'REJECTED' | 'COMPLETED' | 'FAILED'
    >
  >;

type AssertShareStatus =
  Expect<Equal<components['schemas']['ShareState']['status'], 'ACTIVE' | 'EXPIRED' | 'REVOKED' | 'LOCKED'>>;

type AssertShareVerificationMode =
  Expect<
    Equal<
      components['schemas']['ShareState']['verificationMode'],
      'NONE' | 'EXTRACTION_CODE' | 'DESIGNATED_MOBILE' | 'ORGANIZATION_MEMBER'
    >
  >;

type AssertIvrDecisionPurpose =
  Expect<
    Equal<
      components['schemas']['IvrDecision']['purpose'],
      'INBOUND_RESOLVE' | 'ACTIVATION_NEXT_ACTION' | 'DIRECT_AUTHORIZATION'
    >
  >;

type AssertIvrDecisionAction =
  Expect<
    Equal<
      components['schemas']['IvrDecision']['action'],
      | 'GUIDE_ACTIVATION'
      | 'DIAL_TASK_CALLEE'
      | 'ENTER_CALLEE'
      | 'GUIDE_PURCHASE'
      | 'REJECT'
      | 'REJECT_SUSPENDED'
      | 'REJECT_CALL_IN_PROGRESS'
      | null
    >
  >;

type AssertCallEventProcessingStatus =
  Expect<
    Equal<
      components['schemas']['CallEventReceipt']['processingStatus'],
      'APPLIED' | 'QUARANTINED' | 'IGNORED'
    >
  >;

type AssertCatalogOfferCurrency =
  Expect<Equal<components['schemas']['CatalogOffer']['currency'], 'CNY'>>;

// S3 F-05: one direct generated-type assertion that Order.currency is CNY.
type AssertOrderCurrency =
  Expect<Equal<components['schemas']['Order']['currency'], 'CNY'>>;

type AssertOrderSnapshotCurrency =
  Expect<Equal<components['schemas']['OrderSnapshotProjection']['currency'], 'CNY'>>;

type AssertOrderSummaryCurrency =
  Expect<Equal<components['schemas']['OrderSummary']['currency'], 'CNY'>>;

type AssertCatalogVersionSummaryCurrency =
  Expect<Equal<components['schemas']['CatalogVersionSummary']['currency'], 'CNY'>>;

type AssertCatalogVersionDraftCurrency =
  Expect<Equal<components['schemas']['CatalogVersionDraft']['currency'], 'CNY'>>;

type AssertCatalogVersionDraftCreateCurrency =
  Expect<Equal<components['schemas']['CatalogVersionDraftCreateRequest']['currency'], 'CNY'>>;

type AssertCatalogVersionDraftReplaceCurrency =
  Expect<Equal<components['schemas']['CatalogVersionDraftReplaceRequest']['currency'], 'CNY'>>;

type AssertMoneyCurrency =
  Expect<Equal<components['schemas']['Money']['currency'], 'CNY'>>;

type AssertCatalogOfferType =
  Expect<
    Equal<
      components['schemas']['CatalogOffer']['offerType'],
      'SUBSCRIPTION' | 'ONE_TIME' | 'ADD_ON' | 'QUOTE'
    >
  >;

type AssertCatalogBenefitType =
  Expect<
    Equal<
      components['schemas']['CatalogBenefitLine']['entitlementType'],
      'RECORDING_SECONDS' | 'AI_SECONDS' | 'EVIDENCE_COUNT' | 'STORAGE_BYTES'
    >
  >;

type AssertCatalogDraftSaleStatus =
  Expect<Equal<components['schemas']['CatalogVersionDraft']['saleStatus'], 'DRAFT'>>;

type AssertCatalogDraftPublishedNull =
  Expect<Equal<components['schemas']['CatalogVersionDraft']['publishedAt'], null>>;

type AssertTrialDraftEnabledFalse =
  Expect<Equal<components['schemas']['TrialPolicyDraft']['enabled'], false>>;

type AssertTrialDraftPublishedNull =
  Expect<Equal<components['schemas']['TrialPolicyDraft']['publishedAt'], null>>;

type AssertTrialDraftCampaign =
  Expect<Equal<components['schemas']['TrialPolicyDraft']['campaignKey'], 'FIRST-TRIAL'>>;

type AssertCatalogVersionSaleStatus =
  Expect<
    Equal<
      components['schemas']['CatalogVersionState']['saleStatus'],
      'DRAFT' | 'ACTIVE' | 'OFFSALE'
    >
  >;

type AssertOrderCreateDecision =
  Expect<Equal<components['schemas']['OrderCreateRequest']['decision'], 'ACCEPT'>>;

type AssertCatalogVersionPublicationRequiredIdentity =
  Expect<
    Equal<
      Required<Pick<components['schemas']['CatalogVersionPublicationRequest'], 'expectedActiveVersionId' | 'expectedRevision'>>['expectedActiveVersionId'],
      string | null
    >
  >;

type AssertTrialPolicyPublicationRequiredIdentity =
  Expect<
    Equal<
      Required<Pick<components['schemas']['TrialPolicyPublicationRequest'], 'expectedEnabledPolicyVersionId' | 'expectedRevision'>>['expectedEnabledPolicyVersionId'],
      string | null
    >
  >;

type AssertRedactedAuditSnapshotKeys =
  Expect<Equal<keyof components['schemas']['RedactedAuditSnapshot'], 'status' | 'revision' | 'reasonCode'>>;

type AssertAuditFactSnapshotNullable =
  Expect<
    Equal<
      components['schemas']['AuditFact']['beforeSnapshot'],
      components['schemas']['RedactedAuditSnapshot'] | null | undefined
    >
  >;

type AssertCatalogProductVersionSummaryKeys =
  Expect<
    Equal<
      keyof components['schemas']['CatalogProductVersionSummary'],
      'catalogVersionId' | 'versionNo' | 'saleStatus' | 'offerType' | 'audience'
    >
  >;

type AssertCatalogProductSummaryVersions =
  Expect<
    Equal<
      components['schemas']['CatalogProductSummary']['versions'],
      components['schemas']['CatalogProductVersionSummary'][]
    >
  >;

// S2 fix 2: the ambiguous singular CatalogVersionDraft.versionToken is
// absent from the public schema (no A0 version-level owner, no channel
// association). The required CatalogOffer.versionToken stays because C9
// offer selection needs it: an optional or removed offer token would widen
// the indexed-access type beyond plain string and fail this assertion.
type ForbiddenCatalogDraftTokenKey = Extract<
  keyof components['schemas']['CatalogVersionDraft'],
  'versionToken'
>;
type AssertCatalogVersionDraftVersionTokenAbsent =
  Expect<Equal<ForbiddenCatalogDraftTokenKey, never>>;

type AssertCatalogOfferVersionTokenRetained =
  Expect<Equal<components['schemas']['CatalogOffer']['versionToken'], string>>;

// F-05 adversarial negatives: trial draft create/replace reject commercial facts.
// Excess-property errors are reported on the offending property line, so the
// TypeScript suppress directive must sit immediately above that property.
const trialDraftCreateWithPrice: components['schemas']['TrialPolicyDraftCreateRequest'] = {
  versionNo: 1,
  grantedSeconds: 60,
  validDays: 7,
  // @ts-expect-error TrialPolicyDraftCreateRequest must not accept priceCents.
  priceCents: 100,
};
const trialDraftCreateWithCurrency: components['schemas']['TrialPolicyDraftCreateRequest'] = {
  versionNo: 1,
  grantedSeconds: 60,
  validDays: 7,
  // @ts-expect-error TrialPolicyDraftCreateRequest must not accept currency.
  currency: 'USD',
};
const trialDraftReplaceWithPrice: components['schemas']['TrialPolicyDraftReplaceRequest'] = {
  expectedRevision: 1,
  grantedSeconds: 120,
  validDays: 14,
  // @ts-expect-error TrialPolicyDraftReplaceRequest must not accept priceCents.
  priceCents: 100,
};
const trialDraftReplaceWithCurrency: components['schemas']['TrialPolicyDraftReplaceRequest'] = {
  expectedRevision: 1,
  grantedSeconds: 120,
  validDays: 14,
  // @ts-expect-error TrialPolicyDraftReplaceRequest must not accept currency.
  currency: 'USD',
};

// F-03 negatives: expected identity is required (nullable, not optional).
// @ts-expect-error expectedActiveVersionId is required on CatalogVersionPublicationRequest.
const catalogPublishMissingIdentity: components['schemas']['CatalogVersionPublicationRequest'] = {
  expectedRevision: 1,
};
// @ts-expect-error expectedEnabledPolicyVersionId is required on TrialPolicyPublicationRequest.
const trialPublishMissingIdentity: components['schemas']['TrialPolicyPublicationRequest'] = {
  expectedRevision: 1,
};

// F-04 negatives: open snapshot extras are rejected by the closed component.
const openSnapshotExtra: components['schemas']['RedactedAuditSnapshot'] = {
  status: 'ACTIVE',
  // @ts-expect-error RedactedAuditSnapshot admits only status/revision/reasonCode.
  providerPayload: { raw: true },
};

// S2 fix 2 negative: the removed CatalogVersionDraft.versionToken must not
// reappear; the closed component rejects the excess property.
const catalogDraftWithToken: components['schemas']['CatalogVersionDraft'] = {
  catalogVersionId: '00000000-0000-4000-8000-000000000006',
  productId: '00000000-0000-4000-8000-000000000005',
  versionNo: 2,
  saleStatus: 'DRAFT',
  offerType: 'SUBSCRIPTION',
  audience: 'PERSONAL',
  priceCents: 9900,
  currency: 'CNY',
  billingCycle: 'MONTHLY',
  installments: 1,
  grantIntervalMonths: 1,
  benefitLines: [
    {
      entitlementType: 'RECORDING_SECONDS',
      quantity: 600,
      validityDays: 30,
      grantIntervalMonths: 1,
    },
  ],
  channels: ['MINIAPP'],
  revision: 1,
  createdAt: '2026-10-24T00:00:00Z',
  publishedAt: null,
  // @ts-expect-error versionToken is not a public CatalogVersionDraft field.
  versionToken: 'removed-draft-token',
};

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

const orderCreateRequest: components['schemas']['OrderCreateRequest'] = {
  subjectId: '00000000-0000-4000-8000-000000000003',
  catalogOfferId: '00000000-0000-4000-8000-000000000004',
  channel: 'MINIAPP',
  decision: 'ACCEPT',
  policyCode: 'PURCHASE_CONSENT',
  policyVersion: 'v1',
  promptVersion: 'v1',
  contentSha256: '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef',
};

// Compile-time negatives: explicit consent is ACCEPT-only and required.
const orderCreateDecline: components['schemas']['OrderCreateRequest'] = {
  ...orderCreateRequest,
  // @ts-expect-error decision is fixed to ACCEPT; DECLINE is not order creation.
  decision: 'DECLINE',
};
// @ts-expect-error decision is required.
const orderCreateMissingDecision: components['schemas']['OrderCreateRequest'] = {
  subjectId: orderCreateRequest.subjectId,
  catalogOfferId: orderCreateRequest.catalogOfferId,
  channel: orderCreateRequest.channel,
  policyCode: orderCreateRequest.policyCode,
  policyVersion: orderCreateRequest.policyVersion,
  promptVersion: orderCreateRequest.promptVersion,
  contentSha256: orderCreateRequest.contentSha256,
};
// @ts-expect-error contentSha256 is required.
const orderCreateMissingContent: components['schemas']['OrderCreateRequest'] = {
  subjectId: orderCreateRequest.subjectId,
  catalogOfferId: orderCreateRequest.catalogOfferId,
  channel: orderCreateRequest.channel,
  decision: orderCreateRequest.decision,
  policyCode: orderCreateRequest.policyCode,
  policyVersion: orderCreateRequest.policyVersion,
  promptVersion: orderCreateRequest.promptVersion,
};
const orderCreateWithCurrency: components['schemas']['OrderCreateRequest'] = {
  ...orderCreateRequest,
  // @ts-expect-error currency is not a request field on OrderCreateRequest.
  currency: 'USD',
};
const orderCreateWithConsentEvent: components['schemas']['OrderCreateRequest'] = {
  ...orderCreateRequest,
  // @ts-expect-error purchaseConsentEventId is server-derived and never client-supplied.
  purchaseConsentEventId: 'purchase-consent:abc',
};

const catalogVersionDraftCreateRequest: components['schemas']['CatalogVersionDraftCreateRequest'] = {
  productId: '00000000-0000-4000-8000-000000000005',
  versionNo: 2,
  offerType: 'SUBSCRIPTION',
  audience: 'PERSONAL',
  priceCents: 9900,
  currency: 'CNY',
  billingCycle: 'MONTHLY',
  installments: 1,
  grantIntervalMonths: 1,
  benefitLines: [
    {
      entitlementType: 'RECORDING_SECONDS',
      quantity: 600,
      validityDays: 30,
      grantIntervalMonths: 1,
    },
  ],
  channels: ['MINIAPP'],
};

const trialPolicyDraftCreateRequest: components['schemas']['TrialPolicyDraftCreateRequest'] = {
  versionNo: 1,
  grantedSeconds: 60,
  validDays: 7,
};

const trialPolicyDraftReplaceRequest: components['schemas']['TrialPolicyDraftReplaceRequest'] = {
  expectedRevision: 1,
  grantedSeconds: 120,
  validDays: 14,
};

// Keep the fixtures type-checked without emitting or using them at runtime.
export type ClientCompatibilityFixtures = [
  typeof problem,
  typeof claimRequest,
  typeof activationRequest,
  typeof transitionRequest,
  typeof orderCreateRequest,
  typeof catalogVersionDraftCreateRequest,
  typeof trialPolicyDraftCreateRequest,
  typeof trialPolicyDraftReplaceRequest,
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
  AssertOrderCreateTrustedFactsAbsent,
  AssertTrialDraftCreatePublisherAbsent,
  AssertTrialDraftReplacePublisherAbsent,
  AssertCatalogDraftReplaceImmutableAbsent,
  AssertOrderSensitiveFieldsAbsent,
  AssertAuditFactSensitiveFieldsAbsent,
  AssertClaimVerificationMethod,
  AssertActivationDecision,
  AssertActivationStatus,
  AssertBusinessNumberActivationStatus,
  AssertTrialOutcome,
  AssertEvidenceStatus,
  AssertSubjectRiskState,
  AssertTransitionDomain,
  AssertPaymentStatus,
  AssertIssuanceStatus,
  AssertCatalogOfferCurrency,
  AssertOrderCurrency,
  AssertIvrPaths,
  AssertPublicSharePaths,
  AssertCallbackPaths,
  AssertShareRotationNoCapabilityToken,
  AssertShareCreatedCapabilityTokenRequired,
  AssertShareCreatedExtractionCodeOptional,
  AssertShareCreatedNoCreatedAt,
  AssertShareSummaryNoCreatedAt,
  AssertShareRotationRotatedExtractionCodeRequired,
  AssertShareRotationReplayedExtractionCodeForbidden,
  AssertShareRotationRotatedResultLiteral,
  AssertShareRotationReplayedResultLiteral,
  AssertShareRotationWrapperIsBranchUnion,
  AssertShareRotationWrapperNotNever,
  AssertOutboundTaskExpiresAtNonNullable,
  AssertIvrCallEventSequenceNoOptional,
  AssertIvrCallEventPayloadRequired,
  AssertRecordingDeclarationSha256Required,
  AssertIvrDecisionRecordingSecondsRequired,
  AssertIvrDecisionBalanceAsOfRequired,
  AssertIvrDecisionReservationIdRequired,
  AssertIvrDecisionAuthorizedSecondsRequired,
  AssertIvrDecisionAuthorizationExpiresAtRequired,
  AssertIvrDecisionRecordingSecondsNonNullable,
  AssertIvrDecisionBalanceAsOfNonNullable,
  AssertIvrDecisionReservationIdNullable,
  AssertIvrDecisionAuthorizedSecondsNullable,
  AssertIvrDecisionAuthorizationExpiresAtNullable,
  AssertIvrActivationConfirmDecisionRequired,
  AssertIvrDirectAuthPromptVersionRequired,
  AssertIvrDirectAuthConfirmationKeyRequired,
  AssertIvrDirectAuthConfirmationContentRequired,
  AssertShareCreateSecretsAbsent,
  AssertShareRotationRequestSecretsAbsent,
  AssertShareVerificationExtractionCodeKeys,
  AssertShareVerificationNoneModeLiteral,
  AssertShareVerificationExtractionCodeModeLiteral,
  AssertShareVerificationDesignatedMobileModeLiteral,
  AssertShareVerificationOrganizationMemberModeLiteral,
  AssertPublicShareViewContentOptional,
  AssertPublicShareViewContentNotRequired,
  AssertPublicShareViewChallengeOptional,
  AssertMediaAuthorizationRequestNoCredential,
  AssertShareAdmissionRequestNoCredential,
  AssertRecordingDeclarationRequestNoCredential,
  AssertPaymentCallbackPayloadOpaque,
  AssertIvrEventPayloadOpenRecord,
  AssertOpaqueProviderPayloadOpenRecord,
  AssertIvrEventPayloadNotNeverRecord,
  AssertOpaqueProviderPayloadNotNeverRecord,
  AssertOutboundTaskStatus,
  AssertShareStatus,
  AssertShareVerificationMode,
  AssertIvrDecisionPurpose,
  AssertIvrDecisionAction,
  AssertCallEventProcessingStatus,
  AssertOrderSnapshotCurrency,
  AssertOrderSummaryCurrency,
  AssertCatalogVersionSummaryCurrency,
  AssertCatalogVersionDraftCurrency,
  AssertCatalogVersionDraftCreateCurrency,
  AssertCatalogVersionDraftReplaceCurrency,
  AssertMoneyCurrency,
  AssertCatalogOfferType,
  AssertCatalogBenefitType,
  AssertCatalogDraftSaleStatus,
  AssertCatalogDraftPublishedNull,
  AssertTrialDraftEnabledFalse,
  AssertTrialDraftPublishedNull,
  AssertTrialDraftCampaign,
  AssertCatalogVersionSaleStatus,
  AssertOrderCreateDecision,
  AssertCatalogVersionPublicationRequiredIdentity,
  AssertTrialPolicyPublicationRequiredIdentity,
  AssertRedactedAuditSnapshotKeys,
  AssertAuditFactSnapshotNullable,
  AssertCatalogProductVersionSummaryKeys,
  AssertCatalogProductSummaryVersions,
  AssertCatalogVersionDraftVersionTokenAbsent,
  AssertCatalogOfferVersionTokenRetained,
  AssertIvrDecisionReasonCode,
};
