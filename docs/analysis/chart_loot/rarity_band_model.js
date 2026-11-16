const fs = require('fs');
const path = require('path');

const CONFIG = path.join(__dirname, '..', '..', '..', 'data', 'config', 'exiledSector');
const OUT = __dirname;

const FLEET_POINTS_PER_PERCENT = 0.5;
const DP_PER_FLEET_POINT_PATROL = 1.2;
const DP_PER_FLEET_POINT_BOSS = 1.3;
const FLEET_SIZE_CAP = 450;
const BOSS_SIZE_CAP = 1800;
const FLEET_LIMIT = 14;
const REGULAR_FIGHT_DP = 300;
const TREASURE = 0.3;
const TIER_15_TARGET = 50;

const BANDS = {
  exiledSector_augmentation_nanoswarm: 'common', exiledSector_fundamental_nanoswarm: 'common', exiledSector_reshaping_nanoswarm: 'common',
  exiledSector_corrupted_nanoswarm: 'rare', exiledSector_metamorph_nanoswarm: 'rare',
  exiledSector_annulment_nanoswarm: 'unique', exiledSector_tuning_nanoswarm: 'unique', exiledSector_immutable_nanoswarm: 'unique',
  exiledSector_replicator_nanoseed: 'mythic',
  exiledSector_prefix_inhibitor: 'unique', exiledSector_suffix_inhibitor: 'unique',
  exiledSector_beam_inhibitor: 'common', exiledSector_non_beam_energy_inhibitor: 'common', exiledSector_ballistic_inhibitor: 'common',
  exiledSector_missile_inhibitor: 'common',
  exiledSector_shield_inhibitor: 'rare', exiledSector_armour_inhibitor: 'rare', exiledSector_phase_inhibitor: 'rare',
  exiledSector_reforge_cipher: 'common', exiledSector_erasure_cipher: 'common', exiledSector_cartographer_cipher: 'rare',
};
const CHART_ONLY = new Set(['exiledSector_reforge_cipher', 'exiledSector_erasure_cipher', 'exiledSector_cartographer_cipher',
  'exiledSector_beam_inhibitor', 'exiledSector_non_beam_energy_inhibitor', 'exiledSector_ballistic_inhibitor',
  'exiledSector_missile_inhibitor', 'exiledSector_shield_inhibitor', 'exiledSector_armour_inhibitor', 'exiledSector_phase_inhibitor']);
const SHORT = id => id.replace('exiledSector_', '').replace(/_(nanoswarm|nanoseed|inhibitor|cipher)$/, '');

function readCsv(file) {
  const lines = fs.readFileSync(path.join(CONFIG, file), 'utf8').split(/\r?\n/).filter(line => line && !line.startsWith('#'));
  const header = lines[0].split(',');
  return lines.slice(1).map(line => Object.fromEntries(line.split(',').slice(0, header.length).map((cell, index) => [header[index], cell])));
}

const tiers = readCsv('star_chart_tiers.csv').map(row => ({
  tier: +row.tier, fleetSize: +row.fleet_size, fleetsMin: +row.fleets_min, fleetsMax: +row.fleets_max, bossSize: +row.boss_size,
})).filter(row => row.tier >= 1 && row.tier <= 15);

const CHART_MATERIALS_BEFORE_BANDS = {
  exiledSector_augmentation_nanoswarm: [2.07, 9.16], exiledSector_fundamental_nanoswarm: [0.075, 1.34],
  exiledSector_reshaping_nanoswarm: [1.47, 9.15], exiledSector_corrupted_nanoswarm: [0.735, 9.72],
  exiledSector_metamorph_nanoswarm: [0.138, 2.13], exiledSector_annulment_nanoswarm: [0.147, 1.03],
  exiledSector_tuning_nanoswarm: [0.147, 1.03], exiledSector_immutable_nanoswarm: [0.069, 0.781],
  exiledSector_replicator_nanoseed: [0.014, 0.187], exiledSector_prefix_inhibitor: [0.069, 0.781],
  exiledSector_suffix_inhibitor: [0.069, 0.781], exiledSector_beam_inhibitor: [0.01, 0.12],
  exiledSector_non_beam_energy_inhibitor: [0.01, 0.12], exiledSector_ballistic_inhibitor: [0.01, 0.12],
  exiledSector_missile_inhibitor: [0.01, 0.12], exiledSector_shield_inhibitor: [0.01, 0.12],
  exiledSector_armour_inhibitor: [0.01, 0.12], exiledSector_phase_inhibitor: [0.01, 0.12],
  exiledSector_reforge_cipher: [1.47, 9.15], exiledSector_erasure_cipher: [1, 6], exiledSector_cartographer_cipher: [0.075, 1.34],
};
const BATTLE_LOOT_BEFORE_BANDS = {
  exiledSector_augmentation_nanoswarm: [1.2, 0.125], exiledSector_fundamental_nanoswarm: [0.8, 0.125],
  exiledSector_reshaping_nanoswarm: [0.4, 0.125], exiledSector_corrupted_nanoswarm: [0.12, 0.125],
  exiledSector_metamorph_nanoswarm: [0.0332, 0.125], exiledSector_annulment_nanoswarm: [0.0252, 0.125],
  exiledSector_tuning_nanoswarm: [0.0252, 0.125], exiledSector_immutable_nanoswarm: [0.0168, 0.125],
  exiledSector_replicator_nanoseed: [0.0028, 0.125], exiledSector_prefix_inhibitor: [0.0168, 0.125],
  exiledSector_suffix_inhibitor: [0.0168, 0.125],
};

const materials = Object.entries(CHART_MATERIALS_BEFORE_BANDS)
  .map(([id, [tier1, tier15]]) => ({ id, band: BANDS[id], tier1, tier15 }));
const battleLoot = Object.fromEntries(Object.entries(BATTLE_LOOT_BEFORE_BANDS)
  .map(([id, [per100, regularShare]]) => [id, { per_100_dp: per100, regular_share: regularShare }]));

function chartDp(tier) {
  const spread = rolledFleets => {
    const kept = Math.min(FLEET_LIMIT, rolledFleets);
    const cutShare = (rolledFleets - kept) / kept;
    return kept * Math.min(FLEET_SIZE_CAP, tier.fleetSize * (1 + 0.75 * cutShare)) * FLEET_POINTS_PER_PERCENT * DP_PER_FLEET_POINT_PATROL;
  };
  const fleets = (spread(tier.fleetsMin) + spread(tier.fleetsMax)) / 2;
  return fleets + Math.min(BOSS_SIZE_CAP, tier.bossSize) * FLEET_POINTS_PER_PERCENT * DP_PER_FLEET_POINT_BOSS;
}

const todayAmount = (material, tier) => material.tier1 + (material.tier15 - material.tier1) * (tier - 1) / 14;
const byTier = Object.fromEntries(tiers.map(tier => [tier.tier, tier]));
const dp1 = chartDp(byTier[1]);
const dp15 = chartDp(byTier[15]);
const ratePer100 = (material, tier) => todayAmount(material, tier) / (chartDp(byTier[tier]) / 100);
const upperShare = tier => {
  const total = materials.reduce((sum, material) => sum + ratePer100(material, tier), 0);
  return materials.filter(material => material.band !== 'common').reduce((sum, material) => sum + ratePer100(material, tier), 0) / total;
};
const odds = share => share / (1 - share);
const bandShift = tier => odds(upperShare(tier)) / odds(upperShare(1));
const weights = Object.fromEntries(materials.map(material => [material.id,
  ratePer100(material, 15) / (material.band === 'common' ? 1 : bandShift(15))]));

function mix(shift, pool) {
  const scaled = pool.map(material => [material.id, weights[material.id] * (material.band === 'common' ? 1 : shift)]);
  const total = scaled.reduce((sum, [, weight]) => sum + weight, 0);
  return Object.fromEntries(scaled.map(([id, weight]) => [id, weight / total]));
}
const bandOf = (shares, band) => materials.filter(material => material.band === band && shares[material.id] !== undefined)
  .reduce((sum, material) => sum + shares[material.id], 0);

const RATE = TIER_15_TARGET / (chartDp(byTier[15]) / 100);
const fmt = (value, digits = 2) => value.toFixed(digits);
const writeTable = (file, rows) => {
  const header = Object.keys(rows[0]);
  fs.writeFileSync(path.join(OUT, file), [header.join(','), ...rows.map(row => header.map(key => row[key]).join(','))].join('\n') + '\n');
};
const AUGMENTATION = 'exiledSector_augmentation_nanoswarm';
const IMMUTABLE = 'exiledSector_immutable_nanoswarm';
const CORRUPTED = 'exiledSector_corrupted_nanoswarm';
const todayBand = (tier, band) => materials.filter(material => material.band === band).reduce((sum, material) => sum + todayAmount(material, tier), 0);

const tierRows = tiers.map(tier => {
  const dp = chartDp(tier);
  const count = RATE * dp / 100;
  const today = materials.reduce((sum, material) => sum + todayAmount(material, tier.tier), 0);
  const plain = mix(bandShift(tier.tier), materials);
  const treasure = mix(bandShift(tier.tier) * (1 + TREASURE), materials);
  const amount = (shares, id) => count * shares[id];
  return {
    tier: tier.tier, chart_dp: Math.round(dp), band_shift: fmt(bandShift(tier.tier)),
    today_total: fmt(today, 1), plan_total: fmt(count, 1), change: fmt((count / today - 1) * 100, 0) + '%',
    today_augmentation: fmt(todayAmount(materials.find(material => material.id === AUGMENTATION), tier.tier)), plan_augmentation: fmt(amount(plain, AUGMENTATION)),
    today_corrupted: fmt(todayAmount(materials.find(material => material.id === CORRUPTED), tier.tier)), plan_corrupted: fmt(amount(plain, CORRUPTED)),
    today_unique_band: fmt(todayBand(tier.tier, 'unique')), plan_unique_band: fmt(count * bandOf(plain, 'unique')),
    today_immutable: fmt(todayAmount(materials.find(material => material.id === IMMUTABLE), tier.tier)), plan_immutable: fmt(amount(plain, IMMUTABLE)),
    treasure_augmentation: fmt(amount(treasure, AUGMENTATION)), treasure_immutable: fmt(amount(treasure, IMMUTABLE)),
    plan_common_share: fmt(bandOf(plain, 'common') * 100, 1) + '%', treasure_common_share: fmt(bandOf(treasure, 'common') * 100, 1) + '%',
  };
});
writeTable('rarity_band_tiers.csv', tierRows);

const itemRows = materials.map(material => {
  const row = { item: SHORT(material.id), band: material.band, weight: fmt(weights[material.id], 4) };
  for (const tier of [1, 8, 15]) {
    row[`t${tier}_today`] = fmt(todayAmount(material, tier), 3);
    row[`t${tier}_plan`] = fmt(RATE * chartDp(byTier[tier]) / 100 * mix(bandShift(tier), materials)[material.id], 3);
  }
  return row;
});
writeTable('rarity_band_items.csv', itemRows);

const outsidePool = materials.filter(material => !CHART_ONLY.has(material.id));
const tierOneMix = mix(1, outsidePool);
const outsideRows = outsidePool.map(material => {
  const row = battleLoot[material.id];
  const tierOne = RATE * REGULAR_FIGHT_DP / 100 * tierOneMix[material.id];
  return {
    item: SHORT(material.id), band: material.band,
    today: fmt(row ? +row.per_100_dp * +row.regular_share * REGULAR_FIGHT_DP / 100 : 0, 3),
    plan_level_1: fmt(tierOne * 0.5, 3), plan_level_15: fmt(tierOne, 3),
  };
});
const sumColumn = key => outsideRows.reduce((sum, row) => sum + +row[key], 0);
outsideRows.push({ item: 'total', band: '', today: fmt(sumColumn('today'), 3), plan_level_1: fmt(sumColumn('plan_level_1'), 3), plan_level_15: fmt(sumColumn('plan_level_15'), 3) });
writeTable('rarity_band_outside.csv', outsideRows);

console.log(`chart DP: tier 1 ${Math.round(chartDp(byTier[1]))}, tier 15 ${Math.round(chartDp(byTier[15]))}`);
console.log(`crafting items per 100 DP: ${fmt(RATE, 3)} (a tier 15 chart drops ${TIER_15_TARGET})`);
console.log(`augmentation outdrops immutable until a band shift of ${fmt(weights[AUGMENTATION] / weights[IMMUTABLE], 1)}x (tier 15 is ${fmt(bandShift(15))}x)`);
console.table(tierRows);
console.table(itemRows);
console.log(`outside charts, ${REGULAR_FIGHT_DP} DP fight: tier 1 chart values at player level 15, half at level 1`);
console.table(outsideRows);
