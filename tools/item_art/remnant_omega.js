const A = require('./art.js');
const out = __dirname + '/../../graphics/unused/item_art_sheets/';
require('fs').mkdirSync(out, { recursive: true });

const CORE = 'E:/Dev/Starsector/starsector-core/graphics/';
const REMNANT = ['#02060c', '#0a2236', '#1f6f9a', '#7fe0ff', '#eafcff'];
const OMEGA = ['#040208', '#1a0a2e', '#5a2a8c', '#c78cff', '#f6ecff'];
const PATH = ['#0a0503', '#2e1208', '#8a3a12', '#e8902e', '#fff0d0'];

const img = (path) => A.load(CORE + path);
const radiant = img('ships/remnants/remnant_battleship.png');
const shieldModule = img('ships/remnants/remnant_station_shield1.png');
const glimmer = img('ships/remnants/remnant_frigate1.png');
const lumen = img('ships/remnants/remnant_frigate2.png');
const fulgent = img('ships/remnants/remnant_destroyer1.png');
const platform = img('ships/remnants/remnant_weapon_platform1.png');
const alphaCore = img('icons/cargo/ai_core_alpha.png');
const betaCore = img('icons/cargo/ai_core_beta.png');
const gammaCore = img('icons/cargo/ai_core_gamma.png');
const displacerIcon = img('icons/hullsys/displacer.png');
const pathCrest = img('factions/crest_luddic_path.png');
const talon = img('ships/talon/talon.png');
const piranha = img('ships/piranha_bmb.png');
const lasherPather = img('ships/lasher/lasher_pather.png');
const tesseract = img('ships/omega/omega_cruiser.png');
const facet = img('ships/omega/omega_destroyer1.png');
const shardLeft = img('ships/omega/omega_frigate_left.png');
const shardRight = img('ships/omega/omega_frigate_right.png');
const temporalIcon = img('icons/hullsys/temporal_shell.png');
const phaseIcon = img('icons/hullsys/phase_cloak.png');
const disruptor = img('weapons/reality_disruptor_turret_base.png');
const disruptorGlow = img('weapons/reality_disruptor_turret_glow.png');
const disruptorHardpoint = img('weapons/reality_disruptor_hardpoint_base.png');

function base(palette, seed, cx = 0.5, cy = 0.5, stars = 50) {
  const cv = A.canvas();
  A.background(cv, palette, { seed, cx, cy, glow: 0.6, stars, nebula: 0.45 });
  return cv;
}

function lit(source) {
  const copy = { ...source, data: Float32Array.from(source.data) };
  for (let i = 0; i < copy.data.length; i += 4) {
    copy.data[i + 3] *= 0.299 * copy.data[i] + 0.587 * copy.data[i + 1] + 0.114 * copy.data[i + 2];
  }
  return copy;
}

function ring(cv, cx, cy, r, color, core, intensity = 1, a0 = 0, a1 = Math.PI * 2) {
  A.lightPath(cv, A.arc(cx, cy, r, a0, a1, 96), { width: 5, color, intensity: 0.45 * intensity });
  A.lightPath(cv, A.arc(cx, cy, r, a0, a1, 96), { width: 1.5, color: core, intensity: 0.85 * intensity });
}

function streak(cv, x0, y0, x1, y1, color, core) {
  A.lightPath(cv, [[x0, y0], [x1, y1]], { width: 6, color, intensity: 0.5 });
  A.lightPath(cv, [[x0, y0], [x1, y1]], { width: 1.6, color: core, intensity: 0.85 });
}

function remnantSprite(cv, source, opts) {
  return A.sprite(cv, source, { palette: REMNANT, keep: 0.3, contrast: 1.15, glow: { radius: 10, color: '#1f8fc0', strength: 0.5 }, ...opts });
}

function omegaSprite(cv, source, opts) {
  return A.sprite(cv, source, { palette: OMEGA, keep: 0.25, contrast: 1.2, glow: { radius: 10, color: '#8a3ad0', strength: 0.55 }, ...opts });
}

function pathSprite(cv, source, opts) {
  return A.sprite(cv, source, { palette: PATH, keep: 0.3, contrast: 1.15, glow: { radius: 8, color: '#c0501a', strength: 0.5 }, ...opts });
}

function finish(cv, list, strength = 0.55) {
  A.vignette(cv, strength);
  list.push(cv);
}

const radiantSheet = [];
{
  const cv = base(REMNANT, 501, 0.5, 0.6);
  remnantSprite(cv, radiant, { cx: 128, cy: 150, scale: 0.55 });
  A.hexLattice(cv, { size: 12, color: '#7fe0ff', intensity: 0.4, mask: (x, y) => { const d = Math.hypot(x - 128, y - 150); return d > 92 && d < 112 && y < 150 ? 1 : 0; } });
  ring(cv, 128, 150, 104, '#1f8fc0', '#eafcff', 1, Math.PI, Math.PI * 2);
  finish(cv, radiantSheet);
}
{
  const cv = base(REMNANT, 502);
  A.hexLattice(cv, { size: 14, color: '#7fe0ff', intensity: 0.55, mask: (x, y) => { const d = Math.hypot(x - 128, y - 128); return d < 96 ? Math.min(1, (96 - d) / 30) : 0; } });
  ring(cv, 128, 128, 96, '#1f8fc0', '#eafcff', 1.1);
  remnantSprite(cv, shieldModule, { cx: 128, cy: 128, scale: 0.9 });
  finish(cv, radiantSheet, 0.6);
}
{
  const cv = base(REMNANT, 503, 0.4, 0.4);
  remnantSprite(cv, radiant, { cx: 118, cy: 138, scale: 0.68, angle: -0.5 });
  ring(cv, 118, 138, 116, '#1f8fc0', '#eafcff', 0.9, -2.6, -0.4);
  streak(cv, 250, 10, 190, 60, '#1f8fc0', '#eafcff');
  finish(cv, radiantSheet);
}
A.sheet(radiantSheet, out + 'sheet_radiant_shield_lattice.png');

const exchangeSheet = [];
{
  const cv = base(REMNANT, 511);
  remnantSprite(cv, alphaCore, { cx: 128, cy: 128, scale: 1.7, keep: 0.45 });
  ring(cv, 128, 128, 92, '#1f8fc0', '#eafcff');
  for (const a of [-Math.PI / 2, Math.PI / 6, Math.PI * 5 / 6]) A.glowDot(cv, Math.round(128 + Math.cos(a) * 92), Math.round(128 + Math.sin(a) * 92), 7, A.hex('#eafcff'), 0.9);
  finish(cv, exchangeSheet);
}
{
  const cv = base(REMNANT, 512);
  remnantSprite(cv, gammaCore, { cx: 66, cy: 70, scale: 0.8, keep: 0.4 });
  remnantSprite(cv, betaCore, { cx: 66, cy: 190, scale: 0.8, keep: 0.4 });
  remnantSprite(cv, alphaCore, { cx: 184, cy: 128, scale: 1.3, keep: 0.45 });
  streak(cv, 96, 84, 150, 116, '#1f8fc0', '#eafcff');
  streak(cv, 96, 176, 150, 140, '#1f8fc0', '#eafcff');
  finish(cv, exchangeSheet);
}
{
  const cv = base(REMNANT, 513, 0.5, 0.5);
  remnantSprite(cv, betaCore, { cx: 128, cy: 128, scale: 1.9, keep: 0.4 });
  ring(cv, 128, 128, 70, '#1f8fc0', '#eafcff', 0.8, 0, Math.PI * 1.3);
  ring(cv, 128, 128, 104, '#1f8fc0', '#eafcff', 0.6, Math.PI, Math.PI * 2.4);
  finish(cv, exchangeSheet);
}
A.sheet(exchangeSheet, out + 'sheet_equivalent_exchange_core.png');

const ordoSheet = [];
{
  const cv = base(REMNANT, 521);
  remnantSprite(cv, platform, { cx: 128, cy: 128, scale: 0.92 });
  finish(cv, ordoSheet);
}
{
  const cv = base(REMNANT, 522, 0.5, 0.3);
  remnantSprite(cv, fulgent, { cx: 128, cy: 150, scale: 1.25 });
  streak(cv, 108, 70, 108, -10, '#1f8fc0', '#eafcff');
  streak(cv, 148, 70, 148, -10, '#1f8fc0', '#eafcff');
  finish(cv, ordoSheet);
}
{
  const cv = base(REMNANT, 523);
  remnantSprite(cv, platform, { cx: 128, cy: 128, scale: 1.25, crop: { x: 60, y: 60, w: 120, h: 120 }, circle: 70 });
  ring(cv, 128, 128, 98, '#1f8fc0', '#eafcff', 0.9);
  finish(cv, ordoSheet, 0.6);
}
A.sheet(ordoSheet, out + 'sheet_ordo_feed_conversion.png');

const displacerSheet = [];
{
  const cv = base(REMNANT, 531, 0.65, 0.4);
  remnantSprite(cv, lumen, { cx: 80, cy: 170, scale: 1.4, angle: 0.8, alpha: 0.35, glow: null });
  streak(cv, 96, 154, 160, 96, '#1f8fc0', '#eafcff');
  remnantSprite(cv, lumen, { cx: 176, cy: 82, scale: 1.4, angle: 0.8 });
  finish(cv, displacerSheet);
}
{
  const cv = base(REMNANT, 532);
  remnantSprite(cv, displacerIcon, { cx: 128, cy: 128, scale: 2.6, keep: 0.2, contrast: 1.5 });
  ring(cv, 128, 128, 100, '#1f8fc0', '#eafcff', 0.8);
  finish(cv, displacerSheet, 0.6);
}
{
  const cv = base(REMNANT, 533, 0.5, 0.5);
  for (let k = 0; k < 3; k++) remnantSprite(cv, lumen, { cx: 70 + k * 58, cy: 180 - k * 52, scale: 1.2, angle: 0.85, alpha: 0.25 + k * 0.35, glow: k === 2 ? undefined : null });
  finish(cv, displacerSheet);
}
A.sheet(displacerSheet, out + 'sheet_displacer_failsafe.png');

const zealotSheet = [];
{
  const cv = base(PATH, 541);
  A.sprite(cv, pathCrest, { cx: 128, cy: 128, scale: 0.7, palette: PATH, keep: 0, contrast: 1.5, lift: 0.15, circle: 118, alpha: 0.7 });
  for (const [x, y, a] of [[60, 70, 0.7], [196, 70, -0.7], [128, 210, Math.PI]]) pathSprite(cv, talon, { cx: x, cy: y, scale: 2.2, angle: a });
  finish(cv, zealotSheet);
}
{
  const cv = base(PATH, 542, 0.5, 0.75);
  for (const [x, y] of [[128, 64], [82, 120], [174, 120], [128, 176]]) pathSprite(cv, piranha, { cx: x, cy: y, scale: 1.9 });
  streak(cv, 128, 250, 128, 210, '#c0501a', '#fff0d0');
  finish(cv, zealotSheet);
}
{
  const cv = base(PATH, 543, 0.5, 0.6);
  pathSprite(cv, lasherPather, { cx: 128, cy: 160, scale: 1.3 });
  for (const [x, y] of [[70, 60], [128, 40], [186, 60]]) pathSprite(cv, talon, { cx: x, cy: y, scale: 1.8 });
  finish(cv, zealotSheet);
}
A.sheet(zealotSheet, out + 'sheet_pather_wing_zealots.png');

const microburnSheet = [];
{
  const cv = base(OMEGA, 551, 0.5, 0.6);
  streak(cv, 90, 250, 90, 160, '#8a3ad0', '#f6ecff');
  streak(cv, 166, 250, 166, 160, '#8a3ad0', '#f6ecff');
  omegaSprite(cv, shardLeft, { cx: 90, cy: 110, scale: 1.4 });
  omegaSprite(cv, shardRight, { cx: 166, cy: 110, scale: 1.4 });
  finish(cv, microburnSheet);
}
{
  const cv = base(OMEGA, 552, 0.3, 0.7);
  for (let k = 0; k < 4; k++) streak(cv, 20 + k * 12, 250 - k * 6, 110 + k * 8, 130 - k * 4, '#8a3ad0', '#f6ecff');
  omegaSprite(cv, shardLeft, { cx: 160, cy: 96, scale: 1.8, angle: 0.75 });
  finish(cv, microburnSheet);
}
{
  const cv = base(OMEGA, 553);
  const shards = [[80, 90, 0.3], [170, 76, -0.4], [128, 160, 0.1], [60, 190, 0.6], [196, 180, -0.2]];
  for (const [x, y, a] of shards) omegaSprite(cv, a > 0 ? shardLeft : shardRight, { cx: x, cy: y, scale: 0.75, angle: a });
  finish(cv, microburnSheet);
}
A.sheet(microburnSheet, out + 'sheet_shard_microburn_cluster.png');

const stillwellSheet = [];
{
  const cv = base(OMEGA, 561);
  for (const r of [60, 84, 108]) ring(cv, 128, 128, r, '#8a3ad0', '#f6ecff', 0.6);
  omegaSprite(cv, tesseract, { cx: 128, cy: 128, scale: 0.85 });
  finish(cv, stillwellSheet);
}
{
  const cv = base(OMEGA, 562);
  omegaSprite(cv, temporalIcon, { cx: 128, cy: 128, scale: 2.7, keep: 0.15, contrast: 1.5 });
  ring(cv, 128, 128, 104, '#8a3ad0', '#f6ecff', 0.8);
  finish(cv, stillwellSheet, 0.6);
}
{
  const cv = base(OMEGA, 563, 0.5, 0.5);
  omegaSprite(cv, tesseract, { cx: 128, cy: 128, scale: 1.15, crop: { x: 39, y: 54, w: 120, h: 120 }, circle: 64 });
  for (let k = 0; k < 12; k++) {
    const a = k * Math.PI / 6;
    A.lightPath(cv, [[128 + Math.cos(a) * 92, 128 + Math.sin(a) * 92], [128 + Math.cos(a) * 108, 128 + Math.sin(a) * 108]], { width: 2, color: '#f6ecff', intensity: 0.8 });
  }
  ring(cv, 128, 128, 100, '#8a3ad0', '#f6ecff', 0.7);
  finish(cv, stillwellSheet);
}
A.sheet(stillwellSheet, out + 'sheet_tesseract_stillwell.png');

const echoSheet = [];
{
  const cv = base(OMEGA, 571, 0.5, 0.35);
  streak(cv, 128, 90, 128, -10, '#8a3ad0', '#f6ecff');
  A.sprite(cv, disruptor, { cx: 142, cy: 162, scale: 2.2, palette: OMEGA, keep: 0.25, contrast: 1.2, alpha: 0.35 });
  omegaSprite(cv, disruptor, { cx: 120, cy: 150, scale: 2.2 });
  A.sprite(cv, lit(disruptorGlow), { cx: 120, cy: 150, scale: 2.2, alpha: 1 });
  finish(cv, echoSheet);
}
{
  const cv = base(OMEGA, 572, 0.5, 0.3);
  omegaSprite(cv, disruptorHardpoint, { cx: 96, cy: 160, scale: 2.4 });
  omegaSprite(cv, disruptorHardpoint, { cx: 160, cy: 160, scale: 2.4, alpha: 0.4, glow: null });
  streak(cv, 96, 80, 96, -10, '#8a3ad0', '#f6ecff');
  streak(cv, 160, 80, 160, -10, '#5a2a8c', '#c78cff');
  finish(cv, echoSheet);
}
{
  const cv = base(OMEGA, 573);
  omegaSprite(cv, disruptor, { cx: 128, cy: 128, scale: 2.4 });
  A.sprite(cv, lit(disruptorGlow), { cx: 128, cy: 128, scale: 2.4, alpha: 1 });
  ring(cv, 128, 128, 96, '#8a3ad0', '#f6ecff', 0.6, -1.2, 1.2);
  ring(cv, 128, 128, 96, '#8a3ad0', '#f6ecff', 0.6, Math.PI - 1.2, Math.PI + 1.2);
  finish(cv, echoSheet);
}
A.sheet(echoSheet, out + 'sheet_reality_disruptor_echo.png');

const facetSheet = [];
function polygon(cv, cx, cy, r, sides, rotation, intensity) {
  const pts = [];
  for (let k = 0; k <= sides; k++) pts.push([cx + Math.cos(rotation + k * 2 * Math.PI / sides) * r, cy + Math.sin(rotation + k * 2 * Math.PI / sides) * r]);
  A.lightPath(cv, pts, { width: 4, color: '#8a3ad0', intensity: 0.45 * intensity });
  A.lightPath(cv, pts, { width: 1.3, color: '#f6ecff', intensity: 0.85 * intensity });
}
{
  const cv = base(OMEGA, 581);
  polygon(cv, 128, 128, 108, 6, 0, 0.8);
  omegaSprite(cv, facet, { cx: 128, cy: 128, scale: 0.95 });
  finish(cv, facetSheet);
}
{
  const cv = base(OMEGA, 582);
  for (const [x, y] of [[80, 80], [176, 80], [80, 176], [176, 176]]) { polygon(cv, x, y, 30, 6, Math.PI / 6, 0.9); A.glowDot(cv, x, y, 6, A.hex('#c78cff'), 0.8); }
  polygon(cv, 128, 128, 40, 3, -Math.PI / 2, 1);
  finish(cv, facetSheet, 0.6);
}
{
  const cv = base(OMEGA, 583, 0.5, 0.45);
  omegaSprite(cv, facet, { cx: 128, cy: 138, scale: 1.15, angle: 0.3 });
  polygon(cv, 128, 128, 112, 3, -Math.PI / 2, 0.7);
  polygon(cv, 128, 128, 112, 3, Math.PI / 2, 0.5);
  finish(cv, facetSheet);
}
A.sheet(facetSheet, out + 'sheet_facet_mount_geometry.png');

const latticeSheet = [];
{
  const cv = base(OMEGA, 591);
  A.hexLattice(cv, { size: 13, color: '#c78cff', intensity: 0.45, mask: (x, y) => { const d = Math.hypot(x - 128, y - 128); return d < 104 ? Math.min(1, (104 - d) / 40) : 0; } });
  omegaSprite(cv, phaseIcon, { cx: 128, cy: 128, scale: 2.2, keep: 0.15, contrast: 1.5 });
  finish(cv, latticeSheet, 0.6);
}
{
  const cv = base(OMEGA, 592, 0.5, 0.5);
  omegaSprite(cv, shardRight, { cx: 128, cy: 128, scale: 1.7, alpha: 0.55 });
  A.hexLattice(cv, { size: 16, color: '#c78cff', intensity: 0.5, mask: (x, y) => Math.max(0, 1 - Math.hypot(x - 128, y - 128) / 120) });
  finish(cv, latticeSheet);
}
{
  const cv = base(OMEGA, 593);
  for (const r of [40, 70, 100]) ring(cv, 128, 128, r, '#5a2a8c', '#c78cff', 0.6);
  for (let k = 0; k < 6; k++) {
    const a = k * Math.PI / 3;
    A.glowDot(cv, Math.round(128 + Math.cos(a) * 70), Math.round(128 + Math.sin(a) * 70), 5, A.hex('#f6ecff'), 0.9);
  }
  omegaSprite(cv, phaseIcon, { cx: 128, cy: 128, scale: 1.1, keep: 0.15, contrast: 1.5 });
  finish(cv, latticeSheet, 0.6);
}
A.sheet(latticeSheet, out + 'sheet_p_space_lattice.png');

const sheets = [
  ['exiledSector_radiant_shield_lattice', radiantSheet],
  ['exiledSector_equivalent_exchange_core', exchangeSheet],
  ['exiledSector_ordo_feed_conversion', ordoSheet],
  ['exiledSector_displacer_failsafe', displacerSheet],
  ['exiledSector_pather_wing_zealots', zealotSheet],
  ['exiledSector_shard_microburn_cluster', microburnSheet],
  ['exiledSector_tesseract_stillwell', stillwellSheet],
  ['exiledSector_reality_disruptor_echo', echoSheet],
  ['exiledSector_facet_mount_geometry', facetSheet],
  ['exiledSector_p_space_lattice', latticeSheet],
];

if (process.argv[2] === 'save') {
  const dest = __dirname + '/../../graphics/icons/socketables/';
  const picks = process.argv.slice(3).map(Number);
  sheets.forEach(([name, candidates], index) => A.save(candidates[picks[index] ?? 0], dest + name + '.png'));
}
