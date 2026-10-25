'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const B = require('../onecall-business');

const NOW = '2026-10-20T00:00:00.000Z';
const copy = value => JSON.parse(JSON.stringify(value));
const raw = value => copy(value);
const at = seconds => new Date(Date.parse(NOW) + seconds * 1000);
const ACCOUNT = 'ACCOUNT-DEMO-001';
const WORKSPACE = 'personal:ACCOUNT-DEMO-001';
const CALLER_4471 = 'CB-PERSONAL-4471';
const CALLER_6208 = 'CB-PERSONAL-6208';

function createFor(state, overrides = {}) {
  return B.createCallTask(state, Object.assign({
    callerBindingId: CALLER_4471,
    maskedCallee: '137 **** 2240',
    businessType: 'EVIDENCE_CALL',
    idempotencyKey: 'QA-TASK-1',
    replacePending: false,
  }, overrides), NOW);
}

function assertRejectedWithoutMutation(state, action, code) {
  const before = raw(state);
  const result = action();
  assert.equal(result.ok, false);
  if (code) assert.equal(result.code, code);
  assert.deepEqual(state, before);
  return result;
}

function claimedState(seconds = 1) {
  const state = B.createCallTaskDemoState(NOW);
  assert.equal(createFor(state).ok, true);
  const resolved = B.resolveIvrInbound(state, {
    callId: 'CALL-QA-TASK-01',
    normalizedCaller: '18600004471',
    callerTrusted: true,
    callerState: 'ACTIVE',
    availableSeconds: 300,
    directAllowed: true,
  }, at(seconds));
  assert.equal(resolved.ok, true);
  assert.equal(resolved.value.action, 'DIAL_TASK_CALLEE');
  return state;
}

test('demo fixture carries success, no-task, mismatch, no-answer, recording-derived and foreign-account facts', () => {
  const state = B.createCallTaskDemoState(NOW);
  assert.equal(B.callTaskSnapshot(state).ok, true);
  const byCall = id => state.calls.find(call => call.id === id);
  assert.equal(byCall('OC-C2038').taskMatch, 'CLAIMED');
  assert.equal(byCall('OC-C2038').calleeConfirmation, 'NONE');
  assert.equal(byCall('OC-C2038').evidenceStatus, 'AVAILABLE');
  assert.equal(byCall('OC-C2037').action, 'ENTER_CALLEE');
  assert.equal(byCall('OC-C2037').calleeConfirmation, 'DTMF_CONFIRMED');
  assert.equal(byCall('OC-C2036').taskMatch, 'MISMATCH');
  assert.equal(byCall('OC-C2036').progress, 'FAILED');
  assert.equal(byCall('OC-C2036').evidenceStatus, 'NOT_CREATED');
  assert.equal(byCall('OC-C2035').endReason, 'NO_ANSWER');
  assert.equal(byCall('OC-C2035').recordingSeconds, 0);
  const foreign = state.tasks.find(task => task.id === 'CT-DEMO-0201');
  assert.equal(foreign.ownerId, 'ACCOUNT-DEMO-002');
  assert.equal(foreign.status, 'PENDING');
  assert.equal(B.uniqueCallerBinding(state, foreign.normalizedCaller).ownerId, 'ACCOUNT-DEMO-002');
});

test('create freezes the 30-second expiry and retries never renew it', () => {
  const state = B.createCallTaskDemoState(NOW);
  const created = createFor(state);
  assert.equal(created.ok, true);
  assert.equal(created.value.ttlSeconds, 30);
  assert.equal(Date.parse(created.value.expiresAt) - Date.parse(created.value.createdAt), 30000);
  assert.equal(created.value.status, 'PENDING');
  const originalExpiry = created.value.expiresAt;

  const sameKey = createFor(state);
  assert.equal(sameKey.ok, true);
  assert.equal(sameKey.code, 'DUPLICATE');
  assert.equal(sameKey.value.id, created.value.id);
  assert.equal(sameKey.value.expiresAt, originalExpiry);

  const sameCallerCallee = createFor(state, { idempotencyKey: 'QA-TASK-OTHER-KEY' });
  assert.equal(sameCallerCallee.ok, true);
  assert.equal(sameCallerCallee.code, 'DUPLICATE');
  assert.equal(sameCallerCallee.value.id, created.value.id);

  assert.equal(B.callTaskSnapshot(state).ok, true);
  const pending = state.tasks.filter(task => task.normalizedCaller === '18600004471' && ['PENDING', 'CLAIMED'].includes(task.status));
  assert.equal(pending.length, 1);
});

test('one nonterminal task per normalized caller and caller-busy rejection hides other owners', () => {
  const state = B.createCallTaskDemoState(NOW);
  assert.equal(createFor(state).ok, true);
  state.callerBindings.push({
    id: 'CB-OTHER-4471',
    ownerType: 'PERSONAL_ACCOUNT',
    ownerId: 'ACCOUNT-DEMO-002',
    workspaceId: 'personal:ACCOUNT-DEMO-002',
    maskedCaller: '186 **** 4471',
    normalizedCaller: '18600004471',
    numberType: 'MOBILE',
    active: true,
  });
  assert.equal(B.callTaskSnapshot(state).ok, true);
  const busy = assertRejectedWithoutMutation(state, () => createFor(state, {
    callerBindingId: 'CB-OTHER-4471',
    maskedCallee: '138 **** 9999',
    idempotencyKey: 'QA-TASK-OTHER-OWNER',
  }), 'CALLER_BUSY');
  assert.equal(busy.value, undefined);

  state.callerBindings.push({
    id: 'CB-AMBIGUOUS-4471',
    ownerType: 'PERSONAL_ACCOUNT',
    ownerId: 'ACCOUNT-DEMO-003',
    workspaceId: 'personal:ACCOUNT-DEMO-003',
    maskedCaller: '186 **** 4471',
    normalizedCaller: '18600004471',
    numberType: 'MOBILE',
    active: true,
  });
  assert.equal(B.callTaskSnapshot(state).ok, true);
  assert.equal(B.uniqueCallerBinding(state, '18600004471'), null, 'ambiguous binding must fail closed');
  assert.equal(B.uniqueCallerBinding(state, '19900000000'), null, 'missing binding must fail closed');
});

test('explicit replacement only replaces the caller own pending task', () => {
  const state = B.createCallTaskDemoState(NOW);
  const first = createFor(state).value;
  assert.equal(createFor(state, { maskedCallee: '138 **** 1111', idempotencyKey: 'QA-TASK-2' }).code, 'PENDING_TASK_EXISTS');
  const replacement = createFor(state, { maskedCallee: '138 **** 1111', idempotencyKey: 'QA-TASK-3', replacePending: true });
  assert.equal(replacement.ok, true);
  assert.notEqual(replacement.value.id, first.id);
  assert.equal(replacement.value.status, 'PENDING');
  const oldTask = state.tasks.find(task => task.id === first.id);
  assert.equal(oldTask.status, 'REPLACED');
  assert.equal(oldTask.reasonCode, 'REPLACED');
  assert.equal(oldTask.expiresAt, first.expiresAt);
  assert.equal(Date.parse(replacement.value.expiresAt) - Date.parse(replacement.value.createdAt), 30000);
  const nonterminal = state.tasks.filter(task => task.normalizedCaller === '18600004471' && ['PENDING', 'CLAIMED'].includes(task.status));
  assert.deepEqual(nonterminal.map(task => task.id), [replacement.value.id]);
});

test('claim boundary is exclusive at expiresAt and never returns a historical callee', () => {
  const beforeExpiry = B.createCallTaskDemoState(NOW);
  createFor(beforeExpiry);
  const claimed = B.resolveIvrInbound(beforeExpiry, {
    callId: 'CALL-QA-BOUNDARY-OK', normalizedCaller: '18600004471', callerTrusted: true,
    callerState: 'ACTIVE', availableSeconds: 300, directAllowed: true,
  }, at(29.999));
  assert.equal(claimed.ok, true);
  assert.equal(claimed.value.action, 'DIAL_TASK_CALLEE');
  assert.equal(claimed.value.maskedCallee, '137 **** 2240');

  for (const seconds of [30, 60]) {
    const state = B.createCallTaskDemoState(NOW);
    const created = createFor(state).value;
    const result = B.resolveIvrInbound(state, {
      callId: `CALL-QA-BOUNDARY-LATE-${seconds}`, normalizedCaller: '18600004471', callerTrusted: true,
      callerState: 'ACTIVE', availableSeconds: 300, directAllowed: true,
    }, at(seconds));
    assert.equal(result.ok, true);
    assert.equal(result.value.action, 'ENTER_CALLEE');
    assert.equal(result.value.maskedCallee, null);
    assert.equal(result.value.taskPath, false);
    assert.equal(state.tasks.find(task => task.id === created.id).status, 'EXPIRED');
    const call = state.calls.find(item => item.id === result.value.callId);
    assert.equal(call.taskId, null);
    assert.equal(call.maskedCallee, null);
  }
});

test('IVR claim is one atomic mutation and repeated resolutions do not re-claim', () => {
  const state = B.createCallTaskDemoState(NOW);
  const task = createFor(state).value;
  const callerCallsBefore = state.calls.filter(item => item.normalizedCaller === '18600004471').length;
  const result = B.resolveIvrInbound(state, {
    callId: 'CALL-QA-TASK-01', normalizedCaller: '18600004471', callerTrusted: true,
    callerState: 'ACTIVE', availableSeconds: 300, directAllowed: true,
  }, at(1));
  assert.equal(result.ok, true);
  assert.equal(result.value.action, 'DIAL_TASK_CALLEE');
  assert.equal(result.value.taskMatch, 'CLAIMED');
  assert.equal(result.value.calleeConfirmation, 'NONE');
  assert.equal(result.value.maskedCallee, task.maskedCallee);
  const claimedTask = state.tasks.find(item => item.id === task.id);
  assert.equal(claimedTask.status, 'CLAIMED');
  assert.equal(claimedTask.claimedCallId, 'CALL-QA-TASK-01');
  const call = state.calls.find(item => item.id === 'CALL-QA-TASK-01');
  assert.equal(call.progress, 'CLAIMED');
  assert.deepEqual(call.events.map(event => event.type), ['INBOUND_RECEIVED', 'TASK_CLAIMED']);
  assert.equal(state.calls.filter(item => item.normalizedCaller === '18600004471').length, callerCallsBefore + 1);
  assert.equal(state.calls.filter(item => item.id === 'CALL-QA-TASK-01').length, 1);
  assert.equal(B.callTaskSnapshot(state).ok, true);

  const afterClaim = raw(state);
  const competing = B.resolveIvrInbound(state, {
    callId: 'CALL-QA-TASK-02', normalizedCaller: '18600004471', callerTrusted: true,
    callerState: 'ACTIVE', availableSeconds: 300, directAllowed: true,
  }, at(2));
  assert.equal(competing.ok, false);
  assert.equal(competing.code, 'REJECT_CALL_IN_PROGRESS');
  assert.deepEqual(state, afterClaim);

  const replay = B.resolveIvrInbound(state, {
    callId: 'CALL-QA-TASK-01', normalizedCaller: '18600004471', callerTrusted: true,
    callerState: 'ACTIVE', availableSeconds: 300, directAllowed: true,
  }, at(2));
  assert.equal(replay.ok, true);
  assert.equal(replay.code, 'DUPLICATE');
  assert.equal(replay.value.action, 'DIAL_TASK_CALLEE');
  assert.deepEqual(state, afterClaim);
  assert.equal(state.tasks.filter(item => item.normalizedCaller === '18600004471').length, 1);
  assert.equal(state.calls.filter(item => item.normalizedCaller === '18600004471').length, callerCallsBefore + 1);
});

test('no-task path enters DTMF confirmation with no task and rejects a task id', () => {
  const state = B.createCallTaskDemoState(NOW);
  const taskCount = state.tasks.length;
  const entered = B.resolveIvrInbound(state, {
    callId: 'CALL-QA-NOTASK', normalizedCaller: '01088886688', callerTrusted: true,
    callerState: 'ACTIVE', availableSeconds: 300, directAllowed: true,
  }, at(1));
  assert.equal(entered.ok, true);
  assert.equal(entered.value.action, 'ENTER_CALLEE');
  assert.equal(entered.value.calleeConfirmation, 'DTMF_REQUIRED');
  assert.equal(entered.value.progress, 'WAITING_CALLEE_INPUT');
  assert.equal(entered.value.taskPath, false);
  assert.equal(entered.value.taskId, null);
  const call = state.calls.find(item => item.id === 'CALL-QA-NOTASK');
  assert.equal(call.taskId, null);
  assert.equal(call.maskedCallee, null);
  assert.equal(call.maskedCaller, '010 **** 6688');
  assert.equal(state.tasks.length, taskCount);

  assertRejectedWithoutMutation(state, () => B.confirmDirectCallee(state, {
    callId: 'CALL-QA-NOTASK', normalizedCaller: '01088886688', maskedCallee: '139 **** 3816',
    eventId: 'CALL-QA-NOTASK-DTMF-BAD', taskId: 'CT-DEMO-0101',
  }, at(2)), 'INVALID_TRANSITION');

  const confirmed = B.confirmDirectCallee(state, {
    callId: 'CALL-QA-NOTASK', normalizedCaller: '01088886688', maskedCallee: '139 **** 3816',
    eventId: 'CALL-QA-NOTASK-DTMF', taskId: null,
  }, at(2));
  assert.equal(confirmed.ok, true);
  assert.equal(confirmed.value.calleeConfirmation, 'DTMF_CONFIRMED');
  assert.equal(confirmed.value.progress, 'CALLEE_CONFIRMED');
  assert.equal(confirmed.value.taskPath, false);
  const directCall = state.calls.find(item => item.id === 'CALL-QA-NOTASK');
  assert.equal(directCall.maskedCallee, '139 **** 3816');
  assert.equal(state.tasks.length, taskCount);

  const beforeReplay = raw(state);
  const replay = B.confirmDirectCallee(state, {
    callId: 'CALL-QA-NOTASK', normalizedCaller: '01088886688', maskedCallee: '139 **** 3816',
    eventId: 'CALL-QA-NOTASK-DTMF', taskId: null,
  }, at(3));
  assert.equal(replay.code, 'DUPLICATE');
  assert.deepEqual(state, beforeReplay);
});

test('current caller state and entitlement are revalidated before any task claim', () => {
  for (const callerState of ['INACTIVE', 'UNREGISTERED']) {
    const state = B.createCallTaskDemoState(NOW);
    const task = createFor(state).value;
    const before = raw(state);
    const result = B.resolveIvrInbound(state, {
      callId: `CALL-QA-${callerState}`, normalizedCaller: '18600004471', callerTrusted: true,
      callerState, availableSeconds: 300, directAllowed: true,
    }, at(1));
    assert.equal(result.ok, true, callerState);
    assert.equal(result.value.action, 'GUIDE_ACTIVATION', callerState);
    assert.equal(result.value.callId, null, callerState);
    assert.equal(result.value.taskId, null, callerState);
    assert.deepEqual(state, before, `${callerState} must not claim, create a call or mutate state`);
    assert.equal(state.tasks.find(item => item.id === task.id).status, 'PENDING', callerState);
    assert.equal(state.calls.some(item => item.id === `CALL-QA-${callerState}`), false, callerState);
  }

  const zeroState = B.createCallTaskDemoState(NOW);
  const zeroTask = createFor(zeroState).value;
  const zeroBefore = raw(zeroState);
  const zero = B.resolveIvrInbound(zeroState, {
    callId: 'CALL-QA-ZERO', normalizedCaller: '18600004471', callerTrusted: true,
    callerState: 'ACTIVE', availableSeconds: 0, directAllowed: true,
  }, at(1));
  assert.equal(zero.ok, true);
  assert.equal(zero.value.action, 'GUIDE_PURCHASE');
  assert.equal(zero.value.callId, null);
  assert.deepEqual(zeroState, zeroBefore, 'Zero seconds must not claim, create a call or mutate state');
  assert.equal(zeroState.tasks.find(item => item.id === zeroTask.id).status, 'PENDING');
  assert.equal(zeroState.calls.some(item => item.id === 'CALL-QA-ZERO'), false);
});

test('task claim requires a unique binding but not direct DTMF allowance', () => {
  const taskState = B.createCallTaskDemoState(NOW);
  const task = createFor(taskState).value;
  const claimed = B.resolveIvrInbound(taskState, {
    callId: 'CALL-QA-TASK-NO-DIRECT', normalizedCaller: '18600004471', callerTrusted: true,
    callerState: 'ACTIVE', availableSeconds: 300, directAllowed: false,
  }, at(1));
  assert.equal(claimed.ok, true);
  assert.equal(claimed.value.action, 'DIAL_TASK_CALLEE');
  assert.equal(claimed.value.taskId, task.id);
  assert.equal(claimed.value.maskedCallee, task.maskedCallee);
  assert.equal(claimed.value.calleeConfirmation, 'NONE');
  assert.equal(taskState.tasks.find(item => item.id === task.id).status, 'CLAIMED');

  const missingState = B.createCallTaskDemoState(NOW);
  assertRejectedWithoutMutation(missingState, () => B.resolveIvrInbound(missingState, {
    callId: 'CALL-QA-NO-BINDING', normalizedCaller: '13700001111', callerTrusted: true,
    callerState: 'ACTIVE', availableSeconds: 300, directAllowed: true,
  }, at(1)), 'REJECT');
  assert.equal(missingState.calls.some(item => item.id === 'CALL-QA-NO-BINDING'), false);

  const ambiguousState = B.createCallTaskDemoState(NOW);
  const ambiguousTask = createFor(ambiguousState).value;
  ambiguousState.callerBindings.push({
    id: 'CB-AMBIGUOUS-4471',
    ownerType: 'PERSONAL_ACCOUNT',
    ownerId: 'ACCOUNT-DEMO-003',
    workspaceId: 'personal:ACCOUNT-DEMO-003',
    maskedCaller: '186 **** 4471',
    normalizedCaller: '18600004471',
    numberType: 'MOBILE',
    active: true,
  });
  assert.equal(B.callTaskSnapshot(ambiguousState).ok, true);
  assertRejectedWithoutMutation(ambiguousState, () => B.resolveIvrInbound(ambiguousState, {
    callId: 'CALL-QA-AMBIGUOUS', normalizedCaller: '18600004471', callerTrusted: true,
    callerState: 'ACTIVE', availableSeconds: 300, directAllowed: true,
  }, at(1)), 'REJECT');
  assert.equal(ambiguousState.tasks.find(item => item.id === ambiguousTask.id).status, 'PENDING');
  assert.equal(ambiguousState.calls.some(item => item.id === 'CALL-QA-AMBIGUOUS'), false);
});

test('SUSPENDED resolves unique ownership before rejecting a pending task', () => {
  const missingState = B.createCallTaskDemoState(NOW);
  assertRejectedWithoutMutation(missingState, () => B.resolveIvrInbound(missingState, {
    callId: 'CALL-QA-SUSPENDED-NO-BINDING', normalizedCaller: '13700001111', callerTrusted: true,
    callerState: 'SUSPENDED', availableSeconds: 300, directAllowed: true,
  }, at(1)), 'REJECT');
  assert.equal(missingState.calls.some(item => item.id === 'CALL-QA-SUSPENDED-NO-BINDING'), false);

  const ambiguousState = B.createCallTaskDemoState(NOW);
  const ambiguousTask = createFor(ambiguousState).value;
  ambiguousState.callerBindings.push({
    id: 'CB-AMBIGUOUS-4471',
    ownerType: 'PERSONAL_ACCOUNT',
    ownerId: 'ACCOUNT-DEMO-003',
    workspaceId: 'personal:ACCOUNT-DEMO-003',
    maskedCaller: '186 **** 4471',
    normalizedCaller: '18600004471',
    numberType: 'MOBILE',
    active: true,
  });
  assert.equal(B.callTaskSnapshot(ambiguousState).ok, true);
  assertRejectedWithoutMutation(ambiguousState, () => B.resolveIvrInbound(ambiguousState, {
    callId: 'CALL-QA-SUSPENDED-AMBIGUOUS', normalizedCaller: '18600004471', callerTrusted: true,
    callerState: 'SUSPENDED', availableSeconds: 300, directAllowed: true,
  }, at(1)), 'REJECT');
  assert.equal(ambiguousState.tasks.find(item => item.id === ambiguousTask.id).status, 'PENDING');
  assert.equal(ambiguousState.calls.some(item => item.id === 'CALL-QA-SUSPENDED-AMBIGUOUS'), false);

  const ownedState = B.createCallTaskDemoState(NOW);
  const ownedTask = createFor(ownedState).value;
  const callsBefore = ownedState.calls.length;
  const suspended = B.resolveIvrInbound(ownedState, {
    callId: 'CALL-QA-SUSPENDED-OWNED', normalizedCaller: '18600004471', callerTrusted: true,
    callerState: 'SUSPENDED', availableSeconds: 300, directAllowed: true,
  }, at(1));
  assert.equal(suspended.ok, true);
  assert.equal(suspended.code, 'REJECT_SUSPENDED');
  assert.equal(suspended.value.action, 'REJECT_SUSPENDED');
  assert.equal(suspended.value.taskRejected, true);
  assert.equal(suspended.value.callId, null);
  assert.equal(suspended.value.maskedCallee, null);
  assert.equal(ownedState.tasks.find(item => item.id === ownedTask.id).status, 'REJECTED');
  assert.equal(ownedState.tasks.find(item => item.id === ownedTask.id).reasonCode, 'SUSPENDED');
  assert.equal(ownedState.tasks.find(item => item.id === 'CT-DEMO-0201').status, 'PENDING');
  assert.equal(ownedState.calls.length, callsBefore);
  assert.equal(ownedState.calls.some(item => item.id === 'CALL-QA-SUSPENDED-OWNED'), false);
  assert.equal(B.callTaskSnapshot(ownedState).ok, true);

  const noTaskState = B.createCallTaskDemoState(NOW);
  const noTaskBefore = raw(noTaskState);
  const noTask = B.resolveIvrInbound(noTaskState, {
    callId: 'CALL-QA-SUSPENDED-NO-TASK', normalizedCaller: '01088886688', callerTrusted: true,
    callerState: 'SUSPENDED', availableSeconds: 300, directAllowed: true,
  }, at(1));
  assert.equal(noTask.ok, true);
  assert.equal(noTask.code, 'REJECT_SUSPENDED');
  assert.equal(noTask.value.taskRejected, false);
  assert.equal(noTask.value.callId, null);
  assert.equal(noTask.value.maskedCallee, null);
  assert.deepEqual(noTaskState, noTaskBefore);
  assert.equal(noTaskState.calls.some(item => item.id === 'CALL-QA-SUSPENDED-NO-TASK'), false);
});

test('no-task path requires direct DTMF allowance and creates no call when disabled', () => {
  const state = B.createCallTaskDemoState(NOW);
  const before = raw(state);
  const purchase = B.resolveIvrInbound(state, {
    callId: 'CALL-QA-NOTASK-NO-DIRECT', normalizedCaller: '01088886688', callerTrusted: true,
    callerState: 'ACTIVE', availableSeconds: 300, directAllowed: false,
  }, at(1));
  assert.equal(purchase.ok, true);
  assert.equal(purchase.value.action, 'GUIDE_PURCHASE');
  assert.equal(purchase.value.callId, null);
  assert.deepEqual(state, before);
  assert.equal(state.calls.some(item => item.id === 'CALL-QA-NOTASK-NO-DIRECT'), false);
});

test('call facts are append-only, ordered, idempotent and gate evidence on upload', () => {
  const state = claimedState();
  assertRejectedWithoutMutation(state, () => B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-RING-EARLY', type: 'RINGING', at: at(2).toISOString(),
  }, at(2)), 'INVALID_TRANSITION');
  assertRejectedWithoutMutation(state, () => B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-EVIDENCE-EARLY', type: 'EVIDENCE_READY', at: at(2).toISOString(),
  }, at(2)), 'INVALID_TRANSITION');

  const started = B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-OUTBOUND', type: 'OUTBOUND_STARTED', at: at(2).toISOString(),
  }, at(2));
  assert.equal(started.ok, true);
  const afterStart = raw(state);
  const duplicate = B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-OUTBOUND', type: 'OUTBOUND_STARTED', at: at(2).toISOString(),
  }, at(3));
  assert.equal(duplicate.code, 'DUPLICATE');
  assert.deepEqual(state, afterStart);
  assertRejectedWithoutMutation(state, () => B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-OUTBOUND', type: 'RINGING', at: at(2).toISOString(),
  }, at(3)), 'INVALID_TRANSITION');

  for (const [index, type] of ['RINGING', 'ANSWERED', 'BRIDGED', 'RECORDING_STARTED'].entries()) {
    const result = B.recordCallFact(state, {
      callId: 'CALL-QA-TASK-01', eventId: `CALL-QA-EVT-${type}`, type, at: at(3 + index).toISOString(),
    }, at(3 + index));
    assert.equal(result.ok, true, type);
  }
  assertRejectedWithoutMutation(state, () => B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-TIME-REGRESSION', type: 'CALL_ENDED',
    at: at(2).toISOString(), recordingSeconds: 60,
  }, at(2)), 'INVALID_TRANSITION');
  assert.equal(B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-ENDED', type: 'CALL_ENDED',
    at: at(10).toISOString(), recordingSeconds: 60,
  }, at(10)).ok, true);
  assert.equal(state.tasks.find(task => task.claimedCallId === 'CALL-QA-TASK-01').status, 'COMPLETED');
  assert.equal(B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-RECEIVED', type: 'RECORDING_RECEIVED', at: at(11).toISOString(),
  }, at(11)).ok, true);
  assert.equal(B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-EVIDENCE-PROCESSING', type: 'EVIDENCE_PROCESSING', at: at(12).toISOString(),
  }, at(12)).ok, true);
  const ready = B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-EVIDENCE-READY', type: 'EVIDENCE_READY', at: at(13).toISOString(),
  }, at(13));
  assert.equal(ready.ok, true);
  const call = state.calls.find(item => item.id === 'CALL-QA-TASK-01');
  assert.equal(call.progress, 'EVIDENCE_READY');
  assert.equal(call.uploadStatus, 'RECEIVED');
  assert.equal(call.evidenceStatus, 'AVAILABLE');
  assert.equal(call.recordingSeconds, 60);
  assert.equal(B.callTaskSnapshot(state).ok, true);
});

test('pre-bridge failure leaves zero recording, no upload and no evidence', () => {
  const state = claimedState();
  assert.equal(B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-OUTBOUND', type: 'OUTBOUND_STARTED', at: at(2).toISOString(),
  }, at(2)).ok, true);
  assert.equal(B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-RINGING', type: 'RINGING', at: at(3).toISOString(),
  }, at(3)).ok, true);
  const failed = B.recordCallFact(state, {
    callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-FAILED', type: 'CALL_FAILED', at: at(4).toISOString(), detail: 'NO_ANSWER', recordingSeconds: 0,
  }, at(4));
  assert.equal(failed.ok, true);
  const call = state.calls.find(item => item.id === 'CALL-QA-TASK-01');
  assert.equal(call.progress, 'FAILED');
  assert.equal(call.endReason, 'NO_ANSWER');
  assert.equal(call.recordingSeconds, 0);
  assert.equal(call.uploadStatus, 'NOT_STARTED');
  assert.equal(call.evidenceStatus, 'NOT_CREATED');
  const task = state.tasks.find(item => item.claimedCallId === 'CALL-QA-TASK-01');
  assert.equal(task.status, 'FAILED');
  assert.equal(task.reasonCode, 'NO_ANSWER');
  assert.equal(B.callTaskSnapshot(state).ok, true);
});

test('snapshot import rejects duplicate nonterminal tasks, frozen changes, event order and forbidden fields atomically', () => {
  const pendingState = B.createCallTaskDemoState(NOW);
  createFor(pendingState);
  const pendingSnapshot = B.callTaskSnapshot(pendingState).value;
  const pendingTask = pendingSnapshot.tasks.find(task => task.normalizedCaller === '18600004471' && task.status === 'PENDING');

  const duplicateTask = copy(pendingSnapshot);
  const copyTask = copy(pendingTask);
  copyTask.id = 'CT-DUPLICATE';
  copyTask.idempotencyKey = 'IDEM-DUPLICATE';
  duplicateTask.tasks.push(copyTask);

  const frozenExpiry = copy(pendingSnapshot);
  frozenExpiry.tasks.find(task => task.id === pendingTask.id).expiresAt = new Date(Date.parse(pendingTask.expiresAt) + 1).toISOString();

  const claimed = claimedState();
  B.recordCallFact(claimed, { callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-OUTBOUND', type: 'OUTBOUND_STARTED', at: at(2).toISOString() }, at(2));
  B.recordCallFact(claimed, { callId: 'CALL-QA-TASK-01', eventId: 'CALL-QA-EVT-RINGING', type: 'RINGING', at: at(3).toISOString() }, at(3));
  const claimedSnapshot = B.callTaskSnapshot(claimed).value;

  const reversedEvents = copy(claimedSnapshot);
  reversedEvents.calls.find(call => call.id === 'CALL-QA-TASK-01').events.reverse();

  const forbidden = Object.assign(copy(claimedSnapshot), { employees: [] });
  const extraCallField = copy(claimedSnapshot);
  extraCallField.calls.find(call => call.id === 'CALL-QA-TASK-01').ownerId = ACCOUNT;

  for (const [label, candidate] of [
    ['duplicate nonterminal task', duplicateTask],
    ['frozen expiry', frozenExpiry],
    ['reversed event order', reversedEvents],
    ['forbidden top-level field', forbidden],
    ['unknown call field', extraCallField],
  ]) {
    assert.equal(B.callTaskSnapshot(candidate).ok, false, label);
    const target = B.createCallTaskDemoState(NOW);
    const before = raw(target);
    const result = B.loadCallTaskSnapshot(target, candidate);
    assert.equal(result.ok, false, label);
    assert.deepEqual(target, before, label);
  }
});

test('business and call-task snapshots stay independent in both import directions', () => {
  const business = B.createDemoState(NOW);
  const callTask = B.createCallTaskDemoState(NOW);
  createFor(callTask);
  const businessBefore = raw(business);
  const callTaskCandidate = B.callTaskSnapshot(callTask).value;
  const callTaskTarget = B.createCallTaskDemoState(NOW);
  assert.equal(B.loadCallTaskSnapshot(callTaskTarget, callTaskCandidate).ok, true);
  assert.deepEqual(business, businessBefore);
  assert.equal(Object.hasOwn(callTaskCandidate, 'catalog'), false);
  assert.equal(Object.hasOwn(callTaskCandidate, 'orders'), false);
  assert.equal(Object.hasOwn(callTaskCandidate, 'tasks'), true);

  const businessCandidate = B.snapshot(business).value;
  assert.equal(Object.hasOwn(businessCandidate, 'tasks'), false);
  assert.equal(Object.hasOwn(businessCandidate, 'callerBindings'), false);
  const callTaskBefore = raw(callTask);
  const businessTarget = B.createDemoState(NOW);
  assert.equal(B.loadReviewSnapshot(businessTarget, businessCandidate).ok, true);
  assert.deepEqual(callTask, callTaskBefore);
  const businessWithTasks = Object.assign(copy(businessCandidate), { tasks: [] });
  assert.equal(B.loadReviewSnapshot(B.createDemoState(NOW), businessWithTasks).ok, false);
});
