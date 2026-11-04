const fs = require('fs');
const path = require('path');

const TIERS = [
  [1, 200, 4, 6, 3.5, 0, 0, 2.5, 720], [2, 240, 4, 6, 3.5, 1.5, 0, 3, 880], [3, 280, 5, 7, 3.5, 1.5, 2, 3, 1040],
  [4, 320, 6, 8, 4, 2, 2.5, 3.5, 1200], [5, 370, 6, 9, 5, 2, 3, 4, 1400], [6, 420, 7, 9, 5.5, 2.5, 3, 5, 1600],
  [7, 480, 8, 10, 6.5, 3.5, 3.5, 5.5, 1840], [8, 540, 8, 11, 7, 5, 4, 6, 2080], [9, 600, 9, 11, 8, 7, 4.5, 7, 2360],
  [10, 670, 10, 12, 9, 9, 5, 8, 2640], [11, 740, 10, 13, 10, 11.5, 6, 8.5, 2960], [12, 820, 11, 13, 11, 14, 6.5, 9.5, 3280],
  [13, 900, 12, 14, 12.5, 15, 7, 11, 3640], [14, 1000, 12, 14, 14, 16, 8, 12, 4000], [15, 1100, 13, 14, 16, 17, 9, 13.5, 4400],
].map(([tier, fleetSize, fleetsMin, fleetsMax, cache, domain, station, derelict, bossSize]) =>
  ({ tier, fleetSize, fleetsMin, fleetsMax, budget: { cache, domain, station, derelict }, bossSize }));

const FLEET_POINTS_PER_PERCENT = 0.5;
const FLEET_SIZE_CAP = 1200;
const BOSS_SIZE_CAP = 4800;
const MIN_FLEET_SIZE = 30;
const MAX_FLEETS = 20;
const FLEET_LIMIT = 14;
const MAX_POIS = 24;
const DP_PER_FLEET_POINT_PATROL = 1.2;
const DP_PER_FLEET_POINT_BOSS = 1.3;
const DIMINISHING_KNEE_DP = 2000;
const SECTION_KINDS = 9;
const HULL_ITEM_RATE = 0.066;
const UNIQUE_HULL_ITEM_RATE = 0.006;
const HULL_ITEM_SALVAGE_SHARE = 0.5;
const SITE_CRAFTING_ITEM_SHARE = 0.7;
const FEEDSTOCK_PER_100_DP = 5;
const REWARD_ITEM_CHANCE_PER_TIER = 0.1;
const REWARD_FEEDSTOCK = 25;
const CHART_DROPS = {
  systemSalvage: { ciphers: 0.48, catalysts: 0.06 },
  runReward: { ciphers: 3, catalysts: 0.5 },
};

const CHART_MATERIALS = {
  aug: [2.07, 9.16], fund: [0.075, 1.34], resh: [1.47, 9.15], corr: [0.735, 9.72], meta: [0.138, 2.13], annul: [0.147, 1.03],
  tune: [0.147, 1.03], immut: [0.069, 0.781], repl: [0.014, 0.187], pinh: [0.129, 0.954], sinh: [0.129, 0.954],
};
const SITE_MATERIAL_WEIGHTS = {
  aug: 6, fund: 4, resh: 3, corr: 1.5, meta: 0.4, annul: 0.3, tune: 0.3, immut: 0.2, repl: 0.035, pinh: 0.5, sinh: 0.5,
};
const SITE_MATERIAL_WEIGHT_TOTAL = Object.values(SITE_MATERIAL_WEIGHTS).reduce((sum, weight) => sum + weight, 0);
const CIPHERS = { reforge: { rate: 0.15, diminishing: false, weight: 5 }, erasure: { rate: 0.15, diminishing: false, weight: 3 },
  carto: { rate: 0.045, diminishing: true, weight: 2 } };
const CIPHER_WEIGHT_TOTAL = Object.values(CIPHERS).reduce((sum, cipher) => sum + cipher.weight, 0);

const POIS = [
  ['cache', 'supply_cache_small', 1, 10, 1, 8], ['cache', 'supply_cache', 2, 8, 1, 16],
  ['cache', 'weapons_cache_small', 2, 8, 1, 8], ['cache', 'weapons_cache_small_high', 3, 5, 5, 16],
  ['cache', 'weapons_cache', 3, 6, 2, 16], ['cache', 'weapons_cache_high', 4, 4, 6, 16],
  ['cache', 'equipment_cache_small', 2, 6, 1, 10], ['cache', 'equipment_cache', 4, 5, 3, 16],
  ['domain', 'derelict_probe', 3, 4, 2, 16], ['derelict', 'wreck', 2, 4, 1, 16],
  ['derelict', 'wreck', 2, 8, 1, 16], ['derelict', 'wreck', 3, 5, 3, 16],
  ['derelict', 'wreck', 5, 3, 6, 16], ['station', 'orbital_habitat', 5, 4, 3, 16],
  ['station', 'station_mining', 6, 4, 4, 16], ['domain', 'derelict_survey_ship', 6, 3, 5, 16],
  ['station', 'station_research', 8, 3, 6, 16], ['domain', 'derelict_mothership', 14, 1, 12, 16],
].map(([kind, site, cost, weight, minTier, maxTier]) => ({ kind, site, cost, weight, minTier, maxTier }));
const SITES = {
  derelict_probe: [[0.5], 5, 8], derelict_survey_ship: [[0.8, 0.15], 8, 12], derelict_mothership: [[1, 0.4, 0.15], 20, 30],
  station_research: [[1, 0.6, 0.3, 0.1], 20, 30], station_mining: [[1, 0.6, 0.3, 0.1], 15, 22], orbital_habitat: [[1, 0.6, 0.3, 0.1], 10, 16],
  wreck: [[0.1], 5, 6], supply_cache: [[0.4, 0.1], 6, 9], supply_cache_small: [[0.4, 0.1], 5, 7], weapons_cache: [[0.4, 0.1], 6, 9],
  weapons_cache_high: [[0.4, 0.1], 6, 9], weapons_cache_small: [[0.4, 0.1], 5, 7], weapons_cache_small_high: [[0.4, 0.1], 5, 7],
  equipment_cache: [[0.4, 0.1], 6, 9], equipment_cache_small: [[0.4, 0.1], 5, 7],
};

const TOP_ROLLS = {
  fortified: [20, 70, 200], crowded: [1, 2, 3], warded: [90, 240, 540], prefixQuantity: [5, 15, 25],
  plenty: [20, 30, 40], cacheB: [30, 45, 60], domainB: [30, 45, 60], stationB: [30, 45, 60], derelictB: [30, 45, 60], discovery: [1, 2, 3],
  item: [25, 40, 60], rarity: [25, 40, 60], targeted: [40, 60, 80], craftingItem: [30, 45, 60], feedstock: [35, 50, 70], catalyst: [30, 45, 60],
  hoard: [30, 50, 75], bounty: [20, 35, 50],
};
const MODIFIER_NAMES = {
  fortified: 'Fortified', crowded: 'Crowded', warded: 'Warded', twinned: 'Twinned', filler: '', item: 'Salvage', targeted: 'section suffix',
  hoard: 'the Hoard', bounty: 'Bounty', plenty: 'Plenty', discovery: 'Discovery', cacheB: 'Stockpiles', domainB: 'Relics', stationB: 'Outposts',
  derelictB: 'Wreckage', craftingItem: 'Swarming', catalyst: 'Catalysis',
};

const diminishingFactor = dp => dp <= DIMINISHING_KNEE_DP ? 1 : DIMINISHING_KNEE_DP * (2 * Math.sqrt(dp / DIMINISHING_KNEE_DP) - 1) / dp;

function expectedCascadeCount(chances, mult) {
  let reachChance = 1;
  let expected = 0;
  for (const chance of chances) {
    reachChance *= Math.min(1, chance * mult);
    expected += reachChance;
  }
  return expected;
}

function seededRandom(seed) {
  let state = seed >>> 0;
  return () => {
    state = (state * 1664525 + 1013904223) >>> 0;
    return state / 4294967296;
  };
}

function weightedPick(list, weightOf, random) {
  const totalWeight = list.reduce((sum, entry) => sum + weightOf(entry), 0);
  let remaining = random() * totalWeight;
  for (const entry of list) {
    remaining -= weightOf(entry);
    if (remaining <= 0) {
      return entry;
    }
  }
  return list[list.length - 1];
}

function planPois(tier, budgets, extraPois, random) {
  const eligible = POIS.filter(poi => tier >= poi.minTier && tier <= poi.maxTier);
  const planned = [];
  for (const kind of ['domain', 'station', 'derelict', 'cache']) {
    const members = eligible.filter(poi => poi.kind === kind);
    let remaining = budgets[kind];
    while (planned.length < MAX_POIS && remaining > 0 && members.length) {
      const affordable = members.filter(poi => poi.cost <= remaining);
      if (!affordable.length) {
        const cheapestCost = Math.min(...members.map(poi => poi.cost));
        if (random() < remaining / cheapestCost) {
          planned.push(weightedPick(members.filter(poi => poi.cost === cheapestCost), poi => poi.weight, random));
        }
        break;
      }
      const meanCost = affordable.reduce((sum, poi) => sum + poi.cost, 0) / affordable.length;
      const targetCost = Math.max(0.0001, remaining / Math.max(1, Math.round(remaining / meanCost)));
      const picked = weightedPick(affordable, poi => poi.weight / (1 + Math.abs(poi.cost - targetCost) / targetCost), random);
      planned.push(picked);
      remaining -= picked.cost;
    }
  }
  for (let added = 0; added < extraPois && planned.length < MAX_POIS; added++) {
    planned.push(weightedPick(eligible, poi => poi.weight, random));
  }
  return planned;
}

function expectedFleetPoints(tier) {
  const spread = rolledFleets => {
    const kept = Math.min(FLEET_LIMIT, rolledFleets);
    const cutShare = (rolledFleets - kept) / kept;
    return kept * Math.min(FLEET_SIZE_CAP, tier.fleetSize * (1 + 0.75 * cutShare)) * FLEET_POINTS_PER_PERCENT;
  };
  return (spread(tier.fleetsMin) + spread(tier.fleetsMax)) / 2 + Math.min(BOSS_SIZE_CAP, tier.bossSize) * FLEET_POINTS_PER_PERCENT;
}

function chartMaterialAmount(material, tier) {
  const [lowestTierAmount, highestTierAmount] = CHART_MATERIALS[material];
  return lowestTierAmount + (highestTierAmount - lowestTierAmount) * (tier - 1) / 14;
}

function simulateChart(tier, modifiers, samples) {
  const tierGroup = tier.tier <= 5 ? 0 : tier.tier <= 10 ? 1 : 2;
  const roll = key => TOP_ROLLS[key][tierGroup];
  const suffixRoll = key => modifiers.suffixes.filter(suffix => suffix === key).length * roll(key);
  const hasPrefix = key => modifiers.prefixes.includes(key);
  const quantity = modifiers.prefixes.length * roll('prefixQuantity');
  const fleetSizeBonus = hasPrefix('fortified') ? roll('fortified') : 0;
  const extraFleets = hasPrefix('crowded') ? roll('crowded') : 0;
  const bossSize = Math.min(BOSS_SIZE_CAP, tier.bossSize + (hasPrefix('warded') ? roll('warded') : 0));
  const twinnedBoss = hasPrefix('twinned') && tier.tier >= 6;
  const itemMult = 1 + (suffixRoll('item') + quantity) / 100;
  const targetedShare = suffixRoll('targeted') / 100 / SECTION_KINDS;
  const rarityMult = 1 + suffixRoll('rarity') / 100;
  const craftingItemMult = 1 + (suffixRoll('craftingItem') + quantity) / 100;
  const feedstockMult = 1 + (suffixRoll('feedstock') + quantity) / 100;
  const catalystMult = 1 + (suffixRoll('catalyst') + quantity) / 100;
  const spoilsMult = 1 + suffixRoll('hoard') / 100;
  const rewardMult = 1 + (suffixRoll('bounty') + quantity) / 100;
  const rewardCraftingItemMult = 1 + (suffixRoll('bounty') + suffixRoll('craftingItem') + quantity) / 100;
  const rewardCatalystMult = 1 + (suffixRoll('bounty') + suffixRoll('catalyst') + quantity) / 100;
  const budgets = {};
  for (const kind of ['cache', 'domain', 'station', 'derelict']) {
    budgets[kind] = tier.budget[kind] * (1 + (suffixRoll('plenty') + suffixRoll(kind + 'B')) / 100);
  }
  const extraPois = suffixRoll('discovery');
  const tierFleetPoints = expectedFleetPoints(tier);

  const totals = { hull: 0, unique: 0, cat: 0, feed: 0, pois: 0, fleets: 0 };
  for (const key of Object.keys(CHART_MATERIALS).concat(Object.keys(CIPHERS))) {
    totals[key] = 0;
  }
  const random = seededRandom(tier.tier * 7919 + 13);

  const addBattle = (dp, lootDp, fleetPointShare, weight) => {
    const hullFactor = lootDp / 100 * diminishingFactor(dp);
    totals.hull += weight * HULL_ITEM_RATE * hullFactor * (itemMult + targetedShare);
    totals.unique += weight * UNIQUE_HULL_ITEM_RATE * hullFactor * itemMult * rarityMult;
    for (const material of Object.keys(CHART_MATERIALS)) {
      totals[material] += weight * chartMaterialAmount(material, tier.tier) * fleetPointShare * craftingItemMult;
    }
    for (const [cipher, { rate, diminishing }] of Object.entries(CIPHERS)) {
      totals[cipher] += weight * rate * lootDp / 100 * (diminishing ? diminishingFactor(dp) : 1) * craftingItemMult;
    }
    totals.feed += weight * FEEDSTOCK_PER_100_DP * lootDp / 100 * feedstockMult;
    totals.cat += weight * catalystMult * fleetPointShare;
  };

  for (let sample = 0; sample < samples; sample++) {
    const weight = 1 / samples;
    const rolledFleets = tier.fleetsMin + Math.floor(random() * (tier.fleetsMax - tier.fleetsMin + 1));
    const keptFleets = Math.min(FLEET_LIMIT, rolledFleets);
    const fleetCount = Math.min(MAX_FLEETS, keptFleets + extraFleets);
    const fleetSize = (tier.fleetSize + fleetSizeBonus) * (1 + 0.75 * (rolledFleets - keptFleets) / keptFleets);
    const shares = Array.from({ length: fleetCount }, () => 0.75 + random() * 0.5);
    const shareTotal = shares.reduce((sum, share) => sum + share, 0);
    totals.fleets += weight * fleetCount;
    for (const share of shares) {
      const fleetPoints = Math.min(FLEET_SIZE_CAP, Math.max(MIN_FLEET_SIZE, fleetSize * share * fleetCount / shareTotal)) * FLEET_POINTS_PER_PERCENT;
      const dp = fleetPoints * DP_PER_FLEET_POINT_PATROL;
      addBattle(dp, dp, fleetPoints / tierFleetPoints, weight);
    }
    const pois = planPois(tier.tier, budgets, extraPois, random);
    totals.pois += weight * pois.length;
    for (const poi of pois) {
      const [chances, feedstockMin, feedstockMax] = SITES[poi.site];
      totals.hull += weight * HULL_ITEM_SALVAGE_SHARE * (expectedCascadeCount(chances, itemMult) + expectedCascadeCount(chances, 1) * targetedShare);
      const siteCraftingItems = chances[0] * SITE_CRAFTING_ITEM_SHARE * craftingItemMult;
      for (const [material, materialWeight] of Object.entries(SITE_MATERIAL_WEIGHTS)) {
        totals[material] += weight * siteCraftingItems * materialWeight / SITE_MATERIAL_WEIGHT_TOTAL;
      }
      for (const [cipher, { weight: cipherWeight }] of Object.entries(CIPHERS)) {
        totals[cipher] += weight * CHART_DROPS.systemSalvage.ciphers * craftingItemMult * cipherWeight / CIPHER_WEIGHT_TOTAL;
      }
      totals.feed += weight * (feedstockMin + feedstockMax) / 2 * feedstockMult;
      totals.cat += weight * CHART_DROPS.systemSalvage.catalysts * catalystMult;
    }
  }

  const bossFleetPoints = bossSize * FLEET_POINTS_PER_PERCENT;
  const bossDp = bossFleetPoints * DP_PER_FLEET_POINT_BOSS;
  const hullBeforeBoss = totals.hull;
  addBattle(bossDp, bossDp * spoilsMult, bossFleetPoints / tierFleetPoints * spoilsMult, 1);
  totals.hull += Math.exp(-(totals.hull - hullBeforeBoss));
  if (twinnedBoss) {
    addBattle(bossDp, bossDp, bossFleetPoints / tierFleetPoints, 1);
  }

  const rewardChance = (1 + REWARD_ITEM_CHANCE_PER_TIER * (tier.tier - 1)) * rewardMult;
  for (const [material, materialWeight] of Object.entries(SITE_MATERIAL_WEIGHTS)) {
    totals[material] += SITE_CRAFTING_ITEM_SHARE * rewardChance * materialWeight / SITE_MATERIAL_WEIGHT_TOTAL;
  }
  for (const [cipher, { weight: cipherWeight }] of Object.entries(CIPHERS)) {
    totals[cipher] += CHART_DROPS.runReward.ciphers * rewardCraftingItemMult * cipherWeight / CIPHER_WEIGHT_TOTAL;
  }
  totals.feed += REWARD_FEEDSTOCK * rewardChance;
  totals.cat += CHART_DROPS.runReward.catalysts * rewardCatalystMult;
  totals.crafting = Object.keys(CHART_MATERIALS).concat(Object.keys(CIPHERS)).reduce((sum, key) => sum + totals[key], 0);
  return totals;
}

function combinations(list, size, start = 0, chosen = [], found = []) {
  if (chosen.length === size) {
    found.push([...chosen]);
    return found;
  }
  for (let i = start; i < list.length; i++) {
    combinations(list, size, i + 1, [...chosen, list[i]], found);
  }
  return found;
}

const PREFIX_POOL = ['fortified', 'crowded', 'warded', 'twinned', 'filler', 'filler2', 'filler3'];
const SUFFIX_POOLS = {
  hull: ['item', 'targeted', 'targeted2', 'targeted3', 'hoard', 'bounty', 'plenty', 'discovery', 'cacheB', 'stationB', 'domainB', 'derelictB'],
  crafting: ['craftingItem', 'hoard', 'bounty', 'plenty', 'discovery', 'cacheB', 'stationB', 'domainB', 'derelictB'],
  cat: ['catalyst', 'hoard', 'bounty', 'plenty', 'discovery', 'cacheB', 'stationB', 'domainB', 'derelictB'],
};
const modifierKey = poolEntry => poolEntry.replace(/[23]$/, '');

function bestModifiers(tier, metric) {
  let best = null;
  for (const prefixes of combinations(PREFIX_POOL, 3)) {
    if (prefixes.includes('twinned') && tier.tier < 6) {
      continue;
    }
    for (const suffixes of combinations(SUFFIX_POOLS[metric], 3)) {
      if ((suffixes.includes('domainB') && tier.tier < 2) || (suffixes.includes('stationB') && tier.tier < 3)) {
        continue;
      }
      const modifiers = { prefixes: prefixes.map(modifierKey), suffixes: suffixes.map(modifierKey) };
      const totals = simulateChart(tier, modifiers, 20);
      if (!best || totals[metric] > best.totals[metric]) {
        best = { modifiers, totals };
      }
    }
  }
  best.totals = simulateChart(tier, best.modifiers, 4000);
  return best;
}

const modifierList = modifiers => '"' + modifiers.prefixes.concat(modifiers.suffixes).map(key => MODIFIER_NAMES[key]).filter(Boolean).join('; ') + '"';
const NO_MODIFIERS = { prefixes: [], suffixes: [] };
const SIX_RANDOM = { prefixes: ['filler', 'filler', 'filler'], suffixes: [] };

const hullRows = ['tier,fleets,sites,none,six_random,best,best_uniques,best_modifiers'];
const craftingRows = ['tier,none_total,six_random_total,best_total,augmentation,fundamental,reshaping,corrupted,metamorph,annulment,tuning,immutable,'
  + 'replicator,inhibitors,reforging_cipher,annulment_cipher,cartographers_cipher,feedstock,best_modifiers'];
const catalystRows = ['tier,none,six_random,best,best_modifiers'];
for (const tier of TIERS) {
  const none = simulateChart(tier, NO_MODIFIERS, 4000);
  const sixRandom = simulateChart(tier, SIX_RANDOM, 4000);
  const bestHull = bestModifiers(tier, 'hull');
  const bestCrafting = bestModifiers(tier, 'crafting');
  const bestCatalysts = bestModifiers(tier, 'cat');
  hullRows.push([tier.tier, none.fleets.toFixed(1), none.pois.toFixed(1), none.hull.toFixed(1), sixRandom.hull.toFixed(1),
    bestHull.totals.hull.toFixed(1), bestHull.totals.unique.toFixed(2), modifierList(bestHull.modifiers)].join(','));
  craftingRows.push([tier.tier, none.crafting.toFixed(0), sixRandom.crafting.toFixed(0), bestCrafting.totals.crafting.toFixed(0),
    sixRandom.aug.toFixed(1), sixRandom.fund.toFixed(2), sixRandom.resh.toFixed(1), sixRandom.corr.toFixed(1), sixRandom.meta.toFixed(2),
    sixRandom.annul.toFixed(2), sixRandom.tune.toFixed(2), sixRandom.immut.toFixed(2), sixRandom.repl.toFixed(3),
    (sixRandom.pinh + sixRandom.sinh).toFixed(2), sixRandom.reforge.toFixed(1), sixRandom.erasure.toFixed(1), sixRandom.carto.toFixed(1),
    sixRandom.feed.toFixed(0), modifierList(bestCrafting.modifiers)].join(','));
  catalystRows.push([tier.tier, none.cat.toFixed(2), sixRandom.cat.toFixed(2), bestCatalysts.totals.cat.toFixed(2),
    modifierList(bestCatalysts.modifiers)].join(','));
}
const writeTable = (fileName, rows) => fs.writeFileSync(path.join(__dirname, fileName), rows.join('\n') + '\n');
writeTable('chart_hull_items.csv', hullRows);
writeTable('chart_crafting_items.csv', craftingRows);
writeTable('chart_catalysts.csv', catalystRows);
