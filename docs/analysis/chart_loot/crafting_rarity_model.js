const fs = require('fs');
const path = require('path');

const CONFIG = path.join(__dirname, '..', '..', '..', 'data', 'config', 'exiledSector');
const OUT = __dirname;

const RARITY_FLATTENING = 0.4;
const SWEEP_TIER = 15;
const SWEEP_RARITY_PERCENTS = [0, 50, 100, 200, 300, 400, 600, 800];
const SHOWN_ITEMS = ['augmentation', 'reshaping', 'corrupted', 'metamorph', 'annulment', 'immutable', 'replicator', 'prefix', 'cartographer'];

const OLD_BAND_SHIFTS = [1, 1.34, 1.54, 1.68, 1.77, 1.85, 1.9, 1.95, 1.98, 2.01, 2.04, 2.06, 2.08, 2.09, 2.11, 2.12];
const OLD_COMMON_BAND = new Set(['augmentation', 'fundamental', 'reshaping', 'beam', 'non_beam_energy', 'ballistic', 'missile', 'reforge', 'erasure']);

function readCsv(file) {
  const lines = fs.readFileSync(path.join(CONFIG, file), 'utf8').split(/\r?\n/).filter(line => line && !line.startsWith('#'));
  const header = lines[0].split(',');
  return lines.slice(1).map(line => Object.fromEntries(line.split(',').slice(0, header.length).map((cell, index) => [header[index], cell])));
}

const shortKey = itemId => itemId.replace('exiledSector_', '').replace(/_(nanoswarm|nanoseed|inhibitor|cipher)$/, '');
const DROPS = readCsv('crafting_drops.csv').map(row => ({ key: shortKey(row.item), weight: +row.weight }));
const TABLE_WEIGHT = DROPS.reduce((sum, drop) => sum + drop.weight, 0);
const SHIPPED_RARITY = Object.fromEntries(readCsv('star_chart_tiers.csv').map(row => [+row.tier, +row.crafting_rarity || 1]));

const normalised = weights => {
  const total = weights.reduce((sum, weight) => sum + weight, 0);
  return Object.fromEntries(DROPS.map((drop, index) => [drop.key, weights[index] / total]));
};
const bandMix = bandShift => normalised(DROPS.map(drop => drop.weight * (OLD_COMMON_BAND.has(drop.key) ? 1 : bandShift)));
const weightAtRarity = (weight, rarity) => rarity === 1 ? weight : TABLE_WEIGHT * Math.pow(weight / TABLE_WEIGHT, Math.pow(rarity, -RARITY_FLATTENING));
const curveMix = rarity => normalised(DROPS.map(drop => weightAtRarity(drop.weight, rarity)));
const meanRarity = mix => DROPS.reduce((sum, drop) => sum + mix[drop.key] * Math.log(TABLE_WEIGHT / drop.weight), 0);

function matchingRarity(bandShift) {
  const target = meanRarity(bandMix(bandShift));
  let low = 1;
  let high = 10;
  for (let step = 0; step < 60; step++) {
    const middle = (low + high) / 2;
    if (meanRarity(curveMix(middle)) < target) {
      low = middle;
    } else {
      high = middle;
    }
  }
  return (low + high) / 2;
}

const pct = share => (100 * share).toFixed(2);
const writeCsv = (file, rows) => {
  const header = Object.keys(rows[0]);
  fs.writeFileSync(path.join(OUT, file), [header.join(','), ...rows.map(row => header.map(column => row[column]).join(','))].join('\n') + '\n');
};

const tierRows = OLD_BAND_SHIFTS.map((bandShift, index) => {
  const tier = index + 1;
  const matched = matchingRarity(bandShift);
  const band = bandMix(bandShift);
  const curve = curveMix(SHIPPED_RARITY[tier] || 1);
  const row = { tier, band_shift: bandShift, matched_crafting_rarity: matched.toFixed(2), shipped_crafting_rarity: SHIPPED_RARITY[tier] || 1 };
  for (const key of SHOWN_ITEMS) {
    row[`band_${key}`] = pct(band[key]);
    row[`curve_${key}`] = pct(curve[key]);
  }
  return row;
});
writeCsv('crafting_rarity_tiers.csv', tierRows);

const sweepRows = SWEEP_RARITY_PERCENTS.map(rarityPercent => {
  const playerRarity = 1 + rarityPercent / 100;
  const band = bandMix(OLD_BAND_SHIFTS[SWEEP_TIER - 1] * playerRarity);
  const curve = curveMix(SHIPPED_RARITY[SWEEP_TIER] * playerRarity);
  const row = { increased_rarity: rarityPercent };
  for (const key of SHOWN_ITEMS) {
    row[`band_${key}`] = pct(band[key]);
    row[`curve_${key}`] = pct(curve[key]);
  }
  return row;
});
writeCsv('crafting_rarity_sweep.csv', sweepRows);

console.log(tierRows.map(row => `tier ${row.tier}: band shift ${row.band_shift} matches crafting rarity ${row.matched_crafting_rarity}`).join('\n'));
