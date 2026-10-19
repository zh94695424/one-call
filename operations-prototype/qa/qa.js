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
  expect(value.includes(text), `Expected "${text}" in ${selector}, received: ${value.slice(0, 260)}`);
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

async function shot(page, name, width, height = 900) {
  await page.setViewportSize({ width, height });
  await page.screenshot({ path: path.join(screenshotDir, `${name}-${width}.png`), fullPage: true });
}

async function openFresh(page) {
  await page.goto(previewUrl);
  await page.evaluate(() => { location.hash = '/login'; location.reload(); });
  await page.waitForSelector('.login-panel');
}

async function login(page, role = 'ops') {
  await openFresh(page);
  await page.selectOption('#login-role', role);
  await page.fill('#staff-otp', '482916');
  await page.click('[data-action="login"]');
  await page.waitForSelector('.sidebar');
}

async function setRole(page, role) {
  await page.selectOption('#review-role', role);
  await page.waitForTimeout(80);
}

async function go(page, route) {
  await page.evaluate((target) => window.__onecallOps.go(target), route);
  await page.waitForTimeout(60);
}

(async () => {
  const browser = await chromium.launch({ executablePath: browserPath, headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });
  const errors = [];
  page.on('pageerror', (error) => errors.push(error.message));

  try {
    await openFresh(page);
    await expectText(page, '.login-panel', '平台员工登录');
    await page.fill('#staff-otp', '000000');
    await page.click('[data-action="login"]');
    await expectText(page, '#toast-root', '有效二次验证码');
    await page.selectOption('#login-scenario', 'disabled');
    expect(await page.locator('[data-action="login"]').isDisabled(), 'Disabled employee should not be able to log in');

    await login(page, 'ops');
    await expectText(page, '.page-head', '运营总览');
    const routeCount = await page.evaluate(() => window.__onecallOps.routes.length);
    expect(routeCount === 55, `Expected 55 registered routes, received ${routeCount}`);
    await shot(page, 'p0-dashboard', 1440);

    await setRole(page, 'support');
    await go(page, '/users/USR-2048');
    expect((await page.locator('[data-action="request-access"]').count()) === 0, 'Support role must not see complete identity access action');
    await page.evaluate(() => {
      const forged = document.createElement('button');
      forged.dataset.action = 'request-access';
      forged.dataset.object = 'USR-2048';
      forged.dataset.kind = 'identity';
      document.body.appendChild(forged);
      forged.click();
      forged.remove();
    });
    expect((await page.locator('.modal').count()) === 0, 'Forged support access action must be rejected by the handler');
    await expectText(page, '#toast-root', '无权申请');
    await go(page, '/evidence');
    await expectText(page, '#content', '没有访问权限');
    await expectText(page, '#content', '不会确认目标对象是否存在');
    await shot(page, 'p0-access-denied', 1024, 768);

    await setRole(page, 'risk');
    await go(page, '/users/USR-2048');
    expect((await page.locator('.revealed-value').count()) === 0, 'Sensitive identity must be absent before a grant');
    await page.click('[data-action="request-access"]');
    await expectText(page, '.modal', '关联工单');
    await page.click('[data-action="access-submit"]');
    await expectText(page, '.modal', '申请人不能审批自己');
    await page.click('[data-action="access-approve"]');
    await page.fill('#access-otp', '111111');
    await page.click('[data-action="access-verify"]');
    await expectText(page, '#otp-error', '二次验证失败');
    await page.fill('#access-otp', '482916');
    await page.click('[data-action="access-verify"]');
    await expectText(page, '[data-sensitive-session]', '原型虚构');
    await shot(page, 'p0-sensitive-session', 1280);
    await page.evaluate(() => { location.hash = '/users'; });
    await page.waitForSelector('h1');
    await page.evaluate(() => { location.hash = '/users/USR-2048'; });
    await page.waitForTimeout(80);
    expect((await page.locator('[data-sensitive-session]').count()) === 0, 'Sensitive content must clear after leaving the route');

    await setRole(page, 'carrier');
    await go(page, '/numbers/NUM-10116');
    await expectText(page, '#content', 'FAILED');
    await page.click('[data-command="business-retry"]');
    await expectText(page, '.modal', '创建新业务尝试');
    await page.click('[data-action="command-submit"]');
    await expectText(page, '#content', '业务尝试 #3');
    await expectText(page, '#content', '同步成功');

    await go(page, '/numbers/NUM-10082');
    await page.click('[data-command="technical-retry"]');
    await page.click('[data-action="command-submit"]');
    await expectText(page, '#content', '技术尝试 #2');
    await expectText(page, '#content', '等待同请求收敛');
    await shot(page, 'p0-provisioning-retry', 1280);

    await go(page, '/call-reconciliation');
    await page.click('[data-command="call-correction"]');
    await page.click('[data-action="command-submit"]');
    await expectText(page, '.modal', '双人复核');
    await page.click('[data-action="command-approve"]');
    await expectText(page, '#content', '已追加纠正事件');
    await shot(page, 'p0-call-correction', 1280);

    await setRole(page, 'notary');
    await go(page, '/evidence');
    await expectText(page, '#content', '内部处理');
    await expectText(page, '#content', '公证处存证');
    await go(page, '/evidence-quarantine');
    expect(await page.locator('[data-command="release-quarantine"]').count() === 0, 'Notary role must not be able to release quarantine');

    await setRole(page, 'security');
    await go(page, '/evidence-quarantine');
    await page.click('[data-command="release-quarantine"]');
    await page.click('[data-action="command-submit"]');
    await expectText(page, '.modal', '双人复核');
    await page.click('[data-action="command-approve"]');
    await expectText(page, '#content', '已解除并恢复处理');
    await shot(page, 'p0-quarantine-release', 1280);

    await setRole(page, 'auditor');
    await go(page, '/audit');
    await expectText(page, '#content', 'SensitiveAccessVerificationFailed');
    await go(page, '/calls/CALL-78392');
    await expectText(page, '#content', '没有访问权限');

    await setRole(page, 'notary');
    await go(page, '/ai-jobs/AIJ-67018');
    await expectText(page, '#content', '摘要引用校验失败');
    await page.click('[data-action="ai-retry"]');
    await expectText(page, '#content', 'SUCCEEDED');
    await expectText(page, '#content', 'v4');
    await expectText(page, '#content', 'v3 · 逐字稿可用，摘要失败');

    await go(page, '/preservations/NPS-44018');
    await expectText(page, '#content', 'MANUAL_REVIEW');
    await page.click('[data-action="preservation-retry"]');
    await expectText(page, '#content', 'PRESERVED');
    await expectText(page, '#content', 'REC-****8129');
    await expectText(page, '#content', '尝试 #2 · 重复存证错误 0015');
    await shot(page, 'p1-preservation-detail', 1280);

    await go(page, '/notary-reports/NRT-55201');
    await page.click('[data-action="report-complete"]');
    await expectText(page, '#content', 'COMPLETED');
    await page.click('[data-action="report-old-callback"]');
    await expectText(page, '#content', '旧回调');
    await expectText(page, '#content', 'COMPLETED');

    await go(page, '/offline-notary/ONA-61018');
    await expectText(page, '#content', '材料快照 v2');
    await page.click('[data-action="notary-supplement"]');
    await expectText(page, '#content', '材料快照 v3');
    await page.click('[data-action="notary-submit-blocked"]');
    await expectText(page, '#toast-root', '缺少正式线下提交凭据');
    await shot(page, 'p1-offline-notary', 1280);

    await setRole(page, 'product');
    await go(page, '/products/PRD-PER-2026-R4');
    await page.click('[data-action="product-review"]');
    await expectText(page, '#content', 'REVIEWED');
    await page.click('[data-action="product-publish"]');
    await expectText(page, '#content', 'PUBLISHED');
    await go(page, '/catalog-releases/CAT-2026-10-R4');
    await page.click('[data-action="catalog-publish"]');
    await expectText(page, '#content', '已发布');
    await go(page, '/orders');
    await expectText(page, '#content', '没有访问权限');

    await setRole(page, 'support');
    await go(page, '/orders/ORD-20261019-028');
    expect((await page.locator('[data-action="order-grant-reconcile"]').count()) === 0, 'Support must not see entitlement reconcile action');
    await go(page, '/shares');
    expect((await page.locator('[data-action="share-revoke"]').count()) === 0, 'Support must not see share revoke action');
    await page.evaluate(() => {
      const forged = document.createElement('button');
      forged.dataset.action = 'share-revoke';
      document.body.appendChild(forged);
      forged.click();
      forged.remove();
    });
    await expectText(page, '#toast-root', '无权撤销分享');
    await go(page, '/exports');
    expect((await page.locator('[data-action="export-reapply"]').count()) === 0, 'Support must not see export reapply action');

    await setRole(page, 'finance');
    await go(page, '/orders/ORD-20261019-028');
    await expectText(page, '#content', '发放失败');
    await page.click('[data-action="order-grant-reconcile"]');
    await expectText(page, '#content', '补发成功');
    await go(page, '/refunds');
    expect(await page.locator('button:has-text("处理退款")').isDisabled(), 'Refund execution must stay blocked while policy is unresolved');
    await go(page, '/compensations');
    await page.click('[data-action="compensation-approve"]');
    await expectText(page, '#content', 'GRT-CMP-51018');
    await shot(page, 'p1-order-entitlement', 1440);

    await setRole(page, 'ops');
    await go(page, '/shares');
    await page.click('[data-action="share-revoke"]');
    await expectText(page, '#content', 'REVOKED');
    await go(page, '/exports');
    await page.click('[data-action="export-reapply"]');
    await expectText(page, '#content', '可下载');

    await setRole(page, 'super');
    const secondaryDetails = [
      ['/ai-jobs/AIJ-67022', 'EVI-99195', 'TRANSCRIPT_ONLY'],
      ['/preservations/NPS-44011', 'DEAL-99176-R1', 'REC-****8044'],
      ['/notary-reports/NRT-55184', 'TASK-77991', 'MANUAL_REVIEW'],
      ['/offline-notary/ONA-60992', '材料快照 v4', 'PROCESSING'],
      ['/products/PRD-ENT-2026-R3', '企业旗舰版', 'PUBLISHED'],
      ['/orders/ORD-20261018-114', 'AI 加量包快照 A2', 'GRT-76988'],
      ['/invoices/INV-261018-021', '企业电子普通发票', '华启数字服务有限公司'],
    ];
    for (const [route, identity, fact] of secondaryDetails) {
      await go(page, route);
      await expectText(page, '#content', identity);
      await expectText(page, '#content', fact);
      expect((await page.locator('.placeholder').count()) === 0, `${route} must be implemented in P1`);
    }

    const responsiveRoutes = ['/dashboard', '/users', '/numbers/NUM-10082', '/call-reconciliation', '/evidence/EVI-99182', '/ai-jobs/AIJ-67018', '/preservations/NPS-44018', '/notary-reports/NRT-55201', '/offline-notary/ONA-61018', '/products/PRD-PER-2026-R4', '/orders/ORD-20261019-028', '/audit'];
    for (const width of [1024, 1280, 1440]) {
      await page.setViewportSize({ width, height: width === 1024 ? 768 : 900 });
      for (const route of responsiveRoutes) {
        await go(page, route);
        await assertNoPageOverflow(page, width);
      }
    }
    await go(page, '/users');
    await shot(page, 'p0-users', 1024, 768);
    await go(page, '/calls/CALL-78392');
    await shot(page, 'p0-call-detail', 1440);

    expect(errors.length === 0, `Browser errors: ${errors.join(' | ')}`);
    console.log('Operations prototype P0/P1 QA passed: identity isolation, controlled commands, append-only versions, notary/report ordering, product publication, payment/entitlement separation, and responsive checks.');
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error.stack || error);
  process.exit(1);
});

