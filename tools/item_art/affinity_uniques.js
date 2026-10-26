const A = require('./art.js');
const out = __dirname + '/../../graphics/unused/item_art_sheets/';
require('fs').mkdirSync(out, { recursive: true });

const TANIA_MOD = 'E:/Dev/Starsector/mods/Navarchy of Tania - 1.3.0/graphics/';
const NC_MOD = 'C:/Games/Starsector/mods/Nightcross/graphics/';
const TANIA = ['#060a0c', '#16302f', '#3a7470', '#c27a4c', '#f6e4c8'];
const STARGAZER = ['#05020a', '#24093f', '#6a2ea6', '#e0609a', '#fde8f4'];
const stork = A.load(TANIA_MOD + 'ships/white_stork/tania_white_stork_main.png');
const darkSun = A.load(NC_MOD + 'weapons/naai_darksun_turret_base.png');

function base(palette, seed, cx = 0.5, cy = 0.5, stars = 40) {
  const cv = A.canvas();
  A.background(cv, palette, { seed, cx, cy, glow: 0.6, stars, nebula: 0.4 });
  return cv;
}

function blueprintGrid(cv, color, intensity) {
  for (let k = 16; k < 256; k += 32) {
    A.lightPath(cv, [[k, 0], [k, 255]], { width: 0.7, color, intensity });
    A.lightPath(cv, [[0, k], [255, k]], { width: 0.7, color, intensity });
  }
}

const farige = [];
{
  const cv = base(TANIA, 201);
  blueprintGrid(cv, '#3a7470', 0.35);
  A.sprite(cv, stork, { cx: 128, cy: 132, scale: 256 / Math.max(stork.w, stork.h) * 0.85, palette: TANIA, keep: 0.4, contrast: 1.15,
    glow: { radius: 10, color: '#3a7470', strength: 0.5 } });
  A.vignette(cv, 0.55);
  farige.push(cv);
}
{
  const cv = base(TANIA, 202);
  blueprintGrid(cv, '#3a7470', 0.3);
  A.sprite(cv, stork, { cx: 128, cy: 128, scale: 256 / Math.max(stork.w, stork.h) * 0.8, angle: 0.55, palette: TANIA, keep: 0, contrast: 1.4, lift: 0.1,
    alpha: 0.85, glow: { radius: 8, color: '#c27a4c', strength: 0.45 } });
  A.vignette(cv, 0.55);
  farige.push(cv);
}
A.sheet(farige, out + 'sheet_farige_blueprint_vault.png');

const absence = [];
function eclipse(cv, cx, cy, r) {
  A.glowDot(cv, cx, cy, r * 0.9, A.hex('#e0609a'), 0.9);
  A.lightPath(cv, A.arc(cx, cy, r, 0, Math.PI * 2, 128), { width: 5, color: '#a43cc8', intensity: 0.8 });
  A.lightPath(cv, A.arc(cx, cy, r, 0, Math.PI * 2, 128), { width: 1.5, color: '#fff0f8', intensity: 0.95 });
  for (let y = Math.floor(cy - r); y <= cy + r; y++) {
    for (let x = Math.floor(cx - r); x <= cx + r; x++) {
      const d = Math.hypot(x - cx, y - cy);
      if (d < r - 2) {
        const i = (y * cv.w + x) * 3;
        const f = Math.min(1, (r - 2 - d) / 6);
        for (let c = 0; c < 3; c++) cv.rgb[i + c] *= 1 - 0.97 * f;
      }
    }
  }
}
{
  const cv = base(STARGAZER, 211, 0.5, 0.5, 70);
  eclipse(cv, 128, 128, 58);
  A.vignette(cv, 0.6);
  absence.push(cv);
}
{
  const cv = base(STARGAZER, 212, 0.5, 0.42, 70);
  A.sprite(cv, darkSun, { cx: 128, cy: 170, scale: 256 / Math.max(darkSun.w, darkSun.h) * 0.55, palette: STARGAZER, keep: 0.35, contrast: 1.2,
    glow: { radius: 8, color: '#7a2eb0', strength: 0.5 } });
  eclipse(cv, 128, 82, 34);
  A.vignette(cv, 0.55);
  absence.push(cv);
}
A.sheet(absence, out + 'sheet_absence_of_light.png');

if (process.argv[2] === 'save') {
  const dest = __dirname + '/../../graphics/icons/socketables/';
  const [f, a] = process.argv.slice(3).map(Number);
  A.save(farige[f], dest + 'tania_farige_blueprint_vault.png');
  A.save(absence[a], dest + 'na_absence_of_light.png');
}
