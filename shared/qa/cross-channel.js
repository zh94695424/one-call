// Local fixture handoff only: no network, telephony, authentication or payment integration.
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs');
const { pathToFileURL } = require('node:url');
const { chromium } = require('C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/.pnpm/playwright@1.60.0/node_modules/playwright');
const B = require('../onecall-business');
const root = path.resolve(__dirname, '../..');
const copy = value => JSON.parse(JSON.stringify(value));
const facts = value => ({ sessions:value.sessions, evidence:value.evidence, trialGrants:value.trialGrants });
const forbidden = ['employees','employeeLoginSession','employeeCredentials','employeeRoles','customerAuthSession','uiSession','widgetState','code'];

(async () => {
  const browser = await chromium.launch({ headless:true, executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe' });
  const errors = [];
  try {
    const admin = await browser.newPage({viewport:{width:1440,height:960}});
    const miniPage = await browser.newPage({viewport:{width:393,height:852}});
    const web = await browser.newPage({viewport:{width:1280,height:900}});
    for (const page of [admin,miniPage,web]) {
      page.on('pageerror', error => errors.push(error.message));
      page.on('console', message => { if(message.type()==='error') errors.push(message.text()); });
    }
    await Promise.all([
      admin.goto(pathToFileURL(path.join(root,'operations-prototype/preview.html')).href),
      miniPage.goto(pathToFileURL(path.join(root,'prototype/preview.html')).href),
      web.goto(pathToFileURL(path.join(root,'web-prototype/preview.html')).href),
    ]);
    const mini = await (await miniPage.locator('iframe').elementHandle()).contentFrame();
    assert.ok(mini);
    const surfaces = [{frame:admin,hook:'__onecallOps'},{frame:mini,hook:'__onecallMini'},{frame:web,hook:'__onecallWeb'}];
    const snapshot = s => s.frame.evaluate(h=>window[h].businessSnapshot(),s.hook);
    const load = async (s,data) => { const r=await s.frame.evaluate(({h,data})=>window[h].loadReviewSnapshot(data),{h:s.hook,data});assert.equal(r.ok,true,JSON.stringify(r));assert.deepEqual(await snapshot(s),data); };
    const [a,m,w] = surfaces;
    const ago = route => admin.evaluate(route=>window.__onecallOps.go(route),route);
    const mgo = route => mini.evaluate(route=>window.__onecallMini.navigate(route),route);
    const wgo = route => web.evaluate(route=>window.__onecallWeb.navigate(route),route);
    const act = name => admin.locator('[data-action="'+name+'"]').last().click();
    const confirm = async () => {await admin.fill('#reason','跨端本地 fixture 评审');await act('confirm');};
    const demo = async name => {await admin.evaluate(()=>document.querySelector('.review').open=true);await act('demo');await admin.selectOption('#demo-case',name);await act('confirm');return (await snapshot(a)).sessions.at(-1).id;};
    const text = async (frame,selector,value) => assert.ok((await frame.locator(selector).innerText()).includes(value),'Missing '+value+' in '+selector);
    const shot = async (page,dir,name) => {const p=path.join(root,dir,'qa/screenshots','alignment-cross-'+name+'.png');fs.mkdirSync(path.dirname(p),{recursive:true});await page.screenshot({path:p,fullPage:true});};

    await admin.fill('#login-name','admin.lin');await admin.fill('#login-code','482916');await admin.locator('#login-form button').click();
    await web.locator('[data-action="wechat-login"]').click();await web.locator('[data-workspace-choice="personal"]').click();
    const initial = await snapshot(a), historical=copy(initial.orders);
    await ago('/catalog/PERSONAL_BASIC_MONTHLY-V1');await act('edit-product');
    await admin.fill('#pkg-price','41.25');await admin.fill('#pkg-aiMinutes','77');await admin.fill('#pkg-recordingMinutes','83');await admin.fill('#pkg-days','45');await confirm();
    await act('publish');await confirm();
    await ago('/trial-settings');await act('edit-trial');await admin.fill('#trial-seconds','120');await admin.fill('#trial-days','7');await confirm();
    const published=await snapshot(a),offer=published.catalog.find(item=>item.sku==='PERSONAL_BASIC_MONTHLY'&&item.saleStatus==='ACTIVE');
    assert.equal(offer.priceCents,4125);assert.equal(offer.validDays,45);assert.equal(offer.rights.aiMinutes,77);assert.equal(published.rule.seconds,120);assert.deepEqual(published.orders,historical);
    // A validated ordinary account fixture permits a purchase independently of IVR claim.
    const commerce=copy(published);
    commerce.subjects.push({id:'QA-PURCHASE-SUBJECT',maskedNumber:'138 **** 9981',numberType:'MOBILE',source:'MINIAPP',accountId:'ACCOUNT-DEMO-001',identity:'VERIFIED',activation:'ACTIVE',revision:1,claimRisk:'NONE'});
    const chooseMini = async () => {await mgo('plans');await mini.locator('[data-product="个人基础"]').click();await mini.locator('[data-billing-cycle="MONTHLY"]').click();};
    const chooseWeb = async () => {await wgo('plans');await web.locator('[data-web-offer="'+offer.versionId+'"]').click();};
    await load(m,commerce);await load(w,commerce);await chooseMini();await chooseWeb();
    for(const value of [offer.versionId,'41.25','77','83','45','MINIAPP','WEB']) {
      await text(mini,'#oc-screen',value);await text(web,'#content',value);
    }
    for(const channel of ['MINIAPP','WEB'])assert.ok(offer.channels.includes(channel));
    const token=B.offerVersionToken(offer);
    assert.equal((await mini.evaluate(()=>OnecallBusiness.visibleOffers(__onecallMini.businessSnapshot(),{channel:'MINIAPP',audience:'PERSONAL'}).value)).find(item=>item.versionId===offer.versionId).versionToken,token);
    assert.equal((await web.evaluate(()=>OnecallBusiness.visibleOffers(__onecallWeb.businessSnapshot(),{channel:'WEB',audience:'PERSONAL'}).value)).find(item=>item.versionId===offer.versionId).versionToken,token);

    // The pending checkout holds the old token, price and rights through imports.
    for(const mode of ['changed','off-sale','channel-removed']) {
      await load(m,commerce);await load(w,commerce);await chooseMini();await mini.locator('[data-go="purchaseConfirm"]').click();await chooseWeb();
      const changed=copy(commerce),selected=changed.catalog.find(item=>item.versionId===offer.versionId);
      if(mode==='changed'){selected.priceCents=4250;selected.rights.aiMinutes=78;selected.revision++;}
      if(mode==='off-sale')selected.saleStatus='OFFSALE';
      if(mode==='channel-removed')selected.channels=['WEB'];
      await load(m,changed);await text(mini,'#oc-screen','41.25');await mini.locator('[data-order-confirm]').check();await mini.locator('[data-purchase-submit]').click();
      assert.deepEqual((await snapshot(m)).orders,commerce.orders);await text(mini,'#oc-screen',mode==='changed'?'STALE_VERSION':'NOT_SELLABLE');
      if(mode==='channel-removed')selected.channels=['MINIAPP'];
      await load(w,changed);await text(web,'#content','41.25');await web.locator('#checkout-agree').check();await web.locator('[data-action="checkout-pay"]').click();
      assert.deepEqual((await snapshot(w)).orders,commerce.orders);await text(web,'#content',mode==='changed'?'STALE_VERSION':'NOT_SELLABLE');
      if(mode!=='changed') {
        await mgo('plans');await wgo('plans');
        assert.equal(await web.locator('[data-web-offer="'+offer.versionId+'"]').count(),0);
        assert.equal((await mini.evaluate(()=>OnecallBusiness.visibleOffers(__onecallMini.businessSnapshot(),{channel:'MINIAPP',audience:'PERSONAL'}).value)).some(item=>item.versionId===offer.versionId),false);
      }
    }

    // Actual admin UI creates and consumes an unregistered, unverified IVR subject.
    const sessionId=await demo('new');await act('demo-confirm');await act('demo-use');await act('demo-guide');
    const captured=await snapshot(a),session=captured.sessions.find(item=>item.id===sessionId),subjectId=session.subjectId;
    const subject=captured.subjects.find(item=>item.id===subjectId),grant=captured.trialGrants.find(item=>item.subjectId===subjectId),record=captured.evidence.find(item=>item.sessionId===sessionId);
    assert.equal(subject.identity,'UNVERIFIED');assert.equal(subject.accountId,null);assert.equal(grant.grantedSeconds,120);assert.equal(grant.usedSeconds,60);assert.equal(grant.ruleVersion,2);assert.equal(record.identityAtCapture,'UNVERIFIED');assert.equal(session.guidanceStatus,'GUIDED');assert.deepEqual(captured.orders,historical);
    await admin.keyboard.press('Escape');
    await load(m,captured);const selected=await mini.evaluate(()=>__onecallMini.setReviewScenario('returning'));assert.equal(selected.subjectId,subjectId);
    assert.ok(!(await mini.locator('#oc-screen').innerText()).includes(record.id));
    await mgo('claimVerify');await mini.locator('[data-action="claim-submit"]').click();
    const claimed=await snapshot(m);assert.equal(claimed.subjects.find(item=>item.id===subjectId).accountId,'ACCOUNT-DEMO-001');assert.equal(claimed.subjects.find(item=>item.id===subjectId).identity,'UNVERIFIED');assert.deepEqual(facts(claimed),facts(captured));
    await text(mini,'#oc-screen','60 / 120');await text(mini,'#oc-screen',record.id);
    assert.equal(await mini.locator('#oc-screen audio,#oc-screen [download]').count(),0);
    await shot(miniPage,'prototype','returning-393');
    await mgo('claimVerify');await mini.locator('[data-action="claim-submit"]').click();assert.deepEqual(await snapshot(m),claimed);

    await load(w,claimed);await wgo('subject-claim');await web.locator('[data-action="claim-subject"]').click();
    let linked=await snapshot(w);assert.deepEqual(facts(linked),facts(captured));assert.equal(linked.trialGrants.filter(item=>item.subjectId===subjectId).length,1);
    await text(web,'#content',subjectId);await text(web,'#content','未实名');await text(web,'#content','01:00');await shot(web,'web-prototype','returning-1280');
    // An explicit later identity step cannot change identityAtCapture.
    await web.locator('[data-action="verify-identity"]').click();assert.equal((await snapshot(w)).evidence.find(item=>item.id===record.id).identityAtCapture,'UNVERIFIED');
    await chooseWeb();await web.locator('#checkout-agree').check();await web.locator('[data-action="checkout-pay"]').click();
    let order=(await snapshot(w)).orders.at(-1);const frozen=copy(order.snapshot);assert.equal(order.channel,'WEB');assert.equal(order.payment,'UNPAID');assert.equal(order.snapshot.versionId,offer.versionId);
    await web.locator('[data-action="web-payment"][data-result="UNKNOWN"]').click();let pendingQuery=await snapshot(w);assert.equal(pendingQuery.orders.at(-1).payment,'UNKNOWN');
    assert.equal(await web.locator('[data-action="checkout-pay"]').count(),0);
    await web.locator('[data-action="web-payment"][data-result="PAID"]').click();
    let paid=await snapshot(w);assert.equal(paid.orders.length,pendingQuery.orders.length);assert.equal(paid.orders.at(-1).entitlement,'PENDING');assert.deepEqual(paid.entitlements,captured.entitlements);
    await shot(web,'web-prototype','payment-pending-1280');await load(a,paid);await ago('/orders/'+order.id);await text(admin,'#content','支付成功');await text(admin,'#content','待发放');await shot(admin,'operations-prototype','payment-pending-1440');
    const failed=copy(paid);failed.orders.find(item=>item.id===order.id).entitlement='FAILED';
    await load(w,failed);await text(web,'#content','发放失败');await load(m,failed);await mini.evaluate(()=>__onecallMini.setReviewScenario('payment-failed'));await text(mini,'#oc-screen','权益发放失败');
    await load(w,paid);await web.locator('[data-action="web-issue"]').click();
    let issued=await snapshot(w);assert.equal(issued.entitlements.length,captured.entitlements.length+1);assert.deepEqual(issued.orders.at(-1).snapshot,frozen);assert.deepEqual(issued.evidence,captured.evidence);assert.deepEqual(issued.orders.slice(0,historical.length),historical);
    await load(m,issued);await load(a,issued);await mgo('plans');await chooseMini();await mini.locator('[data-go="purchaseConfirm"]').click();await mini.locator('[data-order-confirm]').check();await mini.locator('[data-purchase-submit]').click();
    let miniOrder=(await snapshot(m)).orders.at(-1);assert.equal(miniOrder.channel,'MINIAPP');assert.equal(miniOrder.subjectId,subjectId);assert.equal(miniOrder.payment,'UNPAID');
    await mini.locator('[data-action="simulate-payment"][data-result="PAID"]').click();await text(mini,'#oc-screen','权益待发放');await shot(miniPage,'prototype','payment-pending-393');await mini.locator('[data-action="issue-entitlements"]').click();
    const final=await snapshot(m);assert.equal(final.entitlements.length,captured.entitlements.length+2);assert.ok(final.orders.every(item=>['MINIAPP','WEB'].includes(item.channel)));assert.deepEqual(final.evidence,captured.evidence);assert.deepEqual(final.trialGrants,captured.trialGrants);
    for(const s of surfaces)await load(s,final);
    // Every original event can be replayed without changing any current fact.
    const functions={ACTIVATION:['activateService','eventId'],CLAIM:['claimSubject','requestId'],IVR_CONFIRM:['confirmIvrSession','eventId'],EVIDENCE:['consumeTrial','eventId'],ORDER:['createOrder','idempotencyKey'],PAYMENT:['applyPaymentResult','eventId'],ENTITLEMENT:['issueEntitlements','eventId']};
    const replayed=copy(final);
    for(const [domain,events] of Object.entries(final.processedEvents))for(const event of Object.values(events)) {
      const [name,key]=functions[domain],input=JSON.parse(event.inputFingerprint.slice('INPUT-V1:'.length));
      assert.equal(B[name](replayed,{...input,[key]:event.eventId},new Date()).ok,true,domain);assert.deepEqual(replayed,final);
    }

    // Every top-level forbidden category is separately and atomically rejected.
    for(const s of surfaces)for(const key of forbidden) {
      const before=await snapshot(s),r=await s.frame.evaluate(({h,data})=>window[h].loadReviewSnapshot(data),{h:s.hook,data:{...copy(before),[key]:{}}});
      assert.equal(r.ok,false,s.hook+':'+key);assert.deepEqual(await snapshot(s),before);
    }
    const tampers=[
      state=>{state.sessions.find(item=>item.id===sessionId).confirmedSubjectId='SUBJ-REPEAT-001';},
      state=>{const s=state.sessions.find(item=>item.id===sessionId);s.subjectId=null;s.confirmedSubjectId=null;},
      state=>{state.subjects=state.subjects.filter(item=>item.id!==subjectId);},
      state=>{state.subjects.find(item=>item.id===subjectId).id='SUBJ-REPLACED';},
      state=>{state.subjects.push(copy(state.subjects.find(item=>item.id===subjectId)));},
    ];
    for(const tamper of tampers) {
      const candidate=copy(final);tamper(candidate);
      for(const eventId of ['OPS-REC-'+sessionId,'NEW-TAMPER-EVENT']) {
        const state=copy(candidate),before=copy(state);assert.equal(B.consumeTrial(state,{sessionId,eventId,seconds:60},new Date()).ok,false);assert.deepEqual(state,before);
      }
      for(const s of surfaces) {const r=await s.frame.evaluate(({h,data})=>window[h].loadReviewSnapshot(data),{h:s.hook,data:candidate});assert.equal(r.ok,false);assert.deepEqual(await snapshot(s),final);}
    }

    // Dynamic landline session uses CALLER_CHALLENGE on both client surfaces.
    await demo('fixed');await act('demo-confirm');await act('demo-use');const fixed=await snapshot(a);await admin.keyboard.press('Escape');
    for(const s of [m,w]) {
      await load(s,fixed);const r=await s.frame.evaluate(h=>window[h].setReviewScenario('fixed'),s.hook);assert.equal(r.ok,true);
      if(s===m){await mgo('claimVerify');await text(mini,'#oc-screen','CALLER_CHALLENGE');await mini.locator('[data-action="claim-submit"]').click();}
      else {await wgo('subject-claim');await text(web,'#content','主叫挑战');await web.locator('[data-action="claim-subject"]').click();}
      const result=await snapshot(s);assert.equal(result.claims.at(-1).method,'CALLER_CHALLENGE');assert.deepEqual(facts(result),facts(fixed));
    }
    for(const scenario of ['conflict','recycled'])for(const s of [m,w]) {
      await load(s,final);await s.frame.evaluate(({h,scenario})=>window[h].setReviewScenario(scenario),{h:s.hook,scenario});
      if(s===m){await mgo('claimVerify');await mini.locator('[data-action="claim-submit"]').click();await text(mini,'#oc-screen',B.support.contactLabel);assert.ok(!/EVID-|TRIAL-/.test(await mini.locator('#oc-screen').innerText()));await shot(miniPage,'prototype',scenario+'-393');}
      else {await wgo('subject-claim');await web.locator('[data-action="claim-subject"]').click();await text(web,'#content',B.support.contactLabel);assert.ok(!/EVID-|TRIAL-/.test(await web.locator('#content').innerText()));await shot(web,'web-prototype',scenario+'-1280');}
      assert.deepEqual(await snapshot(s),final);
    }
    // Legal multi-account business snapshots: helpers create every transaction and entitlement.
    const mixed=B.createDemoState(new Date()),now=new Date();
    for(const [id,accountId,mask] of [['ACCOUNT-SUBJECT-A','ACCOUNT-DEMO-001','138 **** 1001'],['ACCOUNT-SUBJECT-B','ACCOUNT-DEMO-001','138 **** 1002'],['ACCOUNT-SUBJECT-OTHER','ACCOUNT-OTHER','138 **** 9001']]){
      mixed.subjects.push({id,accountId,maskedNumber:mask,numberType:'MOBILE',source:'WEB',identity:'VERIFIED',activation:'INACTIVE',revision:1,claimRisk:'NONE'});
      assert.equal(B.activateService(mixed,{subjectId:id,channel:'WEB',verified:true,confirmed:true,consentVersion:B.labels.consentVersion,eventId:'SCOPE-ACT-'+id},now).ok,true);
    }
    const buy=(subjectId,versionId)=>{
      const subject=mixed.subjects.find(item=>item.id===subjectId),offer=mixed.catalog.find(item=>item.versionId===versionId),key='SCOPE-'+subjectId;
      const r=B.createOrder(mixed,{subjectId,accountId:subject.accountId,versionId,versionToken:B.offerVersionToken(offer),channel:'WEB',idempotencyKey:key,confirmed:true,consentVersion:B.labels.consentVersion},now);
      assert.equal(r.ok,true,JSON.stringify(r));const orderId=r.value.id;
      assert.equal(B.applyPaymentResult(mixed,{orderId,result:'PAID',eventId:key+'-PAY'},now).ok,true);
      assert.equal(B.issueEntitlements(mixed,{orderId,eventId:key+'-ISSUE'},now).ok,true);
    };
    buy('ACCOUNT-SUBJECT-OTHER','PERSONAL_PRO_ANNUAL-V1');
    const accountFixtures=[B.snapshot(mixed).value];
    buy('ACCOUNT-SUBJECT-A','PERSONAL_BASIC_MONTHLY-V1');accountFixtures.push(B.snapshot(mixed).value);
    buy('ACCOUNT-SUBJECT-B','PERSONAL_BASIC_QUARTERLY-V1');accountFixtures.push(B.snapshot(mixed).value);
    for(const [count,fixture] of accountFixtures.entries()){
      assert.equal(B.snapshot(fixture).ok,true);
      const own=fixture.orders.filter(item=>item.accountId==='ACCOUNT-DEMO-001');
      for(const s of [m,w])await load(s,fixture);
      await mgo('profile');const metrics=await mini.locator('.metric strong').allTextContents();assert.equal(metrics[1],String(count));assert.equal(metrics[2],String(count));
      await mgo('orders');const miniOrders=await mini.locator('#oc-screen').innerText();
      await wgo('orders');const webOrders=await web.locator('#content').innerText();
      for(const order of fixture.orders){assert.equal(miniOrders.includes(order.id),own.includes(order));assert.equal(webOrders.includes(order.id),own.includes(order));}
      await wgo('rights');await text(web,'#content',count+' 笔已到账');
      const rights=await web.locator('#content').innerText();assert.equal(rights.includes('个人进阶版（年付）'),false);assert.equal(rights.includes('300 分钟'),false);
      for(const order of own)assert.ok(rights.includes(order.snapshot.name));
      if(count===0){await text(mini,'#oc-screen','当前没有共享业务订单');await text(web,'#content','0 笔已到账');}
      if(count===2){await mgo('profile');await shot(miniPage,'prototype','account-scope-profile-393');await shot(web,'web-prototype','account-scope-rights-1280');}
    }
    await ago('/roadmap');await mgo('support');await wgo('invoice-apply');
    for(const [s,selector] of [[a,'#content'],[m,'#oc-screen'],[w,'#content']]) {
      await text(s.frame,selector,B.support.contactLabel);
      assert.equal(await s.frame.locator(selector+' input,'+selector+' textarea,'+selector+' a[href^="tel:"],'+selector+' a[href^="mailto:"]').count(),0);
      assert.ok(!/CS20|INV-|400-/.test(await s.frame.locator(selector).innerText()));
      const support=await s.frame.evaluate(()=>OnecallBusiness.support);assert.deepEqual(support,B.support);
    }
    await shot(admin,'operations-prototype','support-1440');await shot(miniPage,'prototype','support-393');await shot(web,'web-prototype','support-1280');
    const hashes=[];for(const s of surfaces)hashes.push(await s.frame.locator('[data-onecall-business-sha256]').getAttribute('data-onecall-business-sha256'));
    assert.ok(hashes[0]);assert.equal(new Set(hashes).size,1);assert.deepEqual(errors,[]);
    console.log('PASS: admin publication -> both clients; frozen stale/off-sale/channel checkout; dynamic IVR claim/recording/trial continuity; WEB + MINIAPP UNKNOWN/PAID/PENDING/FAILED/ISSUED; all event replays; fixed/conflict/recycled; forbidden imports and five reference tamper cases; account-owned orders/rights (empty, one, two subjects) exclude other accounts; support and generated hash equality; no browser errors.');
  } finally {await browser.close();}
})().catch(error=>{console.error(error.stack||error);process.exitCode=1;});
