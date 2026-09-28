#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
node <<'JS'
const fs = require('node:fs');
const activity = fs.readFileSync('app/src/main/java/com/yamone/arcade2/MainActivity.java', 'utf8');
const art = fs.readFileSync('app/src/main/java/com/yamone/arcade2/ui/ArcadeArt.java', 'utf8');
const color = name => parseInt(activity.match(new RegExp('\\b' + name + ' = 0xFF([0-9A-F]{6})'))[1], 16);
const luminance = n => {
  const c = [n >> 16 & 255, n >> 8 & 255, n & 255].map(v => { v /= 255; return v <= .04045 ? v / 12.92 : ((v + .055) / 1.055) ** 2.4; });
  return c[0] * .2126 + c[1] * .7152 + c[2] * .0722;
};
function check(label, foreground, background) {
  const a = luminance(foreground), b = luminance(background);
  const ratio = (Math.max(a, b) + .05) / (Math.min(a, b) + .05);
  if (ratio < 4.5) throw new Error(label + ' contrast below 4.5: ' + ratio);
  console.log('PASS ' + label + ' contrast ' + ratio.toFixed(2));
}
check('body', color('TEXT'), color('BG'));
check('secondary', color('MUTED'), 0xECE3FF);
check('primary button', color('PANEL'), color('MINT'));
check('destructive button', color('PINK'), 0xFFEDF1);
const accents = [...art.matchAll(/case ([A-Z_]+) -> 0xFF([0-9A-F]{6});/g)];
if (accents.length !== 12) throw new Error('Expected six game accents and six tints');
for (let i = 0; i < 6; i++) check(accents[i][1], parseInt(accents[i][2], 16), parseInt(accents[i + 6][2], 16));
console.log('All 10 menu contrast checks passed.');
JS
