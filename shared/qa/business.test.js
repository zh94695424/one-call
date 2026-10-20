'use strict';

const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const business = require('../onecall-business');

const NOW = '2026-10-20T00:00:00.000Z';
const LATER = '2026-10-20T00:01:00.000Z';
const root = path.resolve(__dirname, '..', '..');
const copy = value => JSON.parse(JSON.stringify(value));
const raw = state => copy(state);

function reorderObject(value) {
  if (Array.isArray(value)) return value.map(reorderObject);
  if (!value || typeof value !== 'object') return value;
  return Object.keys(value).reverse().reduce((result, key) => {
    result[key] = reorderObject(value[key]);
    return result;
  }, {});
}

function prepareBuyer(state, subjectId = 'SUBJ-REPEAT-001', accountId = 'ACCOUNT-BUYER-001') {
  const subject = state.subjects.find(item => item.id === subjectId);
  subject.accountId = accountId;
  subject.identity = 'VERIFIED';
  subject.activation = 'ACTIVE';
  return subject;
}

function orderInput(state, subject, idempotencyKey, overrides = {}) {
  const selected = state.catalog.find(item => item.sku === 'PERSONAL_BASIC_MONTHLY');
  return Object.assign({
    subjectId: subject.id,
    accountId: subject.accountId,
    channel: 'WEB',
    versionId: selected.versionId,
    versionToken: business.offerVersionToken(selected),
    confirmed: true,
    consentVersion: 'ORDER-CONSENT-V1',
    idempotencyKey,
  }, overrides);
}

function assertRejectedWithoutMutation(state, action, code) {
  const before = raw(state);
  const result = action();
  assert.equal(result.ok, false);
  assert.equal(result.code, code);
  assert.deepEqual(state, before);
  return result;
}

function startAndConfirm(state, scenario, suffix) {
  const started = business.startIvrSession(state, { scenario }, NOW);
  assert.equal(started.ok, true);
  const confirmed = business.confirmIvrSession(state, { sessionId: started.value.id, key: '1', eventId: `EVT-CONFIRM-${suffix}` }, NOW);
  assert.equal(confirmed.ok, true);
  return confirmed.value;
}

test('canonical catalog, labels and offline support are internally valid', () => {
  const state = business.createDemoState(NOW);
  assert.equal(business.schemaVersion, 1);
  assert.equal(business.snapshot(state).ok, true);
  assert.equal(business.demoCatalog.length, 19);
  for (const item of business.demoCatalog) {
    assert.equal(business.validateCatalogVersion(item).ok, true, item.sku);
    assert.deepEqual(item.channels, ['MINIAPP', 'WEB']);
    assert.equal(item.channels.includes('IVR'), false);
    assert.equal(item.demo, true);
    assert.equal(item.policyStatus, 'DEMO_NOT_APPROVED');
  }
  const offers = business.visibleOffers(state, { channel: 'WEB', audience: 'PERSONAL' });
  assert.equal(offers.ok, true);
  assert.ok(offers.value.length > 0);
  assert.deepEqual(offers.value.map(item => item.sortOrder), [...offers.value].map(item => item.sortOrder).sort((a, b) => a - b));
  assert.deepEqual(business.support, {
    mode: 'OFFLINE', configured: false, contactLabel: '人工客服联系方式待配置', contactValue: null, hours: null,
    instructions: business.support.instructions,
  });
  assert.equal(business.labels.payment.PAID, '支付成功');
  assert.equal(business.labels.entitlement.ISSUED, '已到账');
});

test('IVR confirmation grants once and rejected confirmation attempts are atomic', () => {
  const state = business.createDemoState(NOW);
  const missing = business.startIvrSession(state, { scenario: 'MISSING_CALLER' }, NOW).value;
  assertRejectedWithoutMutation(state, () => business.confirmIvrSession(state, { sessionId: missing.id, key: '1', eventId: 'EVT-MISSING' }, NOW), 'VERIFICATION_REQUIRED');
  const wrong = business.startIvrSession(state, { scenario: 'WRONG_KEY' }, NOW).value;
  assertRejectedWithoutMutation(state, () => business.confirmIvrSession(state, { sessionId: wrong.id, key: '2', eventId: 'EVT-WRONG' }, NOW), 'CONSENT_REQUIRED');

  const result = startAndConfirm(state, 'NEW_MOBILE', 'NEW');
  assert.equal(result.session.subjectId, result.session.confirmedSubjectId);
  const target = state.subjects.find(item => item.id === result.session.subjectId);
  assert.equal(target.identity, 'UNVERIFIED');
  assert.equal(target.accountId, null);
  assert.equal(target.activation, 'ACTIVE');
  assert.equal(result.grant.grantedSeconds, 300);
  assert.equal(state.trialGrants.filter(item => item.subjectId === target.id).length, 1);

  const beforeReplay = raw(state);
  const replay = business.confirmIvrSession(state, { sessionId: result.session.id, key: '1', eventId: 'EVT-CONFIRM-NEW' }, NOW);
  assert.equal(replay.ok, true);
  assert.equal(replay.code, 'DUPLICATE');
  assert.deepEqual(state, beforeReplay);

  const repeat = startAndConfirm(state, 'NEW_MOBILE', 'REPEAT');
  assert.equal(repeat.session.subjectId, target.id);
  assert.equal(repeat.grant.id, result.grant.id);
  assert.equal(state.trialGrants.filter(item => item.subjectId === target.id).length, 1);
});

test('trial evidence and claim preserve frozen IVR facts and grant snapshot', () => {
  const state = business.createDemoState(NOW);
  const confirmed = startAndConfirm(state, 'NEW_MOBILE', 'CAPTURE');
  const consumed = business.consumeTrial(state, { sessionId: confirmed.session.id, eventId: 'EVT-CAPTURE-60', seconds: 60 }, LATER);
  assert.equal(consumed.ok, true);
  assert.equal(consumed.value.evidence.identityAtCapture, 'UNVERIFIED');
  assert.equal(consumed.value.evidence.subjectId, confirmed.session.confirmedSubjectId);
  const beforeReplay = raw(state);
  const replay = business.consumeTrial(state, { sessionId: confirmed.session.id, eventId: 'EVT-CAPTURE-60', seconds: 60 }, LATER);
  assert.equal(replay.code, 'DUPLICATE');
  assert.deepEqual(state, beforeReplay);

  const frozenSessions = raw(state.sessions);
  const frozenEvidence = raw(state.evidence);
  const frozenGrant = raw(state.trialGrants.find(item => item.subjectId === confirmed.session.subjectId));
  const claim = business.claimSubject(state, {
    subjectId: confirmed.session.subjectId,
    accountId: 'ACCOUNT-DYNAMIC-77',
    verification: { verified: true, subjectId: confirmed.session.subjectId, method: 'SMS' },
    requestId: 'CLAIM-REQUEST-77',
  }, LATER);
  assert.equal(claim.ok, true);
  assert.deepEqual(state.sessions, frozenSessions);
  assert.deepEqual(state.evidence, frozenEvidence);
  assert.equal(state.subjects.find(item => item.id === confirmed.session.subjectId).identity, 'UNVERIFIED');
  assert.deepEqual(state.trialGrants.find(item => item.subjectId === confirmed.session.subjectId), frozenGrant);

  const claimReplayState = raw(state);
  const claimReplay = business.claimSubject(state, {
    subjectId: confirmed.session.subjectId,
    accountId: 'ACCOUNT-DYNAMIC-77',
    verification: { verified: true, subjectId: confirmed.session.subjectId, method: 'SMS' },
    requestId: 'CLAIM-REQUEST-OTHER',
  }, LATER);
  assert.equal(claimReplay.code, 'DUPLICATE');
  assert.deepEqual(state, claimReplayState);

  state.rule = { campaignId: 'FIRST-TRIAL', version: 2, enabled: true, seconds: 600, validDays: 90 };
  const oldGrant = raw(state.trialGrants.find(item => item.subjectId === confirmed.session.subjectId));
  const noReset = business.grantTrialOnce(state, confirmed.session.subjectId, LATER);
  assert.equal(noReset.code, 'DUPLICATE');
  assert.deepEqual(state.trialGrants.find(item => item.subjectId === confirmed.session.subjectId), oldGrant);
});

test('event namespaces preserve immutable first results and reject changed input atomically', () => {
  const state = business.createDemoState(NOW);
  const confirmed = startAndConfirm(state, 'NEW_MOBILE', 'SHARED');
  const consumed = business.consumeTrial(state, { sessionId: confirmed.session.id, eventId: 'SHARED-EVENT', seconds: 60 }, LATER);
  assert.equal(consumed.ok, true);
  const mismatch = assertRejectedWithoutMutation(state, () => business.consumeTrial(state, {
    sessionId: confirmed.session.id,
    eventId: 'SHARED-EVENT',
    seconds: 30,
  }, LATER), 'INVALID_TRANSITION');
  assert.equal(mismatch.ok, false);

  const claim = business.claimSubject(state, {
    subjectId: confirmed.session.subjectId,
    accountId: 'ACCOUNT-SHARED-001',
    verification: { verified: true, subjectId: confirmed.session.subjectId, method: 'SMS' },
    requestId: 'SHARED-EVENT',
  }, LATER);
  assert.equal(claim.ok, true);
  assert.equal(business.consumeTrial(state, { sessionId: confirmed.session.id, eventId: 'LATER-EVIDENCE', seconds: 40 }, LATER).ok, true);

  const beforeReplay = raw(state);
  const evidenceReplay = business.consumeTrial(state, { sessionId: confirmed.session.id, eventId: 'SHARED-EVENT', seconds: 60 }, LATER);
  assert.equal(evidenceReplay.code, 'DUPLICATE');
  assert.deepEqual(evidenceReplay.value, consumed.value);
  assert.equal(evidenceReplay.value.grant.usedSeconds, 60);
  assert.equal(state.trialGrants.find(item => item.subjectId === confirmed.session.subjectId).usedSeconds, 100);
  const confirmationReplay = business.confirmIvrSession(state, { sessionId: confirmed.session.id, key: '1', eventId: 'EVT-CONFIRM-SHARED' }, NOW);
  assert.equal(confirmationReplay.code, 'DUPLICATE');
  assert.deepEqual(confirmationReplay.value, confirmed);
  assert.equal(confirmationReplay.value.grant.usedSeconds, 0);
  assert.deepEqual(state, beforeReplay);
  assert.ok(Object.prototype.hasOwnProperty.call(state.processedEvents.EVIDENCE, '$SHARED-EVENT'));
  assert.ok(Object.prototype.hasOwnProperty.call(state.processedEvents.CLAIM, '$SHARED-EVENT'));
});

test('special event identifiers are safe object keys', () => {
  for (const eventId of ['__proto__', 'constructor', 'prototype']) {
    const state = business.createDemoState(NOW);
    const session = business.startIvrSession(state, { scenario: 'NEW_MOBILE' }, NOW).value;
    const result = business.confirmIvrSession(state, { sessionId: session.id, key: '1', eventId }, NOW);
    assert.equal(result.ok, true, eventId);
    assert.equal(Object.getPrototypeOf(state.processedEvents.IVR_CONFIRM), Object.prototype, eventId);
    assert.ok(Object.prototype.hasOwnProperty.call(state.processedEvents.IVR_CONFIRM, `$${eventId}`), eventId);
    assert.equal(business.snapshot(state).ok, true, eventId);
  }
});

test('conflict and recycled-number claims reject without exposing or mutating history', () => {
  for (const [subjectId, expected] of [['SUBJ-CONFLICT-001', 'CLAIM_CONFLICT'], ['SUBJ-RECYCLED-001', 'RECYCLED_NUMBER_REVIEW']]) {
    const state = business.createDemoState(NOW);
    assertRejectedWithoutMutation(state, () => business.claimSubject(state, {
      subjectId,
      accountId: 'ACCOUNT-BLOCKED',
      verification: { verified: true, subjectId, method: 'SMS' },
      requestId: `REQUEST-${subjectId}`,
    }, LATER), expected);
  }

  for (const [risk, expected] of [['CONFLICT', 'CLAIM_CONFLICT'], ['RECYCLED', 'RECYCLED_NUMBER_REVIEW']]) {
    const state = business.createDemoState(NOW);
    const confirmed = startAndConfirm(state, 'NEW_MOBILE', `RISK-${risk}`);
    const input = {
      subjectId: confirmed.session.subjectId,
      accountId: 'ACCOUNT-RISK-001',
      verification: { verified: true, subjectId: confirmed.session.subjectId, method: 'SMS' },
      requestId: `CLAIM-RISK-${risk}`,
    };
    assert.equal(business.claimSubject(state, input, LATER).ok, true);
    state.subjects.find(item => item.id === confirmed.session.subjectId).claimRisk = risk;
    assert.equal(business.snapshot(state).ok, true);
    assertRejectedWithoutMutation(state, () => business.claimSubject(state, input, LATER), expected);
    assertRejectedWithoutMutation(state, () => business.claimSubject(state, { ...input, requestId: `${input.requestId}-NEW` }, LATER), expected);
  }
});

test('canonical evidence requires exactly one durable EVIDENCE event owner', () => {
  const state = business.createDemoState(NOW);
  const confirmed = startAndConfirm(state, 'NEW_MOBILE', 'REVERSE-COVERAGE');
  const input = { sessionId: confirmed.session.id, eventId: 'EVIDENCE-REVERSE-COVERAGE', seconds: 60 };
  assert.equal(business.consumeTrial(state, input, LATER).ok, true);
  const canonical = business.snapshot(state).value;

  const candidates = [
    payload => { delete payload.processedEvents.EVIDENCE['$EVIDENCE-REVERSE-COVERAGE']; },
    payload => { payload.processedEvents.EVIDENCE['$EVIDENCE-REVERSE-COVERAGE'].result.evidence.id = 'EVID-REPLACED'; },
    payload => {
      payload.processedEvents.EVIDENCE['$EVIDENCE-DUPLICATE-OWNER'] = copy(payload.processedEvents.EVIDENCE['$EVIDENCE-REVERSE-COVERAGE']);
      payload.processedEvents.EVIDENCE['$EVIDENCE-DUPLICATE-OWNER'].eventId = 'EVIDENCE-DUPLICATE-OWNER';
    },
  ];
  for (const tamper of candidates) {
    const invalid = copy(canonical); tamper(invalid);
    assertRejectedWithoutMutation(invalid, () => business.snapshot(invalid), 'INVALID_TRANSITION');
    const target = business.createDemoState(NOW);
    assertRejectedWithoutMutation(target, () => business.loadReviewSnapshot(target, invalid), 'INVALID_TRANSITION');
    for (const eventId of [input.eventId, 'EVIDENCE-NEW-AFTER-TAMPER']) {
      assertRejectedWithoutMutation(invalid, () => business.consumeTrial(invalid, { ...input, eventId }, LATER), 'INVALID_TRANSITION');
    }
  }
});

test('consumeTrial derives the subject only from a valid frozen confirmed session', () => {
  const valid = business.createDemoState(NOW);
  const confirmed = startAndConfirm(valid, 'NEW_MOBILE', 'REFERENCE');
  assert.equal(business.consumeTrial(valid, { sessionId: confirmed.session.id, eventId: 'EVT-REFERENCE', seconds: 60 }, LATER).ok, true);
  const canonical = business.snapshot(valid).value;
  const sessionId = confirmed.session.id;
  const subjectId = confirmed.session.subjectId;
  const otherId = 'SUBJ-REPEAT-001';

  const candidates = [
    snapshot => { snapshot.sessions.find(item => item.id === sessionId).confirmedSubjectId = otherId; },
    snapshot => { const session = snapshot.sessions.find(item => item.id === sessionId); session.subjectId = null; session.confirmedSubjectId = null; },
    snapshot => { snapshot.subjects = snapshot.subjects.filter(item => item.id !== subjectId); },
    snapshot => { snapshot.subjects.find(item => item.id === subjectId).id = 'SUBJ-REPLACED'; },
    snapshot => { snapshot.subjects.push(copy(snapshot.subjects.find(item => item.id === subjectId))); },
  ];

  candidates.forEach((tamper, index) => {
    for (const [eventId, seconds] of [['EVT-REFERENCE', 60], [`EVT-TAMPER-${index}`, 1]]) {
      const state = copy(canonical);
      tamper(state);
      const before = raw(state);
      const result = business.consumeTrial(state, { sessionId, eventId, seconds }, LATER);
      assert.equal(result.ok, false, `${index}:${eventId}`);
      assert.equal(result.code, 'INVALID_TRANSITION', `${index}:${eventId}`);
      assert.deepEqual(state, before, `${index}:${eventId}`);
    }

    const state = copy(canonical);
    tamper(state);
    const target = business.createDemoState(NOW);
    const targetBefore = raw(target);
    const loaded = business.loadReviewSnapshot(target, state);
    assert.equal(loaded.ok, false);
    assert.deepEqual(target, targetBefore);
  });
});

test('trial consumption is bounded by remaining quota', () => {
  const state = business.createDemoState(NOW);
  const confirmed = startAndConfirm(state, 'NEW_MOBILE', 'BOUND');
  const grant = state.trialGrants.find(item => item.subjectId === confirmed.session.subjectId);
  grant.usedSeconds = 280;
  const result = business.consumeTrial(state, { sessionId: confirmed.session.id, eventId: 'EVT-BOUND', seconds: 60 }, LATER);
  assert.equal(result.ok, true);
  assert.equal(result.value.evidence.recordedSeconds, 20);
  assert.equal(result.value.grant.usedSeconds, 300);
  assert.equal(result.value.exhausted, true);
  assert.equal(result.value.message, '额度耗尽，结束取证');
});

test('snapshot import accepts dynamic linked data and atomically rejects forbidden fields', () => {
  const source = business.createDemoState(NOW);
  const confirmed = startAndConfirm(source, 'NEW_LANDLINE', 'ROUNDTRIP');
  assert.equal(business.consumeTrial(source, { sessionId: confirmed.session.id, eventId: 'EVT-ROUNDTRIP', seconds: 25 }, LATER).ok, true);
  const exported = business.snapshot(source);
  assert.equal(exported.ok, true);

  const target = business.createDemoState(NOW);
  const loaded = business.loadReviewSnapshot(target, exported.value);
  assert.equal(loaded.ok, true);
  assert.deepEqual(business.snapshot(target).value, exported.value);
  assert.deepEqual(target.sessions.find(item => item.id === confirmed.session.id), exported.value.sessions.find(item => item.id === confirmed.session.id));
  assert.deepEqual(target.evidence.find(item => item.sessionId === confirmed.session.id), exported.value.evidence.find(item => item.sessionId === confirmed.session.id));

  const forbidden = ['employees', 'employeeLoginSession', 'employeeCredentials', 'employeeRoles', 'customerAuthSession', 'uiSession', 'widgetState', 'code'];
  for (const key of forbidden) {
    const payload = copy(exported.value);
    payload[key] = { injected: true };
    const before = raw(target);
    const rejection = business.loadReviewSnapshot(target, payload);
    assert.equal(rejection.ok, false, key);
    assert.equal(rejection.code, 'INVALID_TRANSITION', key);
    assert.deepEqual(target, before, key);
  }
});

test('imported sequence is a hint and all ID allocators skip occupied next candidates', () => {
  const cases = [];

  {
    const state = business.createDemoState(NOW);
    const buyer = prepareBuyer(state);
    const seeded = business.createOrder(state, orderInput(state, buyer, 'SEED-ORDER'), NOW);
    assert.equal(seeded.ok, true);
    state.orders[0].id = 'ORDER-001001';
    state.sequence = 1000;
    cases.push({ prefix: 'ORDER', state, collection: 'orders', action: imported => business.createOrder(imported, orderInput(imported, imported.subjects.find(item => item.id === buyer.id), 'NEXT-ORDER'), LATER), resultId: result => result.value.id });
  }

  {
    const state = business.createDemoState(NOW);
    state.trialGrants[0].id = 'TRIAL-001001';
    state.subjects.push({ id: 'SUBJ-TRIAL-TARGET', maskedNumber: '155****0001', numberType: 'MOBILE', source: 'WEB', accountId: 'ACCOUNT-TRIAL-TARGET', identity: 'VERIFIED', activation: 'ACTIVE', revision: 1, claimRisk: 'NONE' });
    state.sequence = 1000;
    cases.push({ prefix: 'TRIAL', state, collection: 'trialGrants', action: imported => business.grantTrialOnce(imported, 'SUBJ-TRIAL-TARGET', LATER), resultId: result => result.value.id });
  }

  {
    const state = business.createDemoState(NOW);
    state.subjects.push({ id: 'SUBJ-001001', maskedNumber: '155****0002', numberType: 'MOBILE', source: 'WEB', accountId: null, identity: 'UNVERIFIED', activation: 'INACTIVE', revision: 1, claimRisk: 'NONE' });
    const session = business.startIvrSession(state, { scenario: 'NEW_MOBILE' }, NOW).value;
    state.sequence = 1000;
    cases.push({ prefix: 'SUBJ', state, collection: 'subjects', action: imported => business.confirmIvrSession(imported, { sessionId: session.id, key: '1', eventId: 'ALLOCATE-SUBJ' }, LATER), resultId: result => result.value.session.subjectId });
  }

  {
    const state = business.createDemoState(NOW);
    const buyer = prepareBuyer(state);
    state.activations.push({ id: 'ACT-001001', subjectId: buyer.id, channel: 'WEB', consentVersion: 'SEED-CONSENT', confirmation: 'EXPLICIT', eventId: 'SEED-ACT', at: NOW });
    state.sequence = 1000;
    cases.push({ prefix: 'ACT', state, collection: 'activations', action: imported => business.activateService(imported, { subjectId: buyer.id, channel: 'WEB', verified: true, confirmed: true, consentVersion: 'NEXT-CONSENT', eventId: 'ALLOCATE-ACT' }, LATER), resultId: result => result.value.activation.id });
  }

  {
    const state = business.createDemoState(NOW);
    const confirmed = startAndConfirm(state, 'NEW_MOBILE', 'ALLOCATE-EVID');
    const first = business.consumeTrial(state, { sessionId: confirmed.session.id, eventId: 'ALLOCATE-EVIDENCE-SEED', seconds: 1 }, LATER);
    assert.equal(first.ok, true);
    state.evidence.find(item => item.id === first.value.evidence.id).id = 'EVID-001001';
    state.processedEvents.EVIDENCE['$ALLOCATE-EVIDENCE-SEED'].result.evidence.id = 'EVID-001001';
    state.sequence = 1000;
    cases.push({ prefix: 'EVID', state, collection: 'evidence', action: imported => business.consumeTrial(imported, { sessionId: confirmed.session.id, eventId: 'ALLOCATE-EVIDENCE', seconds: 1 }, LATER), resultId: result => result.value.evidence.id });
  }

  {
    const state = business.createDemoState(NOW);
    const owner = state.subjects.find(item => item.id === 'SUBJ-REPEAT-001');
    owner.accountId = 'ACCOUNT-SEED-CLAIM';
    state.claims.push({ id: 'CLAIM-001001', subjectId: owner.id, accountId: owner.accountId, method: 'SMS', verifiedAt: NOW, requestId: 'SEED-CLAIM' });
    state.sequence = 1000;
    cases.push({ prefix: 'CLAIM', state, collection: 'claims', action: imported => business.claimSubject(imported, { subjectId: 'SUBJ-EXPIRED-001', accountId: 'ACCOUNT-NEXT-CLAIM', verification: { verified: true, subjectId: 'SUBJ-EXPIRED-001', method: 'SMS' }, requestId: 'ALLOCATE-CLAIM' }, LATER), resultId: result => result.value.id });
  }

  {
    const state = business.createDemoState(NOW);
    const buyer = prepareBuyer(state);
    const first = business.createOrder(state, orderInput(state, buyer, 'ENT-ORDER-1'), NOW).value;
    assert.equal(business.applyPaymentResult(state, { orderId: first.id, eventId: 'ENT-PAY-1', result: 'PAID' }, NOW).ok, true);
    assert.equal(business.issueEntitlements(state, { orderId: first.id, eventId: 'ENT-ISSUE-1' }, NOW).ok, true);
    const second = business.createOrder(state, orderInput(state, buyer, 'ENT-ORDER-2'), NOW).value;
    assert.equal(business.applyPaymentResult(state, { orderId: second.id, eventId: 'ENT-PAY-2', result: 'PAID' }, NOW).ok, true);
    state.entitlements[0].id = 'ENT-001001';
    delete state.processedEvents.ENTITLEMENT['$ENT-ISSUE-1'];
    state.sequence = 1000;
    cases.push({ prefix: 'ENT', state, collection: 'entitlements', action: imported => business.issueEntitlements(imported, { orderId: second.id, eventId: 'ALLOCATE-ENT' }, LATER), resultId: result => result.value.id });
  }

  {
    const state = business.createDemoState(NOW);
    state.sessions.push({ id: 'IVR-001001', subjectId: null, confirmedSubjectId: null, maskedCaller: '021-****8821', trustedCaller: true, status: 'RECEIVED', consentVersion: business.labels.consentVersion, key: null, guidanceStatus: 'NOT_GUIDED', scenario: 'NEW_LANDLINE', createdAt: NOW });
    state.sequence = 1000;
    cases.push({ prefix: 'IVR', state, collection: 'sessions', action: imported => business.startIvrSession(imported, { scenario: 'NEW_MOBILE' }, LATER), resultId: result => result.value.id });
  }

  for (const item of cases) {
    const imported = business.createDemoState(NOW);
    assert.equal(business.loadReviewSnapshot(imported, raw(item.state)).ok, true, item.prefix);
    const result = item.action(imported);
    assert.equal(result.ok, true, item.prefix);
    assert.notEqual(item.resultId(result), `${item.prefix}-001001`, item.prefix);
    const ids = imported[item.collection].map(record => record.id);
    assert.equal(new Set(ids).size, ids.length, item.prefix);
    assert.equal(business.snapshot(imported).ok, true, item.prefix);
  }
});

test('client activation requires verification while IVR keeps identity unverified', () => {
  const state = business.createDemoState(NOW);
  state.subjects.push({ id: 'SUBJ-CLIENT-001', maskedNumber: '158****9012', numberType: 'MOBILE', source: 'MINIAPP', accountId: null, identity: 'UNVERIFIED', activation: 'INACTIVE', revision: 1, claimRisk: 'NONE' });
  assertRejectedWithoutMutation(state, () => business.activateService(state, { subjectId: 'SUBJ-CLIENT-001', channel: 'MINIAPP', verified: true, confirmed: true, consentVersion: business.labels.consentVersion, eventId: 'ACT-CLIENT-REJECT' }, NOW), 'VERIFICATION_REQUIRED');
  const target = state.subjects.find(item => item.id === 'SUBJ-CLIENT-001');
  target.accountId = 'ACCOUNT-CLIENT-001';
  target.identity = 'VERIFIED';
  const activated = business.activateService(state, { subjectId: target.id, channel: 'MINIAPP', verified: true, confirmed: true, consentVersion: business.labels.consentVersion, eventId: 'ACT-CLIENT-OK' }, NOW);
  assert.equal(activated.ok, true);
  assert.equal(activated.value.activation.channel, 'MINIAPP');
  assert.equal(activated.value.grant.grantedSeconds, 300);
});

test('activation status transitions are canonical, event-owned and do not regrant trial', () => {
  const state = business.createDemoState(NOW);
  const target = prepareBuyer(state, 'SUBJ-REPEAT-001', 'ACCOUNT-STATUS-001');
  const originalGrant = copy(state.trialGrants.find(item => item.subjectId === target.id));
  const stopInput = { subjectId: target.id, channel: 'WEB', status: 'SUSPENDED', eventId: 'STATUS-STOP-001' };
  const stopped = business.setActivationStatus(state, stopInput, NOW);
  assert.equal(stopped.ok, true);
  assert.equal(stopped.value.fromRevision + 1, stopped.value.toRevision);
  assert.equal(state.subjects.find(item => item.id === target.id).activation, 'SUSPENDED');
  assert.deepEqual(state.trialGrants.find(item => item.subjectId === target.id), originalGrant);
  const stoppedState = raw(state);
  assert.equal(business.setActivationStatus(state, stopInput, LATER).code, 'DUPLICATE');
  assert.deepEqual(state, stoppedState);
  assertRejectedWithoutMutation(state, () => business.setActivationStatus(state, { ...stopInput, status: 'ACTIVE' }, LATER), 'INVALID_TRANSITION');

  const resumeInput = { subjectId: target.id, channel: 'WEB', status: 'ACTIVE', eventId: 'STATUS-RESUME-001' };
  assert.equal(business.setActivationStatus(state, resumeInput, LATER).ok, true);
  assert.equal(state.subjects.find(item => item.id === target.id).activation, 'ACTIVE');
  assert.deepEqual(state.trialGrants.find(item => item.subjectId === target.id), originalGrant);
  assert.equal(state.activationTransitions.length, 2);
  assert.equal(state.activationTransitions[1].fromRevision, state.activationTransitions[0].toRevision);
  assert.equal(business.snapshot(state).ok, true);

  const missingEvent = raw(state);
  delete missingEvent.processedEvents.ACTIVATION_STATUS['$STATUS-STOP-001'];
  assertRejectedWithoutMutation(missingEvent, () => business.snapshot(missingEvent), 'INVALID_TRANSITION');
  assertRejectedWithoutMutation(missingEvent, () => business.setActivationStatus(missingEvent, { ...stopInput, eventId: 'STATUS-NEW' }, LATER), 'INVALID_TRANSITION');
});

test('operations activation transitions preserve web history, audit ownership and atomic replay', () => {
  const state = business.createDemoState(NOW);
  const target = prepareBuyer(state, 'SUBJ-REPEAT-001', 'ACCOUNT-OPERATIONS-001');
  const webStop = { subjectId: target.id, channel: 'WEB', status: 'SUSPENDED', eventId: 'WEB-BEFORE-OPERATIONS' };
  const operationsResume = { subjectId: target.id, channel: 'OPERATIONS', status: 'ACTIVE', eventId: 'OPERATIONS-RESUME-001' };
  assert.equal(business.setActivationStatus(state, webStop, NOW).ok, true);
  const beforeOperations = raw(state);
  const resumed = business.setActivationStatus(state, operationsResume, LATER);
  assert.equal(resumed.ok, true);
  assert.equal(resumed.value.channel, 'OPERATIONS');
  assert.equal(resumed.value.fromRevision, beforeOperations.subjects.find(item => item.id === target.id).revision);
  assert.equal(resumed.value.toRevision, resumed.value.fromRevision + 1);
  assert.equal(state.subjects.find(item => item.id === target.id).revision, resumed.value.toRevision);
  assert.deepEqual(state.processedEvents.ACTIVATION_STATUS['$OPERATIONS-RESUME-001'].result, resumed.value);

  const completed = raw(state);
  assert.equal(business.setActivationStatus(state, operationsResume, LATER).code, 'DUPLICATE');
  assert.deepEqual(state, completed);
  assertRejectedWithoutMutation(state, () => business.setActivationStatus(state, { ...operationsResume, status: 'SUSPENDED' }, LATER), 'INVALID_TRANSITION');
  assertRejectedWithoutMutation(state, () => business.setActivationStatus(state, { ...operationsResume, eventId: 'OPERATIONS-INVALID-CURRENT' }, LATER), 'INVALID_TRANSITION');

  const fresh = business.createDemoState(NOW);
  const freshTarget = prepareBuyer(fresh, 'SUBJ-REPEAT-001', 'ACCOUNT-OPERATIONS-FRESH');
  const freshStop = business.setActivationStatus(fresh, { subjectId: freshTarget.id, channel: 'OPERATIONS', status: 'SUSPENDED', eventId: 'OPERATIONS-FRESH-STOP' }, NOW);
  assert.equal(freshStop.ok, true);
  assert.equal(fresh.activationTransitions.length, 1);
  assert.equal(freshStop.value.channel, 'OPERATIONS');
});

test('mobile and landline lifecycle transitions survive import, replay and failure paths', () => {
  for (const numberType of ['MOBILE', 'LANDLINE']) {
    const state = business.createDemoState(NOW);
    const subjectId = `STATUS-${numberType}`;
    state.subjects.push({ id: subjectId, maskedNumber: numberType === 'MOBILE' ? '138 **** 0020' : '010 **** 0020', numberType, source: 'WEB', accountId: 'ACCOUNT-STATUS-TYPES', identity: 'VERIFIED', activation: 'ACTIVE', revision: 3, claimRisk: 'NONE' });
    const stopInput = { subjectId, channel: 'WEB', status: 'SUSPENDED', eventId: `STATUS-${numberType}-STOP` };
    const restoreInput = { subjectId, channel: 'WEB', status: 'ACTIVE', eventId: `STATUS-${numberType}-RESTORE` };
    assert.equal(business.setActivationStatus(state, stopInput, NOW).ok, true, numberType);
    assert.equal(business.setActivationStatus(state, restoreInput, LATER).ok, true, numberType);
    assert.deepEqual(state.activationTransitions.map(item => [item.fromRevision, item.toRevision]), [[3, 4], [4, 5]], numberType);

    const imported = business.createDemoState(NOW);
    const snapshot = business.snapshot(state).value;
    assert.equal(business.loadReviewSnapshot(imported, snapshot).ok, true, numberType);
    const importedBefore = raw(imported);
    assert.equal(business.setActivationStatus(imported, stopInput, LATER).code, 'DUPLICATE', numberType);
    assert.equal(business.setActivationStatus(imported, restoreInput, LATER).code, 'DUPLICATE', numberType);
    assert.deepEqual(imported, importedBefore, numberType);
    assertRejectedWithoutMutation(imported, () => business.setActivationStatus(imported, { ...stopInput, status: 'ACTIVE' }, LATER), 'INVALID_TRANSITION');
  }
});

test('activation transition ledger rejects every ordering and final-state corruption before replay', () => {
  const valid = business.createDemoState(NOW);
  const target = prepareBuyer(valid, 'SUBJ-REPEAT-001', 'ACCOUNT-LEDGER-001');
  const stopInput = { subjectId: target.id, channel: 'WEB', status: 'SUSPENDED', eventId: 'LEDGER-STOP' };
  const restoreInput = { subjectId: target.id, channel: 'OPERATIONS', status: 'ACTIVE', eventId: 'LEDGER-RESTORE' };
  assert.equal(business.setActivationStatus(valid, stopInput, NOW).ok, true);
  assert.equal(business.setActivationStatus(valid, restoreInput, LATER).ok, true);
  const eventResult = (state, eventId) => state.processedEvents.ACTIVATION_STATUS[`$${eventId}`].result;
  const corruptions = [
    ['out-of-order ledger', state => state.activationTransitions.reverse()],
    ['duplicate revision', state => {
      const second = state.activationTransitions[1];
      second.fromRevision = state.activationTransitions[0].fromRevision;
      second.toRevision = second.fromRevision + 1;
      Object.assign(eventResult(state, second.eventId), copy(second));
    }],
    ['decreasing revision', state => {
      const second = state.activationTransitions[1];
      second.fromRevision = 1;
      second.toRevision = 2;
      Object.assign(eventResult(state, second.eventId), copy(second));
    }],
    ['discontinuous state', state => {
      const second = state.activationTransitions[1];
      second.from = 'ACTIVE';
      second.to = 'SUSPENDED';
      state.subjects.find(item => item.id === second.subjectId).activation = 'SUSPENDED';
      Object.assign(eventResult(state, second.eventId), copy(second));
    }],
    ['tail state mismatch', state => { state.subjects.find(item => item.id === target.id).activation = 'SUSPENDED'; }],
    ['tail revision mismatch', state => { state.subjects.find(item => item.id === target.id).revision = state.activationTransitions.at(-1).toRevision - 1; }],
    ['time regression', state => {
      const second = state.activationTransitions[1];
      second.at = '2026-10-19T23:59:00.000Z';
      Object.assign(eventResult(state, second.eventId), copy(second));
    }],
    ['missing revision anchor', state => {
      const second = state.activationTransitions[1];
      delete second.fromRevision;
      delete eventResult(state, second.eventId).fromRevision;
    }],
  ];
  for (const [label, corrupt] of corruptions) {
    const invalid = raw(valid);
    corrupt(invalid);
    assertRejectedWithoutMutation(invalid, () => business.snapshot(invalid), 'INVALID_TRANSITION');
    const importTarget = business.createDemoState(NOW);
    assertRejectedWithoutMutation(importTarget, () => business.loadReviewSnapshot(importTarget, invalid), 'INVALID_TRANSITION');
    assertRejectedWithoutMutation(invalid, () => business.setActivationStatus(invalid, restoreInput, LATER), 'INVALID_TRANSITION');
    assertRejectedWithoutMutation(invalid, () => business.setActivationStatus(invalid, { ...restoreInput, eventId: `LEDGER-FRESH-${label}` }, LATER), 'INVALID_TRANSITION');
  }
});

test('catalog type, billing cycle and grant schedule matrix is shared and transactional', () => {
  const validCustom = copy(business.demoCatalog.find(item => item.sku === 'PERSONAL_BASIC_ANNUAL'));
  validCustom.grantIntervalMonths = 3;
  validCustom.installments = 4;
  assert.equal(business.validateCatalogVersion(validCustom).ok, true);

  const templates = {
    SUBSCRIPTION: business.demoCatalog.find(item => item.type === 'SUBSCRIPTION'),
    ONE_TIME: business.demoCatalog.find(item => item.type === 'ONE_TIME'),
    ADD_ON: business.demoCatalog.find(item => item.type === 'ADD_ON'),
    QUOTE: business.demoCatalog.find(item => item.type === 'QUOTE'),
  };
  const invalid = [
    ['SUBSCRIPTION', 'ONCE', 1, 0], ['SUBSCRIPTION', 'MONTHLY', 0, 1], ['SUBSCRIPTION', 'ANNUAL', 12, 0],
    ['ONE_TIME', 'ANNUAL', 1, 0], ['ONE_TIME', 'ONCE', 2, 0], ['ONE_TIME', 'ONCE', 1, 1],
    ['ADD_ON', 'MONTHLY', 1, 0], ['ADD_ON', 'ONCE', 2, 0], ['ADD_ON', 'ONCE', 1, 1],
    ['QUOTE', 'ANNUAL', 1, 0], ['QUOTE', 'QUOTE', 2, 0], ['QUOTE', 'QUOTE', 1, 1],
  ];
  for (const [type, billingCycle, installments, grantIntervalMonths] of invalid) {
    const offer = copy(templates[type]);
    Object.assign(offer, { billingCycle, installments, grantIntervalMonths });
    assert.equal(business.validateCatalogVersion(offer).ok, false, `${type}:${billingCycle}:${installments}:${grantIntervalMonths}`);
    const state = business.createDemoState(NOW);
    const buyer = prepareBuyer(state);
    state.catalog.find(item => item.versionId === offer.versionId) && Object.assign(state.catalog.find(item => item.versionId === offer.versionId), offer);
    if (!state.catalog.some(item => item.versionId === offer.versionId)) state.catalog.push(offer);
    const target = business.createDemoState(NOW);
    assertRejectedWithoutMutation(target, () => business.loadReviewSnapshot(target, state), 'INVALID_TRANSITION');
    assertRejectedWithoutMutation(state, () => business.createOrder(state, { ...orderInput(state, buyer, `MATRIX-${type}-${billingCycle}-${installments}-${grantIntervalMonths}`), versionId: offer.versionId, versionToken: `OFFER-V1:invalid` }, NOW), 'INVALID_TRANSITION');
    state.orders.push({ id: `ORDER-MATRIX-${type}-${installments}-${grantIntervalMonths}`, subjectId: buyer.id, accountId: buyer.accountId, channel: 'WEB', snapshot: offer, payment: 'PAID', entitlement: 'PENDING', createdAt: NOW, idempotencyKey: `ORDER-MATRIX-KEY-${type}-${installments}-${grantIntervalMonths}`, consentVersion: 'ORDER-CONSENT-V1' });
    assertRejectedWithoutMutation(state, () => business.issueEntitlements(state, { orderId: state.orders.at(-1).id, eventId: `ISSUE-MATRIX-${type}-${installments}-${grantIntervalMonths}` }, LATER), 'NOT_SELLABLE');
  }
});

test('order payment and entitlement transitions remain separate and idempotent', () => {
  const state = business.createDemoState(NOW);
  const confirmed = startAndConfirm(state, 'NEW_MOBILE', 'ORDER');
  const subject = state.subjects.find(item => item.id === confirmed.session.subjectId);
  subject.accountId = 'ACCOUNT-ORDER-001';
  subject.identity = 'VERIFIED';
  const selected = state.catalog.find(item => item.sku === 'PERSONAL_BASIC_MONTHLY');
  const versionToken = business.offerVersionToken(selected);

  const staleState = raw(state);
  assertRejectedWithoutMutation(state, () => business.createOrder(state, { subjectId: subject.id, accountId: subject.accountId, channel: 'WEB', versionId: selected.versionId, versionToken: 'stale', confirmed: true, consentVersion: 'ORDER-CONSENT-V1', idempotencyKey: 'ORDER-STALE' }, NOW), 'STALE_VERSION');
  assert.deepEqual(state, staleState);
  assert.equal(business.createOrder(state, { subjectId: subject.id, accountId: subject.accountId, channel: 'IVR', versionId: selected.versionId, confirmed: true, consentVersion: 'ORDER-CONSENT-V1', idempotencyKey: 'ORDER-IVR' }, NOW).ok, false);

  const created = business.createOrder(state, { subjectId: subject.id, accountId: subject.accountId, channel: 'WEB', versionId: selected.versionId, versionToken, confirmed: true, consentVersion: 'ORDER-CONSENT-V1', idempotencyKey: 'ORDER-CREATE-1' }, NOW);
  assert.equal(created.ok, true);
  assert.equal(created.value.payment, 'UNPAID');
  assert.equal(created.value.entitlement, 'NOT_ISSUED');
  selected.priceCents = 4100;
  assert.equal(state.orders[0].snapshot.priceCents, 3900);

  const unknown = business.applyPaymentResult(state, { orderId: created.value.id, eventId: 'PAY-UNKNOWN-1', result: 'UNKNOWN' }, LATER);
  assert.equal(unknown.value.payment, 'UNKNOWN');
  assert.equal(unknown.value.entitlement, 'NOT_ISSUED');
  assertRejectedWithoutMutation(state, () => business.issueEntitlements(state, { orderId: created.value.id, eventId: 'ISSUE-EARLY' }, LATER), 'INVALID_TRANSITION');
  assertRejectedWithoutMutation(state, () => business.createOrder(state, { subjectId: subject.id, accountId: subject.accountId, channel: 'MINIAPP', versionId: state.catalog[1].versionId, confirmed: true, consentVersion: 'ORDER-CONSENT-V1', idempotencyKey: 'ORDER-BLOCKED' }, LATER), 'PAYMENT_UNRESOLVED');

  const paid = business.applyPaymentResult(state, { orderId: created.value.id, eventId: 'PAY-PAID-1', result: 'PAID' }, LATER);
  assert.equal(paid.value.payment, 'PAID');
  assert.equal(paid.value.entitlement, 'PENDING');
  assert.equal(state.entitlements.length, 0);
  const issued = business.issueEntitlements(state, { orderId: created.value.id, eventId: 'ISSUE-1' }, LATER);
  assert.equal(issued.ok, true);
  assert.equal(state.orders[0].entitlement, 'ISSUED');
  assert.equal(state.entitlements.length, 1);
  assert.deepEqual(issued.value.snapshot, state.orders[0].snapshot);
  const beforeReplay = raw(state);
  assert.equal(business.applyPaymentResult(state, { orderId: created.value.id, eventId: 'PAY-PAID-1', result: 'PAID' }, LATER).code, 'DUPLICATE');
  assert.equal(business.issueEntitlements(state, { orderId: created.value.id, eventId: 'ISSUE-2' }, LATER).code, 'DUPLICATE');
  assert.deepEqual(state, beforeReplay);
});

test('offer tokens are stable, public and mandatory for atomic checkout', () => {
  const state = business.createDemoState(NOW);
  const buyer = prepareBuyer(state);
  const selected = state.catalog.find(item => item.sku === 'PERSONAL_BASIC_MONTHLY');
  const token = business.offerVersionToken(selected);
  assert.equal(token, business.offerVersionToken(reorderObject(selected)));
  const changed = copy(selected);
  changed.description = `${changed.description}（已变更）`;
  assert.notEqual(token, business.offerVersionToken(changed));
  const visible = business.visibleOffers(state, { channel: 'WEB', audience: 'PERSONAL' });
  assert.equal(visible.ok, true);
  assert.equal(visible.value.find(item => item.versionId === selected.versionId).versionToken, token);

  for (const [label, versionToken] of [['missing', undefined], ['empty', ''], ['stale', 'OFFER-V1:stale']]) {
    const input = orderInput(state, buyer, `TOKEN-${label}`, { versionToken });
    if (versionToken === undefined) delete input.versionToken;
    assertRejectedWithoutMutation(state, () => business.createOrder(state, input, NOW), 'STALE_VERSION');
  }
  assert.equal(business.createOrder(state, orderInput(state, buyer, 'TOKEN-CURRENT'), NOW).ok, true);
});

test('new checkout requires ACTIVE while historical snapshots survive catalog retirement', () => {
  for (const saleStatus of ['DRAFT', 'OFFSALE']) {
    const state = business.createDemoState(NOW);
    const buyer = prepareBuyer(state);
    const selected = state.catalog.find(item => item.sku === 'PERSONAL_BASIC_MONTHLY');
    const oldToken = business.offerVersionToken(selected);
    selected.saleStatus = saleStatus;
    const currentToken = business.offerVersionToken(selected);
    assertRejectedWithoutMutation(state, () => business.createOrder(state, orderInput(state, buyer, `ORDER-${saleStatus}-CURRENT`, { versionToken: currentToken }), NOW), 'NOT_SELLABLE');
    assertRejectedWithoutMutation(state, () => business.createOrder(state, orderInput(state, buyer, `ORDER-${saleStatus}-OLD`, { versionToken: oldToken }), NOW), 'NOT_SELLABLE');
  }

  const state = business.createDemoState(NOW);
  const buyer = prepareBuyer(state);
  const selected = state.catalog.find(item => item.sku === 'PERSONAL_BASIC_MONTHLY');
  const created = business.createOrder(state, orderInput(state, buyer, 'ORDER-HISTORICAL'), NOW);
  assert.equal(created.ok, true);
  const historicalSnapshot = raw(created.value.snapshot);
  const currentSelected = state.catalog.find(item => item.versionId === selected.versionId);
  currentSelected.saleStatus = 'OFFSALE';
  const replacement = copy(currentSelected);
  replacement.versionId = 'PERSONAL_BASIC_MONTHLY-V2';
  replacement.version = 2;
  replacement.priceCents = 4200;
  replacement.saleStatus = 'ACTIVE';
  state.catalog.push(replacement);
  assert.deepEqual(state.orders[0].snapshot, historicalSnapshot);

  const imported = business.createDemoState(NOW);
  assert.equal(business.loadReviewSnapshot(imported, business.snapshot(state).value).ok, true);
  assert.equal(business.applyPaymentResult(imported, { orderId: created.value.id, eventId: 'PAY-HISTORICAL', result: 'PAID' }, LATER).ok, true);
  assert.equal(business.issueEntitlements(imported, { orderId: created.value.id, eventId: 'ISSUE-HISTORICAL' }, LATER).ok, true);
  assert.deepEqual(imported.orders[0].snapshot, historicalSnapshot);
  assert.deepEqual(imported.entitlements[0].snapshot, historicalSnapshot);
});

test('enterprise quotes cannot enter personal order or entitlement transitions', () => {
  const state = business.createDemoState(NOW);
  const enterprise = copy(state.catalog.find(item => item.sku === 'ENTERPRISE_BASIC'));
  const enterprisePurchase = copy(enterprise);
  enterprisePurchase.type = 'SUBSCRIPTION';
  enterprisePurchase.billingCycle = 'ANNUAL';
  enterprisePurchase.priceCents = 100;
  assert.equal(business.validateCatalogVersion(enterprisePurchase).ok, false);

  const personalQuote = copy(state.catalog.find(item => item.sku === 'PERSONAL_BASIC_MONTHLY'));
  personalQuote.type = 'QUOTE';
  personalQuote.billingCycle = 'QUOTE';
  personalQuote.priceCents = null;
  assert.equal(business.validateCatalogVersion(personalQuote).ok, false);

  const buyer = prepareBuyer(state);
  state.orders.push({
    id: 'ORDER-QUOTE-001',
    subjectId: buyer.id,
    accountId: buyer.accountId,
    channel: 'WEB',
    snapshot: enterprise,
    payment: 'PAID',
    entitlement: 'PENDING',
    createdAt: NOW,
    idempotencyKey: 'ORDER-QUOTE-IMPORT',
    consentVersion: 'ORDER-CONSENT-V1',
  });
  const target = business.createDemoState(NOW);
  const targetBefore = raw(target);
  const imported = business.loadReviewSnapshot(target, raw(state));
  assert.equal(imported.ok, false);
  assert.equal(imported.code, 'INVALID_TRANSITION');
  assert.deepEqual(target, targetBefore);
  assertRejectedWithoutMutation(state, () => business.issueEntitlements(state, { orderId: 'ORDER-QUOTE-001', eventId: 'ISSUE-QUOTE' }, LATER), 'NOT_SELLABLE');

  const personalState = business.createDemoState(NOW);
  const personalBuyer = prepareBuyer(personalState);
  personalState.orders.push({
    id: 'ORDER-PERSONAL-QUOTE-001',
    subjectId: personalBuyer.id,
    accountId: personalBuyer.accountId,
    channel: 'WEB',
    snapshot: personalQuote,
    payment: 'PAID',
    entitlement: 'PENDING',
    createdAt: NOW,
    idempotencyKey: 'ORDER-PERSONAL-QUOTE',
    consentVersion: 'ORDER-CONSENT-V1',
  });
  assertRejectedWithoutMutation(personalState, () => business.issueEntitlements(personalState, { orderId: 'ORDER-PERSONAL-QUOTE-001', eventId: 'ISSUE-PERSONAL-QUOTE' }, LATER), 'NOT_SELLABLE');
});

test('payment replay returns the immutable first result after later state changes', () => {
  const state = business.createDemoState(NOW);
  const buyer = prepareBuyer(state);
  const created = business.createOrder(state, orderInput(state, buyer, 'PAYMENT-ORDER'), NOW).value;
  const unknown = business.applyPaymentResult(state, { orderId: created.id, eventId: 'PAY-FIRST', result: 'UNKNOWN' }, NOW);
  assert.equal(unknown.ok, true);
  assert.equal(business.applyPaymentResult(state, { orderId: created.id, eventId: 'PAY-LATER', result: 'PAID' }, LATER).ok, true);
  const beforeReplay = raw(state);
  const replay = business.applyPaymentResult(state, { orderId: created.id, eventId: 'PAY-FIRST', result: 'UNKNOWN' }, NOW);
  assert.equal(replay.code, 'DUPLICATE');
  assert.deepEqual(replay.value, unknown.value);
  assert.equal(replay.value.payment, 'UNKNOWN');
  assert.equal(state.orders[0].payment, 'PAID');
  assert.deepEqual(state, beforeReplay);
});

test('processed event results enforce domain schemas, stable identity and references', () => {
  const state = business.createDemoState(NOW);
  const activatedBuyer = prepareBuyer(state, 'SUBJ-REPEAT-001', 'ACCOUNT-ACTIVATED');
  const activationInput = { subjectId: activatedBuyer.id, channel: 'WEB', verified: true, confirmed: true, consentVersion: 'ACTIVATION-CONSENT-V1', eventId: 'EVENT-ACTIVATION' };
  const activation = business.activateService(state, activationInput, NOW);
  assert.equal(activation.ok, true);

  const confirmed = startAndConfirm(state, 'NEW_MOBILE', 'EVENT-SCHEMAS');
  const firstEvidence = business.consumeTrial(state, { sessionId: confirmed.session.id, eventId: 'EVENT-EVIDENCE-FIRST', seconds: 60 }, LATER);
  assert.equal(firstEvidence.ok, true);
  assert.equal(business.consumeTrial(state, { sessionId: confirmed.session.id, eventId: 'EVENT-EVIDENCE-LATER', seconds: 40 }, LATER).ok, true);
  const claimInput = {
    subjectId: confirmed.session.subjectId,
    accountId: 'ACCOUNT-EVENT-CLAIM',
    verification: { verified: true, subjectId: confirmed.session.subjectId, method: 'SMS' },
    requestId: 'EVENT-CLAIM',
  };
  const claim = business.claimSubject(state, claimInput, LATER);
  assert.equal(claim.ok, true);
  const claimedBuyer = state.subjects.find(item => item.id === confirmed.session.subjectId);
  claimedBuyer.identity = 'VERIFIED';

  const createInput = orderInput(state, claimedBuyer, 'EVENT-ORDER');
  const created = business.createOrder(state, createInput, NOW);
  assert.equal(created.ok, true);
  const unknownInput = { orderId: created.value.id, eventId: 'EVENT-PAY-UNKNOWN', result: 'UNKNOWN' };
  const paidInput = { orderId: created.value.id, eventId: 'EVENT-PAY-PAID', result: 'PAID' };
  const entitlementInput = { orderId: created.value.id, eventId: 'EVENT-ENTITLEMENT' };
  const unknown = business.applyPaymentResult(state, unknownInput, NOW);
  assert.equal(unknown.ok, true);
  const paid = business.applyPaymentResult(state, paidInput, LATER);
  assert.equal(paid.ok, true);
  const entitlement = business.issueEntitlements(state, entitlementInput, LATER);
  assert.equal(entitlement.ok, true);

  const exported = business.snapshot(state);
  assert.equal(exported.ok, true);
  for (const domain of ['ACTIVATION', 'CLAIM', 'IVR_CONFIRM', 'EVIDENCE', 'ORDER', 'PAYMENT', 'ENTITLEMENT']) {
    assert.ok(Object.keys(exported.value.processedEvents[domain]).length > 0, domain);
  }
  assert.deepEqual(exported.value.processedEvents.ACTIVATION_STATUS, {});

  const imported = business.createDemoState(NOW);
  assert.equal(business.loadReviewSnapshot(imported, exported.value).ok, true);
  const replayCases = [
    ['ACTIVATION', target => business.activateService(target, activationInput, NOW), activation.value],
    ['IVR_CONFIRM', target => business.confirmIvrSession(target, { sessionId: confirmed.session.id, key: '1', eventId: 'EVT-CONFIRM-EVENT-SCHEMAS' }, NOW), confirmed],
    ['EVIDENCE', target => business.consumeTrial(target, { sessionId: confirmed.session.id, eventId: 'EVENT-EVIDENCE-FIRST', seconds: 60 }, new Date('2027-12-01T00:00:00.000Z')), firstEvidence.value],
    ['CLAIM', target => business.claimSubject(target, claimInput, LATER), claim.value],
    ['ORDER', target => business.createOrder(target, createInput, NOW), created.value],
    ['PAYMENT UNKNOWN', target => business.applyPaymentResult(target, unknownInput, NOW), unknown.value],
    ['PAYMENT PAID', target => business.applyPaymentResult(target, paidInput, LATER), paid.value],
    ['ENTITLEMENT', target => business.issueEntitlements(target, entitlementInput, LATER), entitlement.value],
  ];
  for (const [domain, replay, expected] of replayCases) {
    const beforeReplay = raw(imported);
    const result = replay(imported);
    assert.equal(result.code, 'DUPLICATE', domain);
    assert.deepEqual(result.value, expected, domain);
    assert.deepEqual(imported, beforeReplay, domain);
  }
  for (const [label, replay] of replayCases) {
    const domain = label.split(' ')[0];
    const invalidHistory = raw(imported);
    invalidHistory.processedEvents[domain]['$MALFORMED-PEER'] = null;
    assertRejectedWithoutMutation(invalidHistory, () => business.snapshot(invalidHistory), 'INVALID_TRANSITION');
    const target = business.createDemoState(NOW);
    assertRejectedWithoutMutation(target, () => business.loadReviewSnapshot(target, invalidHistory), 'INVALID_TRANSITION');
    assertRejectedWithoutMutation(invalidHistory, () => replay(invalidHistory), 'INVALID_TRANSITION');
  }
  assert.equal(firstEvidence.value.grant.usedSeconds, 60);
  assert.equal(imported.trialGrants.find(item => item.id === firstEvidence.value.grant.id).usedSeconds, 100);
  assert.equal(imported.orders.find(item => item.id === created.value.id).payment, 'PAID');

  const eventRecord = (payload, domain, eventId) => payload.processedEvents[domain][`$${eventId}`];
  const invalidPayloads = [
    ['null result', payload => { eventRecord(payload, 'ORDER', 'EVENT-ORDER').result = null; }],
    ['missing field', payload => { delete eventRecord(payload, 'EVIDENCE', 'EVENT-EVIDENCE-FIRST').result.grant; }],
    ['ghost object', payload => { eventRecord(payload, 'PAYMENT', 'EVENT-PAY-UNKNOWN').result.id = 'ORDER-GHOST'; }],
    ['cross-subject result', payload => { eventRecord(payload, 'CLAIM', 'EVENT-CLAIM').result.subjectId = 'SUBJ-REPEAT-001'; }],
    ['wrong domain result', payload => { eventRecord(payload, 'PAYMENT', 'EVENT-PAY-UNKNOWN').result = copy(eventRecord(payload, 'ENTITLEMENT', 'EVENT-ENTITLEMENT').result); }],
    ['fingerprint identity mismatch', payload => {
      const record = eventRecord(payload, 'ORDER', 'EVENT-ORDER');
      const input = JSON.parse(record.inputFingerprint.slice('INPUT-V1:'.length));
      input.subjectId = 'SUBJ-REPEAT-001';
      record.inputFingerprint = `INPUT-V1:${JSON.stringify(input)}`;
    }],
  ];
  for (const [label, tamper] of invalidPayloads) {
    const payload = copy(exported.value);
    tamper(payload);
    const target = business.createDemoState(NOW);
    assertRejectedWithoutMutation(target, () => business.loadReviewSnapshot(target, payload), 'INVALID_TRANSITION');
    assert.equal(business.snapshot(target).ok, true, label);
  }
});

test('creating event identities are one-to-one inside each action domain', () => {
  const orderState = business.createDemoState(NOW);
  const buyer = prepareBuyer(orderState);
  const created = business.createOrder(orderState, orderInput(orderState, buyer, 'ONE-CHECKOUT'), NOW);
  assert.equal(created.ok, true);

  const duplicateOrders = raw(orderState);
  duplicateOrders.orders.push(Object.assign(copy(created.value), { id: 'ORDER-DUPLICATE' }));
  assert.equal(business.snapshot(duplicateOrders).code, 'INVALID_TRANSITION');
  const importTarget = business.createDemoState(NOW);
  assertRejectedWithoutMutation(importTarget, () => business.loadReviewSnapshot(importTarget, duplicateOrders), 'INVALID_TRANSITION');
  for (const order of duplicateOrders.orders) {
    assertRejectedWithoutMutation(duplicateOrders, () => business.applyPaymentResult(duplicateOrders, { orderId: order.id, eventId: `PAY-${order.id}`, result: 'PAID' }, LATER), 'INVALID_TRANSITION');
  }

  const duplicatePaidOrders = raw(duplicateOrders);
  for (const order of duplicatePaidOrders.orders) {
    order.payment = 'PAID';
    order.entitlement = 'PENDING';
  }
  for (const order of duplicatePaidOrders.orders) {
    assertRejectedWithoutMutation(duplicatePaidOrders, () => business.issueEntitlements(duplicatePaidOrders, { orderId: order.id, eventId: `ISSUE-${order.id}` }, LATER), 'INVALID_TRANSITION');
  }

  const activationState = business.createDemoState(NOW);
  const activatedBuyer = prepareBuyer(activationState);
  const sharedEventId = 'SHARED-ACTIVATION-EVENT';
  assert.equal(business.activateService(activationState, { subjectId: activatedBuyer.id, channel: 'WEB', verified: true, confirmed: true, consentVersion: 'CLIENT-CONSENT-V1', eventId: sharedEventId }, NOW).ok, true);
  const started = business.startIvrSession(activationState, { scenario: 'NEW_MOBILE' }, NOW);
  assert.equal(started.ok, true);
  assert.equal(business.confirmIvrSession(activationState, { sessionId: started.value.id, key: '1', eventId: sharedEventId }, NOW).ok, true);
  assert.equal(business.snapshot(activationState).ok, true);

  const clientDuplicate = raw(activationState);
  const clientActivation = clientDuplicate.activations.find(item => item.channel !== 'IVR' && item.eventId === sharedEventId);
  clientDuplicate.activations.push(Object.assign(copy(clientActivation), { id: 'ACT-DUPLICATE-CLIENT' }));
  assert.equal(business.snapshot(clientDuplicate).code, 'INVALID_TRANSITION');
  assertRejectedWithoutMutation(importTarget, () => business.loadReviewSnapshot(importTarget, clientDuplicate), 'INVALID_TRANSITION');

  const ivrDuplicate = raw(activationState);
  const ivrActivation = ivrDuplicate.activations.find(item => item.channel === 'IVR' && item.eventId === sharedEventId);
  ivrDuplicate.activations.push(Object.assign(copy(ivrActivation), { id: 'ACT-DUPLICATE-IVR' }));
  assert.equal(business.snapshot(ivrDuplicate).code, 'INVALID_TRANSITION');
  assertRejectedWithoutMutation(importTarget, () => business.loadReviewSnapshot(importTarget, ivrDuplicate), 'INVALID_TRANSITION');

  const validImport = business.createDemoState(NOW);
  assert.equal(business.loadReviewSnapshot(validImport, business.snapshot(activationState).value).ok, true);
});

test('evidence and entitlement result ownership rejects duplicate histories on import and replay', () => {
  const evidenceState = business.createDemoState(NOW);
  const confirmed = startAndConfirm(evidenceState, 'NEW_MOBILE', 'RESULT-OWNERSHIP');
  const evidenceInputs = ['EVIDENCE-OWNER-FIRST', 'EVIDENCE-OWNER-SECOND'].map(eventId => ({ sessionId: confirmed.session.id, eventId, seconds: 60 }));
  const evidenceResults = evidenceInputs.map(input => business.consumeTrial(evidenceState, input, LATER));
  assert.ok(evidenceResults.every(result => result.ok));
  assert.notEqual(evidenceResults[0].value.evidence.id, evidenceResults[1].value.evidence.id);
  const validImport = business.createDemoState(NOW);
  assert.equal(business.loadReviewSnapshot(validImport, business.snapshot(evidenceState).value).ok, true);
  for (const [index, input] of evidenceInputs.entries()) {
    const before = raw(validImport);
    const replay = business.consumeTrial(validImport, input, LATER);
    assert.equal(replay.code, 'DUPLICATE');
    assert.deepEqual(replay.value, evidenceResults[index].value);
    assert.deepEqual(validImport, before);
  }

  const duplicateEvidence = raw(evidenceState);
  duplicateEvidence.processedEvents.EVIDENCE['$EVIDENCE-OWNER-SECOND'].result = copy(evidenceResults[0].value);

  const entitlementState = business.createDemoState(NOW);
  const buyer = prepareBuyer(entitlementState);
  const order = business.createOrder(entitlementState, orderInput(entitlementState, buyer, 'ENTITLEMENT-OWNER-ORDER'), NOW).value;
  assert.equal(business.applyPaymentResult(entitlementState, { orderId: order.id, eventId: 'ENTITLEMENT-OWNER-PAYMENT', result: 'PAID' }, LATER).ok, true);
  const entitlementInput = { orderId: order.id, eventId: 'ENTITLEMENT-OWNER-FIRST' };
  const entitlement = business.issueEntitlements(entitlementState, entitlementInput, LATER);
  assert.equal(entitlement.ok, true);
  const newKeyInput = { orderId: order.id, eventId: 'ENTITLEMENT-OWNER-SECOND' };
  const beforeNewKey = raw(entitlementState);
  const newKeyReplay = business.issueEntitlements(entitlementState, newKeyInput, LATER);
  assert.equal(newKeyReplay.code, 'DUPLICATE');
  assert.deepEqual(newKeyReplay.value, entitlement.value);
  assert.deepEqual(entitlementState, beforeNewKey);
  assert.equal(Object.keys(entitlementState.processedEvents.ENTITLEMENT).length, 1);

  const duplicateEntitlement = raw(entitlementState);
  duplicateEntitlement.processedEvents.ENTITLEMENT['$ENTITLEMENT-OWNER-SECOND'] = Object.assign(
    copy(duplicateEntitlement.processedEvents.ENTITLEMENT['$ENTITLEMENT-OWNER-FIRST']),
    { eventId: 'ENTITLEMENT-OWNER-SECOND' },
  );
  const cases = [
    [duplicateEvidence, evidenceInputs, (state, input) => business.consumeTrial(state, input, LATER)],
    [duplicateEntitlement, [entitlementInput, newKeyInput], (state, input) => business.issueEntitlements(state, input, LATER)],
  ];
  for (const [invalidState, inputs, replay] of cases) {
    assertRejectedWithoutMutation(invalidState, () => business.snapshot(invalidState), 'INVALID_TRANSITION');
    const importTarget = business.createDemoState(NOW);
    assertRejectedWithoutMutation(importTarget, () => business.loadReviewSnapshot(importTarget, invalidState), 'INVALID_TRANSITION');
    for (const input of inputs) {
      assertRejectedWithoutMutation(invalidState, () => replay(invalidState, input), 'INVALID_TRANSITION');
    }
    assertRejectedWithoutMutation(invalidState, () => replay(invalidState, Object.assign({}, inputs[0], { eventId: 'FRESH-INVALID-HISTORY' })), 'INVALID_TRANSITION');
  }
});

test('valid trial replay survives later exhaustion and expiry without mutation', () => {
  const state = business.createDemoState(NOW);
  const confirmed = startAndConfirm(state, 'NEW_MOBILE', 'REPLAY-LIMITS');
  const first = business.consumeTrial(state, { sessionId: confirmed.session.id, eventId: 'EVENT-REPLAY-FIRST', seconds: 60 }, LATER);
  assert.equal(first.ok, true);
  assert.equal(business.consumeTrial(state, { sessionId: confirmed.session.id, eventId: 'EVENT-REPLAY-EXHAUST', seconds: 240 }, LATER).ok, true);
  const beforeReplay = raw(state);
  const replay = business.consumeTrial(state, { sessionId: confirmed.session.id, eventId: 'EVENT-REPLAY-FIRST', seconds: 60 }, new Date('2027-12-01T00:00:00.000Z'));
  assert.equal(replay.code, 'DUPLICATE');
  assert.deepEqual(replay.value, first.value);
  assert.equal(replay.value.grant.usedSeconds, 60);
  assert.deepEqual(state, beforeReplay);
});

test('generated sources and previews carry the same canonical module hash', () => {
  const modulePath = path.join(root, 'shared', 'onecall-business.js');
  const expectedHash = crypto.createHash('sha256').update(fs.readFileSync(modulePath)).digest('hex');
  const files = [
    'operations-prototype/onecall-operations-admin.html', 'prototype/onecall-miniapp-pages.html', 'web-prototype/evidence-hub.html',
    'operations-prototype/preview.html', 'prototype/preview.html', 'web-prototype/preview.html',
  ];
  for (const relative of files) {
    const contents = fs.readFileSync(path.join(root, relative), 'utf8');
    const hashes = [...contents.matchAll(/data-onecall-business-sha256=(?:"|&quot;)([a-f0-9]{64})(?:"|&quot;)/g)].map(match => match[1]);
    assert.deepEqual(hashes, [expectedHash], relative);
  }
});
