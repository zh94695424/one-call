const { chromium } = require('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/.pnpm/playwright@1.60.0/node_modules/playwright');
const path = require('path');
const { pathToFileURL } = require('url');

const prototypeRoot = path.resolve(__dirname, '..');
const screenshotRoot = path.join(__dirname, 'screenshots');
const previewUrl = pathToFileURL(path.join(prototypeRoot, 'preview.html')).href;
const newPages = [
  'phoneWechatConfirm', 'phoneWechatIssue', 'phoneWechatConflict', 'phoneSms', 'landlineBind', 'landlineVerify', 'landlineReady',
  'trialHome', 'trialCall', 'trialLimit', 'freeEvidenceLimit',
  'callerSelect', 'dialHandoff', 'landlineCall', 'callerMismatch', 'callerUnavailable',
  'evidenceBox', 'evidenceEmpty', 'matterCreate', 'aiCorrection',
  'usageDetail', 'enterprisePlans', 'productDetail', 'purchaseConfirm', 'paymentSuccess', 'paymentIssue', 'revokeAuth', 'evidenceSelect',
  'documentCenter', 'orders', 'tips', 'privacy', 'support',
];
const authorizationCopy = '我已阅读并同意以上全部授权内容，确认绑定号码由本人实际使用，并同意按上述范围处理敏感个人信息、开通运营商局端录音及进行必要的数据共享。';

async function getFrame(page) {
  const handle = await page.locator('iframe').elementHandle();
  const frame = await handle.contentFrame();
  if (!frame) throw new Error('Visualization iframe not available');
  return frame;
}

async function openPage(frame, pageId) {
  await frame.locator(`[data-page="${pageId}"]`).click();
  await frame.waitForTimeout(50);
}

async function assertNoOverflow(frame, label) {
  const metrics = await frame.locator('.phone').evaluate((phone) => {
    const screen = phone.querySelector('#oc-screen');
    return {
      phoneClient: phone.clientWidth,
      phoneScroll: phone.scrollWidth,
      screenClient: screen.clientWidth,
      screenScroll: screen.scrollWidth,
      docClient: document.documentElement.clientWidth,
      docScroll: document.documentElement.scrollWidth,
    };
  });
  if (metrics.phoneScroll > metrics.phoneClient + 1 || metrics.screenScroll > metrics.screenClient + 1 || metrics.docScroll > metrics.docClient + 1) {
    throw new Error(`${label} horizontal overflow: ${JSON.stringify(metrics)}`);
  }
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  });
  const page = await browser.newPage({ viewport: { width: 760, height: 1100 }, deviceScaleFactor: 1 });
  const errors = [];
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (msg) => { if (msg.type() === 'error') errors.push(msg.text()); });
  await page.goto(previewUrl);
  const frame = await getFrame(page);

  const count = await frame.locator('.index-button').count();
  if (count !== 57) throw new Error(`Expected 57 visible pages, got ${count}`);
  if (await frame.locator('[data-page="consent"], [data-go="consent"]').count()) {
    throw new Error('Standalone authorization page is still reachable');
  }
  if (await frame.locator('[data-page="toolboxArchive"], [data-go="toolboxArchive"], [data-page="import"], [data-go="import"]').count()) {
    throw new Error('Archived toolbox is visible');
  }
  for (const pageId of newPages) {
    if (await frame.locator(`[data-page="${pageId}"]`).count() !== 1) throw new Error(`Missing index page ${pageId}`);
    await openPage(frame, pageId);
    if (!(await frame.locator('#oc-screen').innerText()).trim()) throw new Error(`Empty screen for ${pageId}`);
  }

  await openPage(frame, 'phone');
  const phoneText = await frame.locator('#oc-screen').innerText();
  if (!phoneText.includes('微信授权获取手机号') || !phoneText.includes('短信验证码绑定其他手机号') || !phoneText.includes('绑定固话座机')) {
    throw new Error('Phone binding method choice is incomplete');
  }
  await frame.locator('[data-go="phoneWechatConfirm"]').first().click();
  const wechatPhoneSubmit = frame.locator('[data-phone-submit]');
  if (!(await wechatPhoneSubmit.isDisabled())) throw new Error('Wechat phone confirmation must start disabled');
  if ((await frame.locator('.progress-step').count()) !== 4 || (await frame.locator('.progress-head strong').innerText()) !== '3 / 4') {
    throw new Error('Wechat binding must be step 3 of the four-step opening flow');
  }
  if ((await frame.locator('.authorization-item').count()) !== 1) throw new Error('Wechat binding authorization summary must contain one row');
  const wechatAuthorizationText = await frame.locator('.authorization-summary').innerText();
  if (wechatAuthorizationText.includes('授权号码') || wechatAuthorizationText.includes('敏感信息处理与必要共享') || wechatAuthorizationText.includes('局端录音与存证')) throw new Error('Wechat binding still shows removed authorization rows');
  if ((await frame.locator('.check-copy').innerText()) !== authorizationCopy) throw new Error('Wechat binding authorization copy does not match the approved wording');
  await frame.locator('[data-phone-confirm]').check();
  if (await wechatPhoneSubmit.isDisabled()) throw new Error('Wechat phone confirmation did not unlock');
  if ((await wechatPhoneSubmit.innerText()) !== '同意授权并提交开通') throw new Error('Wechat binding submit label is incorrect');
  await wechatPhoneSubmit.click();
  const wechatResultText = await frame.locator('#oc-screen').innerText();
  if (!wechatResultText.includes('微信平台验证') || !wechatResultText.includes('138 **** 6208')) throw new Error('Wechat verification context did not reach the opening result');
  if ((await frame.locator('.progress-step').count()) !== 4 || (await frame.locator('.progress-head strong').innerText()) !== '4 / 4') {
    throw new Error('Opening result must be step 4 of the four-step flow');
  }

  await openPage(frame, 'phoneSms');
  const smsPhoneSubmit = frame.locator('[data-phone-submit]');
  if (!(await smsPhoneSubmit.isDisabled())) throw new Error('SMS phone confirmation must start disabled');
  if ((await frame.locator('.authorization-item').count()) !== 1) throw new Error('SMS binding authorization summary must contain one row');
  const smsAuthorizationText = await frame.locator('.authorization-summary').innerText();
  if (smsAuthorizationText.includes('授权号码') || smsAuthorizationText.includes('敏感信息处理与必要共享') || smsAuthorizationText.includes('局端录音与存证')) throw new Error('SMS binding still shows removed authorization rows');
  if ((await frame.locator('.check-copy').innerText()) !== authorizationCopy) throw new Error('SMS binding authorization copy does not match the approved wording');
  await frame.locator('[data-phone-confirm]').check();
  if (await smsPhoneSubmit.isDisabled()) throw new Error('SMS phone confirmation did not unlock');
  await smsPhoneSubmit.click();
  const smsResultText = await frame.locator('#oc-screen').innerText();
  if (!smsResultText.includes('短信验证码验证') || !smsResultText.includes('186 **** 4471')) throw new Error('SMS verification context did not reach the opening result');

  await openPage(frame, 'landlineBind');
  const landlineSubmit = frame.locator('[data-landline-submit]');
  if (!(await landlineSubmit.isDisabled())) throw new Error('Landline verification must require explicit usage-right confirmation');
  const landlineBindText = await frame.locator('#oc-screen').innerText();
  if (!landlineBindText.includes('仅支持独立固话直线') || !landlineBindText.includes('企业总机、PBX')) throw new Error('Landline scope boundary is missing');
  if (!(await frame.locator('.check-copy').innerText()).includes('合法使用和管理权限')) throw new Error('Landline binding lacks the dedicated legal-use confirmation');
  await frame.locator('[data-landline-confirm]').check();
  if (await landlineSubmit.isDisabled()) throw new Error('Landline confirmation did not unlock verification');
  await landlineSubmit.click();
  const landlineVerifyText = await frame.locator('#oc-screen').innerText();
  if (!landlineVerifyText.includes('95XXX') || !landlineVerifyText.includes('483 621') || !landlineVerifyText.includes('真实主叫')) throw new Error('Landline caller verification instructions are incomplete');
  await frame.locator('[data-go="landlineReady"]').click();
  const landlineReadyText = await frame.locator('#oc-screen').innerText();
  if (!landlineReadyText.includes('010 **** 6688') || !landlineReadyText.includes('运营商主叫验证') || !landlineReadyText.includes('独立固话直线')) throw new Error('Landline provisioning result is incomplete');

  await openPage(frame, 'numbers');
  if (await frame.locator('.number-card').count() !== 3) throw new Error('Number management must show three bound numbers');
  const numbersText = await frame.locator('#oc-screen').innerText();
  if (!numbersText.includes('2 个手机号和 1 个固话均可用于取证') || !numbersText.includes('拨打声讯短号') || !numbersText.includes('17.3 分钟')) throw new Error('Mixed mobile and landline number management is incomplete');

  await openPage(frame, 'capture');
  const captureText = await frame.locator('#oc-screen').innerText();
  if (!captureText.includes('新用户免费试用') || !captureText.includes('5 分钟') || !captureText.includes('主动挂断')) {
    throw new Error('Unopened capture home is missing the five-minute trial guidance');
  }
  if (!captureText.includes('完成 3 步安全开通即可领取') || captureText.includes('实名认证')) {
    throw new Error('Unopened capture home still leads with identity-verification friction');
  }
  if (!captureText.includes('首份材料免费固证') || !captureText.includes('1 份可用') || (await frame.locator('[data-entitlement-mode="free"]').count()) !== 4) {
    throw new Error('Unopened capture home is missing the four-tool free evidence entitlement');
  }
  await frame.locator('[data-go="intro"]').click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('累计 5 分钟免费试用')) throw new Error('Trial opening flow guidance missing');

  await openPage(frame, 'trialHome');
  const trialHomeText = await frame.locator('#oc-screen').innerText();
  if (!trialHomeText.includes('03:18') || !trialHomeText.includes('01:42') || !trialHomeText.includes('05:00')) throw new Error('Trial cumulative usage is incomplete');
  const upgradeAction = frame.locator('.upgrade-action');
  if ((await upgradeAction.count()) !== 1 || !(await upgradeAction.innerText()).includes('选择套餐，解锁更多录音时长') || !(await upgradeAction.innerText()).includes('录音、AI 转写和存储权益')) {
    throw new Error('Trial conversion action is incomplete');
  }
  if (!trialHomeText.includes('首份材料免费固证') || !trialHomeText.includes('1 份可用') || (await frame.locator('[data-entitlement-mode="free"]').count()) !== 4) {
    throw new Error('Trial home is missing the four-tool free evidence entitlement');
  }
  await frame.locator('[data-go="trialCall"]').click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('系统将主动结束通话')) throw new Error('Trial dial cap warning missing');
  await frame.locator('[data-go="dialHandoff"]').click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('微信只能打开系统拨号盘')) throw new Error('Trial SIM handoff guidance missing');
  await frame.locator('[data-go="trialLimit"]').click();
  const trialLimitText = await frame.locator('#oc-screen').innerText();
  if (!trialLimitText.includes('系统已主动挂断') || !trialLimitText.includes('正在固证')) throw new Error('Trial limit result is incomplete');
  await frame.locator('[data-go="productDetail"][data-product="个人基础"]').click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('个人基础')) throw new Error('Trial exhaustion did not carry the paid-plan context');

  await openPage(frame, 'trialHome');
  await frame.locator('[data-go="plans"]').click();
  const activePlanText = await frame.locator('#oc-screen').innerText();
  if (!activePlanText.includes('03:18') || !activePlanText.includes('免费录音剩余')) throw new Error('Active trial balance is inconsistent on the plans page');

  await openPage(frame, 'captureReady');
  const readyText = await frame.locator('#oc-screen').innerText();
  if (!readyText.includes('用手机号拨打取证电话') || !readyText.includes('用固话拨打声讯短号')) throw new Error('Capture home is missing mobile and landline call paths');
  await frame.locator('#oc-bottom-nav [data-go="evidenceBox"]').click();
  if ((await frame.locator('#oc-app-title').textContent()) !== '证据箱') throw new Error('Evidence bottom navigation is not wired');
  await frame.locator('[data-evidence-filter="video"]').click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('当前分类还没有证据')) throw new Error('Filtered-empty evidence state missing');
  await frame.locator('.evidence-filter[data-evidence-filter="all"]').click();
  if (await frame.locator('.evidence-card').count() !== 3) throw new Error('Evidence all filter should show three records');
  await frame.locator('[data-evidence-mode="matter"]').click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('借款纠纷')) throw new Error('Matter grouping did not update');

  await openPage(frame, 'matterCreate');
  await frame.locator('[data-action="matter-save"]').click();
  if ((await frame.locator('[data-evidence-mode="matter"]').getAttribute('aria-pressed')) !== 'true') throw new Error('Saved matter did not return to matter mode');

  await openPage(frame, 'ai');
  await frame.locator('[data-action="ai-summary"]').click();
  if (!(await frame.locator('[data-ai-content]').innerText()).includes('原文')) {
    const text = await frame.locator('[data-ai-content]').innerText();
    if (!text.includes('00:32')) throw new Error('AI summary lacks cited timestamp');
  }
  await frame.locator('[data-go="aiCorrection"]').click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('版本和生成时间保持不变')) throw new Error('AI correction version boundary missing');

  await openPage(frame, 'plans');
  const planText = await frame.locator('#oc-screen').innerText();
  if ((await frame.locator('[data-core-product]').count()) !== 4) throw new Error('Plans page must show four core purchasable products');
  if (!planText.includes('按需购买') || !planText.includes('订阅套餐') || !planText.includes('需要时再加量')) throw new Error('Plans page hierarchy is incomplete');
  if (!planText.includes('季付 ¥105') || !planText.includes('包年 ¥390') || !planText.includes('季付 ¥240') || !planText.includes('包年 ¥890')) throw new Error('Subscription cards are missing quarterly or annual pricing');
  if (planText.includes('价格待发布') || planText.includes('¥--')) throw new Error('Published catalog price is missing from the plans page');
  await frame.locator('[data-go="enterprisePlans"]').click();
  const enterpriseText = await frame.locator('#oc-screen').innerText();
  if (!enterpriseText.includes('企业基础版') || !enterpriseText.includes('企业旗舰版') || !enterpriseText.includes('5 个') || !enterpriseText.includes('20 个') || !enterpriseText.includes('后台配置')) throw new Error('Enterprise package structure is incomplete');
  if (!enterpriseText.includes('企业总机、PBX、分机归属和坐席 PIN 暂不设计')) throw new Error('Enterprise switchboard exclusion is missing');
  await openPage(frame, 'plans');
  await frame.locator('[data-product="30分钟通话包"]').click();
  const callPackDetail = await frame.locator('#oc-screen').innerText();
  if (!callPackDetail.includes('30分钟通话包') || !callPackDetail.includes('30 分钟') || !callPackDetail.includes('¥18.9')) throw new Error('Call pack detail did not use the published catalog snapshot');
  const productPurchase = frame.locator('.primary-action[data-go="purchaseConfirm"]');
  if ((await productPurchase.count()) !== 1 || await productPurchase.isDisabled()) throw new Error('Published product must allow checkout');
  await productPurchase.click();
  const purchaseSubmit = frame.locator('[data-purchase-submit]');
  if (!(await purchaseSubmit.isDisabled())) throw new Error('Purchase submit must start disabled until the order rules are confirmed');
  const confirmText = await frame.locator('#oc-screen').innerText();
  if (!confirmText.includes('30分钟通话包') || !confirmText.includes('¥18.9') || !confirmText.includes('不自动续费')) throw new Error('Checkout snapshot differs from product detail');
  await frame.locator('[data-order-confirm]').check();
  if (await purchaseSubmit.isDisabled()) throw new Error('Purchase confirmation did not unlock payment');
  await purchaseSubmit.click();
  const paymentSuccessText = await frame.locator('#oc-screen').innerText();
  if (!paymentSuccessText.includes('支付成功，权益已到账') || !paymentSuccessText.includes('OC20261019026') || !paymentSuccessText.includes('CALL-30M-2026.10-A')) throw new Error('Payment success receipt is incomplete');
  await openPage(frame, 'paymentIssue');
  const paymentIssueText = await frame.locator('#oc-screen').innerText();
  if (!paymentIssueText.includes('尚未发放权益') || !paymentIssueText.includes('待支付')) throw new Error('Incomplete payment state must not grant entitlement');

  await openPage(frame, 'plans');
  await frame.locator('[data-product="个人基础"]').click();
  if ((await frame.locator('[data-billing-cycle]').count()) !== 3) throw new Error('Subscription detail must offer monthly, quarterly, and annual billing');
  if ((await frame.locator('[data-billing-cycle="MONTHLY"]').getAttribute('aria-checked')) !== 'true') throw new Error('Monthly billing should remain the default selection');
  await frame.locator('[data-billing-cycle="QUARTERLY"]').click();
  const quarterlyText = await frame.locator('#oc-screen').innerText();
  if (!quarterlyText.includes('¥105') || !quarterlyText.includes('约 9 折') || !quarterlyText.includes('省 ¥12') || !quarterlyText.includes('共 3 期') || !quarterlyText.includes('PLAN-BASIC-Q1-2026.10-A')) throw new Error('Quarterly subscription offer is incomplete');
  await frame.locator('[data-go="purchaseConfirm"]').click();
  const quarterlyOrderText = await frame.locator('#oc-screen').innerText();
  if (!quarterlyOrderText.includes('按季度') || !quarterlyOrderText.includes('¥105') || !quarterlyOrderText.includes('共 3 期')) throw new Error('Quarterly checkout did not preserve the selected offer');
  await openPage(frame, 'productDetail');
  await frame.locator('[data-billing-cycle="ANNUAL"]').click();
  const annualText = await frame.locator('#oc-screen').innerText();
  if (!annualText.includes('¥390') || !annualText.includes('约 8.3 折') || !annualText.includes('省 ¥78') || !annualText.includes('共 12 期') || !annualText.includes('PLAN-BASIC-Y1-2026.10-A')) throw new Error('Annual subscription offer is incomplete');
  await frame.locator('[data-go="purchaseConfirm"]').click();
  const annualOrderText = await frame.locator('#oc-screen').innerText();
  if (!annualOrderText.includes('包年') || !annualOrderText.includes('¥390') || !annualOrderText.includes('共 12 期')) throw new Error('Annual checkout did not preserve the selected offer');
  await frame.locator('[data-order-confirm]').check();
  await frame.locator('[data-purchase-submit]').click();
  const annualSuccessText = await frame.locator('#oc-screen').innerText();
  if (!annualSuccessText.includes('包年') || !annualSuccessText.includes('¥390') || !annualSuccessText.includes('PLAN-BASIC-Y1-2026.10-A') || !annualSuccessText.includes('后续各期按订单计划自动发放')) throw new Error('Annual payment result is missing billing-cycle fulfillment details');

  await openPage(frame, 'orders');
  const orderText = await frame.locator('#oc-screen').innerText();
  if (!orderText.includes('¥18.9') || !orderText.includes('¥390') || !orderText.includes('个人基础 · 包年') || !orderText.includes('12 期 · 首期已到账') || !orderText.includes('订单展示购买时锁定')) throw new Error('Order history is missing immutable price and billing-cycle snapshots');
  if (orderText.includes('¥ --') || orderText.includes('正式价格')) throw new Error('Order history still contains unpublished-price placeholders');

  await openPage(frame, 'revokeAuth');
  const revokeSubmit = frame.locator('[data-revoke-submit]');
  if (!(await revokeSubmit.isDisabled())) throw new Error('Revoke submit must start disabled');
  await frame.locator('[data-revoke-confirm]').check();
  if (await revokeSubmit.isDisabled()) throw new Error('Revoke submit did not unlock');

  await openPage(frame, 'support');
  await frame.locator('[data-action="support-type"]').first().click();
  await frame.locator('[data-action="support-start"]').click();
  if (!(await frame.locator('[data-support-result]').innerText()).includes('CS20261014027')) throw new Error('Support receipt missing');

  await openPage(frame, 'call');
  const callText = await frame.locator('#oc-screen').innerText();
  if (!callText.includes('小程序不能指定双卡手机') || !callText.includes('小程序拨号 · 仅支持手机号') || !callText.includes('138 **** 6208')) throw new Error('Call preparation lacks mobile-only or dual-SIM boundary');
  await frame.locator('[data-go="callerSelect"]').click();
  if (await frame.locator('.caller-option').count() !== 2) throw new Error('Mini Program caller selector must show only the two mobile numbers');
  if (!(await frame.locator('#oc-screen').innerText()).includes('固话号码不会出现在这里')) throw new Error('Mobile caller selector does not explain the landline path');
  await frame.locator('[data-caller-id="phone-4471"]').click();
  await frame.locator('.primary-action[data-go="call"]').click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('186 **** 4471')) throw new Error('Selected caller did not return to call preparation');
  await frame.locator('[data-go="dialHandoff"]').click();
  const handoffText = await frame.locator('#oc-screen').innerText();
  if (!handoffText.includes('尾号 4471') || !handoffText.includes('只有与所选号码完全一致')) throw new Error('System dialer handoff is incomplete');
  await frame.locator('.primary-action[data-go="solidified"]').click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('实际主叫\n186 **** 4471')) throw new Error('Verified actual caller missing from evidence result');

  await openPage(frame, 'callerMismatch');
  const mismatchText = await frame.locator('#oc-screen').innerText();
  if (!mismatchText.includes('未扣减') || !mismatchText.includes('未生成') || !mismatchText.includes('不会静默切换')) throw new Error('Caller mismatch fail-closed state incomplete');
  await openPage(frame, 'callerUnavailable');
  if (!(await frame.locator('#oc-screen').innerText()).includes('不允许接通、录音、计费或固证')) throw new Error('Caller unavailable fail-closed state incomplete');

  await openPage(frame, 'landlineCall');
  const landlineCallText = await frame.locator('#oc-screen').innerText();
  if (!landlineCallText.includes('取证声讯短号') || !landlineCallText.includes('010 **** 6688') || !landlineCallText.includes('不录音、不扣额度、不生成证据')) throw new Error('Landline short-code call flow is incomplete');

  await openPage(frame, 'usageDetail');
  const usageText = await frame.locator('#oc-screen').innerText();
  if (!usageText.includes('全部主叫号码用量') || !usageText.includes('固话 010 **** 6688') || !usageText.includes('手机 138 **** 6208') || !usageText.includes('实际主叫号码统计')) throw new Error('Usage detail does not aggregate all actual caller numbers');

  await page.setViewportSize({ width: 320, height: 1000 });
  const visiblePageIds = await frame.locator('.index-button').evaluateAll((buttons) => buttons.map((button) => button.dataset.page));
  for (const pageId of visiblePageIds) {
    await openPage(frame, pageId);
    await assertNoOverflow(frame, `${pageId}@320`);
  }
  await openPage(frame, 'evidenceBox');
  await page.screenshot({ path: path.join(screenshotRoot, 'evidence-320.png'), fullPage: true });
  await openPage(frame, 'trialHome');
  await page.screenshot({ path: path.join(screenshotRoot, 'trial-home-320.png'), fullPage: true });
  await openPage(frame, 'capture');
  await page.screenshot({ path: path.join(screenshotRoot, 'trial-entry-320.png'), fullPage: true });
  await openPage(frame, 'trialLimit');
  await page.screenshot({ path: path.join(screenshotRoot, 'trial-limit-320.png'), fullPage: true });
  await openPage(frame, 'phone');
  await page.screenshot({ path: path.join(screenshotRoot, 'phone-methods-320.png'), fullPage: true });
  await openPage(frame, 'phoneWechatConfirm');
  await page.screenshot({ path: path.join(screenshotRoot, 'phone-wechat-authorization-320.png'), fullPage: true });
  await frame.locator('#oc-screen').evaluate((screen) => { screen.scrollTop = screen.scrollHeight; });
  await frame.locator('.phone').screenshot({ path: path.join(screenshotRoot, 'phone-wechat-authorization-bottom-320.png') });
  await openPage(frame, 'phoneSms');
  await page.screenshot({ path: path.join(screenshotRoot, 'phone-sms-authorization-320.png'), fullPage: true });
  await frame.locator('#oc-screen').evaluate((screen) => { screen.scrollTop = screen.scrollHeight; });
  await frame.locator('.phone').screenshot({ path: path.join(screenshotRoot, 'phone-sms-authorization-bottom-320.png') });
  await openPage(frame, 'dialHandoff');
  await page.screenshot({ path: path.join(screenshotRoot, 'dial-handoff-320.png'), fullPage: true });
  await openPage(frame, 'landlineCall');
  await page.screenshot({ path: path.join(screenshotRoot, 'landline-call-320.png'), fullPage: true });
  await frame.locator('#oc-screen').evaluate((screen) => { screen.scrollTop = screen.scrollHeight; });
  await frame.locator('.phone').screenshot({ path: path.join(screenshotRoot, 'landline-call-bottom-320.png') });
  await openPage(frame, 'numbers');
  await page.screenshot({ path: path.join(screenshotRoot, 'mixed-numbers-320.png'), fullPage: true });
  await openPage(frame, 'plans');
  await page.screenshot({ path: path.join(screenshotRoot, 'plans-320.png'), fullPage: true });
  await openPage(frame, 'enterprisePlans');
  await page.screenshot({ path: path.join(screenshotRoot, 'enterprise-plans-320.png'), fullPage: true });
  await frame.locator('#oc-screen').evaluate((screen) => { screen.scrollTop = screen.scrollHeight; });
  await frame.locator('.phone').screenshot({ path: path.join(screenshotRoot, 'enterprise-plans-bottom-320.png') });
  await openPage(frame, 'plans');
  await frame.locator('[data-product="个人基础"]').click();
  await page.screenshot({ path: path.join(screenshotRoot, 'subscription-annual-320.png'), fullPage: true });
  await frame.locator('[data-go="purchaseConfirm"]').click();
  await page.screenshot({ path: path.join(screenshotRoot, 'purchase-confirm-320.png'), fullPage: true });
  await frame.locator('[data-order-confirm]').check();
  await frame.locator('[data-purchase-submit]').click();
  await page.screenshot({ path: path.join(screenshotRoot, 'payment-success-320.png'), fullPage: true });

  await openPage(frame, 'capture');
  await frame.locator('#oc-screen').evaluate((screen) => { screen.scrollTop = screen.scrollHeight; });
  await frame.locator('.phone').screenshot({ path: path.join(screenshotRoot, 'free-toolbox-320.png') });
  const freePhotoEntry = frame.locator('[data-entitlement-mode="free"][data-go="photoCapture"]');
  if ((await freePhotoEntry.count()) !== 1) throw new Error('Free photo evidence entry missing before entitlement use');
  await freePhotoEntry.click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('本次使用首份材料免费固证权益')) throw new Error('Capture page does not disclose the free entitlement');
  await frame.locator('[data-action="photo-shoot"]').click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('仅在固证成功后扣减')) throw new Error('Material review does not explain success-only deduction');
  await frame.locator('[data-material-submit]').click();
  const freeResultText = await frame.locator('#oc-screen').innerText();
  if (!freeResultText.includes('首份材料已完成免费固证') || !freeResultText.includes('免费固证权益已使用')) throw new Error('Free evidence completion state is incomplete');
  await page.screenshot({ path: path.join(screenshotRoot, 'free-evidence-result-320.png'), fullPage: true });
  await frame.locator('#oc-screen').evaluate((screen) => { screen.scrollTop = screen.scrollHeight; });
  await frame.locator('.phone').screenshot({ path: path.join(screenshotRoot, 'free-evidence-result-actions-320.png') });

  await openPage(frame, 'capture');
  const exhaustedHomeText = await frame.locator('#oc-screen').innerText();
  if (!exhaustedHomeText.includes('0 份可用') || !exhaustedHomeText.includes('免费固证额度已使用')) throw new Error('Free evidence balance did not update after successful solidification');
  await frame.locator('[data-go="freeEvidenceLimit"]').first().click();
  const freeLimitText = await frame.locator('#oc-screen').innerText();
  if (!freeLimitText.includes('免费固证额度已用完') || !freeLimitText.includes('进入相机、麦克风或文件选择器前先校验权益')) throw new Error('Free evidence limit must block before native capture or selection');
  if ((await frame.locator('[data-action="photo-shoot"], [data-action="video-start"], [data-action="audio-start"]').count()) !== 0) throw new Error('Capture control is reachable after the free evidence entitlement is exhausted');
  await page.screenshot({ path: path.join(screenshotRoot, 'free-evidence-limit-320.png'), fullPage: true });
  await frame.locator('[data-product="单次固证包"]').click();
  const singleEvidenceText = await frame.locator('#oc-screen').innerText();
  if (!singleEvidenceText.includes('单次固证包') || !singleEvidenceText.includes('工具材料固证\n1 份') || !singleEvidenceText.includes('¥12.9')) throw new Error('Single evidence package detail is incomplete');

  await page.setViewportSize({ width: 760, height: 1100 });
  await openPage(frame, 'profile');
  await page.screenshot({ path: path.join(screenshotRoot, 'profile-760.png'), fullPage: true });

  if (errors.length) throw new Error(`Browser errors: ${errors.join(' | ')}`);
  await browser.close();
  console.log('PASS: 57-page navigation, mobile and independent-landline caller flows, enterprise package structure, configurable catalog checkout, carrier fail-closed checks, all-caller usage aggregation, and 320px layout verified.');
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
