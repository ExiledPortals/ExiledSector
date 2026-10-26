const A = require('./art.js');
const out = __dirname + '/../../graphics/unused/item_art_sheets/';
require('fs').mkdirSync(out, { recursive: true });

const NC_MOD = 'C:/Games/Starsector/mods/Nightcross/graphics/';
const STARGAZER = ['#05020a', '#24093f', '#6a2ea6', '#e0609a', '#fde8f4'];
const crest = A.load(NC_MOD + 'factions/crest_stargazer.png');
const EVERY_AFFINITY = ['#080604', '#2c2210', '#786030', '#e6c66e', '#fff6da'];

function base(palette, seed, cx = 0.5, cy = 0.5, stars = 50) {
  const cv = A.canvas();
  A.background(cv, palette, { seed, cx, cy, glow: 0.6, stars, nebula: 0.45 });
  return cv;
}

function lit(img) {
  const copy = { ...img, data: Float32Array.from(img.data) };
  for (let i = 0; i < copy.data.length; i += 4) {
    copy.data[i + 3] *= 0.299 * copy.data[i] + 0.587 * copy.data[i + 1] + 0.114 * copy.data[i + 2];
  }
  return copy;
}

function hollowNode(cv, x, y, r, color, intensity) {
  A.lightPath(cv, A.arc(x, y, r, 0, Math.PI * 2, 40), { width: 1.4, color, intensity });
}

const axion = [];
{
  const cv = base(STARGAZER, 301);
  A.sprite(cv, crest, { cx: 128, cy: 128, scale: 0.62, palette: STARGAZER, keep: 0, contrast: 1.6, lift: 0.22, circle: 118, alpha: 0.85 });
  A.lightPath(cv, A.arc(128, 128, 104, 0, Math.PI * 2, 128), { width: 2, color: '#a43cc8', intensity: 0.6 });
  for (let k = 0; k < 8; k++) {
    const a = k * Math.PI / 4;
    hollowNode(cv, 128 + Math.cos(a) * 82, 128 + Math.sin(a) * 82, 10, '#fde8f4', 0.8);
  }
  A.vignette(cv, 0.6);
  axion.push(cv);
}
{
  const cv = base(STARGAZER, 302);
  for (let k = 0; k < 12; k++) {
    const a = k * Math.PI / 6, r = k % 2 === 0 ? 92 : 58;
    hollowNode(cv, 128 + Math.cos(a) * r, 128 + Math.sin(a) * r, 8, '#fde8f4', 0.75);
  }
  A.sprite(cv, crest, { cx: 128, cy: 128, scale: 0.36, palette: STARGAZER, keep: 0, contrast: 1.6, lift: 0.22, circle: 118, glow: { radius: 10, color: '#a43cc8', strength: 0.5 } });
  A.vignette(cv, 0.6);
  axion.push(cv);
}
A.sheet(axion, out + 'sheet_empty_shell_axion.png');

const conduit = [];
function conduitPath(cv, points, colors) {
  for (let k = 1; k < points.length; k++) {
    const color = colors[k % colors.length];
    A.lightPath(cv, [points[k - 1], points[k]], { width: 5, color, intensity: 0.45 });
    A.lightPath(cv, [points[k - 1], points[k]], { width: 1.6, color: '#fff6da', intensity: 0.8 });
  }
  points.forEach(([x, y], k) => {
    const r = k === 0 ? 16 : k === points.length - 1 ? 13 : 6;
    A.lightPath(cv, A.arc(x, y, r, 0, Math.PI * 2, 48), { width: 1.6, color: '#fff6da', intensity: 0.9 });
    A.glowDot(cv, Math.round(x), Math.round(y), r * 0.6, A.hex(colors[k % colors.length]), 0.6);
  });
}
{
  const cv = base(EVERY_AFFINITY, 311, 0.5, 0.5, 30);
  conduitPath(cv, [[60, 200], [92, 170], [120, 150], [138, 118], [166, 96], [196, 60]], ['#e29a48', '#8ce4ff', '#9ccf96']);
  A.vignette(cv, 0.6);
  conduit.push(cv);
}
{
  const cv = base(EVERY_AFFINITY, 312, 0.5, 0.5, 30);
  conduitPath(cv, [[128, 220], [128, 180], [100, 146], [128, 112], [156, 78], [128, 42]], ['#e6c66e']);
  A.vignette(cv, 0.6);
  conduit.push(cv);
}
A.sheet(conduit, out + 'sheet_root_conduit.png');

if (process.argv[2] === 'save') {
  const dest = __dirname + '/../../graphics/icons/socketables/';
  const [e, c] = process.argv.slice(3).map(Number);
  A.save(axion[e], dest + 'na_empty_shell_axion.png');
  A.save(conduit[c ?? 0], dest + 'exiledSector_root_conduit.png');
}
