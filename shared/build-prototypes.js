'use strict';

const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');

const START = '<!-- ONECALL_BUSINESS:START -->';
const END = '<!-- ONECALL_BUSINESS:END -->';
const root = path.resolve(__dirname, '..');
const modulePath = path.join(__dirname, 'onecall-business.js');
const targets = [
  path.join(root, 'operations-prototype', 'onecall-operations-admin.html'),
  path.join(root, 'prototype', 'onecall-miniapp-pages.html'),
  path.join(root, 'web-prototype', 'evidence-hub.html'),
];

function occurrences(value, needle) {
  return value.split(needle).length - 1;
}

function generatedSection(eol) {
  const moduleSource = fs.readFileSync(modulePath, 'utf8').replace(/<\/script/gi, '<\\/script');
  const normalized = moduleSource.replace(/\r\n|\r|\n/g, eol).replace(new RegExp(`${eol}$`), '');
  const hash = crypto.createHash('sha256').update(fs.readFileSync(modulePath)).digest('hex');
  return [
    START,
    `<script data-onecall-business-sha256="${hash}">`,
    normalized,
    '</script>',
    END,
  ].join(eol);
}

function buildTarget(filePath, checkOnly) {
  const current = fs.readFileSync(filePath, 'utf8');
  if (occurrences(current, START) !== 1 || occurrences(current, END) !== 1) {
    throw new Error(`${path.relative(root, filePath)} must contain exactly one generated marker pair`);
  }
  const start = current.indexOf(START);
  const end = current.indexOf(END, start);
  if (end < start) throw new Error(`${path.relative(root, filePath)} has reversed generated markers`);
  const eol = current.includes('\r\n') ? '\r\n' : '\n';
  const expected = current.slice(0, start) + generatedSection(eol) + current.slice(end + END.length);
  if (expected === current) return false;
  if (checkOnly) throw new Error(`${path.relative(root, filePath)} has stale ONECALL business rules; run node shared/build-prototypes.js`);
  fs.writeFileSync(filePath, expected, 'utf8');
  return true;
}

function main(argv) {
  const unknown = argv.filter(argument => argument !== '--check');
  if (unknown.length) throw new Error(`Unknown argument: ${unknown.join(', ')}`);
  const checkOnly = argv.includes('--check');
  let changed = 0;
  for (const target of targets) if (buildTarget(target, checkOnly)) changed += 1;
  process.stdout.write(checkOnly ? 'ONECALL business sections are current.\n' : `Updated ${changed} ONECALL business section(s).\n`);
}

if (require.main === module) {
  try {
    main(process.argv.slice(2));
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 1;
  }
}

module.exports = { START, END, targets, generatedSection, buildTarget };
