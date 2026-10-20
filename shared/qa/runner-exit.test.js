'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const test = require('node:test');

test('Web QA runner propagates a controlled Node failure', () => {
  const temporaryRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'onecall-runner-exit-'));
  const failingScript = path.join(temporaryRoot, 'expected-failure.js');
  try {
    fs.writeFileSync(failingScript, "process.stderr.write('EXPECTED_QA_FAILURE\\n'); process.exit(23);\n", 'utf8');
    const runner = path.resolve(__dirname, '..', '..', 'web-prototype', 'qa', 'run.ps1');
    const result = spawnSync('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', runner, '-QaScriptPath', failingScript], {
      cwd: path.resolve(__dirname, '..', '..'),
      encoding: 'utf8',
      windowsHide: true,
    });
    assert.notEqual(result.status, 0, `runner unexpectedly passed: ${result.stdout}\n${result.stderr}`);
    assert.match(`${result.stdout}\n${result.stderr}`, /EXPECTED_QA_FAILURE/);
    assert.match(`${result.stdout}\n${result.stderr}`, /Web prototype QA failed \(exit 23\)/);
  } finally {
    fs.rmSync(temporaryRoot, { recursive: true, force: true });
  }
});
