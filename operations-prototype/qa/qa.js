const fs = require('fs');
const path = require('path');
const assert = require('node:assert/strict');
const { chromium } = require('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/.pnpm/playwright@1.60.0/node_modules/playwright');
const url = `file:///${path.resolve(__dirname, '../preview.html').replace(/\\/g, '/')}`;
const screenshotDir = path.join(__dirname, 'screenshots');
fs.mkdirSync(screenshotDir, { recursive: true });

(async () => {
  const browser = await chromium.launch({ executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  const snapshot = () => page.evaluate(() => window.__onecallOps.snapshot());
  const go = async route => { await page.evaluate(r => window.__onecallOps.go(r), route); };
  const role = async value => { await page.evaluate(v => window.__onecallOps.setReviewRole(v), value); };
  const mode = async value => { await page.evaluate(v => window.__onecallOps.setReviewDataMode(v), value); };
  const action = async name => { await page.locator(`[data-action="${name}"]`).last().click(); };
  const text = async (selector, value) => assert.ok((await page.locator(selector).innerText()).includes(value), `Missing ${value} in ${selector}`);
  const confirm = async () => { await page.fill('#reason', '业务规则调整确认'); await action('confirm'); };
  const close = async () => { await page.keyboard.press('Escape'); };
  const demo = async scenario => {
    await page.evaluate(() => document.querySelector('.review').open = true);
    await action('demo');
    await page.selectOption('#demo-case', scenario);
    await action('confirm');
    const state = await snapshot();
    return state.sessions[0].id;
  };
  const overflow = async () => {
    const size = await page.evaluate(() => ({ width: innerWidth, doc: document.documentElement.scrollWidth, body: document.body.scrollWidth }));
    assert.ok(size.doc <= size.width && size.body <= size.width, `Page overflow: ${JSON.stringify(size)}`);
  };
  try {
    await page.goto(url + '#/dashboard');
    await text('.login-panel', '运营后台登录');
    await page.fill('#login-code', '000000');
    await page.locator('#login-form button').click();
    await text('#toast-root', '演示验证码');
    await page.fill('#login-code', '482916');
    await page.locator('#login-form button').click();
    assert.equal((await snapshot()).loggedIn, true);
    assert.equal(await page.locator('#toast-root').innerText(), '');
    assert.equal((await page.evaluate(() => window.__onecallOps.routes)).length, 13);
    await page.screenshot({ path: path.join(screenshotDir, 'lean-dashboard-1440.png') });

    // The unclaimed caller is a valid evidence subject without fabricating identity.
    await go('/users/SUB-1002');
    await text('#content', '待认领'); await text('#content', '未实名'); await text('#content', 'EVI-9901');
    const base = await snapshot();
    const originalTrial = JSON.stringify(base.subjects.find(s => s.id === 'SUB-1002').trial);
    await action('suspend'); await action('confirm');
    await text('#modal-error', '至少 4 个字');
    await close();
    assert.equal((await snapshot()).subjects.find(s => s.id === 'SUB-1002').active, true);
    assert.equal(await page.evaluate(() => document.activeElement.dataset.action), 'suspend');
    await action('suspend'); await confirm();
    assert.equal((await snapshot()).subjects.find(s => s.id === 'SUB-1002').active, false);
    await demo('repeat'); await action('demo-confirm'); await text('#modal-error', '业务已暂停'); await close();
    await go('/users/SUB-1002'); await action('resume'); await confirm();
    assert.equal(JSON.stringify((await snapshot()).subjects.find(s => s.id === 'SUB-1002').trial), originalTrial);

    // Current role/page and hidden or removed workflows cannot perform writes.
    await role('ops'); await go('/catalog/PKG-PER-1');
    assert.equal(await page.locator('[data-action="edit-product"]').count(), 0);
    await page.evaluate(() => { const b = document.createElement('button'); b.dataset.action = 'off-sale'; b.dataset.id = 'PKG-PER-1'; b.id = 'forged'; document.body.append(b); b.click(); b.remove(); });
    assert.equal((await snapshot()).catalog.find(p => p.id === 'PKG-PER-1').status, 'ACTIVE');
    assert.equal(await page.locator('.modal').count(), 0);
    for (const old of ['/tickets', '/refunds', '/invoices', '/vendors', '/roles', '/provisioning-attempts']) {
      await go(old); await text('#content', '页面未纳入本期');
    }
    await role('admin');

    // Missing caller and no confirmation create neither subject nor entitlement.
    for (const scenario of ['no-caller', 'no-consent']) {
      const before = await snapshot(); await demo(scenario); await action('demo-confirm');
      await text('#modal-error', '不发试用'); await action('demo-use'); await text('#modal-error', '需要明确开通');
      const after = await snapshot();
      assert.equal(after.subjects.length, before.subjects.length);
      assert.equal(after.evidence.length, before.evidence.length);
      assert.equal(after.activations.length, before.activations.length);
      await close();
    }
    await demo('repeat'); await action('demo-confirm');
    await text('#demo-outcome', '不重复领取');
    assert.equal(JSON.stringify((await snapshot()).subjects.find(s => s.id === 'SUB-1002').trial), originalTrial);
    await close();

    const expiredTrial = (await snapshot()).subjects.find(s => s.id === 'SUB-1005').trial;
    assert.ok(expiredTrial.seconds > expiredTrial.used && expiredTrial.expires < Date.now());
    await demo('expired'); await action('demo-confirm');
    await text('#demo-outcome', '不重复领取');
    const expiredEvidenceCount = (await snapshot()).evidence.length;
    await action('demo-use'); await text('#modal-error', '未到期');
    assert.equal((await snapshot()).evidence.length, expiredEvidenceCount);
    assert.deepEqual((await snapshot()).subjects.find(s => s.id === 'SUB-1005').trial, expiredTrial);
    await close();

    // New IVR caller: explicit key -> trial -> evidence -> verified claim -> miniapp referral.
    const sessionId = await demo('new');
    await action('demo-use'); await text('#modal-error', '需要明确开通');
    await action('demo-confirm');
    let state = await snapshot();
    const subjectId = state.sessions.find(s => s.id === sessionId).subject;
    let subject = state.subjects.find(s => s.id === subjectId);
    assert.equal(subject.identity, '未实名'); assert.equal(subject.account, null); assert.equal(subject.trial.seconds, 300);
    const grants = state.audit.filter(e => e.event === '试用发放').length;
    await action('demo-confirm'); await text('#modal-error', '不重复开通');
    assert.equal((await snapshot()).audit.filter(e => e.event === '试用发放').length, grants);
    await action('demo-use');
    const evidenceBeforeClaim = (await snapshot()).evidence.find(e => e.subject === subjectId);
    assert.equal(evidenceBeforeClaim.seconds, 60); assert.equal(evidenceBeforeClaim.identityAtCapture, '未实名');
    await action('demo-claim');
    state = await snapshot(); subject = state.subjects.find(s => s.id === subjectId);
    assert.ok(subject.account); assert.equal(subject.identity, '未实名'); assert.equal(subject.trial.used, 60);
    assert.deepEqual(state.evidence.find(e => e.id === evidenceBeforeClaim.id), evidenceBeforeClaim);
    await action('demo-claim'); await text('#modal-error', '不重复关联');
    const orderCount = (await snapshot()).orders.length;
    await action('demo-guide'); await text('#demo-outcome', '未创建付费订单');
    assert.equal((await snapshot()).orders.length, orderCount);
    for (let i = 0; i < 4; i++) await action('demo-use');
    await text('#demo-outcome', '已结束取证通话');
    const evidenceCount = (await snapshot()).evidence.length;
    await action('demo-use'); await text('#modal-error', '剩余试用权益');
    assert.equal((await snapshot()).evidence.length, evidenceCount);
    await close();
    await go('/users/' + subjectId); await text('#content', '已耗尽');
    await page.screenshot({ path: path.join(screenshotDir, 'lean-ivr-user-1440.png') });
    await demo('conflict'); await action('demo-confirm'); await action('demo-claim');
    await text('#modal-error', '绑定冲突'); await close();

    // Disabled/expired/exhausted grants cannot be bypassed by repeat caller flows.
    await demo('new'); await action('demo-confirm');
    const pausedSession = (await snapshot()).sessions[0].id;
    const pausedSubject = (await snapshot()).sessions[0].subject;
    await close(); await go('/users/' + pausedSubject); await action('suspend'); await confirm();
    // Read-only session view does not expose the review commands in product UI.
    await go('/ivr-sessions/' + pausedSession);
    assert.equal(await page.locator('[data-action="demo-use"]').count(), 0);

    // Product versioning validates all inputs and keeps immutable order snapshots.
    const ordersBefore = (await snapshot()).orders;
    await go('/catalog/PKG-PER-1'); await action('edit-product');
    await page.fill('#pkg-price', '-1'); await confirm(); await text('#modal-error', '请检查');
    await page.fill('#pkg-price', '39.00'); await page.fill('#pkg-minutes', '0'); await confirm(); await text('#modal-error', '请检查');
    await page.fill('#pkg-minutes', '150'); await confirm();
    let draft = (await snapshot()).catalog.find(p => p.status === 'DRAFT');
    assert.equal(draft.price, 3900); assert.equal(draft.minutes, 150);
    assert.equal((await snapshot()).catalog.find(p => p.id === 'PKG-PER-1').price, 2900);
    await action('publish'); await confirm();
    state = await snapshot();
    assert.equal(state.catalog.find(p => p.id === draft.id).status, 'ACTIVE');
    assert.equal(state.catalog.find(p => p.id === 'PKG-PER-1').status, 'OFFSALE');
    assert.deepEqual(state.orders, ordersBefore);
    await action('off-sale'); await confirm();
    await action('on-sale'); await confirm();
    assert.equal((await snapshot()).catalog.filter(p => p.sku === 'PERSONAL' && p.status === 'ACTIVE').length, 1);
    // Route or role changes cancel the frozen dialog, so stale confirmation is unavailable.
    await action('edit-product'); await role('ops');
    assert.equal(await page.locator('.modal').count(), 0);
    await role('admin'); await go('/catalog/' + draft.id); await action('edit-product');
    await page.evaluate(() => location.hash = '/orders');
    await page.waitForFunction(() => window.__onecallOps.snapshot().route === '/orders');
    assert.equal(await page.locator('.modal').count(), 0);
    await page.setViewportSize({ width:1280, height:900 });
    await go('/catalog'); await page.screenshot({ path: path.join(screenshotDir, 'lean-catalog-1280.png') });

    // Trial rule updates affect only future grants. Disabling does not revoke prior grants.
    const grantsBefore = (await snapshot()).subjects.map(s => [s.id, s.trial]);
    await go('/trial-settings'); await action('edit-trial');
    await page.fill('#trial-seconds', 'NaN'); await confirm(); await text('#modal-error', '整数秒');
    await page.fill('#trial-seconds', '120'); await page.fill('#trial-days', '7'); await confirm();
    state = await snapshot(); assert.equal(state.rule.version, 2);
    assert.deepEqual(state.subjects.map(s => [s.id, s.trial]), grantsBefore);
    await demo('new'); await action('demo-confirm');
    state = await snapshot();
    const newSubject = state.subjects.find(s => s.id === state.sessions[0].subject);
    assert.equal(newSubject.trial.seconds, 120); assert.equal(newSubject.trial.rule, 2);
    await close();
    await go('/trial-settings'); await action('edit-trial'); await page.selectOption('#trial-enabled', 'false'); await confirm();
    await demo('new'); await action('demo-confirm'); await text('#demo-outcome', '新试用领取已关闭');
    state = await snapshot(); assert.equal(state.subjects.find(s => s.id === state.sessions[0].subject).trial, null);
    await action('demo-use'); await text('#modal-error', '剩余试用权益'); await close();
    assert.equal((await snapshot()).subjects.find(s => s.id === newSubject.id).trial.seconds, 120);

    // Query/filter, object identity, no financial write endpoints, keyboard and data states.
    await go('/users'); await page.selectOption('[name="source"]', 'IVR'); await page.selectOption('[name="status"]', 'pending');
    await page.locator('#user-filter button').click(); await text('tbody', 'SUB-1002');
    assert.ok(!(await page.locator('tbody').innerText()).includes('USR-2048'));
    await page.fill('[name="q"]', 'does-not-exist'); await page.locator('#user-filter button').click(); await text('#content', '暂无符合条件');
    await go('/orders/ORD-602'); await text('#content', '支付成功'); await text('#content', '发放中');
    assert.equal(await page.locator('#content [data-action]').count(), 0);
    for (const route of ['/users/UNKNOWN', '/catalog/UNKNOWN', '/orders/UNKNOWN', '/ivr-sessions/UNKNOWN']) { await go(route); await text('#content', '记录不存在'); }
    for (const value of ['loading', 'empty', 'unavailable']) { await mode(value); await text('#content', '原型评审数据状态'); }
    await mode('normal');
    const samples = {'/users/:id':'/users/SUB-1002','/ivr-sessions/:id':'/ivr-sessions/IVR-8001','/catalog/:id':'/catalog/PKG-PER-1','/orders/:id':'/orders/ORD-601'};
    const routeList = await page.evaluate(() => window.__onecallOps.routes);
    for (const width of [720, 1024, 1280, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      for (const route of routeList) { await go(samples[route.path] || route.path); await overflow(); assert.ok((await page.locator('#content').innerText()).length > 30); }
    }
    await page.setViewportSize({ width:1280, height:900 });
    await go('/ivr-sessions/IVR-8001'); await page.screenshot({ path:path.join(screenshotDir,'lean-ivr-session-1280.png') });
    await go('/users/SUB-1002'); await page.screenshot({ path:path.join(screenshotDir,'lean-ivr-user-1280.png') });
    await go('/trial-settings'); await page.screenshot({ path:path.join(screenshotDir,'lean-trial-settings-1280.png') });
    await action('edit-trial'); await page.keyboard.press('Tab');
    assert.equal(await page.evaluate(() => !!document.activeElement.closest('.modal')), true);
    await close();
    await action('logout'); await text('.login-panel','运营后台登录');
    assert.equal(await page.locator('#content').count(),0);
    await page.reload(); await text('.login-panel','运营后台登录');
    assert.deepEqual(errors,[]);
    console.log('Lean operations QA passed: 13 routes; IVR unverified trial/evidence, consent, deduplication, claim/conflict, quota exhaustion; versioned catalog and trial rules; payment isolation; role/page guards; responsive layout and keyboard checks.');
  } finally { await browser.close(); }
})().catch(error => { console.error(error.stack || error); process.exit(1); });
