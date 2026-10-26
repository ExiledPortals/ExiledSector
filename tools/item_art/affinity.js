const A = require('./art.js');
const out = __dirname + '/../../graphics/unused/item_art_sheets/';
require('fs').mkdirSync(out, { recursive: true });

const VANILLA = 'E:/Dev/Starsector/starsector-core/graphics/';
const LOW_TECH = ['#0a0604', '#3a200e', '#8a4a1c', '#e29a48', '#fbe6c6'];
const MIDLINE = ['#05080a', '#14302a', '#3a7656', '#9ccf96', '#eef8e6'];
const HIGH_TECH = ['#05040c', '#1c1442', '#5848c8', '#8ce4ff', '#f4fbff'];
const EVERY_AFFINITY = ['#080604', '#2c2210', '#786030', '#e6c66e', '#fff6da'];

const hellbore = A.load(VANILLA + 'weapons/hellbore_cannon_turret_base.png');
const tachyon = A.load(VANILLA + 'weapons/tachyon_lance_hardpoint_base.png');
const tachyonGlow = A.load(VANILLA + 'weapons/tachyon_lance_hardpoint_glow.png');
const harpoon = A.load(VANILLA + 'missiles/missile_harpoon.png');
const srmPod = A.load(VANILLA + 'weapons/breach_srm_pod_turret.png');
const onslaught = A.load(VANILLA + 'ships/onslaught/onslaught1.png');
const onslaughtArmor = A.load(VANILLA + 'ships/onslaught/onslaught1_armor_left.png');
const conquest = A.load(VANILLA + 'ships/conquest/conquest.png');
const talon = A.load(VANILLA + 'ships/talon/talon.png');
const broadsword = A.load(VANILLA + 'ships/broadsword.png');
const heron = A.load(VANILLA + 'ships/heron/heron.png');

function base(palette, seed, cx = 0.5, cy = 0.5) {
  const cv = A.canvas();
  A.background(cv, palette, { seed, cx, cy, glow: 0.6, stars: 30, nebula: 0.3 });
  return cv;
}

function lit(img) {
  const copy = { ...img, data: Float32Array.from(img.data) };
  for (let i = 0; i < copy.data.length; i += 4) {
    copy.data[i + 3] *= 0.299 * copy.data[i] + 0.587 * copy.data[i + 1] + 0.114 * copy.data[i + 2];
  }
  return copy;
}

function reticle(cv, cx, cy, r, color) {
  A.lightPath(cv, A.arc(cx, cy, r, 0, Math.PI * 2, 96), { width: 1.4, color, intensity: 0.75 });
  for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
    A.lightPath(cv, [[cx + dx * r * 0.55, cy + dy * r * 0.55], [cx + dx * r * 1.25, cy + dy * r * 1.25]], { width: 1.4, color, intensity: 0.75 });
  }
}

function finish(cv, list) {
  A.vignette(cv, 0.55);
  list.push(cv);
}

const items = {};
const glowOf = (palette) => palette[2];

items.old_guard_gun_captains = [];
{
  const cv = base(LOW_TECH, 101);
  A.sprite(cv, hellbore, { cx: 128, cy: 134, scale: 2.7, palette: LOW_TECH, keep: 0.3, contrast: 1.2, glow: { radius: 10, color: glowOf(LOW_TECH), strength: 0.5 } });
  finish(cv, items.old_guard_gun_captains);
}
{
  const cv = base(LOW_TECH, 102, 0.6, 0.35);
  A.sprite(cv, hellbore, { cx: 112, cy: 150, scale: 2.4, angle: 0.6, palette: LOW_TECH, keep: 0.3, contrast: 1.2, glow: { radius: 10, color: glowOf(LOW_TECH), strength: 0.5 } });
  A.glowDot(cv, 176, 70, 16, A.hex('#ffb060'), 0.8);
  A.glowDot(cv, 176, 70, 6, A.hex('#fff2d8'), 1);
  finish(cv, items.old_guard_gun_captains);
}

items.tachyon_lens_technicians = [];
{
  const cv = base(HIGH_TECH, 111, 0.5, 0.3);
  A.lightPath(cv, [[128, 60], [128, -10]], { width: 8, color: '#5848c8', intensity: 0.6 });
  A.lightPath(cv, [[128, 60], [128, -10]], { width: 2.4, color: '#f4fbff', intensity: 0.9 });
  A.sprite(cv, tachyon, { cx: 128, cy: 148, scale: 2.4, palette: HIGH_TECH, keep: 0.3, contrast: 1.2, glow: { radius: 10, color: glowOf(HIGH_TECH), strength: 0.5 } });
  A.sprite(cv, lit(tachyonGlow), { cx: 128, cy: 148, scale: 2.4 });
  finish(cv, items.tachyon_lens_technicians);
}
{
  const cv = base(HIGH_TECH, 112);
  A.sprite(cv, tachyon, { cx: 128, cy: 128, scale: 2.2, angle: 0.75, palette: HIGH_TECH, keep: 0.3, contrast: 1.2, glow: { radius: 10, color: glowOf(HIGH_TECH), strength: 0.5 } });
  reticle(cv, 128, 128, 70, '#8ce4ff');
  finish(cv, items.tachyon_lens_technicians);
}

items.line_yard_ordnance_crew = [];
{
  const cv = base(MIDLINE, 121);
  [[92, 136, -0.08], [128, 124, 0], [164, 136, 0.08]].forEach(([x, y, a]) =>
    A.sprite(cv, harpoon, { cx: x, cy: y, scale: 5.6, angle: a, palette: MIDLINE, keep: 0.3, contrast: 1.25, glow: { radius: 8, color: glowOf(MIDLINE), strength: 0.5 } }));
  finish(cv, items.line_yard_ordnance_crew);
}
{
  const cv = base(MIDLINE, 122);
  A.sprite(cv, srmPod, { cx: 128, cy: 132, scale: 5.2, palette: MIDLINE, keep: 0.3, contrast: 1.2, glow: { radius: 10, color: glowOf(MIDLINE), strength: 0.5 } });
  finish(cv, items.line_yard_ordnance_crew);
}

items.bulwark_riveters = [];
{
  const cv = base(LOW_TECH, 131);
  A.sprite(cv, onslaughtArmor, { crop: { x: 0, y: 60, w: 74, h: 130 }, feather: 10, cx: 128, cy: 128, scale: 1.75, angle: 0.35, palette: LOW_TECH, keep: 0.35,
    contrast: 1.2, glow: { radius: 10, color: glowOf(LOW_TECH), strength: 0.45 } });
  finish(cv, items.bulwark_riveters);
}
{
  const cv = base(LOW_TECH, 132);
  A.sprite(cv, onslaught, { crop: { x: 40, y: 80, w: 208, h: 170 }, feather: 14, cx: 128, cy: 128, scale: 1.3, palette: LOW_TECH, keep: 0.35, contrast: 1.15,
    glow: { radius: 10, color: glowOf(LOW_TECH), strength: 0.45 } });
  finish(cv, items.bulwark_riveters);
}

items.heritage_fire_control = [];
{
  const cv = base(EVERY_AFFINITY, 141, 0.5, 0.45);
  A.sprite(cv, conquest, { crop: { x: 0, y: 0, w: 190, h: 170 }, feather: 14, cx: 128, cy: 150, scale: 1.3, palette: EVERY_AFFINITY, keep: 0.35, contrast: 1.15,
    glow: { radius: 10, color: glowOf(EVERY_AFFINITY), strength: 0.45 } });
  reticle(cv, 128, 96, 40, '#ffe8a8');
  finish(cv, items.heritage_fire_control);
}
{
  const cv = base(EVERY_AFFINITY, 142);
  A.sprite(cv, conquest, { cx: 128, cy: 128, scale: 0.6, angle: 0.6, palette: EVERY_AFFINITY, keep: 0.35, contrast: 1.15,
    glow: { radius: 10, color: glowOf(EVERY_AFFINITY), strength: 0.45 } });
  reticle(cv, 188, 66, 26, '#ffe8a8');
  finish(cv, items.heritage_fire_control);
}

items.mixed_doctrine_flight_instructors = [];
{
  const cv = base(EVERY_AFFINITY, 151, 0.5, 0.6);
  A.sprite(cv, heron, { crop: { x: 0, y: 70, w: 140, h: 180 }, feather: 12, cx: 128, cy: 196, scale: 1.0, palette: EVERY_AFFINITY, keep: 0.35, contrast: 1.15,
    glow: { radius: 8, color: glowOf(EVERY_AFFINITY), strength: 0.4 } });
  [[86, 76], [128, 56], [170, 76]].forEach(([x, y]) =>
    A.sprite(cv, talon, { cx: x, cy: y, scale: 2.4, palette: EVERY_AFFINITY, keep: 0.35, contrast: 1.2, glow: { radius: 6, color: '#e6c66e', strength: 0.6 } }));
  finish(cv, items.mixed_doctrine_flight_instructors);
}
{
  const cv = base(EVERY_AFFINITY, 152);
  [[80, 150, -0.5], [128, 110, 0], [176, 150, 0.5]].forEach(([x, y, a]) =>
    A.sprite(cv, talon, { cx: x, cy: y, scale: 3.6, angle: a, palette: EVERY_AFFINITY, keep: 0.35, contrast: 1.2, glow: { radius: 8, color: '#e6c66e', strength: 0.6 } }));
  finish(cv, items.mixed_doctrine_flight_instructors);
}

items.deck_armorers_guild = [];
{
  const cv = base(EVERY_AFFINITY, 161);
  A.sprite(cv, broadsword, { cx: 128, cy: 132, scale: 5.4, palette: EVERY_AFFINITY, keep: 0.35, contrast: 1.2, glow: { radius: 10, color: '#e6c66e', strength: 0.55 } });
  A.lightPath(cv, A.arc(128, 132, 96, -2.5, -0.64), { width: 3, color: '#e6c66e', intensity: 0.5 });
  finish(cv, items.deck_armorers_guild);
}
{
  const cv = base(EVERY_AFFINITY, 162, 0.5, 0.65);
  A.sprite(cv, heron, { crop: { x: 0, y: 70, w: 140, h: 180 }, feather: 12, cx: 128, cy: 200, scale: 1.0, palette: EVERY_AFFINITY, keep: 0.35, contrast: 1.15,
    glow: { radius: 8, color: glowOf(EVERY_AFFINITY), strength: 0.4 } });
  A.sprite(cv, broadsword, { cx: 128, cy: 80, scale: 3.6, palette: EVERY_AFFINITY, keep: 0.35, contrast: 1.2, glow: { radius: 8, color: '#e6c66e', strength: 0.6 } });
  finish(cv, items.deck_armorers_guild);
}

for (const [name, candidates] of Object.entries(items)) {
  A.sheet(candidates, out + 'sheet_' + name + '.png');
}

if (process.argv[2] === 'save') {
  const dest = __dirname + '/../../graphics/icons/socketables/';
  const picks = process.argv.slice(3).map(Number);
  Object.keys(items).forEach((name, index) => A.save(items[name][picks[index] ?? 0], dest + 'exiledSector_' + name + '.png'));
}
