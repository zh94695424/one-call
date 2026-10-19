const fs = require('fs');
const path = require('path');
const { chromium } = require('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/.pnpm/playwright@1.60.0/node_modules/playwright');

const root = path.resolve(__dirname, '..');
const previewPath = path.join(root, 'preview.html');
const screenshotDir = path.join(__dirname, 'screenshots');
const previewUrl = `file:///${previewPath.replace(/\\/g, '/')}`;
const browserPath = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';

fs.mkdirSync(screenshotDir, { recursive: true });

function expect(condition, message) {
  if (!condition) throw new Error(message);
}

async function expectText(page, selector, text) {
  const value = await page.locator(selector).innerText();
  expect(value.includes(text), `Expected "${text}" in ${selector}, received: ${value.slice(0, 240)}`);
}

async function assertNoPageOverflow(page, width) {
  const dimensions = await page.evaluate(() => ({
    viewport: window.innerWidth,
    document: document.documentElement.scrollWidth,
    body: document.body.scrollWidth,
  }));
  expect(
    dimensions.document <= dimensions.viewport && dimensions.body <= dimensions.viewport,
    `${width}px viewport has page overflow: ${JSON.stringify(dimensions)}`,
  );
}

async function screenshot(page, name, width, height = 900) {
  await page.setViewportSize({ width, height });
  await page.waitForTimeout(80);
  await assertNoPageOverflow(page, width);
  await page.screenshot({ path: path.join(screenshotDir, name), fullPage: true });
}

async function openRoute(page, route) {
  await page.locator(`[data-route="${route}"]`).first().click();
  await page.waitForTimeout(30);
}

(async () => {
  expect(fs.existsSync(previewPath), `Preview file not found: ${previewPath}`);

  const browser = await chromium.launch({
    headless: true,
    executablePath: browserPath,
  });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const pageErrors = [];
  const consoleErrors = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('console', message => {
    if (message.type() === 'error') consoleErrors.push(message.text());
  });

  try {
    await page.goto(previewUrl, { waitUntil: 'load' });
    await page.waitForSelector('#auth-root .auth-card');

    await expectText(page, '#auth-root', '登录证枢能力中枢');
    expect(await page.locator('[data-login-method]').count() === 2, 'Login must offer WeChat QR and mobile verification code');
    await screenshot(page, 'login-wechat-1440.png', 1440);
    await screenshot(page, 'login-wechat-1024.png', 1024);

    await page.locator('#auth-review-select').selectOption('qr-expired');
    await expectText(page, '#auth-root', '二维码已过期');
    await page.locator('[data-action="qr-refresh"]').click();
    await expectText(page, '#auth-root', '打开微信扫一扫');

    await page.locator('#auth-review-select').selectOption('privacy');
    await expectText(page, '#auth-root', '确认隐私与服务规则');
    expect(await page.locator('#privacy-continue').isDisabled(), 'First-login privacy confirmation must block continuation until checked');
    await page.locator('#privacy-agree').check();
    expect(await page.locator('#privacy-continue').isEnabled(), 'Privacy continuation must enable after explicit agreement');
    await page.locator('#privacy-continue').click();
    await expectText(page, '#auth-root', '账户已创建');
    await screenshot(page, 'login-first-use-onboarding-1280.png', 1280);

    await page.locator('#auth-review-select').selectOption('invite');
    await expectText(page, '#auth-root', '你正在接受华启科技有限公司的企业邀请');
    await page.locator('[data-action="wechat-login"]').click();
    await expectText(page, '#auth-root', '确认加入企业');
    await expectText(page, '#auth-root', '普通成员');
    await screenshot(page, 'login-enterprise-invite-1280.png', 1280);
    await page.locator('[data-action="invite-reject"]').click();

    await page.locator('#auth-review-select').selectOption('session-expired');
    await expectText(page, '#auth-root', '登录状态已过期');
    await page.locator('[data-action="wechat-login"]').click();
    await expectText(page, '#content', '设备到货外包装照片');
    await page.locator('[data-action="logout"]').click();

    await page.locator('#auth-review-select').selectOption('member-removed');
    await expectText(page, '#auth-root', '企业访问已被移除');
    await page.locator('[data-action="back-login"]').click();

    await page.locator('[data-login-method="phone"]').click();
    await page.locator('#login-phone').fill('13800006208');
    await page.locator('[data-action="send-login-code"]').click();
    await page.locator('#login-code').fill('000000');
    await page.locator('[data-action="phone-login"]').click();
    await expectText(page, '#auth-root', '验证码不正确或已失效');
    await page.locator('#login-code').fill('482916');
    await page.locator('[data-action="phone-login"]').click();
    await expectText(page, '#auth-root', '选择工作空间');
    expect(await page.locator('[data-workspace-choice]').count() === 3, 'Workspace chooser must list personal and multiple enterprise memberships');
    await page.waitForTimeout(2700);
    await screenshot(page, 'login-workspace-selection-1440.png', 1440);
    await page.locator('[data-workspace-choice="personal"]').click();
    await page.waitForSelector('#content .page-head');

    expect(await page.locator('#workspace-select').inputValue() === 'personal', 'Default workspace must be personal');
    await expectText(page, '#content', '下午好，李晓');
    expect(await page.locator('#side-nav [data-route="plans"]').count() === 1, 'Personal navigation must expose subscription and upgrade');
    expect(await page.locator('#subscription-entry').isVisible(), 'Personal top bar must expose subscription and upgrade');
    await expectText(page, '#subscription-entry', '订阅 / 升级');
    await screenshot(page, 'personal-overview-1440.png', 1440);

    await openRoute(page, 'notifications');
    await expectText(page, '#content', '消息与待办');
    expect(await page.locator('.notification-row').count() === 3, 'Personal notification center must show only records in the current role scope');
    await page.locator('[data-notification-filter="unread"]').click();
    expect(await page.locator('.notification-row.unread').count() === 2, 'Unread filter must show unread records only');
    await page.locator('[data-notification-read="NTF-261019-01"]').click();
    expect(await page.locator('.notification-row.unread').count() === 1, 'Single-message read action must update unread state');
    await screenshot(page, 'notifications-unread-1024.png', 1024);
    await page.locator('[data-action="notification-read-all"]').click();
    await expectText(page, '#content', '未读 0');
    await page.locator('[data-notification-filter="all"]').click();
    await page.locator('[data-notification-open="NTF-261018-03"]').click();
    await expectText(page, '#content', '套餐与权益');
    await openRoute(page, 'overview');

    await openRoute(page, 'evidence');
    expect(await page.locator('[data-evidence-type]').count() === 7, 'Evidence center must expose seven type tabs');
    await expectText(page, '#content', '通话录音');
    await expectText(page, '#content', '聊天截图');

    await page.locator('[data-evidence-check="OC-E1041"]').check();
    await expectText(page, '.bulk-bar', '已选择 1 份证据');
    await page.locator('#evidence-search').fill('采购合同');
    expect(await page.locator('tbody tr:visible').count() === 1, 'Evidence search must filter the visible table rows');
    await page.locator('#evidence-search').fill('');
    await screenshot(page, 'personal-evidence-1440.png', 1440);

    await page.locator('[data-evidence-id="OC-E1041"]').first().click();
    expect(await page.locator('[data-detail-tab]').count() === 4, 'Non-call evidence detail must expose four tabs');
    await page.locator('[data-detail-tab="proof"]').click();
    await expectText(page, '#content', '公证处存证');
    await screenshot(page, 'evidence-detail-1280.png', 1280);
    for (const item of [
      { id: 'OC-E1040', file: 'VID_20261018_174302.mp4', preview: '.video-frame' },
      { id: 'OC-E1039', file: 'AUD_20261018_162011.m4a', preview: '.audio-track' },
      { id: 'OC-E1038', file: '采购合同及补充协议.pdf', preview: '.document-preview' },
      { id: 'OC-E1037', file: 'WX_Chat_20261018_1135.png', preview: '.chat-preview' },
    ]) {
      await openRoute(page, 'evidence');
      await page.locator(`[data-evidence-id="${item.id}"]`).first().click();
      await expectText(page, '#content', item.file);
      expect(await page.locator(item.preview).count() === 1, `${item.id} must render its type-specific preview`);
    }
    await screenshot(page, 'evidence-chat-screenshot-1280.png', 1280);

    await openRoute(page, 'evidence');
    await openRoute(page, 'import-evidence');
    expect(await page.locator('.workflow-step').count() === 3, 'Evidence import must expose three steps');
    await page.locator('[data-action="import-demo"]').click();
    await expectText(page, '#content', '采购合同及补充协议.pdf');
    await page.locator('[data-action="import-next"]').click();
    await expectText(page, '#content', '来源说明');
    await page.locator('[data-action="import-next"]').click();
    await expectText(page, '#content', '自动提交');
    await screenshot(page, 'evidence-import-review-1280.png', 1280);
    await screenshot(page, 'evidence-import-review-1024.png', 1024);
    await page.locator('[data-action="import-submit"]').click();
    await expectText(page, '#content', '2 份文件已提交');
    await openRoute(page, 'evidence');

    await openRoute(page, 'matters');
    await expectText(page, '#content', '把同一事项的多份证据组织到一起');
    await openRoute(page, 'matter-new');
    expect(await page.locator('.workflow-step').count() === 2, 'Matter creation must expose information and evidence selection steps');
    await expectText(page, '#content', '不会改变证据形成时间、所有权、哈希或公证处存证记录');
    await page.locator('[data-action="matter-next"]').click();
    await expectText(page, '#content', '只能关联当前账户或当前企业授权范围内可见的证据');
    expect(await page.locator('[data-matter-evidence]:checked').count() === 3, 'Matter creation should preserve selected evidence state');
    await screenshot(page, 'matter-create-evidence-1280.png', 1280);
    await screenshot(page, 'matter-create-evidence-1024.png', 1024);
    await page.locator('[data-action="matter-create"]').click();
    await expectText(page, '#content', '事情已创建');
    await expectText(page, '#content', '原始证据、哈希和固证时间均未改变');
    await page.locator('[data-action="matter-open-created"]').click();
    await expectText(page, '#content', 'MAT-202610-031');
    await expectText(page, '#content', '3 份证据');
    expect(await page.locator('.quick-list [data-evidence-id]').count() === 3, 'Created matter detail must list exactly the selected evidence');
    for (const id of ['OC-E1042', 'OC-E1041', 'OC-E1038']) {
      expect(await page.locator(`.quick-list [data-evidence-id="${id}"]`).count() === 1, `Created matter detail is missing selected evidence ${id}`);
    }
    await screenshot(page, 'matter-detail-created-1280.png', 1280);
    await screenshot(page, 'matter-detail-created-1024.png', 1024);
    await page.locator('[data-action="matter-manage-start"]').click();
    await page.locator('[data-matter-manage-evidence="OC-E1041"]').uncheck();
    await page.locator('[data-matter-manage-evidence="OC-E1040"]').check();
    await page.locator('#matter-owner-edit').selectOption({ label: '陈雨' });
    await page.locator('[data-action="matter-manage-save"]').click();
    await expectText(page, '#content', '负责人');
    await expectText(page, '#content', '事情档案已调整');
    expect(await page.locator('.quick-list [data-evidence-id="OC-E1040"]').count() === 1, 'Matter maintenance must add a newly selected visible evidence');
    expect(await page.locator('.quick-list [data-evidence-id="OC-E1041"]').count() === 0, 'Matter maintenance must remove only the association, not retain it in the matter list');
    await screenshot(page, 'matter-maintained-1024.png', 1024);
    await page.locator('[data-action="matter-archive"]').click();
    await expectText(page, '#content', '确认归档这个事情');
    await page.locator('[data-action="matter-archive-submit"]').click();
    await expectText(page, '#content', '事情已归档为只读');
    await screenshot(page, 'matter-archived-1024.png', 1024);

    await openRoute(page, 'numbers');
    await page.locator('[data-number-id="phone-6208"]').click();
    await expectText(page, '#content', '号码详情');
    await expectText(page, '#content', '每个号码独立验证、授权和开通');
    await expectText(page, '#content', '不代表已核验运营商实名登记人与账户身份一致');
    await screenshot(page, 'number-detail-1280.png', 1280);
    await screenshot(page, 'number-detail-1024.png', 1024);
    await page.locator('[data-action="number-stop-confirm"]').click();
    await expectText(page, '#content', '既有录音、证据、哈希、存证记录和用量流水继续保留');
    await page.locator('[data-action="number-stop"]').click();
    await expectText(page, '#content', '已停用');
    await screenshot(page, 'number-disabled-1280.png', 1280);
    await page.locator('[data-action="number-reactivate"]').click();
    await openRoute(page, 'numbers');
    await page.locator('[data-route="number-add"]').click();
    await page.locator('[data-action="number-add-next"]').click();
    await page.locator('[data-action="number-add-verify"]').click();
    await page.locator('#number-add-agree').check();
    await page.locator('[data-action="number-add-submit"]').click();
    await expectText(page, '#content', 'CARRIER-ACK-742810');
    await screenshot(page, 'number-add-synchronous-result-1280.png', 1280);
    await openRoute(page, 'numbers');
    expect(await page.locator('[data-number-id]').count() === 3, 'Personal account must allow up to three active caller numbers');
    expect(await page.getByRole('button', { name: '已达 3 个上限' }).isDisabled(), 'Adding a fourth caller number must be disabled');
    await page.locator('[data-number-id="phone-4471"]').click();
    await page.locator('[data-action="number-stop-confirm"]').click();
    await page.locator('[data-action="number-stop"]').click();
    await page.locator('[data-action="number-delete-confirm"]').click();
    await page.locator('[data-action="number-delete"]').click();
    await expectText(page, '#content', '历史录音、证据、哈希、公证处存证记录');
    await page.getByRole('button', { name: '查看授权历史' }).click();
    await expectText(page, '#content', '186 **** 4471');
    await page.locator('[data-agreement-id^="auth-new-"]').click();
    await expectText(page, '#content', 'REQ-OPEN-20261019-0612');
    await expectText(page, '#content', '已撤销');
    await screenshot(page, 'authorization-revoked-detail-1280.png', 1280);

    await openRoute(page, 'calls');
    const callTableText = await page.locator('table').innerText();
    for (const heading of ['实际主叫', '被叫', '号码/接入', '呼叫结果']) {
      expect(callTableText.includes(heading), `Call list is missing ${heading}`);
    }
    expect(callTableText.includes('主叫不一致'), 'Call list must show the caller mismatch record');
    await page.locator('[data-call-id="OC-C2036"]').click();
    await expectText(page, '#content', '未扣录音额度');
    await expectText(page, '#content', '也未生成证据');
    expect(await page.locator('[data-route="share-create"]').count() === 0, 'Failed or mismatched calls must not expose recording sharing');

    await openRoute(page, 'calls');
    await page.locator('[data-call-id="OC-C2038"]').click();
    expect(await page.locator('[data-call-tab]').count() === 4, 'Call detail must expose four tabs');
    for (const tab of ['callinfo', 'proof', 'timeline']) {
      await page.locator(`[data-call-tab="${tab}"]`).click();
      expect(await page.locator(`[data-call-tab="${tab}"]`).getAttribute('class').then(v => v.includes('active')), `Call tab ${tab} did not activate`);
    }
    await page.locator('[data-call-tab="ai"]').click();
    await expectText(page, '#content', '智能要点摘要');
    await page.locator('.audio-tools [data-action="toast"]').click();
    await expectText(page, '#content', '申请下载与导出');
    await page.locator('[data-action="export-submit"]').click();
    await expectText(page, '#content', '验证码不正确');
    await page.locator('#export-code').fill('482916');
    await page.locator('[data-action="export-submit"]').click();
    await expectText(page, '#content', '下载与导出任务');
    await expectText(page, '#content', '已失效');
    await page.locator('[data-action="export-complete"]').first().click();
    await expectText(page, '#content', '可下载');
    await page.locator('[data-action="export-download"]').first().click();
    await expectText(page, '#toast', '受控下载已开始');
    await screenshot(page, 'export-tasks-1440.png', 1440);
    await screenshot(page, 'export-tasks-1024.png', 1024);
    await openRoute(page, 'calls');
    await page.locator('[data-call-id="OC-C2038"]').click();
    await page.locator('[data-call-tab="ai"]').click();
    await page.locator('[data-action="toggle-audio"]').click();
    expect((await page.locator('[data-action="toggle-audio"]').getAttribute('aria-label')) === '暂停', 'Audio control must enter playing state');
    await page.locator('[data-action="toggle-audio"]').click();
    await screenshot(page, 'call-detail-1440.png', 1440, 980);

    await page.locator('[data-action="ai-correction-start"]').click();
    await page.locator('#ai-correction-text').fill('金额应明确为含税总额，并保留原文 00:14 的引用。');
    await page.locator('[data-action="ai-correction-submit"]').click();
    await expectText(page, '#content', '用户纠错建议 COR-001');
    await expectText(page, '#content', '未覆盖 AI v2 原结果');
    await page.locator('[data-action="ai-reprocess"]').click();
    await expectText(page, '#content', 'AI v3');
    expect(await page.locator('[data-ai-version]').count() === 3, 'AI reprocessing must append a new version instead of replacing an existing result');
    await expectText(page, '#content', '上一版本 · AI v2');
    await page.evaluate(() => document.getElementById('toast').classList.remove('visible'));
    await page.waitForTimeout(250);
    await screenshot(page, 'call-ai-version-1024.png', 1024, 1080);

    expect(await page.locator('[data-route="share-create"]').count() === 1, 'Valid call details must expose recording sharing');
    await page.locator('[data-route="share-create"]').click();
    await expectText(page, '#content', '创建录音分享');
    await expectText(page, '#content', '外部 · 提取码');
    await expectText(page, '#content', '最长 30 天');
    expect(await page.locator('#share-download').isChecked() === false, 'External share download must be disabled by default');
    expect(await page.locator('[data-share-group="shareExpiry"][data-share-value="24h"]').getAttribute('class').then(value => value.includes('active')), 'External share must default to 24-hour expiry');
    expect(await page.locator('[data-share-group="shareVisits"][data-share-value="5"]').getAttribute('class').then(value => value.includes('active')), 'External share must default to five successful visits');
    await screenshot(page, 'share-create-1440.png', 1440, 1000);
    await screenshot(page, 'share-create-1024.png', 1024, 1000);
    await page.locator('[data-action="share-create"]').click();
    await expectText(page, '#content', '受控分享已生成');
    await expectText(page, '#content', '链接和验证信息通过不同渠道发送');
    await expectText(page, '#content', '提取码只验证“持有码者”');
    await screenshot(page, 'share-created-1280.png', 1280);

    await page.locator('[data-action="share-preview-new"]').click();
    await page.waitForSelector('.public-share-page');
    await expectText(page, '.public-share-page', '查看分享的通话录音');
    expect(await page.locator('[data-action="wechat-login"]').count() === 0, 'Extraction-code recipient experience must not require platform login');
    await page.locator('#share-code-input').fill('0000');
    await page.locator('[data-action="share-verify"]').click();
    await expectText(page, '.public-share-page', '提取码不正确');
    await page.locator('#share-code-input').fill('8K3P');
    await page.locator('[data-action="share-verify"]').click();
    await expectText(page, '.public-share-page', '录音播放副本');
    await expectText(page, '.public-share-page', '下载未授权');
    expect(!(await page.locator('.public-share-page').innerText()).includes('original.wav'), 'Recipient playback must not expose the original file or storage address');
    await screenshot(page, 'share-recipient-playback-1280.png', 1280, 980);

    await page.evaluate(() => { state.shareAccessState='entry'; state.shareCodeAttempts=2; state.shareCodeInput=''; go('share-access'); });
    await page.locator('#share-code-input').fill('1111');
    await page.locator('[data-action="share-verify"]').click();
    await expectText(page, '.public-share-page', '验证暂时锁定');
    await screenshot(page, 'share-recipient-locked-1024.png', 1024);

    await page.evaluate(() => { state.session='signedIn'; state.shareAccessState='entry'; state.shareCodeAttempts=0; state.shareCodeInput=''; go('shares'); });
    await expectText(page, '#content', '管理已经创建的受控播放授权');
    await expectText(page, '#content', '已过期');
    await expectText(page, '#content', '已撤销');
    await screenshot(page, 'shares-management-1440.png', 1440);

    await page.locator('[data-share-preview="SHR-261018-3D7M"]').click();
    await expectText(page, '.public-share-page', '分享已过期');
    await screenshot(page, 'share-recipient-expired-1024.png', 1024);
    await page.evaluate(() => { state.session='signedIn'; go('shares'); });
    await page.locator('[data-share-preview="SHR-261017-1P6R"]').click();
    await expectText(page, '.public-share-page', '分享已被撤销');
    await screenshot(page, 'share-recipient-revoked-1024.png', 1024);
    await page.evaluate(() => { state.session='signedIn'; go('certificates'); });

    await openRoute(page, 'certificates');
    await openRoute(page, 'certificate-new');
    expect(await page.locator('[data-cert-type]').count() === 3, 'Certificate wizard must offer three distinct deliverables');
    await page.locator('[data-cert-type="notary"]').click();
    await page.locator('[data-action="certificate-next"]').click();
    await expectText(page, '#content', '选择申请材料');
    await page.locator('[data-action="certificate-next"]').click();
    await expectText(page, '#content', '材料检查后报价');
    await screenshot(page, 'certificate-new-review-1280.png', 1280);
    await page.locator('[data-action="certificate-submit"]').click();
    await expectText(page, '#content', '申请已提交');

    await page.evaluate(() => go('application'));
    await page.locator('[data-action="supplement-start"]').click();
    await page.locator('[data-action="supplement-demo"]').click();
    expect(await page.locator('[data-action="supplement-review"]').isEnabled(), 'Supplement review must enable after a material is selected');
    await page.locator('[data-action="supplement-review"]').click();
    await expectText(page, '#content', '原申请快照');
    await expectText(page, '#content', '提交后生成新的不可变材料版本');
    await page.locator('[data-action="supplement-submit"]').click();
    await expectText(page, '#content', 'SUP-SNAPSHOT-261019-02');
    await expectText(page, '#content', '补件审核中');
    await screenshot(page, 'certificate-supplement-submitted-1024.png', 1024, 1000);

    await openRoute(page, 'rights');
    await openRoute(page, 'plans');
    expect(await page.locator('[data-plan-cycle]').count() === 3, 'Personal catalog must offer monthly, quarterly, and yearly cycles');
    await page.locator('[data-plan-cycle="quarter"]').click();
    await expectText(page, '#content', '¥269');
    await page.locator('[data-plan-id="basic"]').click();
    await expectText(page, '#content', '¥105');
    await screenshot(page, 'personal-plans-1440.png', 1440);
    await screenshot(page, 'personal-plans-1024.png', 1024, 900);
    await page.locator('[data-addon-type="recording"]').click();
    await expectText(page, '#content', '以下演示价格、有效期和版本均由后台已发布商品配置返回');
    await page.locator('[data-addon-sku="CALL-100"]').click();
    await page.locator('[data-action="addon-review"]').click();
    await expectText(page, '#content', 'CALL-PACK-2026.10-R2');
    expect(await page.locator('[data-action="addon-pay"]').isDisabled(), 'Add-on payment must require explicit agreement');
    await page.locator('#addon-agree').check();
    await screenshot(page, 'addon-checkout-1024.png', 1024);
    await page.locator('[data-action="addon-pay"]').click();
    await expectText(page, '#content', '录音分钟包购买成功');
    await openRoute(page, 'orders');
    await expectText(page, '#content', 'OC-ADDON-261019-031');
    await page.locator('[data-order-id="OC-ADDON-261019-031"]').click();
    await expectText(page, '#content', '加量商品');
    await screenshot(page, 'addon-order-detail-1024.png', 1024);
    await page.evaluate(() => go('plans'));
    await openRoute(page, 'checkout');
    await expectText(page, '#content', 'PERSONAL-2026.10-R3');
    await page.locator('#checkout-agree').check();
    expect(await page.locator('[data-action="checkout-pay"]').isEnabled(), 'Checkout payment action must enable after agreement');
    await screenshot(page, 'personal-checkout-1280.png', 1280);
    await page.locator('[data-action="checkout-pay"]').click();
    await expectText(page, '#content', '订购成功');

    await openRoute(page, 'orders');
    await expectText(page, '#content', '购买时锁定的商品、周期、金额、权益与开票状态');
    await screenshot(page, 'personal-orders-1440.png', 1440);
    await page.locator('[data-order-id="OC20261001008"]').click();
    await expectText(page, '#content', '不可变商品快照');
    await expectText(page, '#content', '后台后续调整只生成新版本，不覆盖本订单');
    await screenshot(page, 'personal-order-detail-1280.png', 1280);
    await screenshot(page, 'personal-order-detail-1024.png', 1024);
    await openRoute(page, 'invoice-apply');
    expect(await page.locator('[data-action="invoice-submit"]').isDisabled(), 'Invoice submission must require explicit information confirmation');
    await page.locator('#invoice-agree').check();
    expect(await page.locator('[data-action="invoice-submit"]').isEnabled(), 'Invoice submission must enable after confirmation');
    await screenshot(page, 'invoice-apply-1280.png', 1280);
    await screenshot(page, 'invoice-apply-1024.png', 1024);
    await page.locator('[data-action="invoice-submit"]').click();
    await expectText(page, '#content', '开票申请已提交');
    await screenshot(page, 'invoice-submitted-1280.png', 1280);

    await page.locator('#workspace-select').selectOption('enterprise');
    await expectText(page, '#content', '华启科技 · 企业总览');
    for (const route of ['plans', 'orders', 'members', 'access', 'organization']) {
      expect(await page.locator(`[data-route="${route}"]`).count() > 0, `Enterprise navigation is missing ${route}`);
    }
    expect(await page.locator('#side-nav [data-route="shares"]').count() === 1, 'Enterprise admin navigation must expose shares limited by evidence access');
    expect(await page.locator('#subscription-entry').isVisible(), 'Enterprise admin top bar must expose renewal and expansion');
    await expectText(page, '#subscription-entry', '续费 / 扩容');
    await screenshot(page, 'enterprise-overview-1440.png', 1440);

    await openRoute(page, 'evidence');
    await expectText(page, 'table', '部门/成员');
    await expectText(page, 'table', '采购部');

    await openRoute(page, 'members');
    await expectText(page, '#content', '坐席不等同于电话号码或分机');
    await page.locator('[data-action="open-modal"]').click();
    expect(await page.locator('#modal-backdrop').getAttribute('class').then(v => v.includes('visible')), 'Invite member dialog did not open');
    await page.locator('[data-action="close-modal"]').first().click();
    await screenshot(page, 'enterprise-members-1280.png', 1280);

    await page.locator('[data-member-id="member-zhou"]').click();
    await expectText(page, '#content', '周宁 · 法务部');
    expect(await page.locator('[data-member-tab]').count() === 3, 'Member detail must expose profile, usage, and audit tabs');
    await page.locator('[data-member-tab="usage"]').click();
    await expectText(page, '#content', '按实际主叫归集');
    await page.locator('[data-member-tab="audit"]').click();
    await expectText(page, '#content', '角色权限更新');
    await screenshot(page, 'enterprise-member-detail-1280.png', 1280);
    await page.locator('[data-action="member-edit-start"]').click();
    await page.locator('#member-edit-dept').selectOption({ label: '采购部' });
    await page.locator('#member-edit-role').selectOption({ label: '普通成员' });
    await page.locator('[data-action="member-edit-review"]').click();
    await expectText(page, '#content', '历史证据、录音播放授权、号码和用量归属不会随之改写');
    expect(await page.locator('[data-action="member-edit-submit"]').isDisabled(), 'Member changes must require explicit high-risk confirmation');
    await page.locator('#member-edit-agree').check();
    await page.locator('[data-action="member-edit-submit"]').click();
    await expectText(page, '#content', '成员权限变更');
    await expectText(page, '#content', '法务部 → 采购部');
    await screenshot(page, 'enterprise-member-change-audit-1024.png', 1024);

    await openRoute(page, 'rights');
    await openRoute(page, 'plans');
    await expectText(page, '#content', '企业管理员可在这里发起订购、续费、扩容或方案变更');
    await expectText(page, '#content', '企业基础版');
    await expectText(page, '#content', '续费 / 扩容');
    await expectText(page, '#content', '后台商品版本配置');
    await screenshot(page, 'enterprise-plans-1440.png', 1440);
    await page.locator('[data-action="enterprise-consult"]').last().click();
    await expectText(page, '#content', '企业订购与变更');
    expect(await page.locator('[data-action="enterprise-order-submit"]').isDisabled(), 'Enterprise request must require explicit confirmation');
    await page.locator('#enterprise-order-agree').check();
    await page.locator('[data-action="enterprise-order-submit"]').click();
    await expectText(page, '#content', '报价处理中');
    await page.locator('[data-action="enterprise-order-quote"]').click();
    await expectText(page, '#content', '待确认报价');
    await expectText(page, '#content', 'QUO-20261019');
    await page.locator('[data-action="enterprise-order-accept"]').click();
    await expectText(page, '#content', '待合同生效');
    await page.locator('[data-action="enterprise-order-effective"]').click();
    await expectText(page, '#content', '已按合同版本生效');
    await screenshot(page, 'enterprise-order-effective-1440.png', 1440);
    await screenshot(page, 'enterprise-order-effective-1024.png', 1024);

    await openRoute(page, 'orders');
    await expectText(page, '#content', '企业订单与合同');
    await expectText(page, '#content', '报价处理中');
    await page.locator('[data-order-id="ENT20261001001"]').click();
    await expectText(page, '#content', 'ENTERPRISE-2026.10-R2');
    await screenshot(page, 'enterprise-order-detail-1280.png', 1280);

    await openRoute(page, 'access');
    await expectText(page, '#content', '企业管理员默认不能无条件播放所有录音');
    await screenshot(page, 'enterprise-access-1280.png', 1280);

    await openRoute(page, 'organization');
    await expectText(page, '#content', '企业总机、PBX、分机归属、统一外呼号拆分和坐席 PIN 不在本次范围');
    await screenshot(page, 'enterprise-organization-1280.png', 1280);

    await page.evaluate(() => go('agreements'));
    await expectText(page, '#content', '待重新确认');
    await page.locator('[data-action="agreement-reconfirm"]').click();
    await expectText(page, '#content', '授权记录详情');
    await expectText(page, '#content', '重新确认新版协议');
    await screenshot(page, 'authorization-reconfirmed-1280.png', 1280);

    await page.locator('#role-select').selectOption('member');
    await expectText(page, '#content', '下午好，陈雨');
    expect(await page.locator('[data-route="members"]').count() === 0, 'Ordinary member must not see member management');
    expect(await page.locator('[data-route="access"]').count() === 0, 'Ordinary member must not see permission management');
    expect(await page.locator('[data-route="organization"]').count() === 0, 'Ordinary member must not see organization settings');
    expect(await page.locator('#side-nav [data-route="shares"]').count() === 1, 'Ordinary member must be able to manage their own recording shares');
    await screenshot(page, 'enterprise-member-overview-1440.png', 1440);
    await screenshot(page, 'enterprise-member-overview-1024.png', 1024);
    await openRoute(page, 'evidence');
    await expectText(page, '#content', '只展示由本人形成或导入的证据');
    await expectText(page, 'table', 'OC-E1041');
    expect(!(await page.locator('table').innerText()).includes('OC-E1042'), 'Ordinary member evidence must exclude another member records');
    expect(!(await page.locator('table').innerText()).includes('部门/成员'), 'Ordinary member evidence must not expose department/member column');
    await screenshot(page, 'enterprise-member-evidence-1280.png', 1280);
    await openRoute(page, 'calls');
    await expectText(page, '#content', '只展示本人发起且归属可确认的呼叫记录');
    await expectText(page, 'table', '139 **** 7812');
    expect(!(await page.locator('table').innerText()).includes('138 **** 6208'), 'Ordinary member calls must exclude another member caller');
    await openRoute(page, 'rights');
    await expectText(page, '#content', '不展示组织总余额');
    expect(await page.locator('#side-nav [data-route="plans"]').count() === 0, 'Ordinary member must not see enterprise plan navigation');
    expect(!(await page.locator('#subscription-entry').isVisible()), 'Ordinary member must not see enterprise subscription action');
    await page.evaluate(() => go('plans'));
    await expectText(page, '#content', '下午好，陈雨');
    await page.evaluate(() => go('members'));
    await expectText(page, '#content', '下午好，陈雨');
    await page.evaluate(() => go('orders'));
    await expectText(page, '#content', '下午好，陈雨');

    await page.locator('#role-select').selectOption('evidence');
    await expectText(page, '#content', '证据工作台 · 周宁');
    expect(await page.locator('[data-route="members"]').count() === 0, 'Evidence manager must not see member management');
    expect(await page.locator('#side-nav [data-route="shares"]').count() === 1, 'Evidence manager must be able to manage shares within authorized department scope');
    expect(!(await page.locator('#subscription-entry').isVisible()), 'Evidence manager must not see enterprise subscription action');
    await screenshot(page, 'enterprise-evidence-manager-overview-1440.png', 1440);
    await openRoute(page, 'evidence');
    await expectText(page, '#content', '当前仅展示法务部授权范围内的证据');
    const managerEvidence = await page.locator('table').innerText();
    expect(managerEvidence.includes('法务部'), 'Evidence manager must see the authorized department');
    expect(!managerEvidence.includes('采购部'), 'Evidence manager must not see an unauthorized department');
    await screenshot(page, 'enterprise-evidence-manager-list-1280.png', 1280);
    await openRoute(page, 'calls');
    await expectText(page, '#content', '仅展示法务部授权范围内的呼叫记录');
    await expectText(page, 'table', '010 **** 6688');

    await page.locator('#role-select').selectOption('auditor');
    await expectText(page, '#content', '审计概览 · 王璐');
    expect(!(await page.locator('#subscription-entry').isVisible()), 'Auditor must not see enterprise subscription action');
    expect(await page.locator('[data-route="evidence"]').count() === 0, 'Auditor must not see evidence navigation');
    expect(await page.locator('[data-route="calls"]').count() === 0, 'Auditor must not see call recording navigation');
    expect(await page.locator('[data-route="shares"]').count() === 0, 'Auditor must not see recording sharing navigation');
    expect(await page.locator('[data-route="members"]').count() === 0, 'Auditor must not see member management');
    await screenshot(page, 'enterprise-auditor-overview-1440.png', 1440);
    await openRoute(page, 'audit-center');
    await expectText(page, '#content', '不向审计员开放证据正文');
    await expectText(page, 'table', 'AUD-20918');
    await screenshot(page, 'enterprise-auditor-records-1280.png', 1280);
    await page.locator('.page-head-actions [data-action="toast"]').click();
    await expectText(page, '#content', '审计员导出固定脱敏');
    await page.locator('#export-code').fill('482916');
    await page.locator('[data-action="export-submit"]').click();
    await page.locator('[data-action="export-complete"]').first().click();
    await expectText(page, '#content', '部分失败');
    await screenshot(page, 'auditor-export-partial-1280.png', 1280);
    await page.evaluate(() => go('evidence'));
    await expectText(page, '#content', '审计概览 · 王璐');

    for (const width of [1024, 1280, 1440]) {
      await page.setViewportSize({ width, height: 800 });
      await page.reload({ waitUntil: 'load' });
      await page.waitForSelector('[data-action="wechat-login"]');
      await page.locator('[data-action="wechat-login"]').click();
      await page.locator('[data-workspace-choice="personal"]').click();
      await page.waitForSelector('#content .page-head');
      await assertNoPageOverflow(page, width);
      if (width === 1024) {
        await page.screenshot({ path: path.join(screenshotDir, 'personal-overview-1024.png'), fullPage: true });
      }
    }

    expect(pageErrors.length === 0, `Page errors: ${pageErrors.join(' | ')}`);
    expect(consoleErrors.length === 0, `Console errors: ${consoleErrors.join(' | ')}`);
    console.log('PASS: login/session, interaction, role authorization, content, and responsive checks');
    console.log(`Screenshots: ${screenshotDir}`);
  } finally {
    await browser.close();
  }
})().catch(error => {
  console.error(`FAIL: ${error.stack || error.message}`);
  process.exitCode = 1;
});
