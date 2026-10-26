const A = require('./art.js');
const out = __dirname + '/../../graphics/unused/item_art_sheets/';
require('fs').mkdirSync(out, { recursive: true });

const NC_MOD = 'C:/Games/Starsector/mods/Nightcross/graphics/';
const STARGAZER = ['#05020a', '#24093f', '#6a2ea6', '#e0609a', '#fde8f4'];
const corrosion = A.load(NC_MOD + 'weapons/na_corrosionbeam_turret_base.png');
const corrosionGlow = A.load(NC_MOD + 'weapons/na_corrosionbeam_turret_glow.png');
const fossa = A.load(NC_MOD + 'ships/naai/fossamagura/na_fossa.png');
const tessera = A.load(NC_MOD + 'ships/naai/magutessera/na_tessera.png');

function base(seed, cx = 0.5, cy = 0.5) {
  const cv = A.canvas();
  A.background(cv, STARGAZER, { seed, cx, cy, glow: 0.62, stars: 70, nebula: 0.5 });
  return cv;
}

function lightLayer(cv, img, opts) {
  const lit = { ...img, data: Float32Array.from(img.data) };
  for (let i = 0; i < lit.data.length; i += 4) {
    lit.data[i + 3] *= 0.299 * lit.data[i] + 0.587 * lit.data[i + 1] + 0.114 * lit.data[i + 2];
  }
  return A.sprite(cv, lit, opts);
}

function lens(cv, cx, cy, r, strength) {
  A.lightPath(cv, A.arc(cx, cy, r, 0, Math.PI * 2, 96), { width: 4, color: '#a43cc8', intensity: 0.5 * strength });
  A.lightPath(cv, A.arc(cx, cy, r, 0, Math.PI * 2, 96), { width: 1.3, color: '#ffd8f0', intensity: 0.8 * strength });
  A.lightPath(cv, A.arc(cx, cy, r * 0.62, -2.2, -0.9), { width: 1.2, color: '#ffe8f6', intensity: 0.5 * strength });
}

function beam(cv, x0, y0, x1, y1) {
  A.lightPath(cv, [[x0, y0], [x1, y1]], { width: 7, color: '#c03a7a', intensity: 0.55 });
  A.lightPath(cv, [[x0, y0], [x1, y1]], { width: 2, color: '#ffe0ee', intensity: 0.9 });
}

const lensSheet = [];
{
  const cv = base(81, 0.5, 0.35);
  beam(cv, 128, 96, 128, -10);
  A.sprite(cv, corrosion, { cx: 128, cy: 168, scale: 2.4, palette: STARGAZER, keep: 0.35, contrast: 1.2, glow: { radius: 10, color: '#7a2eb0', strength: 0.55 } });
  lightLayer(cv, corrosionGlow, { cx: 128, cy: 168, scale: 2.4, alpha: 1 });
  lens(cv, 128, 74, 30, 1);
  A.vignette(cv, 0.55);
  lensSheet.push(cv);
}
{
  const cv = base(82, 0.5, 0.5);
  lens(cv, 128, 128, 74, 1.1);
  lens(cv, 128, 128, 52, 0.6);
  beam(cv, 40, 216, 128, 128);
  A.lightPath(cv, [[128, 128], [230, 26]], { width: 9, color: '#e0609a', intensity: 0.5 });
  A.lightPath(cv, [[128, 128], [230, 26]], { width: 3, color: '#fff0f6', intensity: 0.9 });
  A.vignette(cv, 0.6);
  lensSheet.push(cv);
}
{
  const cv = base(83, 0.62, 0.38);
  beam(cv, 150, 90, 238, 6);
  A.sprite(cv, corrosion, { cx: 110, cy: 150, scale: 2.1, angle: 0.8, palette: STARGAZER, keep: 0.35, contrast: 1.2, glow: { radius: 10, color: '#7a2eb0', strength: 0.55 } });
  lightLayer(cv, corrosionGlow, { cx: 110, cy: 150, scale: 2.1, angle: 0.8, alpha: 1 });
  lens(cv, 160, 82, 24, 1);
  A.vignette(cv, 0.55);
  lensSheet.push(cv);
}
A.sheet(lensSheet, out + 'sheet_corrosion_lens.png');

const shunt = [];
function stardustStream(cv, points, seed) {
  A.lightPath(cv, points, { width: 6, color: '#7a2eb0', intensity: 0.55 });
  A.lightPath(cv, points, { width: 1.6, color: '#ffd0ec', intensity: 0.85 });
  let s = seed;
  const rand = () => ((s = (s * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff);
  for (let k = 0; k < 26; k++) {
    const t = rand() * (points.length - 1), i = Math.floor(t), f = t - i;
    const [ax, ay] = points[i], [bx, by] = points[Math.min(points.length - 1, i + 1)];
    A.glowDot(cv, Math.round(ax + (bx - ax) * f + (rand() - 0.5) * 18), Math.round(ay + (by - ay) * f + (rand() - 0.5) * 18), 1.4 + rand() * 1.5,
      A.hex('#ffd8f2'), 0.5 + rand() * 0.4);
  }
}
function curve(x0, y0, x1, y1, bend, steps = 40) {
  const pts = [];
  const mx = (x0 + x1) / 2 - (y1 - y0) * bend, my = (y0 + y1) / 2 + (x1 - x0) * bend;
  for (let i = 0; i <= steps; i++) {
    const t = i / steps;
    pts.push([(1 - t) ** 2 * x0 + 2 * (1 - t) * t * mx + t * t * x1, (1 - t) ** 2 * y0 + 2 * (1 - t) * t * my + t * t * y1]);
  }
  return pts;
}
{
  const cv = base(91, 0.35, 0.6);
  A.sprite(cv, fossa, { cx: 86, cy: 160, scale: 0.75, angle: 0.5, palette: STARGAZER, keep: 0.4, contrast: 1.1, glow: { radius: 10, color: '#7a2eb0', strength: 0.5 } });
  A.lightPath(cv, A.arc(86, 160, 70, -2.4, 0.2), { width: 5, color: '#a43cc8', intensity: 0.45 });
  stardustStream(cv, curve(130, 120, 236, 30, 0.25), 7);
  A.vignette(cv, 0.55);
  shunt.push(cv);
}
{
  const cv = base(92, 0.5, 0.5);
  A.sprite(cv, fossa, { cx: 70, cy: 178, scale: 0.55, angle: 0.6, palette: STARGAZER, keep: 0.4, contrast: 1.1, glow: { radius: 8, color: '#7a2eb0', strength: 0.5 } });
  A.sprite(cv, tessera, { cx: 186, cy: 74, scale: 0.42, angle: -2.4, palette: STARGAZER, keep: 0.4, contrast: 1.1, glow: { radius: 8, color: '#7a2eb0', strength: 0.5 } });
  stardustStream(cv, curve(96, 150, 166, 92, 0.3), 11);
  A.vignette(cv, 0.55);
  shunt.push(cv);
}
{
  const cv = base(93, 0.5, 0.5);
  A.lightPath(cv, A.arc(128, 128, 60, -Math.PI, 0), { width: 6, color: '#a43cc8', intensity: 0.6 });
  A.lightPath(cv, A.arc(128, 128, 60, -Math.PI, 0), { width: 1.8, color: '#ffe8f6', intensity: 0.9 });
  stardustStream(cv, curve(188, 128, 236, 220, -0.3), 13);
  stardustStream(cv, curve(68, 128, 20, 220, 0.3), 17);
  A.glowDot(cv, 128, 128, 16, A.hex('#e0609a'), 0.7);
  A.vignette(cv, 0.6);
  shunt.push(cv);
}
A.sheet(shunt, out + 'sheet_cosmic_shunt.png');

if (process.argv[2] === 'save') {
  const dest = __dirname + '/../../graphics/icons/socketables/';
  const [l, s] = process.argv.slice(3).map(Number);
  A.save(lensSheet[l], dest + 'na_corrosion_lens_filter.png');
  A.save(shunt[s], dest + 'na_cosmic_shunt_array.png');
}
