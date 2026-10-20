(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root && typeof root === 'object') root.OnecallBusiness = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  const schemaVersion = 1;
  const DAY_MS = 24 * 60 * 60 * 1000;
  const BUSINESS_FIELDS = [
    'schemaVersion', 'catalog', 'rule', 'subjects', 'claims', 'trialGrants', 'sessions',
    'evidence', 'activations', 'orders', 'entitlements', 'processedEvents', 'sequence',
  ];
  const EVENT_DOMAINS = ['ACTIVATION', 'CLAIM', 'IVR_CONFIRM', 'EVIDENCE', 'ORDER', 'PAYMENT', 'ENTITLEMENT'];
  const ID_COLLECTIONS = {
    ORDER: 'orders', TRIAL: 'trialGrants', SUBJ: 'subjects', ACT: 'activations',
    EVID: 'evidence', CLAIM: 'claims', ENT: 'entitlements', IVR: 'sessions',
  };
  const FORBIDDEN_IMPORT_KEYS = new Set([
    'employees', 'employeeLoginSession', 'employeeCredentials', 'employeeRoles',
    'customerAuthSession', 'uiSession', 'widgetState', 'code',
  ]);
  const ENUMS = {
    numberType: new Set(['MOBILE', 'LANDLINE']),
    identity: new Set(['UNVERIFIED', 'VERIFIED']),
    activation: new Set(['INACTIVE', 'ACTIVE', 'SUSPENDED']),
    claimRisk: new Set(['NONE', 'CONFLICT', 'RECYCLED']),
    claimMethod: new Set(['WECHAT_PHONE', 'SMS', 'CALLER_CHALLENGE']),
    channel: new Set(['MINIAPP', 'WEB', 'IVR']),
    purchaseChannel: new Set(['MINIAPP', 'WEB']),
    sessionStatus: new Set(['RECEIVED', 'CONFIRMED', 'ABANDONED', 'REJECTED']),
    guidanceStatus: new Set(['NOT_GUIDED', 'GUIDED']),
    payment: new Set(['UNPAID', 'UNKNOWN', 'PAID', 'CLOSED']),
    entitlement: new Set(['NOT_ISSUED', 'PENDING', 'ISSUED', 'FAILED']),
    saleStatus: new Set(['DRAFT', 'ACTIVE', 'OFFSALE']),
    type: new Set(['SUBSCRIPTION', 'ONE_TIME', 'ADD_ON', 'QUOTE']),
    audience: new Set(['PERSONAL', 'ENTERPRISE']),
    billingCycle: new Set(['ONCE', 'MONTHLY', 'QUARTERLY', 'ANNUAL', 'QUOTE']),
  };

  const labels = deepFreeze({
    schemaVersion,
    consentVersion: 'ONECALL-SERVICE-2026.10',
    claim: { UNCLAIMED: '待认领', CLAIMED: '已认领', CONFLICT: '认领冲突', RECYCLED: '号码回收风险' },
    identity: { UNVERIFIED: '未实名', VERIFIED: '已实名' },
    activation: { INACTIVE: '未开通', ACTIVE: '已开通', SUSPENDED: '已停用' },
    trial: { NOT_GRANTED: '未领取', AVAILABLE: '试用中', EXHAUSTED: '已耗尽', EXPIRED: '已过期' },
    payment: { UNPAID: '待支付', UNKNOWN: '支付结果待确认', PAID: '支付成功', CLOSED: '已关闭' },
    entitlement: { NOT_ISSUED: '未发放', PENDING: '待发放', ISSUED: '已到账', FAILED: '发放失败' },
    catalog: { DRAFT: '草稿', ACTIVE: '在售', OFFSALE: '已下架' },
  });

  const support = deepFreeze({
    mode: 'OFFLINE',
    configured: false,
    contactLabel: '人工客服联系方式待配置',
    contactValue: null,
    hours: null,
    instructions: {
      consultation: '请准备业务号码、问题发生时间和问题说明，待联系方式配置后由人工客服线下承接。',
      refund: '请准备订单号、支付时间和退款原因，退款资格与处理时效由人工客服线下核验。',
      invoice: '请准备订单号、开票抬头、税号和接收信息，由人工客服线下核验并处理。',
      claim: '请准备脱敏号码、验证方式和冲突说明，由人工客服线下核验；原型不要求上传完整证件。',
    },
  });

  function rights(recordingMinutes, aiMinutes, evidenceCount, storageGB, seats, fixedLines) {
    return { recordingMinutes, aiMinutes, evidenceCount, storageGB, seats, fixedLines };
  }

  function offer(input) {
    return {
      productCode: input.productCode,
      sku: input.sku,
      versionId: `${input.sku}-V1`,
      version: 1,
      name: input.name,
      description: input.description,
      type: input.type,
      audience: input.audience,
      priceCents: input.priceCents,
      validDays: input.validDays,
      billingCycle: input.billingCycle,
      installments: input.installments,
      grantIntervalMonths: input.grantIntervalMonths,
      rights: input.rights,
      channels: ['MINIAPP', 'WEB'],
      sortOrder: input.sortOrder,
      saleStatus: 'ACTIVE',
      demo: true,
      policyStatus: 'DEMO_NOT_APPROVED',
      includedFeatures: input.includedFeatures || [],
    };
  }

  const demoCatalog = deepFreeze([
    offer({ productCode: 'PERSONAL_BASIC', sku: 'PERSONAL_BASIC_MONTHLY', name: '个人基础版（月付）', description: '个人录音与 AI 基础权益演示', type: 'SUBSCRIPTION', audience: 'PERSONAL', priceCents: 3900, validDays: 30, billingCycle: 'MONTHLY', installments: 1, grantIntervalMonths: 1, rights: rights(60, 60, 20, 5, 0, 0), sortOrder: 10 }),
    offer({ productCode: 'PERSONAL_BASIC', sku: 'PERSONAL_BASIC_QUARTERLY', name: '个人基础版（季付）', description: '按月分期发放的季度演示套餐', type: 'SUBSCRIPTION', audience: 'PERSONAL', priceCents: 10500, validDays: 90, billingCycle: 'QUARTERLY', installments: 3, grantIntervalMonths: 1, rights: rights(60, 60, 20, 5, 0, 0), sortOrder: 20 }),
    offer({ productCode: 'PERSONAL_BASIC', sku: 'PERSONAL_BASIC_ANNUAL', name: '个人基础版（年付）', description: '按月分期发放的年度演示套餐', type: 'SUBSCRIPTION', audience: 'PERSONAL', priceCents: 39000, validDays: 365, billingCycle: 'ANNUAL', installments: 12, grantIntervalMonths: 1, rights: rights(60, 60, 20, 5, 0, 0), sortOrder: 30 }),
    offer({ productCode: 'PERSONAL_PRO', sku: 'PERSONAL_PRO_MONTHLY', name: '个人进阶版（月付）', description: '个人高频录音与 AI 权益演示', type: 'SUBSCRIPTION', audience: 'PERSONAL', priceCents: 8900, validDays: 30, billingCycle: 'MONTHLY', installments: 1, grantIntervalMonths: 1, rights: rights(300, 300, 100, 20, 0, 0), sortOrder: 40 }),
    offer({ productCode: 'PERSONAL_PRO', sku: 'PERSONAL_PRO_QUARTERLY', name: '个人进阶版（季付）', description: '按月分期发放的季度演示套餐', type: 'SUBSCRIPTION', audience: 'PERSONAL', priceCents: 24000, validDays: 90, billingCycle: 'QUARTERLY', installments: 3, grantIntervalMonths: 1, rights: rights(300, 300, 100, 20, 0, 0), sortOrder: 50 }),
    offer({ productCode: 'PERSONAL_PRO', sku: 'PERSONAL_PRO_ANNUAL', name: '个人进阶版（年付）', description: '按月分期发放的年度演示套餐', type: 'SUBSCRIPTION', audience: 'PERSONAL', priceCents: 89000, validDays: 365, billingCycle: 'ANNUAL', installments: 12, grantIntervalMonths: 1, rights: rights(300, 300, 100, 20, 0, 0), sortOrder: 60 }),
    offer({ productCode: 'EVIDENCE_ONE', sku: 'EVIDENCE_ONE', name: '单次出证权益', description: '单项证据保存与出证演示', type: 'ONE_TIME', audience: 'PERSONAL', priceCents: 1290, validDays: 30, billingCycle: 'ONCE', installments: 1, grantIntervalMonths: 0, rights: rights(0, 0, 1, 0, 0, 0), sortOrder: 70, includedFeatures: ['单项证据保存'] }),
    offer({ productCode: 'CALL', sku: 'CALL_30', name: '录音 30 分钟包', description: '独立录音加量演示', type: 'ADD_ON', audience: 'PERSONAL', priceCents: 1890, validDays: 30, billingCycle: 'ONCE', installments: 1, grantIntervalMonths: 0, rights: rights(30, 0, 0, 0, 0, 0), sortOrder: 80 }),
    offer({ productCode: 'CALL', sku: 'CALL_100', name: '录音 100 分钟包', description: '独立录音加量演示', type: 'ADD_ON', audience: 'PERSONAL', priceCents: 4990, validDays: 90, billingCycle: 'ONCE', installments: 1, grantIntervalMonths: 0, rights: rights(100, 0, 0, 0, 0, 0), sortOrder: 90 }),
    offer({ productCode: 'CALL', sku: 'CALL_300', name: '录音 300 分钟包', description: '独立录音加量演示', type: 'ADD_ON', audience: 'PERSONAL', priceCents: 12800, validDays: 180, billingCycle: 'ONCE', installments: 1, grantIntervalMonths: 0, rights: rights(300, 0, 0, 0, 0, 0), sortOrder: 100 }),
    offer({ productCode: 'AI_ADDON', sku: 'AI_30', name: 'AI 30 分钟包', description: '独立 AI 加量演示', type: 'ADD_ON', audience: 'PERSONAL', priceCents: 990, validDays: 30, billingCycle: 'ONCE', installments: 1, grantIntervalMonths: 0, rights: rights(0, 30, 0, 0, 0, 0), sortOrder: 110 }),
    offer({ productCode: 'AI_ADDON', sku: 'AI_100', name: 'AI 100 分钟包', description: '独立 AI 加量演示', type: 'ADD_ON', audience: 'PERSONAL', priceCents: 2990, validDays: 90, billingCycle: 'ONCE', installments: 1, grantIntervalMonths: 0, rights: rights(0, 100, 0, 0, 0, 0), sortOrder: 120 }),
    offer({ productCode: 'AI_ADDON', sku: 'AI_300', name: 'AI 300 分钟包', description: '独立 AI 加量演示', type: 'ADD_ON', audience: 'PERSONAL', priceCents: 7900, validDays: 180, billingCycle: 'ONCE', installments: 1, grantIntervalMonths: 0, rights: rights(0, 300, 0, 0, 0, 0), sortOrder: 130 }),
    offer({ productCode: 'STORAGE', sku: 'STORAGE_5', name: '存储 5 GB 包', description: '独立存储加量演示', type: 'ADD_ON', audience: 'PERSONAL', priceCents: 990, validDays: 365, billingCycle: 'ONCE', installments: 1, grantIntervalMonths: 0, rights: rights(0, 0, 0, 5, 0, 0), sortOrder: 140 }),
    offer({ productCode: 'STORAGE', sku: 'STORAGE_10', name: '存储 10 GB 包', description: '独立存储加量演示', type: 'ADD_ON', audience: 'PERSONAL', priceCents: 1200, validDays: 30, billingCycle: 'ONCE', installments: 1, grantIntervalMonths: 0, rights: rights(0, 0, 0, 10, 0, 0), sortOrder: 150 }),
    offer({ productCode: 'STORAGE', sku: 'STORAGE_50', name: '存储 50 GB 包', description: '独立存储加量演示', type: 'ADD_ON', audience: 'PERSONAL', priceCents: 4900, validDays: 90, billingCycle: 'ONCE', installments: 1, grantIntervalMonths: 0, rights: rights(0, 0, 0, 50, 0, 0), sortOrder: 160 }),
    offer({ productCode: 'STORAGE', sku: 'STORAGE_200', name: '存储 200 GB 包', description: '独立存储加量演示', type: 'ADD_ON', audience: 'PERSONAL', priceCents: 16800, validDays: 180, billingCycle: 'ONCE', installments: 1, grantIntervalMonths: 0, rights: rights(0, 0, 0, 200, 0, 0), sortOrder: 170 }),
    offer({ productCode: 'ENTERPRISE_BASIC', sku: 'ENTERPRISE_BASIC', name: '企业基础版', description: '企业方案演示，正式权益与报价待确认', type: 'QUOTE', audience: 'ENTERPRISE', priceCents: null, validDays: 365, billingCycle: 'QUOTE', installments: 1, grantIntervalMonths: 0, rights: rights(1000, 1000, 300, 100, 5, 5), sortOrder: 180 }),
    offer({ productCode: 'ENTERPRISE_FLAGSHIP', sku: 'ENTERPRISE_FLAGSHIP', name: '企业旗舰版', description: '企业方案演示，正式权益与报价待确认', type: 'QUOTE', audience: 'ENTERPRISE', priceCents: null, validDays: 365, billingCycle: 'QUOTE', installments: 1, grantIntervalMonths: 0, rights: rights(6000, 6000, 2000, 1024, 20, 30), sortOrder: 190 }),
  ]);

  const scenarios = {
    NEW_MOBILE: { maskedCaller: '138****2039', trustedCaller: true, numberType: 'MOBILE', claimRisk: 'NONE' },
    NEW_LANDLINE: { maskedCaller: '021-****8821', trustedCaller: true, numberType: 'LANDLINE', claimRisk: 'NONE' },
    REPEAT_CALL: { maskedCaller: '139****7712', trustedCaller: true, numberType: 'MOBILE', claimRisk: 'NONE' },
    TIMEOUT: { maskedCaller: '137****4466', trustedCaller: true, numberType: 'MOBILE', claimRisk: 'NONE' },
    WRONG_KEY: { maskedCaller: '136****8820', trustedCaller: true, numberType: 'MOBILE', claimRisk: 'NONE' },
    MISSING_CALLER: { maskedCaller: null, trustedCaller: false, numberType: 'MOBILE', claimRisk: 'NONE' },
    SUSPENDED: { maskedCaller: '010-****1628', trustedCaller: true, numberType: 'LANDLINE', claimRisk: 'NONE' },
    TRIAL_DISABLED: { maskedCaller: '135****9011', trustedCaller: true, numberType: 'MOBILE', claimRisk: 'NONE' },
    EXPIRED_TRIAL: { maskedCaller: '134****2701', trustedCaller: true, numberType: 'MOBILE', claimRisk: 'NONE' },
    EXHAUSTED_TRIAL: { maskedCaller: '133****3802', trustedCaller: true, numberType: 'MOBILE', claimRisk: 'NONE' },
    CONFLICT: { maskedCaller: '132****4903', trustedCaller: true, numberType: 'MOBILE', claimRisk: 'CONFLICT' },
    RECYCLED: { maskedCaller: '131****5104', trustedCaller: true, numberType: 'MOBILE', claimRisk: 'RECYCLED' },
  };

  function createDemoState(now) {
    const at = iso(now);
    return {
      schemaVersion,
      catalog: clone(demoCatalog),
      rule: { campaignId: 'FIRST-TRIAL', version: 1, enabled: true, seconds: 300, validDays: 30 },
      subjects: [
        subject('SUBJ-REPEAT-001', scenarios.REPEAT_CALL, 'ACTIVE'),
        subject('SUBJ-SUSPENDED-001', scenarios.SUSPENDED, 'SUSPENDED'),
        subject('SUBJ-EXPIRED-001', scenarios.EXPIRED_TRIAL, 'ACTIVE'),
        subject('SUBJ-EXHAUSTED-001', scenarios.EXHAUSTED_TRIAL, 'ACTIVE'),
        subject('SUBJ-CONFLICT-001', scenarios.CONFLICT, 'ACTIVE'),
        subject('SUBJ-RECYCLED-001', scenarios.RECYCLED, 'ACTIVE'),
      ],
      claims: [],
      trialGrants: [
        { id: 'TRIAL-REPEAT-001', subjectId: 'SUBJ-REPEAT-001', campaignId: 'FIRST-TRIAL', ruleVersion: 1, grantedSeconds: 300, usedSeconds: 60, grantedAt: at, expiresAt: new Date(toMs(now) + 30 * DAY_MS).toISOString() },
        { id: 'TRIAL-EXPIRED-001', subjectId: 'SUBJ-EXPIRED-001', campaignId: 'FIRST-TRIAL', ruleVersion: 1, grantedSeconds: 300, usedSeconds: 30, grantedAt: new Date(toMs(now) - 40 * DAY_MS).toISOString(), expiresAt: new Date(toMs(now) - 10 * DAY_MS).toISOString() },
        { id: 'TRIAL-EXHAUSTED-001', subjectId: 'SUBJ-EXHAUSTED-001', campaignId: 'FIRST-TRIAL', ruleVersion: 1, grantedSeconds: 300, usedSeconds: 300, grantedAt: at, expiresAt: new Date(toMs(now) + 30 * DAY_MS).toISOString() },
      ],
      sessions: [],
      evidence: [],
      activations: [],
      orders: [],
      entitlements: [],
      processedEvents: eventStore(),
      sequence: 1000,
    };
  }

  function subject(id, fixture, activation) {
    return { id, maskedNumber: fixture.maskedCaller, numberType: fixture.numberType, source: 'IVR', accountId: null, identity: 'UNVERIFIED', activation, revision: 1, claimRisk: fixture.claimRisk };
  }

  function validateCatalogVersion(value) {
    if (!plain(value)) return fail('INVALID_TRANSITION');
    const codePattern = /^[A-Z][A-Z0-9_-]{2,39}$/;
    if (!codePattern.test(value.productCode || '') || !codePattern.test(value.sku || '')) return fail('INVALID_TRANSITION');
    if (!id(value.versionId) || !safePositive(value.version)) return fail('INVALID_TRANSITION');
    if (!textBetween(value.name, 1, 40) || !textBetween(value.description, 1, 300)) return fail('INVALID_TRANSITION');
    if (!ENUMS.type.has(value.type) || !ENUMS.audience.has(value.audience) || !ENUMS.billingCycle.has(value.billingCycle) || !ENUMS.saleStatus.has(value.saleStatus)) return fail('INVALID_TRANSITION');
    if (value.audience === 'ENTERPRISE' && (value.type !== 'QUOTE' || value.billingCycle !== 'QUOTE' || value.priceCents !== null)) return fail('INVALID_TRANSITION');
    if (value.audience === 'PERSONAL' && (value.type === 'QUOTE' || value.billingCycle === 'QUOTE' || !safeBetween(value.priceCents, 1, 9999999))) return fail('INVALID_TRANSITION');
    if (!safeBetween(value.validDays, 1, 3650) || !safePositive(value.installments) || !safeBetween(value.grantIntervalMonths, 0, 120) || !safeBetween(value.sortOrder, 0, 9999)) return fail('INVALID_TRANSITION');
    if (value.demo !== true || value.policyStatus !== 'DEMO_NOT_APPROVED') return fail('INVALID_TRANSITION');
    if (!Array.isArray(value.channels) || !value.channels.length || new Set(value.channels).size !== value.channels.length || value.channels.some(channel => !ENUMS.purchaseChannel.has(channel))) return fail('INVALID_TRANSITION');
    if (!plain(value.rights)) return fail('INVALID_TRANSITION');
    const limits = { recordingMinutes: 100000, aiMinutes: 100000, evidenceCount: 1000000, storageGB: 100000, seats: 100000, fixedLines: 100000 };
    for (const [key, max] of Object.entries(limits)) if (!safeBetween(value.rights[key], 0, max)) return fail('INVALID_TRANSITION');
    if (value.audience === 'PERSONAL' && (value.rights.seats || value.rights.fixedLines)) return fail('INVALID_TRANSITION');
    if (!Array.isArray(value.includedFeatures) || value.includedFeatures.some(item => !textBetween(item, 1, 100))) return fail('INVALID_TRANSITION');
    const quota = Object.values(value.rights).reduce((sum, current) => sum + current, 0);
    if (quota === 0 && value.includedFeatures.length === 0) return fail('INVALID_TRANSITION');
    return ok(clone(value));
  }

  function visibleOffers(state, query) {
    if (!plain(query) || !ENUMS.purchaseChannel.has(query.channel) || !ENUMS.audience.has(query.audience)) return fail('INVALID_TRANSITION');
    if (!Array.isArray(state && state.catalog)) return fail('INVALID_TRANSITION');
    const selected = state.catalog.filter(item => item.saleStatus === 'ACTIVE' && item.audience === query.audience && item.channels.includes(query.channel));
    for (const item of selected) if (!validateCatalogVersion(item).ok) return fail('INVALID_TRANSITION');
    selected.sort((a, b) => a.sortOrder - b.sortOrder || a.sku.localeCompare(b.sku));
    return ok(selected.map(item => Object.assign(clone(item), { versionToken: offerVersionToken(item) })));
  }

  function offerVersionToken(value) {
    const validation = validateCatalogVersion(value);
    if (!validation.ok) return null;
    return `OFFER-V1:${canonicalSerialize(catalogTokenValue(value))}`;
  }

  function activateService(state, input, now) {
    return mutate(state, draft => {
      if (!plain(input) || !ENUMS.purchaseChannel.has(input.channel) || !id(input.eventId)) return fail('INVALID_TRANSITION');
      const eventInput = { subjectId: input.subjectId, channel: input.channel, verified: input.verified === true, consentVersion: input.consentVersion, confirmed: input.confirmed === true };
      const replay = replayEvent(draft, 'ACTIVATION', input.eventId, eventInput);
      if (replay) return replay;
      const target = one(draft.subjects, item => item.id === input.subjectId);
      if (!target) return fail('NOT_FOUND');
      if (target.activation === 'SUSPENDED') return fail('SUSPENDED');
      if (input.confirmed !== true || !id(input.consentVersion)) return fail('CONSENT_REQUIRED');
      if (input.channel !== 'IVR' && (input.verified !== true || target.identity !== 'VERIFIED' || !target.accountId)) return fail('VERIFICATION_REQUIRED');
      const activation = { id: nextId(draft, 'ACT'), subjectId: target.id, channel: input.channel, consentVersion: input.consentVersion, confirmation: input.channel === 'IVR' ? 'KEY_1' : 'EXPLICIT', eventId: input.eventId, at: iso(now) };
      target.activation = 'ACTIVE';
      target.revision += 1;
      draft.activations.push(activation);
      const grant = grantTrialDraft(draft, target.id, now);
      const value = { activation, grant: grant.ok ? grant.value : null, grantCode: grant.code || 'OK' };
      recordEvent(draft, 'ACTIVATION', input.eventId, eventInput, value);
      return ok(value);
    });
  }

  function grantTrialOnce(state, subjectId, now) {
    return mutate(state, draft => grantTrialDraft(draft, subjectId, now));
  }

  function grantTrialDraft(draft, subjectId, now) {
    const target = one(draft.subjects, item => item.id === subjectId);
    if (!target) return fail('NOT_FOUND');
    if (target.activation === 'SUSPENDED') return fail('SUSPENDED');
    const existing = one(draft.trialGrants, item => item.subjectId === subjectId && item.campaignId === 'FIRST-TRIAL');
    if (existing) return duplicate(existing);
    if (!validRule(draft.rule)) return fail('INVALID_TRANSITION');
    if (!draft.rule.enabled) return fail('TRIAL_DISABLED');
    const grantedAt = iso(now);
    const grant = { id: nextId(draft, 'TRIAL'), subjectId, campaignId: 'FIRST-TRIAL', ruleVersion: draft.rule.version, grantedSeconds: draft.rule.seconds, usedSeconds: 0, grantedAt, expiresAt: new Date(toMs(now) + draft.rule.validDays * DAY_MS).toISOString() };
    draft.trialGrants.push(grant);
    return ok(grant);
  }

  function claimSubject(state, input, now) {
    return mutate(state, draft => {
      if (!plain(input) || !id(input.subjectId) || !id(input.accountId) || !id(input.requestId) || !plain(input.verification)) return fail('VERIFICATION_REQUIRED');
      const eventInput = { subjectId: input.subjectId, accountId: input.accountId, verification: { verified: input.verification.verified === true, subjectId: input.verification.subjectId, method: input.verification.method } };
      const replay = replayEvent(draft, 'CLAIM', input.requestId, eventInput);
      if (replay) return replay;
      const target = one(draft.subjects, item => item.id === input.subjectId);
      if (!target) return fail('NOT_FOUND');
      const verification = input.verification;
      if (verification.verified !== true || verification.subjectId !== target.id || !ENUMS.claimMethod.has(verification.method)) return fail('VERIFICATION_REQUIRED');
      const methodAllowed = target.numberType === 'MOBILE' ? ['WECHAT_PHONE', 'SMS'].includes(verification.method) : verification.method === 'CALLER_CHALLENGE';
      if (!methodAllowed) return fail('VERIFICATION_REQUIRED');
      if (target.accountId === input.accountId) {
        const existing = draft.claims.find(item => item.subjectId === target.id && item.accountId === input.accountId);
        return duplicate(existing || clone(target));
      }
      if (target.accountId && target.accountId !== input.accountId) return fail('CLAIM_CONFLICT');
      if (target.claimRisk === 'CONFLICT') return fail('CLAIM_CONFLICT');
      if (target.claimRisk === 'RECYCLED') return fail('RECYCLED_NUMBER_REVIEW');
      const claim = { id: nextId(draft, 'CLAIM'), subjectId: target.id, accountId: input.accountId, method: verification.method, verifiedAt: iso(now), requestId: input.requestId };
      draft.claims.push(claim);
      target.accountId = input.accountId;
      target.revision += 1;
      recordEvent(draft, 'CLAIM', input.requestId, eventInput, claim);
      return ok(claim);
    });
  }

  function startIvrSession(state, input, now) {
    return mutate(state, draft => {
      if (!plain(input) || !Object.prototype.hasOwnProperty.call(scenarios, input.scenario)) return fail('NOT_FOUND');
      const fixture = scenarios[input.scenario];
      const session = { id: nextId(draft, 'IVR'), subjectId: null, confirmedSubjectId: null, maskedCaller: fixture.maskedCaller, trustedCaller: fixture.trustedCaller, status: 'RECEIVED', consentVersion: labels.consentVersion, key: null, guidanceStatus: 'NOT_GUIDED', scenario: input.scenario, createdAt: iso(now) };
      draft.sessions.push(session);
      return ok(session);
    });
  }

  function confirmIvrSession(state, input, now) {
    return mutate(state, draft => {
      if (!plain(input) || !id(input.sessionId) || !id(input.eventId)) return fail('INVALID_TRANSITION');
      const eventInput = { sessionId: input.sessionId, key: String(input.key) };
      const replay = replayEvent(draft, 'IVR_CONFIRM', input.eventId, eventInput);
      if (replay) return replay;
      const session = one(draft.sessions, item => item.id === input.sessionId);
      if (!session) return fail('NOT_FOUND');
      if (session.status !== 'RECEIVED' || session.subjectId !== null || session.confirmedSubjectId !== null) return fail('INVALID_TRANSITION');
      if (!session.trustedCaller || !session.maskedCaller) return fail('VERIFICATION_REQUIRED');
      if (String(input.key) !== '1' || session.consentVersion !== labels.consentVersion) return fail('CONSENT_REQUIRED');
      const matches = draft.subjects.filter(item => item.maskedNumber === session.maskedCaller);
      if (matches.length > 1) return fail('INVALID_TRANSITION');
      let target = matches[0];
      if (!target) {
        const fixture = scenarios[session.scenario];
        target = subject(nextId(draft, 'SUBJ'), fixture, 'INACTIVE');
        draft.subjects.push(target);
      }
      if (target.activation === 'SUSPENDED') return fail('SUSPENDED');
      session.status = 'CONFIRMED';
      session.key = '1';
      session.subjectId = target.id;
      session.confirmedSubjectId = target.id;
      const activation = { id: nextId(draft, 'ACT'), subjectId: target.id, channel: 'IVR', consentVersion: session.consentVersion, confirmation: 'KEY_1', eventId: input.eventId, at: iso(now), sessionId: session.id };
      target.activation = 'ACTIVE';
      target.revision += 1;
      draft.activations.push(activation);
      const grant = grantTrialDraft(draft, target.id, now);
      const value = { session, activation, grant: grant.ok ? grant.value : null, grantCode: grant.code || 'OK' };
      recordEvent(draft, 'IVR_CONFIRM', input.eventId, eventInput, value);
      return ok(value);
    });
  }

  function consumeTrial(state, input, now) {
    return mutate(state, draft => {
      if (!plain(input) || !id(input.sessionId) || !id(input.eventId) || !safePositive(input.seconds)) return fail('INVALID_TRANSITION');
      const matches = draft.sessions.filter(item => item.id === input.sessionId);
      if (matches.length !== 1) return fail('INVALID_TRANSITION');
      const session = matches[0];
      if (session.status !== 'CONFIRMED' || !id(session.subjectId) || session.subjectId !== session.confirmedSubjectId) return fail('INVALID_TRANSITION');
      const subjects = draft.subjects.filter(item => item.id === session.confirmedSubjectId);
      if (subjects.length !== 1) return fail('INVALID_TRANSITION');
      const target = subjects[0];
      if (!allEvidenceReferencesValid(draft)) return fail('INVALID_TRANSITION');
      const eventInput = { sessionId: input.sessionId, seconds: input.seconds };
      const replay = replayEvent(draft, 'EVIDENCE', input.eventId, eventInput);
      if (replay) return replay;
      if (target.activation === 'SUSPENDED') return fail('SUSPENDED');
      const grants = draft.trialGrants.filter(item => item.subjectId === target.id && item.campaignId === 'FIRST-TRIAL');
      if (grants.length !== 1) return fail('INVALID_TRANSITION');
      const grant = grants[0];
      if (toMs(now) >= Date.parse(grant.expiresAt)) return fail('TRIAL_EXPIRED');
      const remaining = grant.grantedSeconds - grant.usedSeconds;
      if (remaining <= 0) return fail('TRIAL_EXHAUSTED');
      const recordedSeconds = Math.min(input.seconds, remaining);
      const evidenceRecord = { id: nextId(draft, 'EVID'), subjectId: session.confirmedSubjectId, sessionId: session.id, recordedSeconds, identityAtCapture: target.identity, consentVersion: session.consentVersion, createdAt: iso(now), preservationStatus: 'PRESERVED' };
      grant.usedSeconds += recordedSeconds;
      draft.evidence.push(evidenceRecord);
      const value = { evidence: evidenceRecord, grant, exhausted: grant.usedSeconds === grant.grantedSeconds, message: grant.usedSeconds === grant.grantedSeconds ? '额度耗尽，结束取证' : '已按有效录音时长扣减' };
      recordEvent(draft, 'EVIDENCE', input.eventId, eventInput, value);
      return ok(value);
    });
  }

  function createOrder(state, input, now) {
    return mutate(state, draft => {
      if (!plain(input) || !ENUMS.purchaseChannel.has(input.channel) || !id(input.subjectId) || !id(input.accountId) || !id(input.versionId) || !id(input.idempotencyKey)) return fail('INVALID_TRANSITION');
      const eventInput = { subjectId: input.subjectId, accountId: input.accountId, channel: input.channel, versionId: input.versionId, versionToken: input.versionToken, confirmed: input.confirmed === true, consentVersion: input.consentVersion };
      const replay = replayEvent(draft, 'ORDER', input.idempotencyKey, eventInput);
      if (replay) return replay;
      const target = one(draft.subjects, item => item.id === input.subjectId);
      if (!target) return fail('NOT_FOUND');
      if (target.activation === 'SUSPENDED') return fail('SUSPENDED');
      if (target.accountId !== input.accountId || target.identity !== 'VERIFIED') return fail('VERIFICATION_REQUIRED');
      if (input.confirmed !== true || !id(input.consentVersion)) return fail('CONSENT_REQUIRED');
      if (draft.orders.some(order => order.subjectId === target.id && order.payment === 'UNKNOWN')) return fail('PAYMENT_UNRESOLVED');
      const selected = one(draft.catalog, item => item.versionId === input.versionId);
      if (!selected) return fail('NOT_FOUND');
      const valid = validateCatalogVersion(selected);
      if (!valid.ok) return valid;
      if (selected.saleStatus !== 'ACTIVE' || !transactionalOffer(selected, input.channel)) return fail('NOT_SELLABLE');
      if (typeof input.versionToken !== 'string' || !input.versionToken || input.versionToken !== offerVersionToken(selected)) return fail('STALE_VERSION');
      const order = { id: nextId(draft, 'ORDER'), subjectId: target.id, accountId: input.accountId, channel: input.channel, snapshot: clone(selected), payment: 'UNPAID', entitlement: 'NOT_ISSUED', createdAt: iso(now), idempotencyKey: input.idempotencyKey, consentVersion: input.consentVersion };
      draft.orders.push(order);
      recordEvent(draft, 'ORDER', input.idempotencyKey, eventInput, order);
      return ok(order);
    });
  }

  function applyPaymentResult(state, input, now) {
    return mutate(state, draft => {
      if (!plain(input) || !id(input.orderId) || !id(input.eventId) || !ENUMS.payment.has(input.result)) return fail('INVALID_TRANSITION');
      const eventInput = { orderId: input.orderId, result: input.result };
      const replay = replayEvent(draft, 'PAYMENT', input.eventId, eventInput);
      if (replay) return replay;
      const order = one(draft.orders, item => item.id === input.orderId);
      if (!order) return fail('NOT_FOUND');
      if (order.payment === 'PAID') return input.result === 'PAID' ? duplicate(order) : fail('INVALID_TRANSITION');
      if (order.payment === 'CLOSED') return input.result === 'CLOSED' ? duplicate(order) : fail('INVALID_TRANSITION');
      const allowed = order.payment === 'UNPAID' ? ['UNKNOWN', 'PAID', 'CLOSED'] : ['UNPAID', 'PAID', 'CLOSED'];
      if (!allowed.includes(input.result)) return fail('INVALID_TRANSITION');
      order.payment = input.result;
      if (input.result === 'PAID') order.entitlement = 'PENDING';
      recordEvent(draft, 'PAYMENT', input.eventId, eventInput, order);
      return ok(order);
    });
  }

  function issueEntitlements(state, input, now) {
    return mutate(state, draft => {
      if (!plain(input) || !id(input.orderId) || !id(input.eventId)) return fail('INVALID_TRANSITION');
      const eventInput = { orderId: input.orderId };
      const replay = replayEvent(draft, 'ENTITLEMENT', input.eventId, eventInput);
      if (replay) return replay;
      const order = one(draft.orders, item => item.id === input.orderId);
      if (!order) return fail('NOT_FOUND');
      if (!transactionalOffer(order.snapshot, order.channel)) return fail('NOT_SELLABLE');
      const existing = one(draft.entitlements, item => item.orderId === order.id);
      if (existing) return duplicate(existing);
      if (order.payment !== 'PAID' || !['PENDING', 'FAILED'].includes(order.entitlement)) return fail('INVALID_TRANSITION');
      const entitlement = { id: nextId(draft, 'ENT'), orderId: order.id, ownerAccountId: order.accountId, subjectId: order.subjectId, snapshot: clone(order.snapshot), issuedAt: iso(now) };
      draft.entitlements.push(entitlement);
      order.entitlement = 'ISSUED';
      recordEvent(draft, 'ENTITLEMENT', input.eventId, eventInput, entitlement);
      return ok(entitlement);
    });
  }

  function trialStatus(state, subjectId, now) {
    if (!one(state && state.subjects, item => item.id === subjectId)) return fail('NOT_FOUND');
    const grants = state.trialGrants.filter(item => item.subjectId === subjectId && item.campaignId === 'FIRST-TRIAL');
    if (!grants.length) return ok({ status: 'NOT_GRANTED', remainingSeconds: 0, grant: null });
    if (grants.length !== 1) return fail('INVALID_TRANSITION');
    const grant = grants[0];
    const remainingSeconds = Math.max(0, grant.grantedSeconds - grant.usedSeconds);
    const status = remainingSeconds === 0 ? 'EXHAUSTED' : toMs(now) >= Date.parse(grant.expiresAt) ? 'EXPIRED' : 'AVAILABLE';
    return ok({ status, remainingSeconds, grant: clone(grant) });
  }

  function snapshot(state) {
    const validation = validateState(state);
    return validation.ok ? ok(clone(pickBusiness(state))) : validation;
  }

  function loadReviewSnapshot(state, candidate) {
    if (!plain(state)) return fail('INVALID_TRANSITION');
    const validation = validateState(candidate);
    if (!validation.ok) return validation;
    const copied = clone(pickBusiness(candidate));
    replace(state, copied);
    return ok(clone(copied));
  }

  function validateState(candidate) {
    if (!safeData(candidate) || !plain(candidate)) return fail('INVALID_TRANSITION');
    const keys = Object.keys(candidate).sort();
    const allowed = [...BUSINESS_FIELDS].sort();
    if (keys.length !== allowed.length || keys.some((key, index) => key !== allowed[index]) || keys.some(key => FORBIDDEN_IMPORT_KEYS.has(key))) return fail('INVALID_TRANSITION');
    if (candidate.schemaVersion !== schemaVersion || !safeBetween(candidate.sequence, 0, Number.MAX_SAFE_INTEGER) || !plain(candidate.processedEvents)) return fail('INVALID_TRANSITION');
    const arrays = ['catalog', 'subjects', 'claims', 'trialGrants', 'sessions', 'evidence', 'activations', 'orders', 'entitlements'];
    if (arrays.some(key => !Array.isArray(candidate[key]))) return fail('INVALID_TRANSITION');
    if (!validRule(candidate.rule)) return fail('INVALID_TRANSITION');
    if (!unique(candidate.catalog, 'versionId') || !unique(candidate.subjects) || !unique(candidate.claims) || !unique(candidate.claims, 'requestId') || !unique(candidate.trialGrants) || !unique(candidate.sessions) || !unique(candidate.evidence) || !unique(candidate.activations) || !unique(candidate.orders) || !unique(candidate.orders, 'idempotencyKey') || !unique(candidate.entitlements)) return fail('INVALID_TRANSITION');
    if (candidate.catalog.some(item => !validateCatalogVersion(item).ok)) return fail('INVALID_TRANSITION');
    const activeSkus = new Set();
    const draftSkus = new Set();
    for (const item of candidate.catalog) {
      if (item.saleStatus === 'ACTIVE' && activeSkus.has(item.sku)) return fail('INVALID_TRANSITION');
      if (item.saleStatus === 'DRAFT' && draftSkus.has(item.sku)) return fail('INVALID_TRANSITION');
      if (item.saleStatus === 'ACTIVE') activeSkus.add(item.sku);
      if (item.saleStatus === 'DRAFT') draftSkus.add(item.sku);
    }
    for (const item of candidate.subjects) if (!validSubject(item)) return fail('INVALID_TRANSITION');
    const subjectCount = idCount(candidate.subjects);
    const claimedSubjects = new Set();
    for (const claim of candidate.claims) {
      if (!validClaim(claim) || subjectCount.get(claim.subjectId) !== 1) return fail('INVALID_TRANSITION');
      const owner = candidate.subjects.find(item => item.id === claim.subjectId);
      if (owner.accountId !== claim.accountId || claimedSubjects.has(claim.subjectId)) return fail('INVALID_TRANSITION');
      claimedSubjects.add(claim.subjectId);
    }
    const trialKeys = new Set();
    for (const grant of candidate.trialGrants) {
      if (!validGrant(grant) || subjectCount.get(grant.subjectId) !== 1) return fail('INVALID_TRANSITION');
      const key = `${grant.subjectId}:${grant.campaignId}`;
      if (trialKeys.has(key)) return fail('INVALID_TRANSITION');
      trialKeys.add(key);
    }
    for (const session of candidate.sessions) if (!validSession(session, subjectCount)) return fail('INVALID_TRANSITION');
    if (!allEvidenceReferencesValid(candidate)) return fail('INVALID_TRANSITION');
    const activationEventKeys = new Set();
    for (const activation of candidate.activations) {
      if (!validActivation(activation) || subjectCount.get(activation.subjectId) !== 1) return fail('INVALID_TRANSITION');
      const domainKey = `${activation.channel === 'IVR' ? 'IVR_CONFIRM' : 'ACTIVATION'}:${activation.eventId}`;
      if (activationEventKeys.has(domainKey)) return fail('INVALID_TRANSITION');
      activationEventKeys.add(domainKey);
      if (activation.channel === 'IVR') {
        const sessions = candidate.sessions.filter(item => item.id === activation.sessionId);
        if (sessions.length !== 1 || sessions[0].status !== 'CONFIRMED' || sessions[0].confirmedSubjectId !== activation.subjectId) return fail('INVALID_TRANSITION');
      } else if (activation.sessionId !== undefined) return fail('INVALID_TRANSITION');
    }
    for (const session of candidate.sessions) {
      if (session.status === 'CONFIRMED' && candidate.activations.filter(item => item.sessionId === session.id && item.channel === 'IVR' && item.subjectId === session.confirmedSubjectId).length !== 1) return fail('INVALID_TRANSITION');
    }
    const orderCount = idCount(candidate.orders);
    for (const order of candidate.orders) {
      if (!validOrder(order) || subjectCount.get(order.subjectId) !== 1 || !validateCatalogVersion(order.snapshot).ok) return fail('INVALID_TRANSITION');
      const owner = candidate.subjects.find(item => item.id === order.subjectId);
      if (owner.accountId !== order.accountId) return fail('INVALID_TRANSITION');
      if (!transactionalOffer(order.snapshot, order.channel)) return fail('INVALID_TRANSITION');
      if (order.payment !== 'PAID' && order.entitlement !== 'NOT_ISSUED') return fail('INVALID_TRANSITION');
      if (order.payment === 'PAID' && !['PENDING', 'FAILED', 'ISSUED'].includes(order.entitlement)) return fail('INVALID_TRANSITION');
    }
    const entitledOrders = new Set();
    for (const entitlement of candidate.entitlements) {
      if (!validEntitlement(entitlement) || orderCount.get(entitlement.orderId) !== 1 || subjectCount.get(entitlement.subjectId) !== 1 || entitledOrders.has(entitlement.orderId)) return fail('INVALID_TRANSITION');
      const order = candidate.orders.find(item => item.id === entitlement.orderId);
      if (order.subjectId !== entitlement.subjectId || order.accountId !== entitlement.ownerAccountId || order.payment !== 'PAID' || order.entitlement !== 'ISSUED' || canonicalSerialize(order.snapshot) !== canonicalSerialize(entitlement.snapshot) || !transactionalOffer(entitlement.snapshot, order.channel)) return fail('INVALID_TRANSITION');
      entitledOrders.add(entitlement.orderId);
    }
    for (const order of candidate.orders) {
      const count = candidate.entitlements.filter(item => item.orderId === order.id).length;
      if ((order.entitlement === 'ISSUED' && count !== 1) || (order.entitlement !== 'ISSUED' && count !== 0)) return fail('INVALID_TRANSITION');
    }
    if (!validProcessedEvents(candidate)) return fail('INVALID_TRANSITION');
    return ok(true);
  }

  function validProcessedEvents(state) {
    if (!plain(state.processedEvents)) return false;
    const domains = Object.keys(state.processedEvents).sort();
    if (domains.length !== EVENT_DOMAINS.length || domains.some((domain, index) => domain !== [...EVENT_DOMAINS].sort()[index])) return false;
    for (const domain of EVENT_DOMAINS) {
      if (!validEventDomain(state, domain)) return false;
    }
    return true;
  }

  // Import and every replay lookup use the same domain record/ownership checks.
  function validEventDomain(state, domain) {
    if (!EVENT_DOMAINS.includes(domain) || !plain(state.processedEvents) || !plain(state.processedEvents[domain])) return false;
    const resultIdentities = new Set();
    for (const [key, record] of Object.entries(state.processedEvents[domain])) {
      if (!validEventRecord(state, domain, key, record)) return false;
      const resultIdentity = createdResultIdentity(domain, record.result);
      if (resultIdentity !== null && (!id(resultIdentity) || resultIdentities.has(resultIdentity))) return false;
      if (resultIdentity !== null) resultIdentities.add(resultIdentity);
    }
    return true;
  }

  function allEvidenceReferencesValid(state) {
    const sessionCount = idCount(state.sessions || []);
    const subjectCount = idCount(state.subjects || []);
    for (const record of state.evidence || []) {
      if (!validEvidence(record) || sessionCount.get(record.sessionId) !== 1 || subjectCount.get(record.subjectId) !== 1) return false;
      const session = state.sessions.find(item => item.id === record.sessionId);
      if (session.status !== 'CONFIRMED' || record.subjectId !== session.subjectId || record.subjectId !== session.confirmedSubjectId) return false;
    }
    return true;
  }

  function validRule(rule) {
    return plain(rule) && rule.campaignId === 'FIRST-TRIAL' && safePositive(rule.version) && typeof rule.enabled === 'boolean' && safeBetween(rule.seconds, 1, 3600) && safeBetween(rule.validDays, 1, 365);
  }

  function validSubject(item) {
    return plain(item) && id(item.id) && id(item.maskedNumber) && ENUMS.numberType.has(item.numberType) && id(item.source) && (item.accountId === null || id(item.accountId)) && ENUMS.identity.has(item.identity) && ENUMS.activation.has(item.activation) && safePositive(item.revision) && ENUMS.claimRisk.has(item.claimRisk);
  }

  function validClaim(item) {
    return plain(item) && id(item.id) && id(item.subjectId) && id(item.accountId) && ENUMS.claimMethod.has(item.method) && date(item.verifiedAt) && id(item.requestId);
  }

  function validGrant(item) {
    return plain(item) && id(item.id) && id(item.subjectId) && item.campaignId === 'FIRST-TRIAL' && safePositive(item.ruleVersion) && safePositive(item.grantedSeconds) && safeBetween(item.usedSeconds, 0, item.grantedSeconds) && date(item.grantedAt) && date(item.expiresAt) && Date.parse(item.expiresAt) >= Date.parse(item.grantedAt);
  }

  function validSession(item, subjectCount) {
    if (!plain(item) || !id(item.id) || (item.maskedCaller !== null && !id(item.maskedCaller)) || typeof item.trustedCaller !== 'boolean' || !ENUMS.sessionStatus.has(item.status) || !id(item.consentVersion) || (item.key !== null && item.key !== '1') || !ENUMS.guidanceStatus.has(item.guidanceStatus) || !Object.prototype.hasOwnProperty.call(scenarios, item.scenario) || !date(item.createdAt)) return false;
    if (item.status === 'CONFIRMED') return id(item.subjectId) && item.subjectId === item.confirmedSubjectId && subjectCount.get(item.subjectId) === 1 && item.key === '1';
    return item.subjectId === null && item.confirmedSubjectId === null;
  }

  function validEvidence(item) {
    return plain(item) && id(item.id) && id(item.subjectId) && id(item.sessionId) && safePositive(item.recordedSeconds) && ENUMS.identity.has(item.identityAtCapture) && id(item.consentVersion) && date(item.createdAt) && item.preservationStatus === 'PRESERVED';
  }

  function validActivation(item) {
    return plain(item) && id(item.id) && id(item.subjectId) && ENUMS.channel.has(item.channel) && id(item.consentVersion) && ['EXPLICIT', 'KEY_1'].includes(item.confirmation) && id(item.eventId) && date(item.at) && (item.sessionId === undefined || id(item.sessionId));
  }

  function validOrder(item) {
    return plain(item) && id(item.id) && id(item.subjectId) && id(item.accountId) && ENUMS.purchaseChannel.has(item.channel) && plain(item.snapshot) && ENUMS.payment.has(item.payment) && ENUMS.entitlement.has(item.entitlement) && date(item.createdAt) && id(item.idempotencyKey) && id(item.consentVersion);
  }

  function validEntitlement(item) {
    return plain(item) && id(item.id) && id(item.orderId) && id(item.ownerAccountId) && id(item.subjectId) && plain(item.snapshot) && date(item.issuedAt);
  }

  function mutate(state, action) {
    if (!plain(state)) return fail('INVALID_TRANSITION');
    let draft;
    try { draft = clone(pickBusiness(state)); } catch (_) { return fail('INVALID_TRANSITION'); }
    let result;
    try { result = action(draft); } catch (_) { return fail('INVALID_TRANSITION'); }
    if (!result || !result.ok || result.code === 'DUPLICATE') return result || fail('INVALID_TRANSITION');
    const validation = validateState(draft);
    if (!validation.ok) return validation;
    replace(state, draft);
    return ok(clone(result.value), result.code);
  }

  function pickBusiness(state) {
    const output = {};
    for (const key of BUSINESS_FIELDS) output[key] = state[key];
    return output;
  }

  function replace(state, source) {
    for (const key of BUSINESS_FIELDS) state[key] = source[key];
  }

  function eventStore() {
    const store = {};
    for (const domain of EVENT_DOMAINS) store[domain] = {};
    return store;
  }

  function eventKey(eventId) {
    return `$${eventId}`;
  }

  function replayEvent(state, domain, eventId, input) {
    const records = state.processedEvents && state.processedEvents[domain];
    if (!validEventDomain(state, domain)) return fail('INVALID_TRANSITION');
    const record = Object.prototype.hasOwnProperty.call(records, eventKey(eventId)) ? records[eventKey(eventId)] : null;
    if (!record) return null;
    const inputFingerprint = `INPUT-V1:${canonicalSerialize(input)}`;
    if (record.inputFingerprint !== inputFingerprint) return fail('INVALID_TRANSITION');
    return duplicate(record.result);
  }

  // Historical mutable facts may differ; frozen identities, snapshots and
  // references must still resolve uniquely within a valid event domain.
  function validEventRecord(state, domain, key, record) {
    if (!EVENT_DOMAINS.includes(domain) || !exactKeys(record, ['eventId', 'inputFingerprint', 'result']) || !id(record.eventId) || key !== eventKey(record.eventId) || !safeData(record.result)) return false;
    if (typeof record.inputFingerprint !== 'string' || !record.inputFingerprint.startsWith('INPUT-V1:')) return false;
    let input;
    try { input = JSON.parse(record.inputFingerprint.slice('INPUT-V1:'.length)); } catch (_) { return false; }
    if (!plain(input) || !safeData(input) || record.inputFingerprint !== `INPUT-V1:${canonicalSerialize(input)}`) return false;
    if (['subjects', 'claims', 'sessions', 'activations', 'trialGrants', 'evidence', 'orders', 'entitlements'].some(name => !Array.isArray(state[name]))) return false;
    const value = record.result;

    if (domain === 'ACTIVATION') {
      if (!exactKeys(input, ['subjectId', 'channel', 'verified', 'consentVersion', 'confirmed']) || !id(input.subjectId) || !ENUMS.purchaseChannel.has(input.channel) || input.verified !== true || input.confirmed !== true || !id(input.consentVersion)) return false;
      if (!exactKeys(value, ['activation', 'grant', 'grantCode']) || !frozenEntity(state.activations, value.activation, [], validActivation)) return false;
      const activation = value.activation;
      const eventActivation = one(state.activations, item => item.channel !== 'IVR' && item.eventId === record.eventId);
      return eventActivation && eventActivation.id === activation.id && activation.eventId === record.eventId && activation.subjectId === input.subjectId && activation.channel === input.channel && activation.consentVersion === input.consentVersion && activation.confirmation === 'EXPLICIT' && activation.sessionId === undefined && referencedSubject(state, input.subjectId) && validGrantResult(state, value, input.subjectId);
    }

    if (domain === 'IVR_CONFIRM') {
      if (!exactKeys(input, ['sessionId', 'key']) || !id(input.sessionId) || input.key !== '1' || !exactKeys(value, ['session', 'activation', 'grant', 'grantCode'])) return false;
      const subjectCounts = idCount(state.subjects);
      if (!frozenEntity(state.sessions, value.session, ['guidanceStatus'], item => validSession(item, subjectCounts)) || !frozenEntity(state.activations, value.activation, [], validActivation)) return false;
      const session = value.session;
      const activation = value.activation;
      const eventActivation = one(state.activations, item => item.channel === 'IVR' && item.eventId === record.eventId);
      return eventActivation && eventActivation.id === activation.id && session.id === input.sessionId && session.status === 'CONFIRMED' && session.trustedCaller && id(session.maskedCaller) && session.key === '1' && activation.eventId === record.eventId && activation.sessionId === session.id && activation.subjectId === session.confirmedSubjectId && activation.channel === 'IVR' && activation.confirmation === 'KEY_1' && activation.consentVersion === session.consentVersion && referencedSubject(state, session.confirmedSubjectId) && validGrantResult(state, value, session.confirmedSubjectId);
    }

    if (domain === 'EVIDENCE') {
      if (!exactKeys(input, ['sessionId', 'seconds']) || !id(input.sessionId) || !safePositive(input.seconds) || !exactKeys(value, ['evidence', 'grant', 'exhausted', 'message'])) return false;
      if (!frozenEntity(state.evidence, value.evidence, [], validEvidence) || !frozenGrant(state, value.grant, value.evidence.subjectId)) return false;
      const evidenceRecord = value.evidence;
      const session = one(state.sessions, item => item.id === evidenceRecord.sessionId);
      const exhausted = value.grant.usedSeconds === value.grant.grantedSeconds;
      return session && validSession(session, idCount(state.subjects)) && session.status === 'CONFIRMED' && referencedSubject(state, evidenceRecord.subjectId) && evidenceRecord.sessionId === input.sessionId && evidenceRecord.subjectId === session.confirmedSubjectId && evidenceRecord.recordedSeconds <= input.seconds && value.grant.usedSeconds >= evidenceRecord.recordedSeconds && value.exhausted === exhausted && value.message === (exhausted ? '额度耗尽，结束取证' : '已按有效录音时长扣减');
    }

    if (domain === 'CLAIM') {
      if (!exactKeys(input, ['subjectId', 'accountId', 'verification']) || !id(input.subjectId) || !id(input.accountId) || !exactKeys(input.verification, ['verified', 'subjectId', 'method'])) return false;
      const verification = input.verification;
      const owner = one(state.subjects, item => item.id === input.subjectId);
      if (!validSubject(owner) || verification.verified !== true || verification.subjectId !== input.subjectId || !ENUMS.claimMethod.has(verification.method)) return false;
      const methodAllowed = owner.numberType === 'MOBILE' ? ['WECHAT_PHONE', 'SMS'].includes(verification.method) : verification.method === 'CALLER_CHALLENGE';
      const eventClaim = one(state.claims, item => item.requestId === record.eventId);
      return methodAllowed && frozenEntity(state.claims, value, [], validClaim) && eventClaim && eventClaim.id === value.id && value.subjectId === input.subjectId && value.accountId === input.accountId && owner.accountId === value.accountId && value.method === verification.method && value.requestId === record.eventId;
    }

    if (domain === 'ORDER') {
      if (!exactKeys(input, ['subjectId', 'accountId', 'channel', 'versionId', 'versionToken', 'confirmed', 'consentVersion']) || !id(input.subjectId) || !id(input.accountId) || !ENUMS.purchaseChannel.has(input.channel) || !id(input.versionId) || input.confirmed !== true || !id(input.consentVersion)) return false;
      const eventOrder = one(state.orders, item => item.idempotencyKey === record.eventId);
      return frozenOrder(state, value) && eventOrder && eventOrder.id === value.id && value.subjectId === input.subjectId && value.accountId === input.accountId && value.channel === input.channel && value.snapshot.versionId === input.versionId && value.snapshot.saleStatus === 'ACTIVE' && input.versionToken === offerVersionToken(value.snapshot) && value.consentVersion === input.consentVersion && value.idempotencyKey === record.eventId && value.payment === 'UNPAID' && value.entitlement === 'NOT_ISSUED';
    }

    if (domain === 'PAYMENT') {
      if (!exactKeys(input, ['orderId', 'result']) || !id(input.orderId) || !ENUMS.payment.has(input.result)) return false;
      return frozenOrder(state, value) && value.id === input.orderId && value.payment === input.result && value.entitlement === (input.result === 'PAID' ? 'PENDING' : 'NOT_ISSUED');
    }

    if (!exactKeys(input, ['orderId']) || !id(input.orderId) || !frozenEntity(state.entitlements, value, [], validEntitlement)) return false;
    const order = one(state.orders, item => item.id === value.orderId);
    return order && orderFactsValid(order) && referencedSubject(state, value.subjectId) && order.id === input.orderId && order.subjectId === value.subjectId && order.accountId === value.ownerAccountId && order.payment === 'PAID' && order.entitlement === 'ISSUED' && canonicalSerialize(order.snapshot) === canonicalSerialize(value.snapshot);
  }

  function referencedSubject(state, subjectId) {
    return validSubject(one(state.subjects, item => item.id === subjectId));
  }

  function validGrantResult(state, value, subjectId) {
    if (value.grant === null) return value.grantCode === 'TRIAL_DISABLED';
    return ['OK', 'DUPLICATE'].includes(value.grantCode) && frozenGrant(state, value.grant, subjectId);
  }

  function frozenGrant(state, value, subjectId) {
    if (!frozenEntity(state.trialGrants, value, ['usedSeconds'], validGrant) || value.subjectId !== subjectId || !referencedSubject(state, subjectId)) return false;
    const current = one(state.trialGrants, item => item.id === value.id);
    return value.usedSeconds <= current.usedSeconds;
  }

  function frozenOrder(state, value) {
    if (!frozenEntity(state.orders, value, ['payment', 'entitlement'], orderFactsValid) || !referencedSubject(state, value.subjectId)) return false;
    const owner = one(state.subjects, item => item.id === value.subjectId);
    return owner.accountId === value.accountId;
  }

  function orderFactsValid(value) {
    if (!validOrder(value) || !transactionalOffer(value.snapshot, value.channel)) return false;
    return value.payment === 'PAID' ? ['PENDING', 'FAILED', 'ISSUED'].includes(value.entitlement) : value.entitlement === 'NOT_ISSUED';
  }

  function frozenEntity(items, value, mutableFields, validator) {
    if (!validator(value)) return false;
    const current = one(items, item => item.id === value.id);
    if (!validator(current)) return false;
    const frozen = item => Object.fromEntries(Object.entries(item).filter(([key]) => !mutableFields.includes(key)));
    return canonicalSerialize(frozen(value)) === canonicalSerialize(frozen(current));
  }

  function exactKeys(value, keys) {
    if (!plain(value)) return false;
    const actual = Object.keys(value).sort();
    const expected = [...keys].sort();
    return actual.length === expected.length && actual.every((key, index) => key === expected[index]);
  }

  function createdResultIdentity(domain, value) {
    if (domain === 'PAYMENT') return null;
    if (!plain(value)) return undefined;
    if (domain === 'ACTIVATION' || domain === 'IVR_CONFIRM') return plain(value.activation) ? value.activation.id : undefined;
    if (domain === 'EVIDENCE') return plain(value.evidence) ? value.evidence.id : undefined;
    return value.id;
  }

  function recordEvent(state, domain, eventId, input, result) {
    const records = state.processedEvents[domain];
    const key = eventKey(eventId);
    if (!plain(records) || Object.prototype.hasOwnProperty.call(records, key)) throw new Error('Event already recorded');
    records[key] = { eventId, inputFingerprint: `INPUT-V1:${canonicalSerialize(input)}`, result: clone(result) };
  }

  function nextId(state, prefix) {
    const collection = ID_COLLECTIONS[prefix];
    if (!collection || !Array.isArray(state[collection])) throw new Error('Unknown ID prefix');
    const used = new Set(state[collection].map(item => item && item.id));
    let sequence = state.sequence;
    let candidate;
    do {
      if (sequence >= Number.MAX_SAFE_INTEGER) throw new Error('ID sequence exhausted');
      sequence += 1;
      candidate = `${prefix}-${String(sequence).padStart(6, '0')}`;
    } while (used.has(candidate));
    state.sequence = sequence;
    return candidate;
  }

  function transactionalOffer(value, channel) {
    return validateCatalogVersion(value).ok && value.audience === 'PERSONAL' && value.type !== 'QUOTE' && value.billingCycle !== 'QUOTE' && safeBetween(value.priceCents, 1, 9999999) && value.rights.seats === 0 && value.rights.fixedLines === 0 && ENUMS.purchaseChannel.has(channel) && value.channels.includes(channel);
  }

  function catalogTokenValue(value) {
    const copyValue = clone(value);
    delete copyValue.versionToken;
    return copyValue;
  }

  function canonicalSerialize(value) {
    if (value === undefined) return '"$undefined"';
    if (value === null || typeof value === 'boolean' || typeof value === 'string') return JSON.stringify(value);
    if (typeof value === 'number') {
      if (!Number.isFinite(value)) throw new TypeError('Canonical data must be finite');
      return JSON.stringify(value);
    }
    if (Array.isArray(value)) return `[${value.map(canonicalSerialize).join(',')}]`;
    if (!plain(value)) throw new TypeError('Canonical data must be plain');
    return `{${Object.keys(value).sort().map(key => `${JSON.stringify(key)}:${canonicalSerialize(value[key])}`).join(',')}}`;
  }

  function unique(items, key) {
    const counts = idCount(items, key);
    return items.every(item => plain(item) && id(item[key || 'id'])) && [...counts.values()].every(count => count === 1);
  }

  function idCount(items, key) {
    const counts = new Map();
    for (const item of items) {
      const value = item && item[key || 'id'];
      counts.set(value, (counts.get(value) || 0) + 1);
    }
    return counts;
  }

  function one(items, predicate) {
    if (!Array.isArray(items)) return null;
    const matches = items.filter(predicate);
    return matches.length === 1 ? matches[0] : null;
  }

  function safeData(value, seen) {
    if (value === null || ['string', 'number', 'boolean'].includes(typeof value)) return typeof value !== 'number' || Number.isFinite(value);
    if (typeof value !== 'object') return false;
    const visited = seen || new Set();
    if (visited.has(value)) return false;
    visited.add(value);
    if (!Array.isArray(value) && !plain(value)) return false;
    for (const key of Object.keys(value)) {
      if (['__proto__', 'prototype', 'constructor'].includes(key) || !safeData(value[key], visited)) return false;
    }
    visited.delete(value);
    return true;
  }

  function clone(value) {
    return JSON.parse(JSON.stringify(value));
  }

  function deepFreeze(value) {
    if (value && typeof value === 'object' && !Object.isFrozen(value)) {
      Object.freeze(value);
      Object.values(value).forEach(deepFreeze);
    }
    return value;
  }

  function plain(value) {
    if (!value || typeof value !== 'object' || Array.isArray(value)) return false;
    const prototype = Object.getPrototypeOf(value);
    return prototype === Object.prototype || prototype === null;
  }

  function safeBetween(value, min, max) { return Number.isSafeInteger(value) && value >= min && value <= max; }
  function safePositive(value) { return safeBetween(value, 1, Number.MAX_SAFE_INTEGER); }
  function id(value) { return typeof value === 'string' && value.trim().length > 0 && value.length <= 160; }
  function textBetween(value, min, max) { return typeof value === 'string' && value.trim().length >= min && value.length <= max; }
  function date(value) { return typeof value === 'string' && Number.isFinite(Date.parse(value)); }
  function toMs(value) { const result = value instanceof Date ? value.getTime() : Date.parse(value); if (!Number.isFinite(result)) throw new TypeError('A valid time is required'); return result; }
  function iso(value) { return new Date(toMs(value)).toISOString(); }
  function ok(value, code) { return { ok: true, code: code || 'OK', value: clone(value) }; }
  function duplicate(value) { return { ok: true, code: 'DUPLICATE', value: clone(value) }; }
  function fail(code) { return { ok: false, code }; }

  return deepFreeze({
    schemaVersion,
    labels,
    support,
    demoCatalog,
    createDemoState,
    validateCatalogVersion,
    visibleOffers,
    offerVersionToken,
    createOrder,
    applyPaymentResult,
    issueEntitlements,
    activateService,
    grantTrialOnce,
    claimSubject,
    startIvrSession,
    confirmIvrSession,
    consumeTrial,
    trialStatus,
    snapshot,
    loadReviewSnapshot,
  });
});
