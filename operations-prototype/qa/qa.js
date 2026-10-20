const fs = require('fs');
const path = require('path');
const assert = require('node:assert/strict');
const { chromium } = require('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/.pnpm/playwright@1.60.0/node_modules/playwright');
const url = 'file:///' + path.resolve(__dirname, '../preview.html').replace(/\\/g, '/');
const screenshotDir = path.join(__dirname, 'screenshots');
fs.mkdirSync(screenshotDir, { recursive: true });
(async () => {
 const browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});
 const page=await browser.newPage({viewport:{width:1440,height:960}});
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 const state=()=>page.evaluate(()=>window.__onecallOps.snapshot());
 const business=()=>page.evaluate(()=>window.__onecallOps.businessSnapshot());
 const go=r=>page.evaluate(r=>window.__onecallOps.go(r),r);
 const action=async a=>page.locator('[data-action="'+a+'"]').last().click();
 const text=async(sel,value)=>assert.ok((await page.locator(sel).innerText()).includes(value),'Missing '+value+' in '+sel);
 const close=()=>page.keyboard.press('Escape');
 const confirm=async()=>{await page.fill('#reason','业务规则调整确认');await action('confirm');};
 const shot=name=>page.screenshot({path:path.join(screenshotDir,'alignment-'+name+'.png'),fullPage:!name.includes('form')});
 const login=async(name='admin.lin',code='482916')=>{await page.fill('#login-name',name);await page.fill('#login-code',code);await page.locator('#login-form button').click();};
 const logout=()=>action('logout');
 const forged=async(a,id)=>page.evaluate(({a,id})=>{const b=document.createElement('button');b.dataset.action=a;b.dataset.id=id;document.body.append(b);b.click();b.remove();},{a,id});
 const immutable=b=>JSON.stringify({orders:b.orders,trialGrants:b.trialGrants,evidence:b.evidence,entitlements:b.entitlements});
 const demo=async scenario=>{await page.evaluate(()=>document.querySelector('.review').open=true);await action('demo');await page.selectOption('#demo-case',scenario);await action('confirm');return (await business()).sessions.at(-1).id;};
 const overflow=async()=>{const s=await page.evaluate(()=>({width:innerWidth,doc:document.documentElement.scrollWidth,body:document.body.scrollWidth}));assert.ok(s.doc<=s.width&&s.body<=s.width,'Document overflow '+JSON.stringify(s));};
 const reload=async()=>{await page.goto(url+'#/dashboard');await page.reload();};
 const employeeAction=async(id,a)=>{await go('/employees/'+id);await action(a);await confirm();};
 try {
  await reload();await text('.login-panel','运营后台登录');assert.equal(await page.locator('#login-role').count(),0);await shot('login-1440');
  await login('admin.lin','000000');await text('#toast-root','登录名或 DEMO 码错误');assert.equal((await state()).loggedIn,false);
  await login('disabled.li','826350');assert.equal((await state()).loggedIn,false);await login();
  assert.equal((await state()).session.employeeId,'EMP-001');
  assert.deepEqual(await page.locator('nav[aria-label="主导航"] [data-route]').evaluateAll(els=>els.map(x=>x.dataset.route)),['/dashboard','/users','/catalog','/orders','/employees','/audit']);
  assert.equal(await page.locator('#review-role').count(),0);
  await go('/employees');await shot('employees-list-1440');
  await go('/employees/EMP-002');await shot('employee-detail-1440');

  // Employee create, normalization, cancel/focus, validation, immutable attribution.
  const startEmployees=(await state()).employees;
  await go('/employees');await action('create-employee');await page.fill('#employee-login','admin.lin');await page.fill('#employee-name','新员工');await confirm();await text('#modal-error','不能重复');await close();
  assert.deepEqual((await state()).employees,startEmployees);assert.equal(await page.evaluate(()=>document.activeElement.dataset.action),'create-employee');
  await action('create-employee');await page.fill('#employee-login','new.operator');await page.fill('#employee-name','新运营同事');await page.selectOption('#employee-role','ops');await action('confirm');await text('#modal-error','至少 4 个字');await confirm();
  const createCode=await page.locator('#credential-result').innerText();assert.match(createCode,/^\d{6}$/);await close();
  const created=(await state()).employees.find(e=>e.loginName==='new.operator');assert.ok(created);
  await go('/employees/'+created.id);await action('edit-employee');await page.fill('#employee-name','新同事改名');await confirm();assert.equal((await state()).employees.find(e=>e.id===created.id).displayName,'新同事改名');
  let last=(await state()).audit[0];assert.equal(last.actorEmployeeId,'EMP-001');assert.equal(last.actorDisplayNameAtAction,'林管理员');assert.equal(last.roleAtAction,'admin');assert.ok(!JSON.stringify(last).includes(createCode));
  await logout();await login('new.operator',createCode);assert.equal((await state()).session.employeeId,created.id);
  await go('/employees');await text('#content','无权访问');await forged('create-employee','');assert.equal(await page.locator('.modal').count(),0);
  const beforeLow=await business();await go('/catalog/PERSONAL_BASIC_MONTHLY-V1');for(const a of ['edit-product','edit-draft','publish','off-sale','on-sale'])await forged(a,'PERSONAL_BASIC_MONTHLY-V1');
  await go('/catalog');await forged('create-product','');await go('/trial-settings');await forged('edit-trial','');await go('/employees/EMP-001');await forged('reset-credential','EMP-001');
  assert.equal(await page.locator('.modal').count(),0);assert.deepEqual(await business(),beforeLow);assert.deepEqual((await state()).employees,[]);const opsImport=await page.evaluate(()=>{const h=window.__onecallOps,b=h.businessSnapshot();b.rule.seconds=999;return h.loadReviewSnapshot(b);});assert.equal(opsImport.code,'FORBIDDEN');assert.deepEqual(await business(),beforeLow);
  // Operators retain user pause/resume.
  await go('/users/SUB-1002');await action('suspend');await close();await action('suspend');await confirm();assert.equal((await business()).subjects.find(s=>s.id==='SUB-1002').activation,'SUSPENDED');await action('resume');await confirm();
  assert.equal((await state()).audit[0].actorEmployeeId,created.id);
  await logout();await login();

  // Reset code appears once, old code and disabled login are rejected.
  await employeeAction(created.id,'reset-credential');const resetCode=await page.locator('#credential-result').innerText();assert.notEqual(resetCode,createCode);await close();
  assert.equal(await page.locator('#credential-result').count(),0);
  await logout();await login('new.operator',createCode);assert.equal((await state()).loggedIn,false);await login('new.operator',resetCode);assert.equal((await state()).loggedIn,true);await logout();await login();
  await employeeAction(created.id,'disable-employee');await logout();await login('new.operator',resetCode);assert.equal((await state()).loggedIn,false);await login();await employeeAction(created.id,'enable-employee');

  // Last active administrator protection and self role/disable/reset revocation.
  await employeeAction('EMP-002','disable-employee');
  const sole=(await state()).employees.find(e=>e.id==='EMP-001');await go('/employees/EMP-001');await action('disable-employee');await confirm();await text('#modal-error','最后一个启用管理员');await close();
  await action('edit-employee');await page.selectOption('#employee-role','ops');await confirm();await text('#modal-error','最后一个启用管理员');await close();assert.deepEqual((await state()).employees.find(e=>e.id==='EMP-001'),sole);
  await employeeAction('EMP-002','enable-employee');
  await go('/employees/EMP-001');await action('edit-employee');await page.selectOption('#employee-role','ops');await confirm();assert.equal((await state()).loggedIn,false);assert.equal(await page.locator('.modal').count(),0);
  await login('admin.chen','593027');assert.equal((await state()).session.employeeId,'EMP-002');await go('/employees/EMP-001');await action('edit-employee');await page.selectOption('#employee-role','admin');await confirm();
  await logout();await login();await employeeAction('EMP-001','disable-employee');assert.equal((await state()).loggedIn,false);await login();assert.equal((await state()).loggedIn,false);
  await login('admin.chen','593027');await employeeAction('EMP-001','enable-employee');await logout();await login();
  await employeeAction('EMP-001','reset-credential');const ownCode=await page.locator('#credential-result').innerText();assert.equal((await state()).loggedIn,false);await close();await forged('confirm','');await login('admin.lin','482916');assert.equal((await state()).loggedIn,false);await login('admin.lin',ownCode);assert.equal((await state()).loggedIn,true);
  assert.ok(!(await page.locator('#root').innerText()).includes(ownCode));assert.ok(!JSON.stringify((await state()).audit).includes(ownCode));

  // Business snapshots never contain employees, credentials, auth or widget state.
  const clean=await business();assert.deepEqual(Object.keys(clean).sort(),['schemaVersion','catalog','rule','subjects','claims','trialGrants','sessions','evidence','activations','orders','entitlements','processedEvents','sequence'].sort());
  const rejected=await page.evaluate(()=>{const h=window.__onecallOps,b=h.businessSnapshot();b.employees=[];return h.loadReviewSnapshot(b);});assert.equal(rejected.ok,false);assert.deepEqual(await business(),clean);

  // Unknown IDs never select a fallback; legacy financial routes have no writes.
  for(const r of ['/users/UNKNOWN','/catalog/UNKNOWN','/orders/UNKNOWN','/ivr-sessions/UNKNOWN','/employees/UNKNOWN']){await go(r);await text('#content','记录不存在');}
  for(const r of ['/tickets','/refunds','/invoices','/vendors','/roles']){await go(r);await text('#content','页面未纳入本期');}
  await go('/roadmap');await text('#content','人工客服联系方式待配置');assert.equal(await page.locator('#content a[href^="tel:"],#content a[href^="mailto:"]').count(),0);

  // Catalog adds a DRAFT, rejects invalid commercial terms/channels, edits same ID.
  const historical=immutable(await business());
  await go('/catalog');await action('create-product');await shot('catalog-form-1440');await page.locator('#reason').scrollIntoViewIfNeeded();await shot('catalog-form-bottom-1440');
  await page.fill('#pkg-code','DEMO_NEW');await page.fill('#pkg-sku','DEMO_NEW_ONCE');await page.fill('#pkg-name','录音测试包');await page.fill('#pkg-description','仅用于本地配置评审');await page.fill('#pkg-price','-1');await confirm();await text('#modal-error','请检查');
  for(const bad of ['NaN','1.001','0','100000']){await page.fill('#pkg-price',bad);await confirm();await text('#modal-error','请检查');}
  await page.fill('#pkg-price','19.90');await page.fill('#pkg-seats','1');await confirm();await text('#modal-error','个人不能携带企业');await page.fill('#pkg-seats','0');
  for(const channel of ['MINIAPP','WEB'])await page.locator('[name="pkg-channel"][value="'+channel+'"]').uncheck();await confirm();await text('#modal-error','请检查');await page.locator('[name="pkg-channel"][value="WEB"]').check();
  await page.fill('#pkg-recordingMinutes','30');await confirm();let b=await business(),draft=b.catalog.find(p=>p.sku==='DEMO_NEW_ONCE');assert.equal(draft.saleStatus,'DRAFT');assert.deepEqual(draft.channels,['WEB']);assert.equal(draft.priceCents,1990);
  const draftId=draft.versionId;await action('edit-draft');await page.fill('#pkg-name','录音测试包修订');await confirm();draft=(await business()).catalog.find(p=>p.versionId===draftId);assert.equal(draft.revision,2);assert.equal((await business()).catalog.filter(p=>p.sku===draft.sku).length,1);
  const draftCheckout=await page.evaluate(id=>{const b=window.__onecallOps.businessSnapshot(),p=b.catalog.find(p=>p.versionId===id);return window.OnecallBusiness.createOrder(b,{subjectId:'SUB-1003',accountId:'USR-2271',channel:'WEB',versionId:id,versionToken:window.OnecallBusiness.offerVersionToken(p),confirmed:true,consentVersion:'QA-CONSENT',idempotencyKey:'QA-DRAFT'},new Date());},draftId);assert.equal(draftCheckout.code,'NOT_SELLABLE');
  // Cancel / Esc / double submit do not create extra versions or audit.
  await action('edit-draft');const cancelBefore=await state();await page.fill('#pkg-name','取消的修改');await close();assert.deepEqual((await state()).business,cancelBefore.business);assert.equal((await state()).audit.length,cancelBefore.audit.length);assert.equal(await page.evaluate(()=>document.activeElement.dataset.action),'edit-draft');
  await action('edit-draft');await page.fill('#pkg-name','确认一次');await page.fill('#reason','业务配置评审确认');const beforeDouble=(await state()).audit.length;await page.evaluate(()=>{const b=document.querySelector('[data-action="confirm"]');b.click();b.click();});assert.equal((await state()).audit.length,beforeDouble+1);
  // A legitimate review import changes a target revision while a confirmation is pending.
  await action('edit-draft');await page.fill('#pkg-name','不应写入');const staleCandidate=await business();staleCandidate.catalog.find(p=>p.versionId===draftId).revision++;await page.evaluate(b=>window.__onecallOps.loadReviewSnapshot(b),staleCandidate);await confirm();await text('#modal-error','修订或配置已变化');assert.equal((await business()).catalog.find(p=>p.versionId===draftId).name,'确认一次');await close();
  await go('/catalog/'+draftId);await action('publish');await confirm();assert.equal((await business()).catalog.find(p=>p.versionId===draftId).saleStatus,'ACTIVE');
  assert.equal(immutable(await business()),historical);

  // Current catalog versioning is distinct from the preserved pre-alignment orders.
  await go('/catalog/PERSONAL_BASIC_MONTHLY-V1');await action('edit-product');await page.fill('#pkg-price','41.25');await page.fill('#pkg-aiMinutes','77');await confirm();
  b=await business();const v2=b.catalog.find(p=>p.sku==='PERSONAL_BASIC_MONTHLY'&&p.saleStatus==='DRAFT');assert.equal(v2.version,2);assert.equal(v2.rights.aiMinutes,77);
  await action('publish');await confirm();b=await business();assert.equal(b.catalog.find(p=>p.versionId==='PERSONAL_BASIC_MONTHLY-V1').saleStatus,'OFFSALE');assert.equal(b.catalog.find(p=>p.versionId===v2.versionId).saleStatus,'ACTIVE');assert.equal(immutable(b),historical);
  await shot('catalog-history-1440');await action('off-sale');await confirm();const visibility=await page.evaluate(id=>window.OnecallBusiness.visibleOffers(window.__onecallOps.businessSnapshot(),{channel:'WEB',audience:'PERSONAL'}).value.some(p=>p.versionId===id),v2.versionId);assert.equal(visibility,false);await action('on-sale');await confirm();
  const visibleChannels=await page.evaluate(id=>['WEB','MINIAPP'].map(channel=>window.OnecallBusiness.visibleOffers(window.__onecallOps.businessSnapshot(),{channel,audience:'PERSONAL'}).value.some(p=>p.versionId===id)),draftId);assert.deepEqual(visibleChannels,[true,false]);
  const checkout=await page.evaluate(({id,v2})=>{const B=window.OnecallBusiness,b=window.__onecallOps.businessSnapshot(),input={subjectId:'SUB-1003',accountId:'USR-2271',channel:'WEB',versionId:v2,confirmed:true,consentVersion:'QA-CONSENT',idempotencyKey:'QA-CURRENT'};input.versionToken=B.offerVersionToken(b.catalog.find(p=>p.versionId===v2));const current=B.createOrder(b,input,new Date());const restricted=window.__onecallOps.businessSnapshot(),p=restricted.catalog.find(p=>p.versionId===id),offchannel=B.createOrder(restricted,{...input,channel:'MINIAPP',versionId:id,versionToken:B.offerVersionToken(p),idempotencyKey:'QA-OFFCHANNEL'},new Date());return {current,offchannel};},{id:draftId,v2:v2.versionId});assert.equal(checkout.current.ok,true);assert.equal(checkout.current.value.snapshot.priceCents,4125);assert.equal(checkout.current.value.snapshot.versionId,v2.versionId);assert.equal(checkout.offchannel.code,'NOT_SELLABLE');
  await go('/catalog');await page.selectOption('[name="status"]','DRAFT');await page.locator('#catalog-filter button').click();await text('#content','暂无符合条件');await go('/catalog');await page.selectOption('[name="audience"]','ENTERPRISE');await page.locator('#catalog-filter button').click();await text('#content','企业基础版');

  // Trial edits preserve every old grant/order/evidence; a first new caller uses v2.
  const beforeTrial=immutable(await business());await go('/trial-settings');await action('edit-trial');await page.fill('#trial-seconds','NaN');await confirm();await text('#modal-error','整数秒');await page.fill('#trial-seconds','120');await page.fill('#trial-days','7');await confirm();assert.equal((await business()).rule.version,2);assert.equal(immutable(await business()),beforeTrial);
  const sessionId=await demo('new');await action('demo-use');await text('#modal-error','需要明确开通');await action('demo-confirm');b=await business();const sid=b.sessions.find(s=>s.id===sessionId).subjectId;let u=b.subjects.find(s=>s.id===sid),grant=b.trialGrants.find(g=>g.subjectId===sid);assert.equal(u.identity,'UNVERIFIED');assert.equal(u.accountId,null);assert.equal(grant.grantedSeconds,120);assert.equal(grant.ruleVersion,2);
  await action('demo-confirm');await text('#demo-outcome','不重复开通');assert.equal((await business()).trialGrants.filter(g=>g.subjectId===sid).length,1);
  await action('demo-use');const evidence=(await business()).evidence.find(e=>e.subjectId===sid);assert.equal(evidence.recordedSeconds,60);assert.equal(evidence.identityAtCapture,'UNVERIFIED');
  const links=(await business()).sessions.map(s=>[s.id,s.subjectId,s.confirmedSubjectId]);await action('demo-claim');b=await business();assert.equal(b.subjects.find(s=>s.id===sid).identity,'UNVERIFIED');assert.deepEqual(b.evidence.find(e=>e.id===evidence.id),evidence);assert.deepEqual(b.sessions.map(s=>[s.id,s.subjectId,s.confirmedSubjectId]),links);
  const count=b.orders.length;await action('demo-guide');await text('#demo-outcome','未创建付费订单');assert.equal((await business()).orders.length,count);await action('demo-use');await action('demo-use');await text('#modal-error','剩余试用权益');await close();await go('/users/'+sid);await text('#content','已耗尽');await shot('user-trial-1440');
  for(const scenario of ['no-caller','no-consent']){const prev=await business();await demo(scenario);await action('demo-confirm');await text('#modal-error','不发试用');await action('demo-use');const after=await business();assert.deepEqual(after.trialGrants,prev.trialGrants);assert.deepEqual(after.evidence,prev.evidence);assert.deepEqual(after.activations,prev.activations);assert.equal(after.subjects.length,prev.subjects.length);await close();}
  await demo('expired');await action('demo-confirm');await action('demo-use');await text('#modal-error','未到期');await close();await demo('conflict');await action('demo-confirm');await action('demo-claim');await text('#modal-error','绑定冲突');await close();
  await go('/users/SUB-1002');await action('suspend');await confirm();const oldGrant=(await business()).trialGrants.find(g=>g.subjectId==='SUB-1002');await action('resume');await confirm();assert.deepEqual((await business()).trialGrants.find(g=>g.subjectId==='SUB-1002'),oldGrant);
  await go('/users');await page.selectOption('[name="source"]','IVR');await page.selectOption('[name="status"]','pending');await page.locator('#user-filter button').click();await text('tbody','SUB-1002');await page.fill('[name="q"]','absent');await page.locator('#user-filter button').click();await text('#content','暂无符合条件');
  await go('/orders/ORD-602');await text('#content','支付成功');await text('#content','待发放');assert.equal(await page.locator('#content [data-action]').count(),0);
  await go('/orders/ORD-601');await text('#content','¥29.00');await text('#content','100');assert.equal((await business()).orders.find(o=>o.id==='ORD-601').snapshot.priceCents,2900);
  await go('/catalog/PKG-PER-1');assert.equal(await page.locator('#content [data-action]').count(),0);const historyBefore=await business();for(const a of ['on-sale','edit-product','publish'])await forged(a,'PKG-PER-1');assert.deepEqual(await business(),historyBefore);assert.equal(await page.locator('.modal').count(),0);

  // Logout and hash/page transitions close pending mutations.
  await go('/catalog/'+v2.versionId);await action('edit-product');await page.evaluate(()=>location.hash='/orders');await page.waitForFunction(()=>window.__onecallOps.snapshot().route==='/orders');assert.equal(await page.locator('.modal').count(),0);
  await go('/trial-settings');await action('edit-trial');await forged('logout','');assert.equal((await state()).loggedIn,false);assert.equal(await page.locator('.modal').count(),0);await forged('confirm','');await login('admin.lin',ownCode);

  // Every addressable route and important modal fits all required desktop widths.
  const samples={'/users/:id':'/users/SUB-1002','/ivr-sessions/:id':'/ivr-sessions/IVR-8001','/catalog/:id':'/catalog/'+v2.versionId,'/orders/:id':'/orders/ORD-601','/employees/:id':'/employees/EMP-002'};
  for(const width of [720,1024,1280,1440]){
   await page.setViewportSize({width,height:900});
   for(const r of await page.evaluate(()=>window.__onecallOps.routes)){await go(samples[r.path]||r.path);await overflow();assert.ok((await page.locator('#content').innerText()).length>20);}
   await go('/catalog');await action('create-product');await overflow();await close();
  }
  await page.setViewportSize({width:1280,height:900});await go('/trial-settings');await shot('trial-settings-1280');await action('edit-trial');await page.keyboard.press('Tab');assert.equal(await page.evaluate(()=>!!document.activeElement.closest('.modal')),true);await close();
  await go('/employees');await shot('employees-list-1280');await page.setViewportSize({width:720,height:900});await go('/employees/EMP-002');await shot('employee-detail-720');
  for(const mode of ['empty','loading','unavailable']){await page.evaluate(v=>window.__onecallOps.setReviewDataMode(v),mode);await text('#content','原型评审数据状态');await forged('disable-employee','EMP-002');assert.equal(await page.locator('.modal').count(),0);}await page.evaluate(()=>window.__onecallOps.setReviewDataMode('normal'));
  await logout();await overflow();await page.reload();await text('.login-panel','运营后台登录');
  // Disabled new grants use a fresh known caller; existing first-trial grants survive.
  await login();await go('/trial-settings');const existingGrants=(await business()).trialGrants;await action('edit-trial');await page.selectOption('#trial-enabled','false');await confirm();await demo('new');await action('demo-confirm');await text('#demo-outcome','新试用领取已关闭');assert.deepEqual((await business()).trialGrants,existingGrants);await action('demo-use');await text('#modal-error','剩余试用权益');await close();
  assert.deepEqual(errors,[]);
  console.log('Alignment S2 operations QA passed: six primary entries; independent employee login/create/edit/enable/disable/reset; last admin and self session revocation; DOM/route guards; frozen stale/double/cancel confirmations; complete versioned canonical catalog and trial; historical snapshots; IVR basic regression; responsive 1440/1280/1024/720.');
 } finally {await browser.close();}
})().catch(e=>{console.error(e.stack||e);process.exit(1);});
