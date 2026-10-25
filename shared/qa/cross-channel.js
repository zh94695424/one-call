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
const forbidden = ['employees','employeeLoginSession','employeeCredentials','employeeRoles','customerAuthSession','accounts','authIdentities','identityProfiles','organizations','memberships','uiSession','widgetState','code'];

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
    const load = async (s,data) => { const context=await s.frame.evaluate(h=>window[h].accountContext?.()||null,s.hook);const r=await s.frame.evaluate(({h,data})=>window[h].loadReviewSnapshot(data),{h:s.hook,data});assert.equal(r.ok,true,JSON.stringify(r));assert.deepEqual(await snapshot(s),data);assert.deepEqual(await s.frame.evaluate(h=>window[h].accountContext?.()||null,s.hook),context,'Business import must preserve authenticated identities and workspaces'); };
    const [a,m,w] = surfaces;
    const ago = route => admin.evaluate(route=>window.__onecallOps.go(route),route);
    const mgo = route => mini.evaluate(route=>window.__onecallMini.navigate(route),route);
    const wgo = route => web.evaluate(route=>window.__onecallWeb.navigate(route),route);
    const act = name => admin.locator('[data-action="'+name+'"]').last().click();
    const confirm = async () => {await admin.fill('#reason','跨端本地 fixture 评审');await act('confirm');};
    const demo = async name => {await admin.evaluate(()=>document.querySelector('.review').open=true);await act('demo');await admin.selectOption('#demo-case',name);await act('confirm');return (await snapshot(a)).sessions.at(-1).id;};
    const text = async (frame,selector,value) => {const actual=await frame.locator(selector).innerText();assert.ok(actual.includes(value),'Missing '+value+' in '+selector+'; rendered: '+actual);};
    const shot = async (page,dir,name) => {const p=path.join(root,dir,'qa/screenshots','alignment-cross-'+name+'.png');fs.mkdirSync(path.dirname(p),{recursive:true});await page.screenshot({path:p,fullPage:true});};

    await admin.fill('#login-name','admin.lin');await admin.fill('#login-code','482916');await admin.locator('#login-form button').click();
    await web.locator('[data-action="wechat-login"]').click();await web.locator('[data-workspace-choice="personal:ACCOUNT-DEMO-001"]').click();
    const miniAccount=await mini.evaluate(()=>__onecallMini.accountContext()),webAccount=await web.evaluate(()=>__onecallWeb.accountContext());
    assert.equal(miniAccount.account.id,webAccount.accountId);assert.equal(webAccount.accountId,'ACCOUNT-DEMO-001');assert.deepEqual(miniAccount.identities,webAccount.identities);
    const identityDirectory=copy(B.demoAccountDirectory);
    for(const key of ['accounts','authIdentities','identityProfiles','organizations','memberships'])for(const s of surfaces)assert.equal(Object.hasOwn(await snapshot(s),key),false);
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
    const functions={ACTIVATION:['activateService','eventId'],ACTIVATION_STATUS:['setActivationStatus','eventId'],CLAIM:['claimSubject','requestId'],IVR_CONFIRM:['confirmIvrSession','eventId'],EVIDENCE:['consumeTrial','eventId'],ORDER:['createOrder','idempotencyKey'],PAYMENT:['applyPaymentResult','eventId'],ENTITLEMENT:['issueEntitlements','eventId']};
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

    // Mini fixed-line setup commits a canonical subject that Operations and Web can import.
    await load(m,final);await mgo('landlineBind');await mini.locator('[data-landline-confirm]').check();await mini.locator('[data-landline-submit]').click();
    await mini.locator('[data-go="landlineReady"]').click();await text(mini,'#oc-screen','固话已验证并开通');await text(mini,'#oc-screen','CALLER_CHALLENGE');
    const landlineState=await snapshot(m),landlineSubject=landlineState.subjects.find(item=>item.id==='MINI-LANDLINE-01088886688');
    assert.ok(landlineSubject);assert.equal(landlineSubject.accountId,'ACCOUNT-DEMO-001');assert.equal(landlineSubject.numberType,'LANDLINE');assert.equal(landlineSubject.identity,'VERIFIED');assert.equal(landlineSubject.activation,'ACTIVE');
    assert.equal(landlineState.activations.filter(item=>item.subjectId===landlineSubject.id&&item.channel==='MINIAPP').length,1);
    assert.equal(landlineState.trialGrants.filter(item=>item.subjectId===landlineSubject.id).length,1);assert.equal(landlineState.trialGrants.find(item=>item.subjectId===landlineSubject.id).grantedSeconds,final.rule.seconds);
    await shot(miniPage,'prototype','landline-activation-393');
    await mgo('landlineVerify');await mini.locator('[data-go="landlineReady"]').click();assert.deepEqual(await snapshot(m),landlineState);
    await load(a,landlineState);await ago('/users/'+landlineSubject.id);await text(admin,'#content',landlineSubject.id);await text(admin,'#content','ACCOUNT-DEMO-001');await text(admin,'#content','独立固话');await text(admin,'#content','已实名');await text(admin,'#content','已开通');
    await shot(admin,'operations-prototype','landline-activation-1440');
    await load(w,landlineState);assert.deepEqual(await snapshot(w),landlineState);
    await wgo('numbers');await web.locator('[data-number-id="fixed-6688"]').click();await text(web,'#content','010 **** 6688');
    await web.locator('[data-action="number-stop-confirm"]').click();await web.locator('[data-action="number-stop"]').click();
    const webStoppedLandline=await snapshot(w),webStoppedSubject=webStoppedLandline.subjects.find(item=>item.id===landlineSubject.id),webStopTransition=webStoppedLandline.activationTransitions.at(-1);
    assert.equal(webStoppedSubject.activation,'SUSPENDED');assert.equal(webStopTransition.subjectId,landlineSubject.id);assert.equal(webStopTransition.channel,'WEB');assert.equal(webStopTransition.fromRevision,landlineSubject.revision);assert.equal(webStopTransition.toRevision,webStoppedSubject.revision);
    assert.equal(webStoppedLandline.subjects.filter(item=>item.maskedNumber==='010 **** 6688'&&item.accountId==='ACCOUNT-DEMO-001').length,1);assert.equal(webStoppedLandline.trialGrants.filter(item=>item.subjectId===landlineSubject.id).length,1);
    await shot(web,'web-prototype','landline-lifecycle-stopped-1280');
    await web.locator('[data-action="number-reactivate"]').click();
    const webRestoredLandline=await snapshot(w),webRestoredSubject=webRestoredLandline.subjects.find(item=>item.id===landlineSubject.id),webRestoreTransition=webRestoredLandline.activationTransitions.at(-1);
    assert.equal(webRestoredSubject.activation,'ACTIVE');assert.equal(webRestoreTransition.subjectId,landlineSubject.id);assert.equal(webRestoreTransition.fromRevision,webStoppedSubject.revision);assert.equal(webRestoreTransition.toRevision,webRestoredSubject.revision);assert.equal(webRestoredLandline.activations.filter(item=>item.subjectId===landlineSubject.id).length,1);assert.equal(webRestoredLandline.trialGrants.filter(item=>item.subjectId===landlineSubject.id).length,1);
    const importedRestoreInput={subjectId:landlineSubject.id,channel:'WEB',status:'ACTIVE',eventId:webRestoreTransition.eventId},webRestoredFrozen=copy(webRestoredLandline);
    assert.equal((await web.evaluate(input=>window.__onecallWeb.applyActivationStatus(input),importedRestoreInput)).code,'DUPLICATE');assert.deepEqual(await snapshot(w),webRestoredFrozen);
    assert.equal((await web.evaluate(input=>window.__onecallWeb.applyActivationStatus({...input,status:'SUSPENDED'}),importedRestoreInput)).ok,false);assert.deepEqual(await snapshot(w),webRestoredFrozen);
    await shot(web,'web-prototype','landline-lifecycle-restored-1280');
    for(const s of surfaces)await load(s,final);

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

    // Call-task/IVR diagnostics: frozen 30-second expiry, atomic claim, no-task DTMF,
    // task-versus-no-task vocabulary and account/workspace isolation.
    const callTaskSnapshot = s => s.frame.evaluate(h=>window[h].callTaskSnapshot(),s.hook);
    const loadCallTask = async (s,data) => {const r=await s.frame.evaluate(({h,data})=>window[h].loadCallTaskSnapshot(data),{h:s.hook,data});assert.equal(r.ok,true,JSON.stringify(r));assert.deepEqual(await callTaskSnapshot(s),data);};
    await mgo('callerSelect');
    await mini.locator('[data-action="select-caller"][data-caller-id="phone-4471"]').click();
    await mini.locator('.primary-action[data-go="call"]').click();
    await mini.locator('[data-go="dialHandoff"]').click();
    assert.equal(await mini.evaluate(()=>window.__onecallMini.currentPage()),'dialHandoff');
    const miniCreatedState=await callTaskSnapshot(m);
    assert.equal(miniCreatedState.ttlSeconds,30);
    const miniCreatedTask=miniCreatedState.tasks.find(task=>task.normalizedCaller==='18600004471'&&['PENDING','CLAIMED'].includes(task.status));
    assert.ok(miniCreatedTask,'Mini UI must create one pending task');
    assert.equal(Date.parse(miniCreatedTask.expiresAt)-Date.parse(miniCreatedTask.createdAt),30000);
    await text(mini,'#oc-screen',miniCreatedTask.id);
    await text(mini,'#oc-screen','待呼入');
    assert.equal(await mini.locator('[data-task-ttl]').getAttribute('data-task-ttl'),'30');

    const claimBase=Date.now();
    const claimedTaskState=copy(miniCreatedState);
    const claimedAt=claimBase+1000;
    const resolution=B.resolveIvrInbound(claimedTaskState,{callId:'CALL-QA-TASK-01',normalizedCaller:'18600004471',callerTrusted:true,callerState:'ACTIVE',availableSeconds:300,directAllowed:true},new Date(claimedAt));
    assert.equal(resolution.ok,true,JSON.stringify(resolution));
    assert.equal(resolution.value.action,'DIAL_TASK_CALLEE');
    assert.equal(resolution.value.calleeConfirmation,'NONE');
    assert.equal(claimedTaskState.tasks.find(task=>task.id===miniCreatedTask.id).status,'CLAIMED');
    assert.equal(claimedTaskState.tasks.find(task=>task.id===miniCreatedTask.id).claimedCallId,'CALL-QA-TASK-01');
    const competing=B.resolveIvrInbound(copy(claimedTaskState),{callId:'CALL-QA-TASK-02',normalizedCaller:'18600004471',callerTrusted:true,callerState:'ACTIVE',availableSeconds:300,directAllowed:true},new Date(claimedAt+1000));
    assert.equal(competing.ok,false);assert.equal(competing.code,'REJECT_CALL_IN_PROGRESS');
    const replay=B.resolveIvrInbound(copy(claimedTaskState),{callId:'CALL-QA-TASK-01',normalizedCaller:'18600004471',callerTrusted:true,callerState:'ACTIVE',availableSeconds:300,directAllowed:true},new Date(claimedAt+1000));
    assert.equal(replay.code,'DUPLICATE');
    assert.equal(replay.value.action,'DIAL_TASK_CALLEE');
    assert.equal(claimedTaskState.tasks.filter(task=>task.normalizedCaller==='18600004471'&&['PENDING','CLAIMED'].includes(task.status)).length,1);
    assert.equal(claimedTaskState.calls.filter(call=>call.id==='CALL-QA-TASK-01').length,1);
    const callFacts=[['OUTBOUND_STARTED',2000],['RINGING',3000],['ANSWERED',4000],['BRIDGED',5000],['RECORDING_STARTED',6000],['CALL_ENDED',10000,{recordingSeconds:45}],['RECORDING_RECEIVED',11000],['EVIDENCE_PROCESSING',12000],['EVIDENCE_READY',13000]];
    for(const [type,offset,extra] of callFacts){const input=Object.assign({callId:'CALL-QA-TASK-01',eventId:'CALL-QA-TASK-01-'+type,type,at:new Date(claimBase+offset).toISOString()},extra||{});const recorded=B.recordCallFact(claimedTaskState,input,new Date(claimBase+offset));assert.equal(recorded.ok,true,type+' '+JSON.stringify(recorded));}
    const claimedCall=claimedTaskState.calls.find(call=>call.id==='CALL-QA-TASK-01');
    assert.equal(claimedCall.progress,'EVIDENCE_READY');assert.equal(claimedCall.uploadStatus,'RECEIVED');assert.equal(claimedCall.evidenceStatus,'AVAILABLE');
    assert.equal(claimedTaskState.tasks.find(task=>task.id===miniCreatedTask.id).status,'COMPLETED');
    assert.equal(claimedTaskState.tasks.filter(task=>task.normalizedCaller==='18600004471'&&['PENDING','CLAIMED'].includes(task.status)).length,0);

    const businessBeforeTask={a:await snapshot(a),m:await snapshot(m),w:await snapshot(w)};
    const webContextBeforeTask=await web.evaluate(()=>window.__onecallWeb.accountContext());
    for(const s of surfaces)await loadCallTask(s,claimedTaskState);
    assert.deepEqual(await callTaskSnapshot(a),claimedTaskState);assert.deepEqual(await callTaskSnapshot(m),claimedTaskState);assert.deepEqual(await callTaskSnapshot(w),claimedTaskState);
    assert.deepEqual(await snapshot(a),businessBeforeTask.a);assert.deepEqual(await snapshot(m),businessBeforeTask.m);assert.deepEqual(await snapshot(w),businessBeforeTask.w);
    assert.deepEqual(await web.evaluate(()=>window.__onecallWeb.accountContext()),webContextBeforeTask);
    await mgo('callStatus');await text(mini,'#oc-screen',miniCreatedTask.id);await text(mini,'#oc-screen','CALL-QA-TASK-01');await text(mini,'#oc-screen','证据可用');await text(mini,'#oc-screen','任务自动外呼');
    // The imported evidence-ready call was never launched from this client, so
    // the heading must not claim the system dialer was opened while the task
    // fact row truthfully reports it was not.
    const unlaunchedEvidenceText=await mini.locator('#oc-screen').innerText();
    assert.ok(unlaunchedEvidenceText.includes('证据可用'),'Unlaunched evidence-ready Mini status must include 证据可用');
    assert.ok(!unlaunchedEvidenceText.includes('系统拨号盘已打开'),'Unlaunched evidence-ready Mini heading must not claim the dialer was opened');
    const dialerFact=mini.locator('#oc-screen .meta-item').filter({hasText:'系统拨号盘'});
    assert.equal(await dialerFact.count(),1,'Mini status must render one system dialer task fact');
    assert.equal((await dialerFact.locator('strong').innerText()).trim(),'未打开','Unlaunched evidence-ready task fact must remain 未打开');
    assert.ok(!(await mini.locator('#oc-screen').innerText()).includes('CT-DEMO-0201'),'Mini must not render a foreign-account task');
    await wgo('calls');await text(web,'#content',miniCreatedTask.id);await text(web,'#content','CALL-QA-TASK-01');await text(web,'#content','证据可用');await text(web,'#content','任务自动外呼');
    assert.ok(!(await web.locator('#content').innerText()).includes('CT-DEMO-0201'),'Web must not render a foreign-account task');
    await ago('/ivr-sessions');await text(admin,'#content',miniCreatedTask.id);await text(admin,'#content','CALL-QA-TASK-01');await text(admin,'#content','任务自动外呼');
    assert.ok((await admin.locator('#content').innerText()).includes('CT-DEMO-0201'),'Operations must see the foreign-account task');
    // Let the application-owned toast lifecycle finish, including the opacity
    // transition, so an earlier authorization toast cannot cover the task-parity
    // evidence screenshot.
    await web.waitForFunction(()=>{const toast=document.getElementById('toast');return !toast||getComputedStyle(toast).opacity==='0';});
    await shot(miniPage,'prototype','call-task-393');await shot(web,'web-prototype','call-task-1280');await shot(admin,'operations-prototype','ivr-task-1440');

    const expiryState=B.createCallTaskDemoState(new Date(claimBase));
    const expiryTask=B.createCallTask(expiryState,{callerBindingId:'CB-PERSONAL-4471',maskedCallee:'135 **** 1111',businessType:'EVIDENCE_CALL',idempotencyKey:'QA-EXPIRY-TASK',replacePending:false},new Date(claimBase));
    assert.equal(expiryTask.ok,true,JSON.stringify(expiryTask));
    const expired=B.resolveIvrInbound(expiryState,{callId:'CALL-QA-EXPIRED',normalizedCaller:'18600004471',callerTrusted:true,callerState:'ACTIVE',availableSeconds:300,directAllowed:true},new Date(claimBase+30000));
    assert.equal(expired.ok,true);assert.equal(expired.value.action,'ENTER_CALLEE');assert.equal(expired.value.maskedCallee,null);
    assert.equal(expiryState.tasks.find(task=>task.id===expiryTask.value.id).status,'EXPIRED');
    assert.equal(expiryState.calls.find(call=>call.id==='CALL-QA-EXPIRED').taskId,null);

    const directState=B.createCallTaskDemoState(new Date(claimBase));
    const directTaskCount=directState.tasks.length;
    const direct=B.resolveIvrInbound(directState,{callId:'CALL-QA-DIRECT',normalizedCaller:'01088886688',callerTrusted:true,callerState:'ACTIVE',availableSeconds:300,directAllowed:true},new Date(claimBase));
    assert.equal(direct.ok,true);assert.equal(direct.value.action,'ENTER_CALLEE');assert.equal(direct.value.taskPath,false);
    const directConfirmed=B.confirmDirectCallee(directState,{callId:'CALL-QA-DIRECT',normalizedCaller:'01088886688',maskedCallee:'139 **** 3816',eventId:'CALL-QA-DIRECT-DTMF',taskId:null},new Date(claimBase+1000));
    assert.equal(directConfirmed.ok,true);assert.equal(directConfirmed.value.taskPath,false);assert.equal(directConfirmed.value.calleeConfirmation,'DTMF_CONFIRMED');
    assert.equal(directState.calls.find(call=>call.id==='CALL-QA-DIRECT').taskId,null);assert.equal(directState.tasks.length,directTaskCount);

    for(const s of surfaces){const before=await callTaskSnapshot(s);for(const key of ['employees','customerAuthSession','code']){const r=await s.frame.evaluate(({h,data})=>window[h].loadCallTaskSnapshot(data),{h:s.hook,data:Object.assign(copy(before),{[key]:{}})});assert.equal(r.ok,false,s.hook+':'+key);assert.deepEqual(await callTaskSnapshot(s),before);}}
    const taskBeforeBusinessImport={a:await callTaskSnapshot(a),m:await callTaskSnapshot(m),w:await callTaskSnapshot(w)};
    for(const s of surfaces)await load(s,s===a?businessBeforeTask.a:s===m?businessBeforeTask.m:businessBeforeTask.w);
    assert.deepEqual(await callTaskSnapshot(a),taskBeforeBusinessImport.a);assert.deepEqual(await callTaskSnapshot(m),taskBeforeBusinessImport.m);assert.deepEqual(await callTaskSnapshot(w),taskBeforeBusinessImport.w);
    // Visible enterprise authorization revocations must survive actual cross-channel imports.
    for(const item of [
      {id:'auth-6688',number:'010 **** 6688',owner:'ACCOUNT-DEMO-001'},
      {id:'auth-9056',number:'021 **** 9056',owner:'ACCOUNT-DEMO-001'},
      {id:'auth-3360',number:'186 **** 3360',owner:'ACCOUNT-DEMO-003'},
      {id:'auth-7812',number:'139 **** 7812',owner:'ACCOUNT-DEMO-002',reconfirm:true},
    ]){
      await web.reload({waitUntil:'load'});
      await web.locator('[data-action="wechat-login"]').click();await web.locator('[data-workspace-choice="personal:ACCOUNT-DEMO-001"]').click();
      await web.locator('#workspace-select').selectOption('organization:ORG-HUAQI-001');
      await web.locator('#side-nav [data-route="agreements"]').click();
      await web.locator('[data-agreement-id="'+item.id+'"]').filter({hasText:'查看快照'}).click();
      if(item.reconfirm)await web.locator('[data-action="agreement-reconfirm"]').click();
      const before=await snapshot(w),matches=before.subjects.filter(subject=>subject.maskedNumber===item.number);
      assert.equal(matches.length,1);const original=matches[0];assert.equal(original.accountId,item.owner);assert.equal(original.activation,'ACTIVE');assert.equal(original.revision,2);
      await web.locator('[data-action="agreement-revoke-confirm"]').click();
      const recordId=await web.locator('[data-action="agreement-revoke"]').getAttribute('data-agreement-id');
      if(item.reconfirm)assert.notEqual(recordId,item.id);
      await web.locator('[data-action="agreement-revoke"]').click();
      const revoked=await snapshot(w),transition=revoked.activationTransitions.at(-1);
      assert.equal(revoked.activationTransitions.length,before.activationTransitions.length+1);
      assert.deepEqual(revoked.subjects,before.subjects.map(subject=>subject.id===original.id?{...subject,activation:'SUSPENDED',revision:3}:subject));
      assert.equal(transition.subjectId,original.id);assert.equal(transition.channel,'WEB');assert.equal(transition.from,'ACTIVE');assert.equal(transition.to,'SUSPENDED');assert.equal(transition.fromRevision,2);assert.equal(transition.toRevision,3);
      const owners=Object.values(revoked.processedEvents.ACTIVATION_STATUS).filter(event=>event.result.id===transition.id);
      assert.equal(owners.length,1);assert.deepEqual(owners[0].result,transition);
      for(const key of Object.keys(before).filter(key=>!['subjects','activationTransitions','processedEvents','sequence'].includes(key)))assert.deepEqual(revoked[key],before[key]);
      await text(web,'#content','已撤销');await text(web,'.timeline-item:has-text("用户撤销后续授权")',transition.id);
      for(const target of [a,m]){
        await load(target,revoked);
        assert.deepEqual((await snapshot(target)).subjects.find(subject=>subject.id===original.id),revoked.subjects.find(subject=>subject.id===original.id));
      }
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
    assert.deepEqual(B.demoAccountDirectory,identityDirectory);
    for(const s of [m,w])assert.deepEqual(await s.frame.evaluate(()=>OnecallBusiness.demoAccountDirectory),identityDirectory);
    assert.ok(hashes[0]);assert.equal(new Set(hashes).size,1);assert.deepEqual(errors,[]);
    console.log('PASS: admin publication -> both clients; frozen stale/off-sale/channel checkout; dynamic IVR claim/recording/trial continuity; WEB + MINIAPP UNKNOWN/PAID/PENDING/FAILED/ISSUED; all event replays; fixed/conflict/recycled; forbidden imports and five reference tamper cases; account-owned orders/rights (empty, one, two subjects) exclude other accounts; Mini task creation with frozen 30-second expiry, atomic shared claim, task auto-dial and no-task DTMF, recording/upload/evidence separation, parity across all three surfaces, foreign-account task isolation and bidirectional call-task/business import independence; four enterprise admin authorization revocations import into Operations and Mini; support and generated hash equality; no browser errors.');
  } finally {await browser.close();}
})().catch(error=>{console.error(error.stack||error);process.exitCode=1;});
