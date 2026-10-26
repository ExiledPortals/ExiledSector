const A = require('./art.js');
const out = __dirname + '/../../graphics/unused/item_art_sheets/';
require('fs').mkdirSync(out, { recursive: true });

const NC_MOD = 'C:/Games/Starsector/mods/Nightcross/graphics/';
const NC = ['#03050c', '#0e1a44', '#2547c4', '#79c6ff', '#f2f8ff'];
const elyurias = A.load(NC_MOD + 'ships/na/elyurias/elyurias.png');
const tempus = A.load(NC_MOD + 'ships/na/tempusspatium/na_tempus.png');
const saturation = A.load(NC_MOD + 'weapons/na_hardfluxcannon_turret.png');
const saturationGlow = A.load(NC_MOD + 'weapons/na_hardfluxcannon_turret_glow.png');
const bolter = A.load(NC_MOD + 'weapons/na_pulsecannon_turret_base.png');
const bolterGlow = A.load(NC_MOD + 'weapons/na_pulsecannon_turret_glow.png');
const logo = A.load(NC_MOD + 'icons/nightcross_icon.png');

function base(seed, cx = 0.5, cy = 0.5) {
  const cv = A.canvas();
  A.background(cv, NC, { seed, cx, cy, glow: 0.6, stars: 45 });
  return cv;
}

function lightLayer(cv, img, opts) {
  const lit = { ...img, data: Float32Array.from(img.data) };
  for (let i = 0; i < lit.data.length; i += 4) {
    const lum = 0.299 * lit.data[i] + 0.587 * lit.data[i + 1] + 0.114 * lit.data[i + 2];
    lit.data[i + 3] *= lum;
  }
  return A.sprite(cv, lit, opts);
}

const aria = [];
{
  const cv = base(41, 0.5, 0.5);
  A.sprite(cv, saturation, { cx: 128, cy: 132, scale: 5.6, palette: NC, keep: 0.35, contrast: 1.2, glow: { radius: 12, color: '#2a58e0', strength: 0.6 } });
  lightLayer(cv, saturationGlow, { cx: 128, cy: 132, scale: 5.6, alpha: 0.9 });
  A.vignette(cv, 0.55);
  aria.push(cv);
}
{
  const cv = base(42, 0.4, 0.45);
  A.sprite(cv, elyurias, { crop: { x: 0, y: 30, w: 130, h: 110 }, feather: 14, cx: 118, cy: 128, scale: 2.0, angle: 0.2, palette: NC, keep: 0.45, contrast: 1.15,
    glow: { radius: 10, color: '#2a58e0', strength: 0.45 } });
  A.lightPath(cv, [[30, 196], [90, 168], [150, 150], [226, 132]], { width: 2.2, color: '#7fd4ff', intensity: 0.8 });
  A.lightPath(cv, [[40, 220], [110, 190], [170, 178], [232, 166]], { width: 1.6, color: '#b8e8ff', intensity: 0.6 });
  A.vignette(cv, 0.55);
  aria.push(cv);
}
{
  const cv = base(43, 0.5, 0.55);
  A.sprite(cv, saturation, { cx: 128, cy: 140, scale: 4.2, angle: 0.55, palette: NC, keep: 0.35, contrast: 1.2, glow: { radius: 10, color: '#2a58e0', strength: 0.55 } });
  lightLayer(cv, saturationGlow, { cx: 128, cy: 140, scale: 4.2, angle: 0.55, alpha: 0.9 });
  A.lightPath(cv, A.arc(128, 140, 92, -2.6, -0.4), { width: 2, color: '#79c6ff', intensity: 0.45 });
  A.vignette(cv, 0.55);
  aria.push(cv);
}
A.sheet(aria, out + 'sheet_aria.png');

const ion = [];
function arcs(cv, cx, cy, seed, reach) {
  [[-1.2, 0.9], [0.1, 1], [1.4, 0.8], [2.6, 0.7]].forEach(([a, f], k) => {
    const pts = A.bolt(cx, cy, cx + Math.cos(a) * reach * f, cy + Math.sin(a) * reach * f, seed + k * 7, 14, 5);
    A.lightPath(cv, pts, { width: 3.2, color: '#3a6cff', intensity: 0.5 });
    A.lightPath(cv, pts, { width: 1.1, color: '#e8f6ff', intensity: 0.95 });
  });
}
{
  const cv = base(51, 0.5, 0.5);
  A.sprite(cv, bolter, { cx: 128, cy: 136, scale: 5.6, palette: NC, keep: 0.35, contrast: 1.2, glow: { radius: 12, color: '#2a58e0', strength: 0.55 } });
  lightLayer(cv, bolterGlow, { cx: 128, cy: 136, scale: 5.6, alpha: 1 });
  arcs(cv, 128, 70, 5, 70);
  A.vignette(cv, 0.55);
  ion.push(cv);
}
{
  const cv = base(52, 0.5, 0.5);
  for (let k = 0; k < 5; k++) {
    const y = 80 + k * 24;
    A.lightPath(cv, A.arc(128, y, 46, 0, Math.PI), { width: 4, color: '#1d3a9a', intensity: 0.9 });
    A.lightPath(cv, A.arc(128, y, 46, 0, Math.PI), { width: 1.4, color: '#a8dcff', intensity: 0.9 });
  }
  arcs(cv, 128, 128, 9, 90);
  A.glowDot(cv, 128, 128, 18, A.hex('#79c6ff'), 0.7);
  A.vignette(cv, 0.6);
  ion.push(cv);
}
{
  const cv = base(53, 0.5, 0.6);
  A.sprite(cv, bolter, { cx: 128, cy: 160, scale: 4.2, angle: -0.4, palette: NC, keep: 0.35, contrast: 1.2, glow: { radius: 10, color: '#2a58e0', strength: 0.5 } });
  lightLayer(cv, bolterGlow, { cx: 128, cy: 160, scale: 4.2, angle: -0.4, alpha: 1 });
  arcs(cv, 104, 104, 13, 95);
  A.vignette(cv, 0.55);
  ion.push(cv);
}
A.sheet(ion, out + 'sheet_ion.png');

const veil = [];
function veilOver(cv, layer, intensity) {
  A.hexLattice(cv, { size: 15, color: '#8fd8ff', intensity, width: 0.6, mask: (x, y) => {
    let m = 0;
    for (const [dx, dy] of [[0, 0], [6, 0], [-6, 0], [0, 6], [0, -6]]) {
      const xx = x + dx, yy = y + dy;
      if (xx >= 0 && yy >= 0 && xx < cv.w && yy < cv.h) m = Math.max(m, layer[(yy * cv.w + xx) * 4 + 3]);
    }
    return m;
  } });
}
{
  const cv = base(61, 0.5, 0.5);
  const layer = A.sprite(cv, elyurias, { cx: 128, cy: 130, scale: 0.78, palette: NC, keep: 0.4, contrast: 1.1, glow: { radius: 12, color: '#3a7cff', strength: 0.6 } });
  veilOver(cv, layer, 0.3);
  A.vignette(cv, 0.55);
  veil.push(cv);
}
{
  const cv = base(62, 0.5, 0.5);
  const layer = A.sprite(cv, tempus, { cx: 128, cy: 132, scale: 1.0, palette: NC, keep: 0.4, contrast: 1.1, glow: { radius: 12, color: '#3a7cff', strength: 0.6 } });
  veilOver(cv, layer, 0.3);
  A.vignette(cv, 0.55);
  veil.push(cv);
}
{
  const cv = base(63, 0.5, 0.5);
  const layer = A.sprite(cv, tempus, { crop: { x: 20, y: 40, w: 160, h: 150 }, feather: 14, cx: 128, cy: 128, scale: 1.55, palette: NC, keep: 0.4,
    contrast: 1.1, glow: { radius: 10, color: '#3a7cff', strength: 0.5 } });
  veilOver(cv, layer, 0.32);
  A.vignette(cv, 0.55);
  veil.push(cv);
}
A.sheet(veil, out + 'sheet_veil.png');

const warranty = [];
function seal(cv, cx, cy, r, scale, strength) {
  lightLayer(cv, logo, { crop: { x: 22, y: 22, w: 136, h: 136 }, feather: 10, cx, cy, scale, palette: NC, keep: 0, contrast: 1.4, lift: 0.15, alpha: strength });
  A.lightPath(cv, A.arc(cx, cy, r, 0, Math.PI * 2, 96), { width: 2.2, color: '#79c6ff', intensity: 0.7 * strength });
  A.lightPath(cv, A.arc(cx, cy, r + 9, 0, Math.PI * 2, 96), { width: 1, color: '#c8ecff', intensity: 0.5 * strength });
}
{
  const cv = base(71, 0.5, 0.5);
  seal(cv, 128, 128, 84, 0.7, 1);
  A.vignette(cv, 0.6);
  warranty.push(cv);
}
{
  const cv = base(72, 0.5, 0.45);
  for (let k = 0; k < 7; k++) {
    const y = 58 + k * 18, len = k === 6 ? 70 : 150 - (k % 3) * 18;
    A.lightPath(cv, [[52, y], [52 + len, y]], { width: 1.4, color: '#5a86d8', intensity: 0.55 });
  }
  seal(cv, 176, 182, 44, 0.36, 1);
  A.vignette(cv, 0.55);
  warranty.push(cv);
}
{
  const cv = base(73, 0.5, 0.5);
  seal(cv, 128, 120, 70, 0.55, 0.85);
  for (let k = 0; k < 3; k++) {
    A.lightPath(cv, [[60, 214 + k * 10], [196 - k * 22, 214 + k * 10]], { width: 1.2, color: '#5a86d8', intensity: 0.5 });
  }
  A.vignette(cv, 0.6);
  warranty.push(cv);
}
A.sheet(warranty, out + 'sheet_warranty.png');

if (process.argv[2] === 'save') {
  const dest = __dirname + '/../../graphics/icons/socketables/';
  const [a, i, v, w] = process.argv.slice(3).map(Number);
  A.save(aria[a], dest + 'na_project_aria_retrofit.png');
  A.save(ion[i], dest + 'na_ion_discharge_coils.png');
  A.save(veil[v], dest + 'na_hardlight_veil_projectors.png');
  A.save(warranty[w], dest + 'na_extended_warranty_contract.png');
}
