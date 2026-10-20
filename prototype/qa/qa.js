const { chromium } = require('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/.pnpm/playwright@1.60.0/node_modules/playwright');
const path = require('path');
const assert = require('node:assert/strict');
const { pathToFileURL } = require('url');

const prototypeRoot = path.resolve(__dirname, '..');
const screenshotRoot = path.join(__dirname, 'screenshots');
const previewUrl = pathToFileURL(path.join(prototypeRoot, 'preview.html')).href;
const newPages = [
  'phoneWechatConfirm', 'phoneWechatIssue', 'phoneWechatConflict', 'phoneSms', 'landlineBind', 'landlineVerify', 'landlineReady',
  'ivrReturn', 'claimVerify', 'claimResult', 'claimBlocked', 'trialHome', 'trialCall', 'trialLimit', 'freeEvidenceLimit',
  'callerSelect', 'dialHandoff', 'landlineCall', 'callerMismatch', 'callerUnavailable',
  'evidenceBox', 'evidenceEmpty', 'matterCreate', 'aiCorrection',
  'usageDetail', 'enterprisePlans', 'productDetail', 'purchaseConfirm', 'paymentSuccess', 'paymentIssue', 'revokeAuth', 'evidenceSelect',
  'documentCenter', 'orders', 'tips', 'privacy', 'support',
];
const authorizationCopy = '我已阅读并同意以上全部授权内容，确认绑定号码由本人实际使用，并同意按上述范围处理敏感个人信息、开通一通话内部录音服务及进行必要的数据共享。';

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
  try {
  const errors = [];
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (msg) => { if (msg.type() === 'error') errors.push(msg.text()); });
  await page.goto(previewUrl);
  const frame = await getFrame(page);

  const count = await frame.locator('.index-button').count();
  if (count !== 61) throw new Error(`Expected 61 visible pages, got ${count}`);
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

  const ordinaryInitial=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
  await openPage(frame,'provisioning');
  assert.ok(!(await frame.locator('#oc-screen').innerText()).includes('内部服务已开通'));
  // A failed verification or shared activation rejection must stay on the actual form.
  for(const mode of ['wrong-code','wrong-number','unverified','suspended']){
    const fixture=JSON.parse(JSON.stringify(ordinaryInitial));
    if(mode==='unverified')fixture.subjects.find(item=>item.id==='MINI-ACCOUNT-001').identity='UNVERIFIED';
    if(mode==='suspended')fixture.subjects.find(item=>item.id==='MINI-ACCOUNT-001').activation='SUSPENDED';
    assert.equal((await frame.evaluate(data=>window.__onecallMini.loadReviewSnapshot(data),fixture)).ok,true);
    await openPage(frame,mode==='wrong-code'||mode==='wrong-number'?'phoneSms':'phoneWechatConfirm');
    if(mode==='wrong-code')await frame.locator('#oc-code').fill('000000');
    if(mode==='wrong-number')await frame.locator('#oc-mobile').fill('18600009999');
    await frame.locator('[data-phone-confirm]').check();await frame.locator('[data-phone-submit]').click();
    assert.equal(await frame.evaluate(()=>window.__onecallMini.currentPage()),mode==='wrong-code'||mode==='wrong-number'?'phoneSms':'phoneWechatConfirm');
    assert.ok((await frame.locator('[data-activation-error]').innerText()).includes(mode==='suspended'?'SUSPENDED':'VERIFICATION_REQUIRED'));
    assert.deepEqual(await frame.evaluate(()=>window.__onecallMini.businessSnapshot()),fixture);
  }
  const alternateSeed=JSON.parse(JSON.stringify(ordinaryInitial));
  alternateSeed.subjects.find(item=>item.id==='MINI-ACCOUNT-001').maskedNumber='137 **** 1000';
  assert.equal((await frame.evaluate(data=>window.__onecallMini.loadReviewSnapshot(data),alternateSeed)).ok,true);
  for(const pageId of ['phoneWechatConfirm','phoneSms']){
    await openPage(frame,pageId);await frame.locator('[data-phone-confirm]').check();await frame.locator('[data-phone-submit]').click();
    assert.equal(await frame.evaluate(()=>window.__onecallMini.currentPage()),'provisioning');
  }
  const createdNumbers=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
  const createdTargets=['138 **** 6208','186 **** 4471'].map(mask=>createdNumbers.subjects.find(item=>item.maskedNumber===mask));
  assert.ok(createdTargets.every(item=>item&&item.accountId==='ACCOUNT-DEMO-001'&&item.activation==='ACTIVE'));
  assert.notEqual(createdTargets[0].id,createdTargets[1].id,'New WeChat and SMS numbers need distinct subjects');
  for(const target of createdTargets){
    assert.equal(createdNumbers.activations.filter(item=>item.subjectId===target.id).length,1);
    assert.equal(createdNumbers.trialGrants.filter(item=>item.subjectId===target.id).length,1);
  }
  assert.equal((await frame.evaluate(data=>window.__onecallMini.loadReviewSnapshot(data),ordinaryInitial)).ok,true);
  async function assertOrdinaryActivation(mask,method){
    const state=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
    const target=state.subjects.find(item=>item.maskedNumber===mask&&item.accountId==='ACCOUNT-DEMO-001');
    assert.ok(target,'Verified number must resolve to the current account subject');
    assert.equal(target.identity,'VERIFIED');assert.equal(target.activation,'ACTIVE');
    const activation=state.activations.find(item=>item.subjectId===target.id&&item.channel==='MINIAPP');
    const grants=state.trialGrants.filter(item=>item.subjectId===target.id);
    assert.ok(activation);assert.equal(grants.length,1);assert.equal(activation.consentVersion,'ONECALL-SERVICE-2026.10');
    assert.deepEqual(state.evidence,ordinaryInitial.evidence);
    assert.deepEqual(state.trialGrants.filter(item=>item.subjectId!==target.id&&item.subjectId!=='MINI-ACCOUNT-001'),ordinaryInitial.trialGrants);
    assert.ok((await frame.locator('#oc-screen').innerText()).includes(method));
    const shotName=method==='微信平台验证'?'wechat':'sms';
    const captureOrdinary=async stage=>{
      await frame.locator('#oc-screen').evaluate(screen=>{screen.scrollTop=0;});
      await frame.waitForTimeout(100);
      await frame.locator('.phone').screenshot({path:path.join(screenshotRoot,'alignment-ordinary-'+shotName+'-'+stage+'-393.png'),animations:'disabled'});
    };
    await page.setViewportSize({width:393,height:852});await assertNoOverflow(frame,'ordinary result');
    await captureOrdinary('result');
    await frame.locator('[data-go="trialHome"]').click();
    assert.equal(await frame.locator('[data-selected-subject]').getAttribute('data-selected-subject'),target.id);
    assert.ok((await frame.locator('[data-selected-subject]').innerText()).includes(mask));
    assert.equal(await frame.locator('.trial-remaining').innerText(),'05:00');
    await captureOrdinary('trial');
    await frame.locator('[data-go="numbers"]').click();
    assert.equal(await frame.locator('[data-selected-subject]').getAttribute('data-selected-subject'),target.id);
    assert.ok((await frame.locator('[data-selected-subject]').innerText()).includes(mask));
    await captureOrdinary('numbers');
    await page.setViewportSize({width:760,height:1100});
    return target.id;
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
  await assertOrdinaryActivation('138 **** 6208','微信平台验证');

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
  const smsSubjectId=await assertOrdinaryActivation('186 **** 4471','短信验证码验证');
  const activated=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
  await openPage(frame,'phoneSms');await frame.locator('[data-phone-confirm]').check();await frame.locator('[data-phone-submit]').click();
  assert.deepEqual(await frame.evaluate(()=>window.__onecallMini.businessSnapshot()),activated,'Repeat activation must not regrant trial');
  assert.equal(activated.activations.at(-1).subjectId,smsSubjectId);

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
  const beforeLandline=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
  await openPage(frame,'landlineReady');
  assert.ok(!(await frame.locator('#oc-screen').innerText()).includes('固话已验证并开通'));
  for(const mode of ['unverified','suspended']){
    const fixture=JSON.parse(JSON.stringify(beforeLandline));
    fixture.subjects.push({id:'MINI-LANDLINE-01088886688',maskedNumber:'010 **** 6688',numberType:'LANDLINE',source:'MINIAPP',accountId:'ACCOUNT-DEMO-001',identity:mode==='unverified'?'UNVERIFIED':'VERIFIED',activation:mode==='suspended'?'SUSPENDED':'INACTIVE',revision:1,claimRisk:'NONE'});
    assert.equal((await frame.evaluate(data=>window.__onecallMini.loadReviewSnapshot(data),fixture)).ok,true);
    await openPage(frame,'landlineVerify');await frame.locator('[data-go="landlineReady"]').click();
    assert.equal(await frame.evaluate(()=>window.__onecallMini.currentPage()),'landlineVerify');
    assert.ok((await frame.locator('[data-activation-error]').innerText()).includes(mode==='suspended'?'SUSPENDED':'VERIFICATION_REQUIRED'));
    assert.deepEqual(await frame.evaluate(()=>window.__onecallMini.businessSnapshot()),fixture);
  }
  assert.equal((await frame.evaluate(data=>window.__onecallMini.loadReviewSnapshot(data),beforeLandline)).ok,true);
  await openPage(frame,'landlineVerify');await frame.locator('[data-go="landlineReady"]').click();
  const landlineReadyText = await frame.locator('#oc-screen').innerText();
  if (!landlineReadyText.includes('010 **** 6688') || !landlineReadyText.includes('CALLER_CHALLENGE') || !landlineReadyText.includes('独立固话直线') || !landlineReadyText.includes('05:00')) throw new Error('Landline provisioning result is incomplete');
  const landlineState=await frame.evaluate(()=>window.__onecallMini.businessSnapshot()),landline=landlineState.subjects.find(item=>item.id==='MINI-LANDLINE-01088886688');
  assert.ok(landline);assert.equal(landline.accountId,'ACCOUNT-DEMO-001');assert.equal(landline.identity,'VERIFIED');assert.equal(landline.activation,'ACTIVE');
  assert.equal(landlineState.activations.filter(item=>item.subjectId===landline.id&&item.channel==='MINIAPP').length,1);
  assert.equal(landlineState.trialGrants.filter(item=>item.subjectId===landline.id).length,1);
  const captureLandline=async stage=>{await frame.locator('#oc-screen').evaluate(screen=>{screen.scrollTop=0;});await frame.waitForTimeout(100);await frame.locator('.phone').screenshot({path:path.join(screenshotRoot,'alignment-landline-activation-'+stage+'-393.png'),animations:'disabled'});};
  await page.setViewportSize({width:393,height:852});await captureLandline('result');
  await frame.locator('[data-go="trialHome"]').click();assert.equal(await frame.locator('[data-selected-subject]').getAttribute('data-selected-subject'),landline.id);assert.ok((await frame.locator('[data-selected-subject]').innerText()).includes('010 **** 6688'));await captureLandline('trial');
  await frame.locator('[data-go="numbers"]').click();assert.equal(await frame.locator('[data-selected-subject]').getAttribute('data-selected-subject'),landline.id);assert.ok((await frame.locator('[data-selected-subject]').innerText()).includes('010 **** 6688'));await captureLandline('numbers');
  await page.setViewportSize({width:760,height:1100});
  await openPage(frame,'landlineVerify');await frame.locator('[data-go="landlineReady"]').click();assert.deepEqual(await frame.evaluate(()=>window.__onecallMini.businessSnapshot()),landlineState,'Repeated landline activation must not regrant or duplicate activation');
  assert.equal((await frame.evaluate(data=>window.__onecallMini.loadReviewSnapshot(data),activated)).ok,true);

  await openPage(frame, 'numbers');
  if (await frame.locator('.number-card').count() !== 3) throw new Error('Number management must show three bound numbers');
  const numbersText = await frame.locator('#oc-screen').innerText();
  if (!numbersText.includes('2 个手机号和 1 个固话均可用于取证') || !numbersText.includes('拨打声讯短号') || !numbersText.includes('17.3 分钟')) throw new Error('Mixed mobile and landline number management is incomplete');

  await openPage(frame, 'capture');
  const captureText=await frame.locator('#oc-screen').innerText();
  if(!captureText.includes('DEMO_NOT_APPROVED')||!captureText.includes('05:00')||!captureText.includes('主动挂断'))throw Error('Current shared trial rule missing');
  if((await frame.locator('[data-entitlement-mode="free"]').count())!==4)throw Error('Four free capture tools missing');
  await openPage(frame,'trialHome');
  const trialText=await frame.locator('#oc-screen').innerText();
  if(!trialText.includes('186 **** 4471')||!trialText.includes('00:00')||!trialText.includes('05:00'))throw Error('Successful ordinary subject trial continuity missing');
  await frame.locator('[data-go="trialCall"]').click();
  if(!(await frame.locator('#oc-screen').innerText()).includes('剩余'))throw Error('Trial balance guidance missing');
  await openPage(frame,'trialLimit');
  if(!(await frame.locator('#oc-screen').innerText()).includes('正在固证'))throw Error('Trial result preservation missing');

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

  // Shared catalog and separate order/payment/entitlement transitions.
  await openPage(frame,'plans');
  if(!(await frame.locator('#oc-screen').innerText()).includes('DEMO_NOT_APPROVED'))throw Error('Demo catalog policy label missing');
  await frame.locator('[data-product="个人基础"]').click();
  if(await frame.locator('[data-billing-cycle]').count()!==3)throw Error('Three subscription cycles missing');
  await frame.locator('[data-billing-cycle="QUARTERLY"]').click();
  if(!(await frame.locator('#oc-screen').innerText()).includes('PERSONAL_BASIC_QUARTERLY-V1'))throw Error('Canonical catalog version missing');
  await frame.locator('[data-go="purchaseConfirm"]').click();
  if(!await frame.locator('[data-purchase-submit]').isDisabled())throw Error('Order needs consent');
  await frame.locator('[data-order-confirm]').check();
  await frame.locator('[data-purchase-submit]').click();
  let snapshot=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
  const order=snapshot.orders.at(-1),frozen=JSON.stringify(order.snapshot);
  if(order.channel!=='MINIAPP'||order.payment!=='UNPAID'||snapshot.entitlements.length)throw Error('Order creation conflates payment');
  await frame.locator('[data-action="simulate-payment"][data-result="UNKNOWN"]').click();
  const beforeQuery=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
  if(beforeQuery.orders.at(-1).payment!=='UNKNOWN')throw Error('Unknown payment missing');
  await frame.locator('[data-action="query-payment"]').click();
  snapshot=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
  if(snapshot.orders.length!==beforeQuery.orders.length||snapshot.orders.at(-1).entitlement!=='PENDING'||snapshot.entitlements.length)throw Error('Payment must await issue');
  await frame.locator('[data-action="issue-entitlements"]').click();
  snapshot=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
  if(snapshot.entitlements.length!==1||snapshot.orders.at(-1).entitlement!=='ISSUED'||JSON.stringify(snapshot.orders.at(-1).snapshot)!==frozen)throw Error('Frozen order or issue mismatch');
  await openPage(frame,'enterprisePlans');
  if(await frame.locator('[data-purchase-submit]').count()||!(await frame.locator('#oc-screen').innerText()).includes('QUOTE'))throw Error('Enterprise must be consultation only');
  await openPage(frame,'orders');
  if(!(await frame.locator('#oc-screen').innerText()).includes(order.id))throw Error('Canonical order history missing');

  await openPage(frame, 'revokeAuth');
  const revokeSubmit = frame.locator('[data-revoke-submit]');
  if (!(await revokeSubmit.isDisabled())) throw new Error('Revoke submit must start disabled');
  await frame.locator('[data-revoke-confirm]').check();
  if (await revokeSubmit.isDisabled()) throw new Error('Revoke submit did not unlock');

  await openPage(frame,'support');
  const supportText=await frame.locator('#oc-screen').innerText();
  if(!supportText.includes('人工客服联系方式待配置')||await frame.locator('#oc-screen input, #oc-screen textarea, [data-action="support-start"]').count()||/CS20|INV-|400-/.test(supportText))throw Error('Offline support must not accept submissions');

  await openPage(frame, 'call');
  const callText = await frame.locator('#oc-screen').innerText();
  if (!callText.includes('小程序不能指定双卡手机') || !callText.includes('小程序拨号 · 仅支持手机号') || !callText.includes('186 **** 4471')) throw new Error('Call preparation lacks the activated mobile or dual-SIM boundary');
  await frame.locator('[data-go="callerSelect"]').click();
  if (await frame.locator('.caller-option').count() !== 2) throw new Error('Mini Program caller selector must show only the two mobile numbers');
  if (!(await frame.locator('#oc-screen').innerText()).includes('固话号码不会出现在这里')) throw new Error('Mobile caller selector does not explain the landline path');
  await frame.locator('[data-caller-id="phone-6208"]').click();
  await frame.locator('.primary-action[data-go="call"]').click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('138 **** 6208')) throw new Error('Caller selection did not switch to the other mobile');
  await frame.locator('[data-go="callerSelect"]').click();
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
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-evidence-320.png'), fullPage: true });
  await openPage(frame, 'trialHome');
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-trial-home-320.png'), fullPage: true });
  await openPage(frame, 'capture');
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-trial-entry-320.png'), fullPage: true });
  await openPage(frame, 'trialLimit');
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-trial-limit-320.png'), fullPage: true });
  await openPage(frame, 'phone');
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-phone-methods-320.png'), fullPage: true });
  await openPage(frame, 'phoneWechatConfirm');
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-phone-wechat-authorization-320.png'), fullPage: true });
  await frame.locator('#oc-screen').evaluate((screen) => { screen.scrollTop = screen.scrollHeight; });
  await frame.locator('.phone').screenshot({ path: path.join(screenshotRoot, 'alignment-phone-wechat-authorization-bottom-320.png') });
  await openPage(frame, 'phoneSms');
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-phone-sms-authorization-320.png'), fullPage: true });
  await frame.locator('#oc-screen').evaluate((screen) => { screen.scrollTop = screen.scrollHeight; });
  await frame.locator('.phone').screenshot({ path: path.join(screenshotRoot, 'alignment-phone-sms-authorization-bottom-320.png') });
  await openPage(frame, 'dialHandoff');
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-dial-handoff-320.png'), fullPage: true });
  await openPage(frame, 'landlineCall');
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-landline-call-320.png'), fullPage: true });
  await frame.locator('#oc-screen').evaluate((screen) => { screen.scrollTop = screen.scrollHeight; });
  await frame.locator('.phone').screenshot({ path: path.join(screenshotRoot, 'alignment-landline-call-bottom-320.png') });
  await openPage(frame, 'numbers');
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-mixed-numbers-320.png'), fullPage: true });
  await openPage(frame,'plans');
  await page.screenshot({path:path.join(screenshotRoot,'alignment-plans-320.png'),fullPage:true});
  await openPage(frame,'enterprisePlans');
  await page.screenshot({path:path.join(screenshotRoot,'alignment-enterprise-plans-320.png'),fullPage:true});

  await openPage(frame, 'capture');
  await frame.locator('#oc-screen').evaluate((screen) => { screen.scrollTop = screen.scrollHeight; });
  await frame.locator('.phone').screenshot({ path: path.join(screenshotRoot, 'alignment-free-toolbox-320.png') });
  const freePhotoEntry = frame.locator('[data-entitlement-mode="free"][data-go="photoCapture"]');
  if ((await freePhotoEntry.count()) !== 1) throw new Error('Free photo evidence entry missing before entitlement use');
  await freePhotoEntry.click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('本次使用首份材料免费固证权益')) throw new Error('Capture page does not disclose the free entitlement');
  await frame.locator('[data-action="photo-shoot"]').click();
  if (!(await frame.locator('#oc-screen').innerText()).includes('仅在固证成功后扣减')) throw new Error('Material review does not explain success-only deduction');
  await frame.locator('[data-material-submit]').click();
  const freeResultText = await frame.locator('#oc-screen').innerText();
  if (!freeResultText.includes('首份材料已完成免费固证') || !freeResultText.includes('免费固证权益已使用')) throw new Error('Free evidence completion state is incomplete');
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-free-evidence-result-320.png'), fullPage: true });
  await frame.locator('#oc-screen').evaluate((screen) => { screen.scrollTop = screen.scrollHeight; });
  await frame.locator('.phone').screenshot({ path: path.join(screenshotRoot, 'alignment-free-evidence-result-actions-320.png') });

  await openPage(frame, 'capture');
  const exhaustedHomeText = await frame.locator('#oc-screen').innerText();
  if (!exhaustedHomeText.includes('0 份可用') || !exhaustedHomeText.includes('免费固证额度已使用')) throw new Error('Free evidence balance did not update after successful solidification');
  await frame.locator('[data-go="freeEvidenceLimit"]').first().click();
  const freeLimitText = await frame.locator('#oc-screen').innerText();
  if (!freeLimitText.includes('免费固证额度已用完') || !freeLimitText.includes('进入相机、麦克风或文件选择器前先校验权益')) throw new Error('Free evidence limit must block before native capture or selection');
  if ((await frame.locator('[data-action="photo-shoot"], [data-action="video-start"], [data-action="audio-start"]').count()) !== 0) throw new Error('Capture control is reachable after the free evidence entitlement is exhausted');
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-free-evidence-limit-320.png'), fullPage: true });
  await frame.locator('[data-product="单次固证包"]').click();
  const singleEvidenceText = await frame.locator('#oc-screen').innerText();
  if (!singleEvidenceText.includes('单次出证权益') || !singleEvidenceText.includes('单项证据保存') || !singleEvidenceText.includes('¥12.90')) throw new Error('Single evidence package detail is incomplete');

  // Review selection is read-only. Same-number claim changes association only.
  const canonical=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
  for(const scenario of ['returning','conflict','recycled','expired','exhausted','suspended']){
    const before=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
    const result=await frame.evaluate(name=>window.__onecallMini.setReviewScenario(name),scenario);
    if(!result.ok)throw Error('Missing scenario '+scenario);
    if(JSON.stringify(await frame.evaluate(()=>window.__onecallMini.businessSnapshot()))!==JSON.stringify(before))throw Error('Scenario selection changed business');
    const hidden=await frame.locator('#oc-screen').innerText();
    if(/TRIAL-|EVID-|到期 20/.test(hidden))throw Error('Preverification history leak');
    await frame.locator('[data-go="claimVerify"]').click();
    await frame.locator('[data-action="claim-submit"]').click();
    const resultText=await frame.locator('#oc-screen').innerText();
    if(['conflict','recycled'].includes(scenario)){
      if(!resultText.includes('人工客服联系方式待配置')||/TRIAL-|EVID-/.test(resultText))throw Error('Blocked claim history leak');
      await page.screenshot({path:path.join(screenshotRoot,'alignment-claim-'+scenario+'-320.png'),fullPage:true});
    }else{
      const after=await frame.evaluate(()=>window.__onecallMini.businessSnapshot()),target=after.subjects.find(item=>item.id===result.subjectId);
      if(target.identity!=='UNVERIFIED'||JSON.stringify(before.trialGrants)!==JSON.stringify(after.trialGrants)||JSON.stringify(before.evidence)!==JSON.stringify(after.evidence)||JSON.stringify(before.sessions)!==JSON.stringify(after.sessions))throw Error('Claim rewrote history');
      await page.screenshot({path:path.join(screenshotRoot,'alignment-claim-'+scenario+'-320.png'),fullPage:true});
      if(scenario==='returning'){
        await openPage(frame,'plans');await frame.locator('[data-product="个人基础"]').click();await frame.locator('[data-go="purchaseConfirm"]').click();await frame.locator('[data-order-confirm]').check();await frame.locator('[data-purchase-submit]').click();
        if((await frame.evaluate(()=>window.__onecallMini.businessSnapshot())).orders.length!==before.orders.length)throw Error('Unverified claimed subject bought through unrelated verified subject');
      }
    }
    await frame.evaluate(data=>window.__onecallMini.loadReviewSnapshot(data),canonical);
  }
  await page.setViewportSize({width:393,height:852});
  const associatedScenario=await frame.evaluate(()=>window.__onecallMini.setReviewScenario('returning'));
  assert.equal(associatedScenario.ok,true,'Missing returning fixture for same-account risk coverage');
  await openPage(frame,'claimVerify');await frame.locator('[data-action="claim-submit"]').click();
  const associated=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
  assert.equal(associated.subjects.find(item=>item.id===associatedScenario.subjectId).accountId,'ACCOUNT-DEMO-001');
  for(const risk of ['CONFLICT','RECYCLED']){
    const fixture=JSON.parse(JSON.stringify(associated));fixture.subjects.find(item=>item.id===associatedScenario.subjectId).claimRisk=risk;
    for(const request of ['original','new']){
      assert.equal((await frame.evaluate(data=>window.__onecallMini.loadReviewSnapshot(data),fixture)).ok,true);
      if(request==='new')await frame.evaluate(id=>window.__onecallMini.setClaimRequestId(id),'MINI-CLAIM-RISK-'+risk);
      await openPage(frame,'claimVerify');await frame.locator('[data-action="claim-submit"]').click();
      const blockedText=await frame.locator('#oc-screen').innerText();
      assert.ok(blockedText.includes('人工客服联系方式待配置')&&!/EVID-|TRIAL-/.test(blockedText),risk+' same-account claim exposed protected history');
      assert.deepEqual(await frame.evaluate(()=>window.__onecallMini.businessSnapshot()),fixture,risk+' '+request+' request mutated a blocked same-account claim');
      if(request==='original')await page.screenshot({path:path.join(screenshotRoot,'alignment-claim-same-account-'+risk.toLowerCase()+'-393.png'),fullPage:true});
    }
  }
  assert.equal((await frame.evaluate(data=>window.__onecallMini.loadReviewSnapshot(data),canonical)).ok,true);
  const beforeInvalid=await frame.evaluate(()=>window.__onecallMini.businessSnapshot());
  for(const key of ['employees','customerAuthSession','widgetState']){
    const result=await frame.evaluate(({b,key})=>window.__onecallMini.loadReviewSnapshot({...b,[key]:{}}),{b:beforeInvalid,key});
    if(result.ok||JSON.stringify(await frame.evaluate(()=>window.__onecallMini.businessSnapshot()))!==JSON.stringify(beforeInvalid))throw Error('Non-atomic forbidden import');
  }

  await page.setViewportSize({ width: 393, height: 852 });
  for (const pageId of ['ivrReturn','claimVerify','claimResult','claimBlocked','plans','purchaseConfirm','paymentSuccess','support']) {
    await openPage(frame, pageId); await assertNoOverflow(frame, pageId + ' 393px');
  }
  await page.screenshot({ path: path.join(screenshotRoot, 'alignment-support-393.png'), fullPage: true });
  await page.setViewportSize({ width: 760, height: 1100 });
  await openPage(frame, 'profile');
  await frame.locator('#oc-screen').evaluate(screen=>{screen.scrollTop=0;});
  await frame.waitForTimeout(100);
  await frame.locator('.phone').screenshot({ path: path.join(screenshotRoot, 'alignment-profile-760.png'), animations: 'disabled' });

  if (errors.length) throw new Error(`Browser errors: ${errors.join(' | ')}`);
  console.log('PASS: 61-page navigation, mobile and independent-landline caller flows, enterprise package structure, configurable catalog checkout, carrier fail-closed checks, all-caller usage aggregation, and 320px layout verified.');
  } finally { await browser.close(); }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
