const A = require('./art.js');
const out = __dirname + '/../../graphics/unused/item_art_sheets/';
require('fs').mkdirSync(out, { recursive: true });

const TANIA_MOD = 'E:/Dev/Starsector/mods/Navarchy of Tania - 1.3.0/graphics/';
const TANIA = ['#060a0c', '#16302f', '#3a7470', '#c27a4c', '#f6e4c8'];
const hawk = A.load(TANIA_MOD + 'ships/black_hawk/tania_black_hawk_main.png');
const fafnirTurret = A.load(TANIA_MOD + 'weapons/tania_fafnir_hvartillery_turret.png');
const fafnirHardpoint = A.load(TANIA_MOD + 'weapons/tania_fafnir_hvartillery_hardpoint.png');
const fafnirShell = A.load(TANIA_MOD + 'missiles/tania_fafnir_large.png');
console.log('hawk', hawk.w, hawk.h);

function base(seed, cx = 0.5, cy = 0.5) {
  const cv = A.canvas();
  A.background(cv, TANIA, { seed, cx, cy, glow: 0.6 });
  return cv;
}

function shellStreak(cv, x, y, angle, length, scale) {
  const dx = Math.cos(angle), dy = Math.sin(angle);
  A.lightPath(cv, [[x - dx * length, y - dy * length], [x, y]], { width: 2.2 * scale, color: '#e89a5a', intensity: 0.55 });
  A.lightPath(cv, [[x - dx * length * 0.5, y - dy * length * 0.5], [x, y]], { width: 1.1 * scale, color: '#fff2dc', intensity: 0.7 });
  A.sprite(cv, fafnirShell, { cx: x, cy: y, scale: scale * 3.2, angle: angle + Math.PI / 2, palette: TANIA, keep: 0.35, contrast: 1.3, lift: 0.12,
    glow: { radius: 6, color: '#f0a060', strength: 0.9 } });
}

const fafnir = [];
{
  const cv = base(11, 0.5, 0.62);
  A.sprite(cv, fafnirTurret, { cx: 128, cy: 150, scale: 2.9, angle: -0.35, palette: TANIA, keep: 0.25, contrast: 1.25,
    glow: { radius: 10, color: '#3a8c86', strength: 0.55 } });
  shellStreak(cv, 186, 46, -1.92, 70, 1);
  A.vignette(cv, 0.55);
  fafnir.push(cv);
}
{
  const cv = base(12, 0.5, 0.5);
  [[88, 140, -0.12], [128, 128, 0], [168, 140, 0.12]].forEach(([x, y, a]) =>
    A.sprite(cv, fafnirShell, { cx: x, cy: y, scale: 7.5, angle: a, palette: TANIA, keep: 0.3, contrast: 1.35, lift: 0.08,
      glow: { radius: 9, color: '#d88850', strength: 0.6 } }));
  A.vignette(cv, 0.6);
  fafnir.push(cv);
}
{
  const cv = base(13, 0.5, 0.35);
  A.sprite(cv, fafnirHardpoint, { cx: 128, cy: 160, scale: 3.1, palette: TANIA, keep: 0.25, contrast: 1.2,
    glow: { radius: 10, color: '#3a8c86', strength: 0.5 } });
  A.glowDot(cv, 128, 52, 10, A.hex('#f3b070'), 0.9);
  shellStreak(cv, 128, 30, -Math.PI / 2, 40, 0.9);
  A.vignette(cv, 0.55);
  fafnir.push(cv);
}
A.sheet(fafnir, out + 'sheet_fafnir.png');

const breech = [];
{
  const cv = base(21, 0.45, 0.5);
  A.sprite(cv, hawk, { crop: { x: 0, y: Math.round(hawk.h * 0.48), w: hawk.w, h: Math.round(hawk.h * 0.26) }, cx: 128, cy: 128, scale: 1.25, feather: 14,
    palette: TANIA, keep: 0.45, contrast: 1.15, glow: { radius: 8, color: '#2f6f6a', strength: 0.4 } });
  A.vignette(cv, 0.6);
  breech.push(cv);
}
{
  const cv = base(22, 0.5, 0.5);
  A.sprite(cv, hawk, { crop: { x: 0, y: Math.round(hawk.h * 0.4), w: Math.round(hawk.w * 0.55), h: Math.round(hawk.h * 0.3) }, cx: 140, cy: 128, scale: 1.8, feather: 16,
    angle: 0.5, palette: TANIA, keep: 0.4, contrast: 1.2, glow: { radius: 8, color: '#2f6f6a', strength: 0.4 } });
  A.vignette(cv, 0.6);
  breech.push(cv);
}
{
  const cv = base(23, 0.5, 0.55);
  A.sprite(cv, hawk, { cx: 128, cy: 132, scale: 0.5, angle: 0.62, palette: TANIA, keep: 0.5, contrast: 1.1,
    glow: { radius: 10, color: '#2f6f6a', strength: 0.5 } });
  A.vignette(cv, 0.55);
  breech.push(cv);
}
A.sheet(breech, out + 'sheet_breech.png');

const lowArc = [];
function shieldArc(cv, cx, cy, r, half, strength) {
  A.lightPath(cv, A.arc(cx, cy, r, -Math.PI / 2 - half, -Math.PI / 2 + half), { width: 7, color: '#3fa7a0', intensity: 0.55 * strength });
  A.lightPath(cv, A.arc(cx, cy, r, -Math.PI / 2 - half, -Math.PI / 2 + half), { width: 2.4, color: '#e8fff6', intensity: 0.9 * strength });
}
{
  const cv = base(31, 0.5, 0.7);
  A.sprite(cv, hawk, { crop: { x: 0, y: 0, w: hawk.w, h: Math.round(hawk.h * 0.3) }, feather: 12, cx: 128, cy: 190, scale: 1.05, palette: TANIA, keep: 0.45,
    contrast: 1.1, glow: { radius: 8, color: '#2f6f6a', strength: 0.4 } });
  shieldArc(cv, 128, 175, 120, 0.42, 1);
  A.glowDot(cv, 128, 128, 14, A.hex('#7fe0d0'), 0.55);
  A.vignette(cv, 0.55);
  lowArc.push(cv);
}
{
  const cv = base(32, 0.5, 0.5);
  A.glowDot(cv, 128, 150, 26, A.hex('#3fa7a0'), 0.8);
  A.glowDot(cv, 128, 150, 9, A.hex('#f6fff8'), 1);
  shieldArc(cv, 128, 150, 70, 0.55, 1.1);
  shieldArc(cv, 128, 150, 92, 0.42, 0.55);
  A.vignette(cv, 0.6);
  lowArc.push(cv);
}
{
  const cv = base(33, 0.5, 0.6);
  A.sprite(cv, hawk, { cx: 128, cy: 230, scale: 0.62, palette: TANIA, keep: 0.45, contrast: 1.1, glow: { radius: 8, color: '#2f6f6a', strength: 0.4 } });
  shieldArc(cv, 128, 150, 96, 0.36, 1.1);
  A.vignette(cv, 0.55);
  lowArc.push(cv);
}
A.sheet(lowArc, out + 'sheet_low_arc.png');

module.exports = { fafnir, breech, lowArc };
if (process.argv[2] === 'save') {
  const dest = __dirname + '/../../graphics/icons/socketables/';
  const [f, b, l] = process.argv.slice(3).map(Number);
  A.save(fafnir[f], dest + 'tania_fafnir_osmium_shell_stock.png');
  A.save(breech[b], dest + 'tania_black_hawk_breech_refit.png');
  A.save(lowArc[l], dest + 'tania_low_arc_shield_emitter_core.png');
}
