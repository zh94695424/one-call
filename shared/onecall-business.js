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
    'evidence', 'activations', 'activationTransitions', 'orders', 'entitlements', 'processedEvents', 'sequence',
  ];
  const EVENT_DOMAINS = ['ACTIVATION', 'ACTIVATION_STATUS', 'CLAIM', 'IVR_CONFIRM', 'EVIDENCE', 'ORDER', 'PAYMENT', 'ENTITLEMENT'];
  const ID_COLLECTIONS = {
    ORDER: 'orders', TRIAL: 'trialGrants', SUBJ: 'subjects', ACT: 'activations',
    ASTAT: 'activationTransitions', EVID: 'evidence', CLAIM: 'claims', ENT: 'entitlements', IVR: 'sessions',
  };
  const FORBIDDEN_IMPORT_KEYS = new Set([
    'employees', 'employeeLoginSession', 'employeeCredentials', 'employeeRoles',
    'customerAuthSession', 'uiSession', 'widgetState', 'code',
  ]);
  // The call-task domain is a separate local review fixture. It is never part of
  // the strict business snapshot above and has its own schema and validator.
  const CALL_TASK_SCHEMA_VERSION = 1;
  const CALL_TASK_TTL_SECONDS = 30;
  const CALL_TASK_FIELDS = ['schemaVersion', 'ttlSeconds', 'callerBindings', 'tasks', 'calls', 'sequence'];
  const CALL_TASK_REASONS = new Set(['TTL_EXPIRED', 'CALLER_MISMATCH', 'NO_ANSWER', 'BUSY', 'OUTBOUND_FAILED', 'REPLACED', 'CANCELLED', 'SUSPENDED']);
  const CALL_TASK_FAILURE_REASONS = new Set(['TTL_EXPIRED', 'CALLER_MISMATCH', 'NO_ANSWER', 'BUSY', 'OUTBOUND_FAILED', 'SUSPENDED']);
  const CALL_TASK_NONTERMINAL = new Set(['PENDING', 'CLAIMED']);
  const CALL_TASK_ACTIVE_PROGRESS = new Set(['WAITING_CALLEE_INPUT', 'CALLEE_CONFIRMED', 'CLAIMED', 'OUTBOUND_STARTED', 'RINGING', 'ANSWERED', 'BRIDGED', 'RECORDING']);
  const CALL_TASK_ENUMS = {
    ownerType: new Set(['PERSONAL_ACCOUNT', 'ORGANIZATION']),
    numberType: new Set(['MOBILE', 'LANDLINE']),
    businessType: new Set(['EVIDENCE_CALL']),
    taskStatus: new Set(['PENDING', 'CLAIMED', 'COMPLETED', 'FAILED', 'EXPIRED', 'REPLACED', 'CANCELLED', 'REJECTED']),
    ivrAction: new Set(['DIAL_TASK_CALLEE', 'ENTER_CALLEE', 'GUIDE_ACTIVATION', 'GUIDE_PURCHASE', 'REJECT', 'REJECT_CALL_IN_PROGRESS', 'REJECT_SUSPENDED']),
    callAction: new Set(['DIAL_TASK_CALLEE', 'ENTER_CALLEE', 'REJECT']),
    taskMatch: new Set(['CLAIMED', 'NONE', 'MISMATCH', 'EXPIRED', 'REJECTED', 'CONFLICT']),
    callProgress: new Set(['WAITING_INBOUND', 'WAITING_CALLEE_INPUT', 'CALLEE_CONFIRMED', 'CLAIMED', 'OUTBOUND_STARTED', 'RINGING', 'ANSWERED', 'BRIDGED', 'RECORDING', 'ENDED', 'RECORDING_RECEIVED', 'EVIDENCE_PROCESSING', 'EVIDENCE_READY', 'FAILED']),
    calleeConfirmation: new Set(['NONE', 'DTMF_REQUIRED', 'DTMF_CONFIRMED', 'NOT_APPLICABLE']),
    upload: new Set(['NOT_STARTED', 'UPLOADING', 'RECEIVED', 'FAILED']),
    evidence: new Set(['NOT_CREATED', 'PROCESSING', 'AVAILABLE', 'FAILED']),
    eventType: new Set(['INBOUND_RECEIVED', 'TASK_CLAIMED', 'DTMF_CALLEE_CONFIRMED', 'OUTBOUND_STARTED', 'RINGING', 'ANSWERED', 'BRIDGED', 'RECORDING_STARTED', 'CALL_ENDED', 'RECORDING_RECEIVED', 'EVIDENCE_PROCESSING', 'EVIDENCE_READY', 'CALL_FAILED']),
    eventSource: new Set(['MINI', 'IVR', 'ONECALL']),
    callerState: new Set(['ACTIVE', 'INACTIVE', 'SUSPENDED', 'UNREGISTERED']),
  };
  const CALL_TASK_EVENT_SOURCE = {
    INBOUND_RECEIVED: 'IVR', TASK_CLAIMED: 'ONECALL', DTMF_CALLEE_CONFIRMED: 'IVR',
    OUTBOUND_STARTED: 'IVR', RINGING: 'IVR', ANSWERED: 'IVR', BRIDGED: 'IVR', RECORDING_STARTED: 'IVR',
    CALL_ENDED: 'IVR', RECORDING_RECEIVED: 'ONECALL', EVIDENCE_PROCESSING: 'ONECALL', EVIDENCE_READY: 'ONECALL',
  };
  const CALL_TASK_EVENT_PROGRESS = {
    TASK_CLAIMED: 'CLAIMED', DTMF_CALLEE_CONFIRMED: 'CALLEE_CONFIRMED', OUTBOUND_STARTED: 'OUTBOUND_STARTED',
    RINGING: 'RINGING', ANSWERED: 'ANSWERED', BRIDGED: 'BRIDGED', RECORDING_STARTED: 'RECORDING',
    CALL_ENDED: 'ENDED', RECORDING_RECEIVED: 'RECORDING_RECEIVED', EVIDENCE_PROCESSING: 'EVIDENCE_PROCESSING',
    EVIDENCE_READY: 'EVIDENCE_READY', CALL_FAILED: 'FAILED',
  };
  const ENUMS = {
    numberType: new Set(['MOBILE', 'LANDLINE']),
    identity: new Set(['UNVERIFIED', 'VERIFIED']),
    activation: new Set(['INACTIVE', 'ACTIVE', 'SUSPENDED']),
    claimRisk: new Set(['NONE', 'CONFLICT', 'RECYCLED']),
    claimMethod: new Set(['WECHAT_PHONE', 'SMS', 'CALLER_CHALLENGE']),
    channel: new Set(['MINIAPP', 'WEB', 'IVR']),
    activationStatusChannel: new Set(['MINIAPP', 'WEB', 'OPERATIONS']),
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

  const ACCOUNT_ENUMS = {
    status: new Set(['ACTIVE', 'DISABLED']),
    provider: new Set(['WECHAT', 'PHONE']),
    realNameStatus: new Set(['UNVERIFIED', 'VERIFIED']),
    role: new Set(['ADMIN', 'MEMBER', 'EVIDENCE_MANAGER', 'AUDITOR']),
  };
  const ROLE_LABELS = deepFreeze({
    ADMIN: '企业管理员',
    MEMBER: '普通成员',
    EVIDENCE_MANAGER: '证据管理员',
    AUDITOR: '企业审计员',
  });
  const ACCOUNT_DIRECTORY = deepFreeze({
    accounts: [
      { id: 'ACCOUNT-DEMO-001', displayName: '李晓', status: 'ACTIVE' },
      { id: 'ACCOUNT-DEMO-002', displayName: '陈雨', status: 'ACTIVE' },
      { id: 'ACCOUNT-DEMO-003', displayName: '周宁', status: 'ACTIVE' },
      { id: 'ACCOUNT-DEMO-004', displayName: '王璐', status: 'ACTIVE' },
      { id: 'ACCOUNT-DEMO-099', displayName: '停用账户', status: 'DISABLED' },
    ],
    authIdentities: [
      { id: 'AUTH-WX-LI', accountId: 'ACCOUNT-DEMO-001', provider: 'WECHAT', providerSubject: 'WX-DEMO-LI', maskedLabel: '微信账户 · 李晓', status: 'ACTIVE' },
      { id: 'AUTH-PHONE-LI', accountId: 'ACCOUNT-DEMO-001', provider: 'PHONE', providerSubject: '13800006208', maskedLabel: '138 **** 6208', status: 'ACTIVE' },
      { id: 'AUTH-WX-CHEN', accountId: 'ACCOUNT-DEMO-002', provider: 'WECHAT', providerSubject: 'WX-DEMO-CHEN', maskedLabel: '微信账户 · 陈雨', status: 'ACTIVE' },
      { id: 'AUTH-PHONE-CHEN', accountId: 'ACCOUNT-DEMO-002', provider: 'PHONE', providerSubject: '13900007812', maskedLabel: '139 **** 7812', status: 'ACTIVE' },
      { id: 'AUTH-WX-ZHOU', accountId: 'ACCOUNT-DEMO-003', provider: 'WECHAT', providerSubject: 'WX-DEMO-ZHOU', maskedLabel: '微信账户 · 周宁', status: 'ACTIVE' },
      { id: 'AUTH-PHONE-ZHOU', accountId: 'ACCOUNT-DEMO-003', provider: 'PHONE', providerSubject: '18600003360', maskedLabel: '186 **** 3360', status: 'ACTIVE' },
      { id: 'AUTH-WX-WANG', accountId: 'ACCOUNT-DEMO-004', provider: 'WECHAT', providerSubject: 'WX-DEMO-WANG', maskedLabel: '微信账户 · 王璐', status: 'ACTIVE' },
      { id: 'AUTH-PHONE-WANG', accountId: 'ACCOUNT-DEMO-004', provider: 'PHONE', providerSubject: '13700004102', maskedLabel: '137 **** 4102', status: 'ACTIVE' },
      { id: 'AUTH-PHONE-DISABLED', accountId: 'ACCOUNT-DEMO-099', provider: 'PHONE', providerSubject: '13600009999', maskedLabel: '136 **** 9999', status: 'ACTIVE' },
    ],
    identityProfiles: [
      { accountId: 'ACCOUNT-DEMO-001', realNameStatus: 'VERIFIED', maskedCredential: '310***********241X' },
      { accountId: 'ACCOUNT-DEMO-002', realNameStatus: 'VERIFIED', maskedCredential: '320***********518X' },
      { accountId: 'ACCOUNT-DEMO-003', realNameStatus: 'VERIFIED', maskedCredential: '310***********372X' },
      { accountId: 'ACCOUNT-DEMO-004', realNameStatus: 'VERIFIED', maskedCredential: '110***********460X' },
      { accountId: 'ACCOUNT-DEMO-099', realNameStatus: 'UNVERIFIED', maskedCredential: null },
    ],
    organizations: [
      { id: 'ORG-HUAQI-001', name: '华启科技有限公司', shortName: '华启科技', status: 'ACTIVE' },
      { id: 'ORG-YUNTU-001', name: '云拓商贸有限公司', shortName: '云拓商贸', status: 'ACTIVE' },
      { id: 'ORG-DISABLED-001', name: '停用组织', shortName: '停用组织', status: 'DISABLED' },
    ],
    memberships: [
      { id: 'MEM-HUAQI-LI', accountId: 'ACCOUNT-DEMO-001', organizationId: 'ORG-HUAQI-001', role: 'ADMIN', department: '采购部', status: 'ACTIVE' },
      { id: 'MEM-YUNTU-LI', accountId: 'ACCOUNT-DEMO-001', organizationId: 'ORG-YUNTU-001', role: 'MEMBER', department: '采购部', status: 'ACTIVE' },
      { id: 'MEM-HUAQI-CHEN', accountId: 'ACCOUNT-DEMO-002', organizationId: 'ORG-HUAQI-001', role: 'MEMBER', department: '采购部', status: 'ACTIVE' },
      { id: 'MEM-HUAQI-ZHOU', accountId: 'ACCOUNT-DEMO-003', organizationId: 'ORG-HUAQI-001', role: 'EVIDENCE_MANAGER', department: '法务部', status: 'ACTIVE' },
      { id: 'MEM-HUAQI-WANG', accountId: 'ACCOUNT-DEMO-004', organizationId: 'ORG-HUAQI-001', role: 'AUDITOR', department: '合规部', status: 'ACTIVE' },
      { id: 'MEM-YUNTU-CHEN-DISABLED', accountId: 'ACCOUNT-DEMO-002', organizationId: 'ORG-YUNTU-001', role: 'MEMBER', department: '采购部', status: 'DISABLED' },
      { id: 'MEM-DISABLED-ACCOUNT', accountId: 'ACCOUNT-DEMO-099', organizationId: 'ORG-HUAQI-001', role: 'ADMIN', department: '测试部', status: 'ACTIVE' },
      { id: 'MEM-DISABLED-ORG', accountId: 'ACCOUNT-DEMO-001', organizationId: 'ORG-DISABLED-001', role: 'ADMIN', department: '测试部', status: 'ACTIVE' },
    ],
  });
  const demoAccountDirectory = deepFreeze(redactAccountDirectory(ACCOUNT_DIRECTORY));

  if (!validateAccountDirectory(ACCOUNT_DIRECTORY).ok) throw new Error('Invalid demo account directory');

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
    taskStatus: { PENDING: '待呼入', CLAIMED: '已领取', COMPLETED: '已完成', FAILED: '失败', EXPIRED: '已过期', REPLACED: '已替换', CANCELLED: '已取消', REJECTED: '已拒绝' },
    ivrAction: { DIAL_TASK_CALLEE: '任务自动外呼', ENTER_CALLEE: '无任务 DTMF 输入', GUIDE_ACTIVATION: '引导开通', GUIDE_PURCHASE: '引导购买', REJECT: '拒绝', REJECT_CALL_IN_PROGRESS: '呼叫处理中', REJECT_SUSPENDED: '业务已停用' },
    taskMatch: { CLAIMED: '已领取', NONE: '无任务', MISMATCH: '主叫不匹配', EXPIRED: '已过期', REJECTED: '已拒绝', CONFLICT: '数据冲突' },
    callProgress: { WAITING_INBOUND: '等待呼入', WAITING_CALLEE_INPUT: '等待被叫输入', CALLEE_CONFIRMED: '被叫已确认', CLAIMED: '任务已领取', OUTBOUND_STARTED: '已启动 B 路外呼', RINGING: '被叫振铃', ANSWERED: '被叫已接听', BRIDGED: '已桥接', RECORDING: '录音中', ENDED: '通话已结束', RECORDING_RECEIVED: '录音已接收', EVIDENCE_PROCESSING: '证据处理中', EVIDENCE_READY: '证据可用', FAILED: '通话失败' },
    uploadStatus: { NOT_STARTED: '未上传', UPLOADING: '上传中', RECEIVED: '已接收', FAILED: '上传失败' },
    evidenceStatus: { NOT_CREATED: '未创建', PROCESSING: '处理中', AVAILABLE: '可用', FAILED: '处理失败' },
    ownership: '运营商提供号码、线路和通信承载；自研 IVR 负责呼入接听、B 路外呼、双向桥接、录音、限时挂断及通话/录音上传；ONECALL 负责业务开通、待拨任务、权益与证据处理。',
    callPath: {
      task: '任务路径：IVR 原子领取后按冻结被叫自动外呼，电话中不要求确认被叫号码。',
      noTask: '无任务路径：只有没有有效待拨任务时，IVR 才引导主叫通过 DTMF 输入并确认被叫；不读取或猜测历史任务被叫。',
      launch: '系统拨号盘已打开仅表示客户端调用了系统拨号能力，不代表已接通、已领取或正在录音。',
      localFixture: '本地演示/评审状态，不连接真实线路、媒体引擎、对象存储或后台。',
    },
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

  function createAccountDirectoryFixture() {
    return clone(ACCOUNT_DIRECTORY);
  }

  function redactAccountDirectory(directory) {
    const output = clone(directory);
    output.authIdentities = output.authIdentities.map(({ providerSubject, ...identity }) => identity);
    return output;
  }

  function validateAccountDirectory(candidate) {
    if (!plain(candidate) || !exactKeys(candidate, ['accounts', 'authIdentities', 'identityProfiles', 'organizations', 'memberships'])) return fail('INVALID_ACCOUNT_DIRECTORY');
    if (Object.values(candidate).some(value => !Array.isArray(value))) return fail('INVALID_ACCOUNT_DIRECTORY');
    if (!unique(candidate.accounts) || !unique(candidate.authIdentities) || !unique(candidate.identityProfiles, 'accountId') || !unique(candidate.organizations) || !unique(candidate.memberships)) return fail('INVALID_ACCOUNT_DIRECTORY');
    const accounts = new Set(candidate.accounts.map(item => item.id));
    const organizations = new Set(candidate.organizations.map(item => item.id));
    const providerSubjects = new Set();
    const activeMemberships = new Set();
    for (const account of candidate.accounts) {
      if (!exactKeys(account, ['id', 'displayName', 'status']) || !id(account.id) || !textBetween(account.displayName, 1, 40) || !ACCOUNT_ENUMS.status.has(account.status)) return fail('INVALID_ACCOUNT_DIRECTORY');
    }
    for (const identity of candidate.authIdentities) {
      if (!exactKeys(identity, ['id', 'accountId', 'provider', 'providerSubject', 'maskedLabel', 'status']) || !id(identity.id) || !accounts.has(identity.accountId) || !ACCOUNT_ENUMS.provider.has(identity.provider) || !id(identity.providerSubject) || !textBetween(identity.maskedLabel, 1, 80) || !ACCOUNT_ENUMS.status.has(identity.status)) return fail('INVALID_ACCOUNT_DIRECTORY');
      if (identity.provider === 'PHONE' && !/^1\d{10}$/.test(identity.providerSubject)) return fail('INVALID_ACCOUNT_DIRECTORY');
      const providerKey = `${identity.provider}:${identity.providerSubject}`;
      if (providerSubjects.has(providerKey)) return fail('INVALID_ACCOUNT_DIRECTORY');
      providerSubjects.add(providerKey);
    }
    for (const profile of candidate.identityProfiles) {
      if (!exactKeys(profile, ['accountId', 'realNameStatus', 'maskedCredential']) || !accounts.has(profile.accountId) || !ACCOUNT_ENUMS.realNameStatus.has(profile.realNameStatus)) return fail('INVALID_ACCOUNT_DIRECTORY');
      if (profile.maskedCredential !== null && !textBetween(profile.maskedCredential, 1, 40)) return fail('INVALID_ACCOUNT_DIRECTORY');
    }
    if (candidate.accounts.some(account => !candidate.identityProfiles.some(profile => profile.accountId === account.id))) return fail('INVALID_ACCOUNT_DIRECTORY');
    for (const organization of candidate.organizations) {
      if (!exactKeys(organization, ['id', 'name', 'shortName', 'status']) || !id(organization.id) || !textBetween(organization.name, 1, 80) || !textBetween(organization.shortName, 1, 40) || !ACCOUNT_ENUMS.status.has(organization.status)) return fail('INVALID_ACCOUNT_DIRECTORY');
    }
    for (const membership of candidate.memberships) {
      if (!exactKeys(membership, ['id', 'accountId', 'organizationId', 'role', 'department', 'status']) || !id(membership.id) || !accounts.has(membership.accountId) || !organizations.has(membership.organizationId) || !ACCOUNT_ENUMS.role.has(membership.role) || !textBetween(membership.department, 1, 40) || !ACCOUNT_ENUMS.status.has(membership.status)) return fail('INVALID_ACCOUNT_DIRECTORY');
      if (membership.status === 'ACTIVE') {
        const membershipKey = `${membership.accountId}:${membership.organizationId}`;
        if (activeMemberships.has(membershipKey)) return fail('INVALID_ACCOUNT_DIRECTORY');
        activeMemberships.add(membershipKey);
      }
    }
    return ok({ valid: true });
  }

  function accountById(accountId) {
    const account = ACCOUNT_DIRECTORY.accounts.find(item => item.id === accountId && item.status === 'ACTIVE');
    if (!account) return fail('NOT_FOUND');
    const profile = ACCOUNT_DIRECTORY.identityProfiles.find(item => item.accountId === account.id);
    return ok({ account: clone(account), profile: clone(profile) });
  }

  function loginIdentitiesForAccount(accountId) {
    const accountResult = accountById(accountId);
    if (!accountResult.ok) return accountResult;
    const identities = ACCOUNT_DIRECTORY.authIdentities
      .filter(item => item.accountId === accountId && item.status === 'ACTIVE')
      .map(({ providerSubject, ...identity }) => clone(identity));
    return ok(identities);
  }

  function workspacesForAccount(accountId) {
    const accountResult = accountById(accountId);
    if (!accountResult.ok) return accountResult;
    const account = accountResult.value.account;
    const personal = {
      id: `personal:${account.id}`,
      type: 'PERSONAL',
      accountId: account.id,
      name: '个人空间',
      organizationId: null,
      organizationName: null,
      membershipId: null,
      role: 'PERSONAL',
      roleLabel: '个人用户',
      department: null,
    };
    const organizations = ACCOUNT_DIRECTORY.memberships
      .filter(item => item.accountId === account.id && item.status === 'ACTIVE')
      .map(membership => ({ membership, organization: ACCOUNT_DIRECTORY.organizations.find(item => item.id === membership.organizationId && item.status === 'ACTIVE') }))
      .filter(item => item.organization)
      .map(({ membership, organization }) => ({
        id: `organization:${organization.id}`,
        type: 'ORGANIZATION',
        accountId: account.id,
        name: organization.shortName,
        organizationId: organization.id,
        organizationName: organization.name,
        membershipId: membership.id,
        role: membership.role,
        roleLabel: ROLE_LABELS[membership.role],
        department: membership.department,
      }));
    return ok([personal, ...organizations]);
  }

  function resolveDemoLogin(input) {
    if (!plain(input) || !['PHONE', 'WECHAT'].includes(input.method)) return fail('INVALID_CREDENTIALS');
    let identity = null;
    if (input.method === 'PHONE') {
      const phone = typeof input.phone === 'string' ? input.phone.replace(/\s/g, '') : '';
      if (!/^1\d{10}$/.test(phone) || input.code !== '482916') return fail('INVALID_CREDENTIALS');
      identity = ACCOUNT_DIRECTORY.authIdentities.find(item => item.provider === 'PHONE' && item.providerSubject === phone && item.status === 'ACTIVE');
    } else {
      if (!id(input.providerSubject)) return fail('INVALID_CREDENTIALS');
      identity = ACCOUNT_DIRECTORY.authIdentities.find(item => item.provider === 'WECHAT' && item.providerSubject === input.providerSubject && item.status === 'ACTIVE');
    }
    if (!identity) return fail('INVALID_CREDENTIALS');
    const accountResult = accountById(identity.accountId);
    if (!accountResult.ok) return fail('INVALID_CREDENTIALS');
    const identities = loginIdentitiesForAccount(identity.accountId);
    const workspaces = workspacesForAccount(identity.accountId);
    if (!identities.ok || !workspaces.ok) return fail('INVALID_CREDENTIALS');
    return ok({
      account: accountResult.value.account,
      profile: accountResult.value.profile,
      identities: identities.value,
      workspaces: workspaces.value,
    });
  }

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
      activationTransitions: [],
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
    const recurring = ['MONTHLY', 'QUARTERLY', 'ANNUAL'];
    if (value.type === 'SUBSCRIPTION' && (!recurring.includes(value.billingCycle) || !safePositive(value.installments) || !safePositive(value.grantIntervalMonths))) return fail('INVALID_TRANSITION');
    if (['ONE_TIME', 'ADD_ON'].includes(value.type) && (value.billingCycle !== 'ONCE' || value.installments !== 1 || value.grantIntervalMonths !== 0)) return fail('INVALID_TRANSITION');
    if (value.type === 'QUOTE' && (value.billingCycle !== 'QUOTE' || value.installments !== 1 || value.grantIntervalMonths !== 0)) return fail('INVALID_TRANSITION');
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

  function setActivationStatus(state, input, now) {
    return mutate(state, draft => {
      if (!plain(input) || !id(input.subjectId) || !ENUMS.activationStatusChannel.has(input.channel) || !['ACTIVE', 'SUSPENDED'].includes(input.status) || !id(input.eventId)) return fail('INVALID_TRANSITION');
      if (!activationTransitionLedgerValid(draft) || !activationStatusEventCoverageValid(draft)) return fail('INVALID_TRANSITION');
      const eventInput = { subjectId: input.subjectId, channel: input.channel, status: input.status };
      const replay = replayEvent(draft, 'ACTIVATION_STATUS', input.eventId, eventInput);
      if (replay) return replay;
      const target = one(draft.subjects, item => item.id === input.subjectId);
      if (!target) return fail('NOT_FOUND');
      if (target.activation === 'INACTIVE' || target.activation === input.status) return fail('INVALID_TRANSITION');
      const fromRevision = target.revision;
      const toRevision = fromRevision + 1;
      const transition = { id: nextId(draft, 'ASTAT'), subjectId: target.id, channel: input.channel, from: target.activation, to: input.status, fromRevision, toRevision, eventId: input.eventId, at: iso(now) };
      target.activation = input.status;
      target.revision = toRevision;
      draft.activationTransitions.push(transition);
      recordEvent(draft, 'ACTIVATION_STATUS', input.eventId, eventInput, transition);
      return ok(transition);
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
      const target = one(draft.subjects, item => item.id === input.subjectId);
      if (!target) return fail('NOT_FOUND');
      if (target.claimRisk === 'CONFLICT') return fail('CLAIM_CONFLICT');
      if (target.claimRisk === 'RECYCLED') return fail('RECYCLED_NUMBER_REVIEW');
      const replay = replayEvent(draft, 'CLAIM', input.requestId, eventInput);
      if (replay) return replay;
      const verification = input.verification;
      if (verification.verified !== true || verification.subjectId !== target.id || !ENUMS.claimMethod.has(verification.method)) return fail('VERIFICATION_REQUIRED');
      const methodAllowed = target.numberType === 'MOBILE' ? ['WECHAT_PHONE', 'SMS'].includes(verification.method) : verification.method === 'CALLER_CHALLENGE';
      if (!methodAllowed) return fail('VERIFICATION_REQUIRED');
      if (target.accountId === input.accountId) {
        const existing = draft.claims.find(item => item.subjectId === target.id && item.accountId === input.accountId);
        return duplicate(existing || clone(target));
      }
      if (target.accountId && target.accountId !== input.accountId) return fail('CLAIM_CONFLICT');
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
      if (!allEvidenceReferencesValid(draft) || !evidenceEventCoverageValid(draft)) return fail('INVALID_TRANSITION');
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

  // ---------------------------------------------------------------------------
  // Call-task demo domain (separate from the strict business snapshot)
  // ---------------------------------------------------------------------------

  function createCallTaskDemoState(now) {
    const base = toMs(now);
    const offset = ms => new Date(ms).toISOString();
    const event = (callId, index, type, at, detail) => ({
      eventId: `${callId}-EV${String(index).padStart(2, '0')}`,
      type,
      at: offset(at),
      source: type === 'CALL_FAILED' ? 'IVR' : (CALL_TASK_EVENT_SOURCE[type] || 'IVR'),
      detail: detail || '',
    });
    const successEvents = (callId, start, seconds) => [
      event(callId, 1, 'INBOUND_RECEIVED', start),
      event(callId, 2, 'TASK_CLAIMED', start + 1000),
      event(callId, 3, 'OUTBOUND_STARTED', start + 2000),
      event(callId, 4, 'RINGING', start + 4000),
      event(callId, 5, 'ANSWERED', start + 8000),
      event(callId, 6, 'BRIDGED', start + 8000),
      event(callId, 7, 'RECORDING_STARTED', start + 9000),
      event(callId, 8, 'CALL_ENDED', start + 9000 + seconds * 1000),
      event(callId, 9, 'RECORDING_RECEIVED', start + 9000 + seconds * 1000 + 12000),
      event(callId, 10, 'EVIDENCE_PROCESSING', start + 9000 + seconds * 1000 + 22000),
      event(callId, 11, 'EVIDENCE_READY', start + 9000 + seconds * 1000 + 32000),
    ];
    const directEvents = (callId, start, seconds) => {
      const events = successEvents(callId, start, seconds);
      events[1] = event(callId, 2, 'DTMF_CALLEE_CONFIRMED', start + 1000, '021 **** 9056');
      return events;
    };
    const callerBindings = [
      { id: 'CB-PERSONAL-6208', ownerType: 'PERSONAL_ACCOUNT', ownerId: 'ACCOUNT-DEMO-001', workspaceId: 'personal:ACCOUNT-DEMO-001', maskedCaller: '138 **** 6208', normalizedCaller: '13800006208', numberType: 'MOBILE', active: true },
      { id: 'CB-PERSONAL-4471', ownerType: 'PERSONAL_ACCOUNT', ownerId: 'ACCOUNT-DEMO-001', workspaceId: 'personal:ACCOUNT-DEMO-001', maskedCaller: '186 **** 4471', normalizedCaller: '18600004471', numberType: 'MOBILE', active: true },
      { id: 'CB-ORG-6688', ownerType: 'ORGANIZATION', ownerId: 'ORG-HUAQI-001', workspaceId: 'organization:ORG-HUAQI-001', maskedCaller: '010 **** 6688', normalizedCaller: '01088886688', numberType: 'LANDLINE', active: true },
      { id: 'CB-PERSONAL-7812', ownerType: 'PERSONAL_ACCOUNT', ownerId: 'ACCOUNT-DEMO-002', workspaceId: 'personal:ACCOUNT-DEMO-002', maskedCaller: '139 **** 7812', normalizedCaller: '13900007812', numberType: 'MOBILE', active: true },
    ];
    const t101 = base - 10 * 60 * 1000;
    const t103 = base - 25 * 60 * 1000;
    const t104 = base - 2 * DAY_MS;
    const tNoTask = base - 20 * 60 * 1000;
    const tMismatch = base - 15 * 60 * 1000;
    const tasks = [
      { id: 'CT-DEMO-0101', callerBindingId: 'CB-PERSONAL-6208', normalizedCaller: '13800006208', maskedCaller: '138 **** 6208', maskedCallee: '139 **** 3816', ownerType: 'PERSONAL_ACCOUNT', ownerId: 'ACCOUNT-DEMO-001', workspaceId: 'personal:ACCOUNT-DEMO-001', createdByAccountId: 'ACCOUNT-DEMO-001', businessType: 'EVIDENCE_CALL', status: 'COMPLETED', reasonCode: null, ttlSeconds: CALL_TASK_TTL_SECONDS, createdAt: offset(t101), expiresAt: offset(t101 + CALL_TASK_TTL_SECONDS * 1000), dialerLaunchedAt: offset(t101 + 2000), claimedCallId: 'OC-C2038', claimedAt: offset(t101 + 5000), idempotencyKey: 'IDEM-CT-DEMO-0101', revision: 2 },
      { id: 'CT-DEMO-0102', callerBindingId: 'CB-PERSONAL-6208', normalizedCaller: '13800006208', maskedCaller: '138 **** 6208', maskedCallee: '138 **** 7720', ownerType: 'PERSONAL_ACCOUNT', ownerId: 'ACCOUNT-DEMO-001', workspaceId: 'personal:ACCOUNT-DEMO-001', createdByAccountId: 'ACCOUNT-DEMO-001', businessType: 'EVIDENCE_CALL', status: 'EXPIRED', reasonCode: 'TTL_EXPIRED', ttlSeconds: CALL_TASK_TTL_SECONDS, createdAt: offset(base - 40 * 60 * 1000), expiresAt: offset(base - 40 * 60 * 1000 + CALL_TASK_TTL_SECONDS * 1000), dialerLaunchedAt: null, claimedCallId: null, claimedAt: null, idempotencyKey: 'IDEM-CT-DEMO-0102', revision: 2 },
      { id: 'CT-DEMO-0103', callerBindingId: 'CB-PERSONAL-6208', normalizedCaller: '13800006208', maskedCaller: '138 **** 6208', maskedCallee: '135 **** 7781', ownerType: 'PERSONAL_ACCOUNT', ownerId: 'ACCOUNT-DEMO-001', workspaceId: 'personal:ACCOUNT-DEMO-001', createdByAccountId: 'ACCOUNT-DEMO-001', businessType: 'EVIDENCE_CALL', status: 'FAILED', reasonCode: 'NO_ANSWER', ttlSeconds: CALL_TASK_TTL_SECONDS, createdAt: offset(t103), expiresAt: offset(t103 + CALL_TASK_TTL_SECONDS * 1000), dialerLaunchedAt: offset(t103 + 2000), claimedCallId: 'OC-C2035', claimedAt: offset(t103 + 4000), idempotencyKey: 'IDEM-CT-DEMO-0103', revision: 2 },
      { id: 'CT-DEMO-0104', callerBindingId: 'CB-PERSONAL-7812', normalizedCaller: '13900007812', maskedCaller: '139 **** 7812', maskedCallee: '137 **** 2240', ownerType: 'PERSONAL_ACCOUNT', ownerId: 'ACCOUNT-DEMO-002', workspaceId: 'personal:ACCOUNT-DEMO-002', createdByAccountId: 'ACCOUNT-DEMO-002', businessType: 'EVIDENCE_CALL', status: 'COMPLETED', reasonCode: null, ttlSeconds: CALL_TASK_TTL_SECONDS, createdAt: offset(t104), expiresAt: offset(t104 + CALL_TASK_TTL_SECONDS * 1000), dialerLaunchedAt: offset(t104 + 2000), claimedCallId: 'OC-C2034', claimedAt: offset(t104 + 5000), idempotencyKey: 'IDEM-CT-DEMO-0104', revision: 2 },
      { id: 'CT-DEMO-0201', callerBindingId: 'CB-PERSONAL-7812', normalizedCaller: '13900007812', maskedCaller: '139 **** 7812', maskedCallee: '137 **** 2240', ownerType: 'PERSONAL_ACCOUNT', ownerId: 'ACCOUNT-DEMO-002', workspaceId: 'personal:ACCOUNT-DEMO-002', createdByAccountId: 'ACCOUNT-DEMO-002', businessType: 'EVIDENCE_CALL', status: 'PENDING', reasonCode: null, ttlSeconds: CALL_TASK_TTL_SECONDS, createdAt: offset(base - 10000), expiresAt: offset(base - 10000 + CALL_TASK_TTL_SECONDS * 1000), dialerLaunchedAt: null, claimedCallId: null, claimedAt: null, idempotencyKey: 'IDEM-CT-DEMO-0201', revision: 1 },
    ];
    const calls = [
      { id: 'OC-C2038', taskId: 'CT-DEMO-0101', taskMatch: 'CLAIMED', action: 'DIAL_TASK_CALLEE', taskPath: true, normalizedCaller: '13800006208', expectedCallerMasked: '138 **** 6208', maskedCaller: '138 **** 6208', maskedCallee: '139 **** 3816', calleeConfirmation: 'NONE', progress: 'EVIDENCE_READY', endReason: null, recordingSeconds: 522, uploadStatus: 'RECEIVED', evidenceStatus: 'AVAILABLE', events: successEvents('OC-C2038', t101 + 4000, 522) },
      { id: 'OC-C2037', taskId: null, taskMatch: 'NONE', action: 'ENTER_CALLEE', taskPath: false, normalizedCaller: '01088886688', expectedCallerMasked: null, maskedCaller: '010 **** 6688', maskedCallee: '021 **** 9056', calleeConfirmation: 'DTMF_CONFIRMED', progress: 'EVIDENCE_READY', endReason: null, recordingSeconds: 1038, uploadStatus: 'RECEIVED', evidenceStatus: 'AVAILABLE', events: directEvents('OC-C2037', tNoTask, 1038) },
      { id: 'OC-C2036', taskId: 'CT-DEMO-0102', taskMatch: 'MISMATCH', action: 'REJECT', taskPath: true, normalizedCaller: '18600004471', expectedCallerMasked: '138 **** 6208', maskedCaller: '186 **** 4471', maskedCallee: null, calleeConfirmation: 'NOT_APPLICABLE', progress: 'FAILED', endReason: 'CALLER_MISMATCH', recordingSeconds: 0, uploadStatus: 'NOT_STARTED', evidenceStatus: 'NOT_CREATED', events: [event('OC-C2036', 1, 'INBOUND_RECEIVED', tMismatch), event('OC-C2036', 2, 'CALL_FAILED', tMismatch + 2000, 'CALLER_MISMATCH')] },
      { id: 'OC-C2035', taskId: 'CT-DEMO-0103', taskMatch: 'CLAIMED', action: 'DIAL_TASK_CALLEE', taskPath: true, normalizedCaller: '13800006208', expectedCallerMasked: '138 **** 6208', maskedCaller: '138 **** 6208', maskedCallee: '135 **** 7781', calleeConfirmation: 'NONE', progress: 'FAILED', endReason: 'NO_ANSWER', recordingSeconds: 0, uploadStatus: 'NOT_STARTED', evidenceStatus: 'NOT_CREATED', events: [event('OC-C2035', 1, 'INBOUND_RECEIVED', t103 + 4000), event('OC-C2035', 2, 'TASK_CLAIMED', t103 + 4000), event('OC-C2035', 3, 'OUTBOUND_STARTED', t103 + 6000), event('OC-C2035', 4, 'RINGING', t103 + 8000), event('OC-C2035', 5, 'CALL_FAILED', t103 + 30000, 'NO_ANSWER')] },
      { id: 'OC-C2034', taskId: 'CT-DEMO-0104', taskMatch: 'CLAIMED', action: 'DIAL_TASK_CALLEE', taskPath: true, normalizedCaller: '13900007812', expectedCallerMasked: '139 **** 7812', maskedCaller: '139 **** 7812', maskedCallee: '137 **** 2240', calleeConfirmation: 'NONE', progress: 'EVIDENCE_READY', endReason: null, recordingSeconds: 744, uploadStatus: 'RECEIVED', evidenceStatus: 'AVAILABLE', events: successEvents('OC-C2034', t104 + 4000, 744) },
    ];
    const state = { schemaVersion: CALL_TASK_SCHEMA_VERSION, ttlSeconds: CALL_TASK_TTL_SECONDS, callerBindings, tasks, calls, sequence: 2000 };
    if (!validateCallTask(state).ok) throw new Error('Invalid call-task demo fixture');
    return state;
  }

  function createCallTask(state, input, now) {
    return callTaskMutate(state, draft => {
      if (!plain(input)) return fail('INVALID_TRANSITION');
      const allowed = ['callerBindingId', 'maskedCallee', 'businessType', 'idempotencyKey', 'replacePending'];
      if (Object.keys(input).some(key => !allowed.includes(key))) return fail('INVALID_TRANSITION');
      if (!id(input.callerBindingId) || !textBetween(input.maskedCallee, 1, 40) || !id(input.idempotencyKey)) return fail('INVALID_TRANSITION');
      if (input.businessType !== undefined && input.businessType !== 'EVIDENCE_CALL') return fail('INVALID_TRANSITION');
      if (input.replacePending !== undefined && typeof input.replacePending !== 'boolean') return fail('INVALID_TRANSITION');
      const binding = one(draft.callerBindings, item => item.id === input.callerBindingId);
      if (!binding) return fail('NOT_FOUND');
      if (!binding.active) return fail('CALLER_BINDING_INACTIVE');
      const time = toMs(now);
      const sameKey = one(draft.tasks, item => item.idempotencyKey === input.idempotencyKey);
      const sameFingerprint = sameKey && sameKey.callerBindingId === binding.id && sameKey.maskedCallee === input.maskedCallee && sameKey.businessType === 'EVIDENCE_CALL';
      if (sameKey && !sameFingerprint) return fail('INVALID_TRANSITION');
      reapExpiredCallTasks(draft, binding.normalizedCaller, now);
      if (sameKey) return duplicate(sameKey);
      const activeCall = draft.calls.find(item => item.normalizedCaller === binding.normalizedCaller && CALL_TASK_ACTIVE_PROGRESS.has(item.progress));
      if (activeCall) return fail('CALL_IN_PROGRESS');
      const pending = one(draft.tasks, item => item.normalizedCaller === binding.normalizedCaller && CALL_TASK_NONTERMINAL.has(item.status));
      if (pending && pending.status === 'CLAIMED') return fail('CALL_IN_PROGRESS');
      if (pending) {
        const sameOwner = pending.ownerType === binding.ownerType && pending.ownerId === binding.ownerId && pending.workspaceId === binding.workspaceId;
        if (!sameOwner) return fail('CALLER_BUSY');
        if (pending.maskedCallee === input.maskedCallee) return duplicate(pending);
        if (input.replacePending !== true) return fail('PENDING_TASK_EXISTS');
        pending.status = 'REPLACED';
        pending.reasonCode = 'REPLACED';
        pending.revision += 1;
      }
      const task = {
        id: nextCallTaskId(draft, 'CT'),
        callerBindingId: binding.id,
        normalizedCaller: binding.normalizedCaller,
        maskedCaller: binding.maskedCaller,
        maskedCallee: input.maskedCallee,
        ownerType: binding.ownerType,
        ownerId: binding.ownerId,
        workspaceId: binding.workspaceId,
        createdByAccountId: binding.ownerId,
        businessType: 'EVIDENCE_CALL',
        status: 'PENDING',
        reasonCode: null,
        ttlSeconds: CALL_TASK_TTL_SECONDS,
        createdAt: iso(now),
        expiresAt: new Date(time + CALL_TASK_TTL_SECONDS * 1000).toISOString(),
        dialerLaunchedAt: null,
        claimedCallId: null,
        claimedAt: null,
        idempotencyKey: input.idempotencyKey,
        revision: 1,
      };
      draft.tasks.push(task);
      return ok(task);
    });
  }

  function cancelCallTask(state, input, now) {
    return callTaskMutate(state, draft => {
      if (!plain(input) || !id(input.taskId) || !id(input.accountId) || !id(input.workspaceId)) return fail('INVALID_TRANSITION');
      const task = one(draft.tasks, item => item.id === input.taskId);
      if (!task) return fail('NOT_FOUND');
      if (task.ownerType !== 'PERSONAL_ACCOUNT' || task.ownerId !== input.accountId || task.workspaceId !== input.workspaceId) return fail('NOT_FOUND');
      if (task.status !== 'PENDING') return fail('INVALID_TRANSITION');
      if (toMs(now) >= Date.parse(task.expiresAt)) return fail('TASK_EXPIRED');
      task.status = 'CANCELLED';
      task.reasonCode = 'CANCELLED';
      task.revision += 1;
      return ok(task);
    });
  }

  function recordDialerLaunch(state, input, now) {
    return callTaskMutate(state, draft => {
      if (!plain(input) || !id(input.taskId) || !id(input.accountId) || !id(input.workspaceId)) return fail('INVALID_TRANSITION');
      const task = one(draft.tasks, item => item.id === input.taskId);
      if (!task) return fail('NOT_FOUND');
      if (task.ownerType !== 'PERSONAL_ACCOUNT' || task.ownerId !== input.accountId || task.workspaceId !== input.workspaceId) return fail('NOT_FOUND');
      if (task.status !== 'PENDING') return fail('INVALID_TRANSITION');
      if (toMs(now) >= Date.parse(task.expiresAt)) return fail('TASK_EXPIRED');
      if (task.dialerLaunchedAt) return duplicate({ taskId: task.id, dialerLaunchedAt: task.dialerLaunchedAt });
      task.dialerLaunchedAt = iso(now);
      task.revision += 1;
      return ok({ taskId: task.id, dialerLaunchedAt: task.dialerLaunchedAt });
    });
  }

  function resolveIvrInbound(state, input, now) {
    return callTaskMutate(state, draft => {
      if (!plain(input) || !id(input.callId) || !textBetween(input.normalizedCaller, 1, 20)) return fail('INVALID_TRANSITION');
      if (typeof input.callerTrusted !== 'boolean' || !CALL_TASK_ENUMS.callerState.has(input.callerState)) return fail('INVALID_TRANSITION');
      if (!safeBetween(input.availableSeconds, 0, 86400) || typeof input.directAllowed !== 'boolean') return fail('INVALID_TRANSITION');
      const existing = one(draft.calls, item => item.id === input.callId);
      if (existing) {
        if (existing.normalizedCaller !== input.normalizedCaller) return fail('INVALID_TRANSITION');
        return duplicate(decisionForCall(existing));
      }
      if (!input.callerTrusted) return fail('REJECT');
      const normalizedCaller = input.normalizedCaller;
      // Ownership is resolved from the unique active caller binding before any
      // branch that writes a task or creates a call. Missing or ambiguous
      // ownership fails closed.
      const binding = uniqueCallerBinding(draft, normalizedCaller);
      if (input.callerState === 'SUSPENDED') {
        if (!binding) return fail('REJECT');
        let rejectedTask = false;
        for (const task of draft.tasks) {
          if (task.status === 'PENDING'
            && task.normalizedCaller === normalizedCaller
            && task.callerBindingId === binding.id
            && task.ownerType === binding.ownerType
            && task.ownerId === binding.ownerId
            && task.workspaceId === binding.workspaceId) {
            task.status = 'REJECTED';
            task.reasonCode = 'SUSPENDED';
            task.revision += 1;
            rejectedTask = true;
          }
        }
        return ok({ action: 'REJECT_SUSPENDED', callId: null, taskId: null, taskMatch: 'REJECTED', progress: null, maskedCallee: null, calleeConfirmation: 'NOT_APPLICABLE', taskPath: false, taskRejected: rejectedTask }, 'REJECT_SUSPENDED');
      }
      const activeCall = draft.calls.find(item => item.normalizedCaller === normalizedCaller && CALL_TASK_ACTIVE_PROGRESS.has(item.progress));
      if (activeCall) return fail('REJECT_CALL_IN_PROGRESS');
      reapExpiredCallTasks(draft, normalizedCaller, now);
      if (!binding) return fail('REJECT');
      if (input.callerState === 'INACTIVE' || input.callerState === 'UNREGISTERED') {
        return ok({ action: 'GUIDE_ACTIVATION', callId: null, taskId: null, taskMatch: 'NONE', progress: null, maskedCallee: null, calleeConfirmation: 'NOT_APPLICABLE', taskPath: false }, 'GUIDE');
      }
      if (input.availableSeconds <= 0) {
        return ok({ action: 'GUIDE_PURCHASE', callId: null, taskId: null, taskMatch: 'NONE', progress: null, maskedCallee: null, calleeConfirmation: 'NOT_APPLICABLE', taskPath: false }, 'GUIDE');
      }
      const callerPending = draft.tasks.filter(item => item.normalizedCaller === normalizedCaller && item.status === 'PENDING');
      // Claiming is ownership-scoped exactly like the SUSPENDED branch: the
      // task must belong to the unique active binding resolved above. A
      // same-caller pending task owned by another scope must not be claimable
      // and must not fall through to the no-task DTMF path.
      const pending = callerPending.find(item => item.callerBindingId === binding.id
        && item.ownerType === binding.ownerType
        && item.ownerId === binding.ownerId
        && item.workspaceId === binding.workspaceId);
      if (!pending && callerPending.length) return fail('REJECT');
      if (pending && toMs(now) < Date.parse(pending.expiresAt)) {
        const at = iso(now);
        pending.status = 'CLAIMED';
        pending.claimedCallId = input.callId;
        pending.claimedAt = at;
        pending.revision += 1;
        const call = {
          id: input.callId,
          taskId: pending.id,
          taskMatch: 'CLAIMED',
          action: 'DIAL_TASK_CALLEE',
          taskPath: true,
          normalizedCaller,
          expectedCallerMasked: pending.maskedCaller,
          maskedCaller: pending.maskedCaller,
          maskedCallee: pending.maskedCallee,
          calleeConfirmation: 'NONE',
          progress: 'CLAIMED',
          endReason: null,
          recordingSeconds: 0,
          uploadStatus: 'NOT_STARTED',
          evidenceStatus: 'NOT_CREATED',
          events: [
            { eventId: `${input.callId}-INBOUND`, type: 'INBOUND_RECEIVED', at, source: 'IVR', detail: '' },
            { eventId: `${input.callId}-CLAIM`, type: 'TASK_CLAIMED', at, source: 'ONECALL', detail: pending.id },
          ],
        };
        draft.calls.push(call);
        return ok({ action: 'DIAL_TASK_CALLEE', callId: call.id, taskId: pending.id, taskMatch: 'CLAIMED', progress: 'CLAIMED', maskedCallee: pending.maskedCallee, calleeConfirmation: 'NONE', taskPath: true });
      }
      // directAllowed only gates the no-task path; a valid task is claimed above.
      if (input.directAllowed !== true) {
        return ok({ action: 'GUIDE_PURCHASE', callId: null, taskId: null, taskMatch: 'NONE', progress: null, maskedCallee: null, calleeConfirmation: 'NOT_APPLICABLE', taskPath: false }, 'GUIDE');
      }
      const call = {
        id: input.callId,
        taskId: null,
        taskMatch: 'NONE',
        action: 'ENTER_CALLEE',
        taskPath: false,
        normalizedCaller,
        expectedCallerMasked: null,
        maskedCaller: binding.maskedCaller,
        maskedCallee: null,
        calleeConfirmation: 'DTMF_REQUIRED',
        progress: 'WAITING_CALLEE_INPUT',
        endReason: null,
        recordingSeconds: 0,
        uploadStatus: 'NOT_STARTED',
        evidenceStatus: 'NOT_CREATED',
        events: [{ eventId: `${input.callId}-INBOUND`, type: 'INBOUND_RECEIVED', at: iso(now), source: 'IVR', detail: '' }],
      };
      draft.calls.push(call);
      return ok({ action: 'ENTER_CALLEE', callId: call.id, taskId: null, taskMatch: 'NONE', progress: 'WAITING_CALLEE_INPUT', maskedCallee: null, calleeConfirmation: 'DTMF_REQUIRED', taskPath: false });
    });
  }

  function confirmDirectCallee(state, input, now) {
    return callTaskMutate(state, draft => {
      if (!plain(input) || !id(input.callId) || !textBetween(input.normalizedCaller, 1, 20) || !textBetween(input.maskedCallee, 1, 40) || !id(input.eventId)) return fail('INVALID_TRANSITION');
      if (input.taskId !== undefined && input.taskId !== null) return fail('INVALID_TRANSITION');
      const call = one(draft.calls, item => item.id === input.callId);
      if (!call) return fail('NOT_FOUND');
      if (call.normalizedCaller !== input.normalizedCaller) return fail('INVALID_TRANSITION');
      const existing = findCallEvent(draft, input.eventId);
      if (existing) {
        if (existing.call.id !== input.callId || existing.event.type !== 'DTMF_CALLEE_CONFIRMED' || existing.event.detail !== input.maskedCallee) return fail('INVALID_TRANSITION');
        return duplicate({ callId: call.id, maskedCallee: call.maskedCallee, progress: 'CALLEE_CONFIRMED', calleeConfirmation: 'DTMF_CONFIRMED', taskPath: false });
      }
      if (call.action !== 'ENTER_CALLEE' || call.taskId !== null || call.taskPath !== false) return fail('INVALID_TRANSITION');
      if (call.progress !== 'WAITING_CALLEE_INPUT' || call.calleeConfirmation !== 'DTMF_REQUIRED') return fail('INVALID_TRANSITION');
      const last = call.events[call.events.length - 1];
      if (toMs(now) < Date.parse(last.at)) return fail('INVALID_TRANSITION');
      call.events.push({ eventId: input.eventId, type: 'DTMF_CALLEE_CONFIRMED', at: iso(now), source: 'IVR', detail: input.maskedCallee });
      call.maskedCallee = input.maskedCallee;
      call.calleeConfirmation = 'DTMF_CONFIRMED';
      call.progress = 'CALLEE_CONFIRMED';
      return ok({ callId: call.id, maskedCallee: call.maskedCallee, progress: 'CALLEE_CONFIRMED', calleeConfirmation: 'DTMF_CONFIRMED', taskPath: false });
    });
  }

  function recordCallFact(state, input, now) {
    return callTaskMutate(state, draft => {
      if (!plain(input) || !id(input.callId) || !id(input.eventId) || !CALL_TASK_ENUMS.eventType.has(input.type)) return fail('INVALID_TRANSITION');
      const call = one(draft.calls, item => item.id === input.callId);
      if (!call) return fail('NOT_FOUND');
      const at = input.at === undefined ? iso(now) : input.at;
      if (!date(at)) return fail('INVALID_TRANSITION');
      const detail = input.detail === undefined ? '' : input.detail;
      if (typeof detail !== 'string' || detail.length > 120) return fail('INVALID_TRANSITION');
      if (input.recordingSeconds !== undefined && !safeBetween(input.recordingSeconds, 0, 86400)) return fail('INVALID_TRANSITION');
      const existing = findCallEvent(draft, input.eventId);
      if (existing) {
        if (existing.call.id !== input.callId || existing.event.type !== input.type || existing.event.detail !== detail || existing.event.at !== at) return fail('INVALID_TRANSITION');
        return duplicate({ callId: call.id, eventId: input.eventId, type: input.type, at: existing.event.at, detail });
      }
      const last = call.events[call.events.length - 1];
      if (toMs(at) < Date.parse(last.at)) return fail('INVALID_TRANSITION');
      if (!allowedNextCallEvents(call).includes(input.type)) return fail('INVALID_TRANSITION');
      if (input.type === 'CALL_FAILED' && !CALL_TASK_FAILURE_REASONS.has(detail)) return fail('INVALID_TRANSITION');
      if (input.type === 'TASK_CLAIMED') return fail('INVALID_TRANSITION');
      if (input.type === 'DTMF_CALLEE_CONFIRMED') return fail('INVALID_TRANSITION');
      if (input.type === 'INBOUND_RECEIVED') return fail('INVALID_TRANSITION');
      const event = { eventId: input.eventId, type: input.type, at, source: input.type === 'CALL_FAILED' ? 'IVR' : CALL_TASK_EVENT_SOURCE[input.type], detail };
      call.events.push(event);
      const beforeBridge = ['INBOUND_RECEIVED', 'TASK_CLAIMED', 'DTMF_CALLEE_CONFIRMED', 'OUTBOUND_STARTED', 'RINGING'].includes(last.type);
      if (input.type === 'CALL_ENDED') {
        call.recordingSeconds = input.recordingSeconds === undefined ? call.recordingSeconds : input.recordingSeconds;
        call.progress = 'ENDED';
      } else if (input.type === 'CALL_FAILED') {
        call.recordingSeconds = beforeBridge ? 0 : (input.recordingSeconds || 0);
        call.endReason = detail;
        call.progress = 'FAILED';
      } else if (input.type === 'RECORDING_RECEIVED') {
        call.uploadStatus = 'RECEIVED';
        call.progress = 'RECORDING_RECEIVED';
      } else if (input.type === 'EVIDENCE_PROCESSING') {
        call.evidenceStatus = 'PROCESSING';
        call.progress = 'EVIDENCE_PROCESSING';
      } else if (input.type === 'EVIDENCE_READY') {
        call.evidenceStatus = 'AVAILABLE';
        call.progress = 'EVIDENCE_READY';
      } else {
        call.progress = CALL_TASK_EVENT_PROGRESS[input.type];
      }
      const task = call.taskId ? one(draft.tasks, item => item.id === call.taskId) : null;
      if (task && task.claimedCallId === call.id) {
        if (input.type === 'CALL_ENDED' && task.status === 'CLAIMED') {
          task.status = 'COMPLETED';
          task.reasonCode = null;
          task.revision += 1;
        }
        if (input.type === 'CALL_FAILED' && task.status === 'CLAIMED') {
          task.status = 'FAILED';
          task.reasonCode = detail;
          task.revision += 1;
        }
      }
      return ok({ callId: call.id, eventId: event.eventId, type: event.type, at: event.at, detail: event.detail, progress: call.progress });
    });
  }

  function callTaskSnapshot(state) {
    const validation = validateCallTask(state);
    return validation.ok ? ok(clone(pickCallTask(state))) : validation;
  }

  function loadCallTaskSnapshot(state, candidate) {
    if (!plain(state)) return fail('INVALID_CALL_TASK');
    const validation = validateCallTask(candidate);
    if (!validation.ok) return validation;
    const copied = clone(pickCallTask(candidate));
    replaceCallTask(state, copied);
    return ok(clone(copied));
  }

  function uniqueCallerBinding(state, normalizedCaller) {
    if (!plain(state) || !Array.isArray(state.callerBindings) || typeof normalizedCaller !== 'string') return null;
    const matches = state.callerBindings.filter(item => item && item.active === true && item.normalizedCaller === normalizedCaller);
    return matches.length === 1 ? clone(matches[0]) : null;
  }

  function callTaskMutate(state, action) {
    if (!plain(state)) return fail('INVALID_CALL_TASK');
    let draft;
    try { draft = clone(pickCallTask(state)); } catch (_) { return fail('INVALID_CALL_TASK'); }
    let result;
    try { result = action(draft); } catch (_) { return fail('INVALID_CALL_TASK'); }
    if (!result) return fail('INVALID_CALL_TASK');
    if (!result.ok) return result;
    // Duplicate and guide outcomes can carry a deterministic expiry reaping
    // side effect, so the validated draft (identical otherwise) replaces state.
    const validation = validateCallTask(draft);
    if (!validation.ok) return validation;
    replaceCallTask(state, draft);
    if (result.code === 'DUPLICATE') return duplicate(result.value);
    return ok(clone(result.value), result.code);
  }

  function pickCallTask(state) {
    const output = {};
    for (const key of CALL_TASK_FIELDS) output[key] = state[key];
    return output;
  }

  function replaceCallTask(state, source) {
    for (const key of CALL_TASK_FIELDS) state[key] = source[key];
  }

  function nextCallTaskId(state, prefix) {
    const collection = prefix === 'CT' ? state.tasks : state.calls;
    const used = new Set(collection.map(item => item.id));
    let sequence = state.sequence;
    let candidate;
    do {
      if (sequence >= Number.MAX_SAFE_INTEGER) throw new Error('Call-task ID sequence exhausted');
      sequence += 1;
      candidate = `${prefix}-${sequence}`;
    } while (used.has(candidate));
    state.sequence = sequence;
    return candidate;
  }

  function reapExpiredCallTasks(draft, normalizedCaller, now) {
    const time = toMs(now);
    for (const task of draft.tasks) {
      if (task.normalizedCaller === normalizedCaller && task.status === 'PENDING' && time >= Date.parse(task.expiresAt)) {
        task.status = 'EXPIRED';
        task.reasonCode = 'TTL_EXPIRED';
        task.revision += 1;
      }
    }
  }

  function decisionForCall(call) {
    return {
      action: call.action,
      callId: call.id,
      taskId: call.taskId,
      taskMatch: call.taskMatch,
      progress: call.progress,
      maskedCallee: call.maskedCallee,
      calleeConfirmation: call.calleeConfirmation,
      taskPath: call.taskPath,
    };
  }

  function findCallEvent(state, eventId) {
    for (const call of state.calls) {
      for (const event of call.events) {
        if (event.eventId === eventId) return { call, event };
      }
    }
    return null;
  }

  function allowedNextCallEvents(call) {
    const last = call.events.length ? call.events[call.events.length - 1].type : null;
    if (last === 'INBOUND_RECEIVED') return call.taskPath ? ['TASK_CLAIMED', 'CALL_FAILED'] : ['DTMF_CALLEE_CONFIRMED', 'CALL_FAILED'];
    if (last === 'TASK_CLAIMED' || last === 'DTMF_CALLEE_CONFIRMED') return ['OUTBOUND_STARTED', 'CALL_FAILED'];
    if (last === 'OUTBOUND_STARTED') return ['RINGING', 'CALL_FAILED'];
    if (last === 'RINGING') return ['ANSWERED', 'CALL_FAILED'];
    if (last === 'ANSWERED') return ['BRIDGED', 'CALL_FAILED'];
    if (last === 'BRIDGED') return ['RECORDING_STARTED', 'CALL_FAILED'];
    if (last === 'RECORDING_STARTED') return ['CALL_ENDED', 'CALL_FAILED'];
    if (last === 'CALL_ENDED') return ['RECORDING_RECEIVED'];
    if (last === 'RECORDING_RECEIVED') return ['EVIDENCE_PROCESSING'];
    if (last === 'EVIDENCE_PROCESSING') return ['EVIDENCE_READY'];
    return [];
  }

  function validateCallTask(candidate) {
    if (!safeData(candidate) || !plain(candidate)) return fail('INVALID_CALL_TASK');
    const keys = Object.keys(candidate).sort();
    const allowed = [...CALL_TASK_FIELDS].sort();
    if (keys.length !== allowed.length || keys.some((key, index) => key !== allowed[index])) return fail('INVALID_CALL_TASK');
    if (candidate.schemaVersion !== CALL_TASK_SCHEMA_VERSION || candidate.ttlSeconds !== CALL_TASK_TTL_SECONDS) return fail('INVALID_CALL_TASK');
    if (!safeBetween(candidate.sequence, 0, Number.MAX_SAFE_INTEGER)) return fail('INVALID_CALL_TASK');
    if (!Array.isArray(candidate.callerBindings) || !Array.isArray(candidate.tasks) || !Array.isArray(candidate.calls)) return fail('INVALID_CALL_TASK');
    if (!unique(candidate.callerBindings) || !unique(candidate.tasks) || !unique(candidate.tasks, 'idempotencyKey') || !unique(candidate.calls)) return fail('INVALID_CALL_TASK');
    for (const binding of candidate.callerBindings) if (!validCallerBinding(binding)) return fail('INVALID_CALL_TASK');
    const bindingsById = new Map(candidate.callerBindings.map(item => [item.id, item]));
    const tasksById = new Map(candidate.tasks.map(item => [item.id, item]));
    const callsById = new Map(candidate.calls.map(item => [item.id, item]));
    for (const task of candidate.tasks) {
      if (!validCallTask(task, bindingsById)) return fail('INVALID_CALL_TASK');
    }
    const eventIds = new Set();
    for (const call of candidate.calls) {
      if (!validCall(call, tasksById, candidate.callerBindings, eventIds)) return fail('INVALID_CALL_TASK');
    }
    const nonterminalByCaller = new Map();
    for (const task of candidate.tasks) {
      if (!CALL_TASK_NONTERMINAL.has(task.status)) continue;
      if (nonterminalByCaller.has(task.normalizedCaller)) return fail('INVALID_CALL_TASK');
      nonterminalByCaller.set(task.normalizedCaller, task.id);
    }
    const activeByCaller = new Map();
    for (const call of candidate.calls) {
      if (!CALL_TASK_ACTIVE_PROGRESS.has(call.progress)) continue;
      if (activeByCaller.has(call.normalizedCaller)) return fail('INVALID_CALL_TASK');
      activeByCaller.set(call.normalizedCaller, call.id);
    }
    for (const task of candidate.tasks) {
      if (['CLAIMED', 'COMPLETED', 'FAILED'].includes(task.status)) {
        const call = callsById.get(task.claimedCallId);
        if (!call || call.taskId !== task.id) return fail('INVALID_CALL_TASK');
        if (task.status === 'CLAIMED' && !CALL_TASK_ACTIVE_PROGRESS.has(call.progress)) return fail('INVALID_CALL_TASK');
        if (task.status === 'COMPLETED' && !['ENDED', 'RECORDING_RECEIVED', 'EVIDENCE_PROCESSING', 'EVIDENCE_READY'].includes(call.progress)) return fail('INVALID_CALL_TASK');
        if (task.status === 'FAILED' && call.progress !== 'FAILED') return fail('INVALID_CALL_TASK');
      }
    }
    return ok(true);
  }

  function validCallerBinding(binding) {
    if (!exactKeys(binding, ['id', 'ownerType', 'ownerId', 'workspaceId', 'maskedCaller', 'normalizedCaller', 'numberType', 'active'])) return false;
    if (!id(binding.id) || !CALL_TASK_ENUMS.ownerType.has(binding.ownerType) || !id(binding.ownerId) || !id(binding.workspaceId) || !textBetween(binding.maskedCaller, 1, 40) || !textBetween(binding.normalizedCaller, 1, 20) || !CALL_TASK_ENUMS.numberType.has(binding.numberType) || typeof binding.active !== 'boolean') return false;
    if (binding.ownerType === 'PERSONAL_ACCOUNT' && binding.workspaceId !== `personal:${binding.ownerId}`) return false;
    if (binding.ownerType === 'ORGANIZATION' && binding.workspaceId !== `organization:${binding.ownerId}`) return false;
    return true;
  }

  function validCallTask(task, bindingsById) {
    if (!exactKeys(task, ['id', 'callerBindingId', 'normalizedCaller', 'maskedCaller', 'maskedCallee', 'ownerType', 'ownerId', 'workspaceId', 'createdByAccountId', 'businessType', 'status', 'reasonCode', 'ttlSeconds', 'createdAt', 'expiresAt', 'dialerLaunchedAt', 'claimedCallId', 'claimedAt', 'idempotencyKey', 'revision'])) return false;
    if (!id(task.id) || !id(task.callerBindingId) || !textBetween(task.normalizedCaller, 1, 20) || !textBetween(task.maskedCaller, 1, 40) || !textBetween(task.maskedCallee, 1, 40)) return false;
    if (!CALL_TASK_ENUMS.ownerType.has(task.ownerType) || !id(task.ownerId) || !id(task.workspaceId) || !id(task.createdByAccountId)) return false;
    if (!CALL_TASK_ENUMS.businessType.has(task.businessType) || !CALL_TASK_ENUMS.taskStatus.has(task.status)) return false;
    if (task.reasonCode !== null && !CALL_TASK_REASONS.has(task.reasonCode)) return false;
    if (task.ttlSeconds !== CALL_TASK_TTL_SECONDS) return false;
    if (!date(task.createdAt) || !date(task.expiresAt) || Date.parse(task.expiresAt) - Date.parse(task.createdAt) !== CALL_TASK_TTL_SECONDS * 1000) return false;
    if (task.dialerLaunchedAt !== null && !date(task.dialerLaunchedAt)) return false;
    if (!id(task.idempotencyKey) || !safePositive(task.revision)) return false;
    const binding = bindingsById.get(task.callerBindingId);
    if (!binding || binding.normalizedCaller !== task.normalizedCaller || binding.maskedCaller !== task.maskedCaller || binding.ownerType !== task.ownerType || binding.ownerId !== task.ownerId || binding.workspaceId !== task.workspaceId) return false;
    if (task.status === 'PENDING') {
      if (task.reasonCode !== null || task.claimedCallId !== null || task.claimedAt !== null) return false;
    } else if (task.status === 'CLAIMED') {
      if (task.reasonCode !== null || !id(task.claimedCallId) || !date(task.claimedAt)) return false;
    } else if (task.status === 'COMPLETED') {
      if (task.reasonCode !== null || !id(task.claimedCallId) || !date(task.claimedAt)) return false;
    } else if (task.status === 'FAILED') {
      if (!CALL_TASK_FAILURE_REASONS.has(task.reasonCode) || !id(task.claimedCallId) || !date(task.claimedAt)) return false;
    } else {
      if (task.claimedCallId !== null || task.claimedAt !== null) return false;
      if (task.status === 'EXPIRED' && task.reasonCode !== 'TTL_EXPIRED') return false;
      if (task.status === 'REPLACED' && task.reasonCode !== 'REPLACED') return false;
      if (task.status === 'CANCELLED' && task.reasonCode !== 'CANCELLED') return false;
      if (task.status === 'REJECTED' && !['SUSPENDED', 'CALLER_MISMATCH'].includes(task.reasonCode)) return false;
    }
    if (task.claimedAt !== null && (Date.parse(task.claimedAt) < Date.parse(task.createdAt) || Date.parse(task.claimedAt) >= Date.parse(task.expiresAt))) return false;
    return true;
  }

  function validCall(call, tasksById, callerBindings, eventIds) {
    if (!exactKeys(call, ['id', 'taskId', 'taskMatch', 'action', 'taskPath', 'normalizedCaller', 'expectedCallerMasked', 'maskedCaller', 'maskedCallee', 'calleeConfirmation', 'progress', 'endReason', 'recordingSeconds', 'uploadStatus', 'evidenceStatus', 'events'])) return false;
    if (!id(call.id) || !textBetween(call.normalizedCaller, 1, 20) || !textBetween(call.maskedCaller, 1, 40)) return false;
    if (call.expectedCallerMasked !== null && !textBetween(call.expectedCallerMasked, 1, 40)) return false;
    if (call.maskedCallee !== null && !textBetween(call.maskedCallee, 1, 40)) return false;
    if (!CALL_TASK_ENUMS.callAction.has(call.action) || !CALL_TASK_ENUMS.taskMatch.has(call.taskMatch) || !CALL_TASK_ENUMS.calleeConfirmation.has(call.calleeConfirmation)) return false;
    if (!CALL_TASK_ENUMS.callProgress.has(call.progress) || !CALL_TASK_ENUMS.upload.has(call.uploadStatus) || !CALL_TASK_ENUMS.evidence.has(call.evidenceStatus)) return false;
    if (typeof call.taskPath !== 'boolean' || !safeBetween(call.recordingSeconds, 0, 86400)) return false;
    if (call.endReason !== null && !CALL_TASK_REASONS.has(call.endReason)) return false;
    if (call.taskId !== null && !id(call.taskId)) return false;
    if (call.taskPath !== (call.taskId !== null)) return false;
    if (!Array.isArray(call.events) || !call.events.length) return false;
    for (const [index, event] of call.events.entries()) {
      if (!exactKeys(event, ['eventId', 'type', 'at', 'source', 'detail']) || !id(event.eventId) || !CALL_TASK_ENUMS.eventType.has(event.type) || !date(event.at) || !CALL_TASK_ENUMS.eventSource.has(event.source) || typeof event.detail !== 'string' || event.detail.length > 120) return false;
      if (eventIds.has(event.eventId)) return false;
      eventIds.add(event.eventId);
      if (index === 0) {
        if (event.type !== 'INBOUND_RECEIVED' || event.source !== 'IVR') return false;
        continue;
      }
      if (!allowedNextCallEvents({ events: call.events.slice(0, index), taskPath: call.taskPath }).includes(event.type)) return false;
      if (Date.parse(event.at) < Date.parse(call.events[index - 1].at)) return false;
      if (event.type === 'CALL_FAILED') {
        if (!CALL_TASK_FAILURE_REASONS.has(event.detail)) return false;
        if (event.source !== 'IVR' && event.source !== 'ONECALL') return false;
      } else if (CALL_TASK_EVENT_SOURCE[event.type] !== event.source) return false;
    }
    const has = type => call.events.some(event => event.type === type);
    const last = call.events[call.events.length - 1];
    const derivedProgress = last.type === 'INBOUND_RECEIVED' ? (call.action === 'ENTER_CALLEE' ? 'WAITING_CALLEE_INPUT' : 'WAITING_INBOUND') : CALL_TASK_EVENT_PROGRESS[last.type];
    if (call.progress !== derivedProgress) return false;
    const derivedUpload = has('RECORDING_RECEIVED') ? 'RECEIVED' : 'NOT_STARTED';
    const derivedEvidence = has('EVIDENCE_READY') ? 'AVAILABLE' : has('EVIDENCE_PROCESSING') ? 'PROCESSING' : 'NOT_CREATED';
    if (call.uploadStatus !== derivedUpload || call.evidenceStatus !== derivedEvidence) return false;
    if (call.action === 'DIAL_TASK_CALLEE') {
      if (!call.taskPath || call.taskMatch !== 'CLAIMED' || call.calleeConfirmation !== 'NONE' || !call.maskedCallee) return false;
      const task = tasksById.get(call.taskId);
      if (!task || task.claimedCallId !== call.id || task.normalizedCaller !== call.normalizedCaller || task.maskedCallee !== call.maskedCallee || task.maskedCaller !== call.expectedCallerMasked) return false;
      if (!has('TASK_CLAIMED') || !call.events.some(event => event.type === 'TASK_CLAIMED' && event.at === task.claimedAt)) return false;
    } else if (call.action === 'ENTER_CALLEE') {
      if (call.taskPath || call.taskMatch !== 'NONE' || call.taskId !== null || call.expectedCallerMasked !== null) return false;
      if (call.calleeConfirmation === 'DTMF_REQUIRED') {
        if (call.progress !== 'WAITING_CALLEE_INPUT' || has('DTMF_CALLEE_CONFIRMED') || call.maskedCallee !== null) return false;
      } else if (call.calleeConfirmation === 'DTMF_CONFIRMED') {
        if (call.progress === 'WAITING_CALLEE_INPUT' || !has('DTMF_CALLEE_CONFIRMED') || !call.maskedCallee) return false;
      } else return false;
    } else {
      if (call.progress !== 'FAILED' || call.calleeConfirmation !== 'NOT_APPLICABLE' || call.maskedCallee !== null || !call.endReason) return false;
      if (call.taskId !== null) {
        const task = tasksById.get(call.taskId);
        if (!task || call.taskMatch !== 'MISMATCH' || task.claimedCallId === call.id) return false;
        if (call.expectedCallerMasked === null || call.expectedCallerMasked === call.maskedCaller) return false;
      } else if (call.taskMatch !== 'NONE') return false;
    }
    if (call.progress === 'FAILED') {
      if (!has('CALL_FAILED') || call.endReason !== last.detail) return false;
      const failureIndex = call.events.findIndex(event => event.type === 'CALL_FAILED');
      const beforeBridge = !call.events.slice(0, failureIndex).some(event => event.type === 'BRIDGED');
      if (beforeBridge && (call.recordingSeconds !== 0 || call.uploadStatus !== 'NOT_STARTED' || call.evidenceStatus !== 'NOT_CREATED')) return false;
    } else if (call.endReason !== null) return false;
    if (has('CALL_ENDED')) {
      if (!has('RECORDING_STARTED')) return false;
      if (call.uploadStatus === 'RECEIVED' && call.recordingSeconds <= 0) return false;
    } else if (call.recordingSeconds !== 0) return false;
    if (has('EVIDENCE_READY') && !has('EVIDENCE_PROCESSING')) return false;
    if (has('EVIDENCE_PROCESSING') && call.uploadStatus !== 'RECEIVED') return false;
    if (has('RECORDING_RECEIVED') && !has('CALL_ENDED')) return false;
    const binding = uniqueBindingFromList(callerBindings, call.normalizedCaller);
    if (binding && binding.maskedCaller !== call.maskedCaller) return false;
    return true;
  }

  function uniqueBindingFromList(callerBindings, normalizedCaller) {
    const matches = callerBindings.filter(item => item.active === true && item.normalizedCaller === normalizedCaller);
    return matches.length === 1 ? matches[0] : null;
  }

  function validateState(candidate) {
    if (!safeData(candidate) || !plain(candidate)) return fail('INVALID_TRANSITION');
    const keys = Object.keys(candidate).sort();
    const allowed = [...BUSINESS_FIELDS].sort();
    if (keys.length !== allowed.length || keys.some((key, index) => key !== allowed[index]) || keys.some(key => FORBIDDEN_IMPORT_KEYS.has(key))) return fail('INVALID_TRANSITION');
    if (candidate.schemaVersion !== schemaVersion || !safeBetween(candidate.sequence, 0, Number.MAX_SAFE_INTEGER) || !plain(candidate.processedEvents)) return fail('INVALID_TRANSITION');
    const arrays = ['catalog', 'subjects', 'claims', 'trialGrants', 'sessions', 'evidence', 'activations', 'activationTransitions', 'orders', 'entitlements'];
    if (arrays.some(key => !Array.isArray(candidate[key]))) return fail('INVALID_TRANSITION');
    if (!validRule(candidate.rule)) return fail('INVALID_TRANSITION');
    if (!unique(candidate.catalog, 'versionId') || !unique(candidate.subjects) || !unique(candidate.claims) || !unique(candidate.claims, 'requestId') || !unique(candidate.trialGrants) || !unique(candidate.sessions) || !unique(candidate.evidence) || !unique(candidate.activations) || !unique(candidate.activationTransitions) || !unique(candidate.orders) || !unique(candidate.orders, 'idempotencyKey') || !unique(candidate.entitlements)) return fail('INVALID_TRANSITION');
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
    if (!activationTransitionLedgerValid(candidate)) return fail('INVALID_TRANSITION');
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
    if (!validProcessedEvents(candidate) || !evidenceEventCoverageValid(candidate) || !activationStatusEventCoverageValid(candidate)) return fail('INVALID_TRANSITION');
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

  function evidenceEventCoverageValid(state) {
    if (!Array.isArray(state.evidence) || !validEventDomain(state, 'EVIDENCE')) return false;
    const coverage = new Map();
    for (const record of Object.values(state.processedEvents.EVIDENCE)) {
      const evidenceId = record && record.result && record.result.evidence && record.result.evidence.id;
      coverage.set(evidenceId, (coverage.get(evidenceId) || 0) + 1);
    }
    return state.evidence.length === coverage.size && state.evidence.every(item => coverage.get(item.id) === 1);
  }

  function activationStatusEventCoverageValid(state) {
    if (!Array.isArray(state.activationTransitions) || !validEventDomain(state, 'ACTIVATION_STATUS')) return false;
    const coverage = new Map();
    for (const record of Object.values(state.processedEvents.ACTIVATION_STATUS)) {
      const transitionId = record && record.result && record.result.id;
      coverage.set(transitionId, (coverage.get(transitionId) || 0) + 1);
    }
    return state.activationTransitions.length === coverage.size && state.activationTransitions.every(item => coverage.get(item.id) === 1);
  }

  function activationTransitionLedgerValid(state) {
    if (!Array.isArray(state.subjects) || !Array.isArray(state.activationTransitions)) return false;
    const subjectCount = idCount(state.subjects);
    const transitionTails = new Map();
    for (const transition of state.activationTransitions) {
      if (!validActivationTransition(transition) || subjectCount.get(transition.subjectId) !== 1) return false;
      const previous = transitionTails.get(transition.subjectId);
      if (previous && (
        transition.fromRevision < previous.toRevision ||
        transition.from !== previous.to ||
        Date.parse(transition.at) < Date.parse(previous.at)
      )) return false;
      transitionTails.set(transition.subjectId, transition);
    }
    for (const [subjectId, transition] of transitionTails) {
      const subject = one(state.subjects, item => item.id === subjectId);
      if (!validSubject(subject) || subject.revision < transition.toRevision || subject.activation !== transition.to) return false;
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

  function validActivationTransition(item) {
    return exactKeys(item, ['id', 'subjectId', 'channel', 'from', 'to', 'fromRevision', 'toRevision', 'eventId', 'at']) && id(item.id) && id(item.subjectId) && ENUMS.activationStatusChannel.has(item.channel) && ['ACTIVE', 'SUSPENDED'].includes(item.from) && ['ACTIVE', 'SUSPENDED'].includes(item.to) && item.from !== item.to && safePositive(item.fromRevision) && item.toRevision === item.fromRevision + 1 && safePositive(item.toRevision) && id(item.eventId) && date(item.at);
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
    if (['subjects', 'claims', 'sessions', 'activations', 'activationTransitions', 'trialGrants', 'evidence', 'orders', 'entitlements'].some(name => !Array.isArray(state[name]))) return false;
    const value = record.result;

    if (domain === 'ACTIVATION') {
      if (!exactKeys(input, ['subjectId', 'channel', 'verified', 'consentVersion', 'confirmed']) || !id(input.subjectId) || !ENUMS.purchaseChannel.has(input.channel) || input.verified !== true || input.confirmed !== true || !id(input.consentVersion)) return false;
      if (!exactKeys(value, ['activation', 'grant', 'grantCode']) || !frozenEntity(state.activations, value.activation, [], validActivation)) return false;
      const activation = value.activation;
      const eventActivation = one(state.activations, item => item.channel !== 'IVR' && item.eventId === record.eventId);
      return eventActivation && eventActivation.id === activation.id && activation.eventId === record.eventId && activation.subjectId === input.subjectId && activation.channel === input.channel && activation.consentVersion === input.consentVersion && activation.confirmation === 'EXPLICIT' && activation.sessionId === undefined && referencedSubject(state, input.subjectId) && validGrantResult(state, value, input.subjectId);
    }

    if (domain === 'ACTIVATION_STATUS') {
      if (!exactKeys(input, ['subjectId', 'channel', 'status']) || !id(input.subjectId) || !ENUMS.activationStatusChannel.has(input.channel) || !['ACTIVE', 'SUSPENDED'].includes(input.status)) return false;
      const eventTransition = one(state.activationTransitions, item => item.eventId === record.eventId);
      return frozenEntity(state.activationTransitions, value, [], validActivationTransition) && eventTransition && eventTransition.id === value.id && value.subjectId === input.subjectId && value.channel === input.channel && value.to === input.status && value.eventId === record.eventId && referencedSubject(state, input.subjectId);
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
    demoAccountDirectory,
    createAccountDirectoryFixture,
    validateAccountDirectory,
    resolveDemoLogin,
    workspacesForAccount,
    accountById,
    loginIdentitiesForAccount,
    createDemoState,
    validateCatalogVersion,
    visibleOffers,
    offerVersionToken,
    createOrder,
    applyPaymentResult,
    issueEntitlements,
    activateService,
    setActivationStatus,
    grantTrialOnce,
    claimSubject,
    startIvrSession,
    confirmIvrSession,
    consumeTrial,
    trialStatus,
    snapshot,
    loadReviewSnapshot,
    CALL_TASK_TTL_SECONDS,
    createCallTaskDemoState,
    createCallTask,
    cancelCallTask,
    recordDialerLaunch,
    resolveIvrInbound,
    confirmDirectCallee,
    recordCallFact,
    callTaskSnapshot,
    loadCallTaskSnapshot,
    validateCallTask,
    uniqueCallerBinding,
  });
});
